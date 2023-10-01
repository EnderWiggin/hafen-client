/*
 *  This file is part of the Haven & Hearth game client.
 *  Copyright (C) 2009 Fredrik Tolf <fredrik@dolda2000.com>, and
 *                     Björn Johannessen <johannessen.bjorn@gmail.com>
 *
 *  Redistribution and/or modification of this file is subject to the
 *  terms of the GNU Lesser General Public License, version 3, as
 *  published by the Free Software Foundation.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  Other parts of this source tree adhere to other copying
 *  rights. Please see the file `COPYING' in the root directory of the
 *  source tree for details.
 *
 *  A copy the GNU Lesser General Public License is distributed along
 *  with the source tree of which this file is a part in the file
 *  `doc/LPGL-3'. If it is missing for any reason, please see the Free
 *  Software Foundation's website at <http://www.fsf.org/>, or write
 *  to the Free Software Foundation, Inc., 59 Temple Place, Suite 330,
 *  Boston, MA 02111-1307 USA
 */

package haven;

import rx.functions.Action0;

import java.util.*;
import java.awt.image.WritableRaster;
import java.util.function.BiConsumer;

public class Inventory extends Widget implements DTarget {
    public static final Coord sqsz = UI.scale(new Coord(33, 33));
    public static final Tex invsq;
    public boolean dropul = true;
    private boolean canDropItems = false;
    private boolean dropEnabled = false;
    public ExtInventory ext;
    Action0 dropsCallback;
    public Coord isz;
    public int cachedSize = -1;
    public boolean[] sqmask = null;
    public static final Comparator<WItem> ITEM_COMPARATOR_ASC = new Comparator<WItem>() {
	@Override
	public int compare(WItem o1, WItem o2) {
	    QualityList ql1 = o1.itemq.get();
	    double q1 = (ql1 != null && !ql1.isEmpty()) ? ql1.single().value : 0;

	    QualityList ql2 = o2.itemq.get();
	    double q2 = (ql2 != null && !ql2.isEmpty()) ? ql2.single().value : 0;

	    return Double.compare(q1, q2);
	}
    };
    public static final Comparator<WItem> ITEM_COMPARATOR_DESC = new Comparator<WItem>() {
	@Override
	public int compare(WItem o1, WItem o2) {
	    return ITEM_COMPARATOR_ASC.compare(o2, o1);
	}
    };

    public boolean locked = false;
    Map<GItem, WItem> wmap = new HashMap<GItem, WItem>();

    static {
	Coord sz = sqsz.add(1, 1);
	WritableRaster buf = PUtils.imgraster(sz);
	for(int i = 1, y = sz.y - 1; i < sz.x - 1; i++) {
	    buf.setSample(i, 0, 0, 20); buf.setSample(i, 0, 1, 28); buf.setSample(i, 0, 2, 21); buf.setSample(i, 0, 3, 167);
	    buf.setSample(i, y, 0, 20); buf.setSample(i, y, 1, 28); buf.setSample(i, y, 2, 21); buf.setSample(i, y, 3, 167);
	}
	for(int i = 1, x = sz.x - 1; i < sz.y - 1; i++) {
	    buf.setSample(0, i, 0, 20); buf.setSample(0, i, 1, 28); buf.setSample(0, i, 2, 21); buf.setSample(0, i, 3, 167);
	    buf.setSample(x, i, 0, 20); buf.setSample(x, i, 1, 28); buf.setSample(x, i, 2, 21); buf.setSample(x, i, 3, 167);
	}
	for(int y = 1; y < sz.y - 1; y++) {
	    for(int x = 1; x < sz.x - 1; x++) {
		buf.setSample(x, y, 0, 36); buf.setSample(x, y, 1, 52); buf.setSample(x, y, 2, 38); buf.setSample(x, y, 3, 125);
	    }
	}
	invsq = new TexI(PUtils.rasterimg(buf));
    }

    @RName("inv")
    public static class $_ implements Factory {
	public Widget create(UI ui, Object[] args) {
	    return(new ExtInventory((Coord)args[0]));
	}
    }

    public void draw(GOut g) {
	Coord c = new Coord();
	int mo = 0;
	for(c.y = 0; c.y < isz.y; c.y++) {
	    for(c.x = 0; c.x < isz.x; c.x++) {
		if((sqmask != null) && sqmask[mo++])
		    continue;
		g.image(invsq, c.mul(sqsz));
	    }
	}
	super.draw(g);
    }
	
    public Inventory(Coord sz) {
	super(sqsz.mul(sz).add(1, 1));
	isz = sz;
    }
    
    public boolean mousewheel(Coord c, int amount) {
	if(locked){return false;}
	if(ui.modshift) {
	    ExtInventory minv = getparent(GameUI.class).maininvext;
	    if(minv != this.parent) {
		if(amount < 0)
		    wdgmsg("invxf", minv.wdgid(), 1);
		else if(amount > 0)
		    minv.wdgmsg("invxf", parent.wdgid(), 1);
	    }
	}
	return(true);
    }

    @Override
    public boolean mousedown(Coord c, int button) {
	return !locked && super.mousedown(c, button);
    }

    public void addchild(Widget child, Object... args) {
	add(child);
	Coord c = (Coord)args[0];
	if(child instanceof GItem) {
	    GItem i = (GItem)child;
	    wmap.put(i, add(new WItem(i), c.mul(sqsz).add(1, 1)));
	    i.sendttupdate = canDropItems;
	    if(dropEnabled) {
		tryDrop(wmap.get(i));
	    }
	    itemsChanged();
	}
    }
    
    @Override
    public void destroy() {
	ItemAutoDrop.removeCallback(dropsCallback);
	super.destroy();
    }
    
    public void cdestroy(Widget w) {
	super.cdestroy(w);
	if(w instanceof GItem) {
	    GItem i = (GItem)w;
	    ui.destroy(wmap.remove(i));
	    itemsChanged();
	}
    }
    
    public boolean drop(Coord cc, Coord ul) {
	if(!locked) {
	    Coord dc;
	    if(dropul)
		dc = ul.add(sqsz.div(2)).div(sqsz);
	    else
		dc = cc.div(sqsz);
	    wdgmsg("drop", dc);
	}
	return(true);
    }
	
    public boolean iteminteract(Coord cc, Coord ul) {
	return(false);
    }
	
    public void uimsg(String msg, Object... args) {
	if(msg.equals("sz")) {
	    isz = (Coord)args[0];
	    resize(invsq.sz().add(UI.scale(new Coord(-1, -1))).mul(isz).add(UI.scale(new Coord(1, 1))));
	    sqmask = null;
	    cachedSize = -1;
	} else if(msg == "mask") {
	    boolean[] nmask;
	    if(args[0] == null) {
		nmask = null;
	    } else {
		nmask = new boolean[isz.x * isz.y];
		byte[] raw = (byte[])args[0];
		for(int i = 0; i < isz.x * isz.y; i++)
		    nmask[i] = (raw[i >> 3] & (1 << (i & 7))) != 0;
	    }
	    this.sqmask = nmask;
	    cachedSize = -1;
	} else if(msg == "mode") {
	    dropul = (((Integer)args[0]) == 0);
	} else {
	    super.uimsg(msg, args);
	}
    }

    @Override
    public void wdgmsg(Widget sender, String msg, Object... args) {
	if(msg.equals("transfer-same")) {
	    process(getSame((GItem) args[0], (Boolean) args[1]), "transfer");
	} else if(msg.equals("drop-same")) {
	    process(getSame((GItem) args[0], (Boolean) args[1]), "drop");
	} else if(msg.equals("ttupdate") && sender instanceof GItem && wmap.containsKey(sender)) {
	    if(dropEnabled) {
		tryDrop(wmap.get(sender));
	    }
	} else {
	    super.wdgmsg(sender, msg, args);
	}
    }

    private void process(List<WItem> items, String action) {
	for (WItem item : items){
	    item.item.wdgmsg(action, Coord.z);
	}
    }

    private List<WItem> getSame(GItem item, Boolean ascending) {
	String name = item.resname();
	GSprite spr = item.spr();
	List<WItem> items = new ArrayList<>();
	for(Widget wdg = lchild; wdg != null; wdg = wdg.prev) {
	    if(wdg.visible && wdg instanceof WItem) {
		WItem wItem = (WItem) wdg;
		GItem child = wItem.item;
		if(item.matches == child.matches && isSame(name, spr, child)) {
		    items.add(wItem);
		}
	    }
	}
	items.sort(ascending ? ITEM_COMPARATOR_ASC : ITEM_COMPARATOR_DESC);
	return items;
    }
    
    private static boolean isSame(String name, GSprite spr, GItem item) {
	try {
	    return item.resname().equals(name) && ((spr == item.spr()) || (spr != null && spr.same(item.spr())));
	} catch (Loading ignored) {}
	return false;
    }
    
    public int size() {
	if(cachedSize >= 0) {return cachedSize;}
	
	if(sqmask == null) {
	    cachedSize = isz.x * isz.y;
	} else {
	    cachedSize = 0;
	    for (boolean b : sqmask) {
		if(!b) {cachedSize++;}
	    }
	}
	
	return cachedSize;
    }
    
    public int filled() {
	int count = 0;
	for (Widget wdg = lchild; wdg != null; wdg = wdg.prev) {
	    if(wdg instanceof WItem) {
		Coord sz = ((WItem) wdg).lsz;
		count += sz.x * sz.y;
	    }
	}
	return count;
    }
    
    public int free() {
	return size() - filled();
    }
    
    public Coord findPlaceFor(Coord size) {
	boolean[] slots = new boolean[isz.x * isz.y];
	for (Widget wdg = lchild; wdg != null; wdg = wdg.prev) {
	    if(wdg instanceof WItem) {
		Coord p = wdg.c.div(sqsz);
		Coord sz = ((WItem) wdg).lsz;
		fill(slots, isz, p, sz);
	    }
	}
	Coord t = new Coord(0, 0);
	for (t.y = 0; t.y <= isz.y - size.y; t.y++) {
	    for (t.x = 0; t.x <= isz.x - size.x; t.x++) {
		if(fits(slots, isz, t, size)) {
		    return t;
		}
	    }
	}
	return null;
    }
    
    private static void fill(boolean[] slots, Coord isz, Coord p, Coord sz) {
	for (int x = 0; x < sz.x; x++) {
	    for (int y = 0; y < sz.y; y++) {
		if(p.x + x < isz.x && p.y + y < isz.y) {
		    slots[p.x + x + isz.x * (p.y + y)] = true;
		}
	    }
	}
    }
    
    private static boolean fits(boolean[] slots, Coord isz, Coord p, Coord sz) {
	for (int x = 0; x < sz.x; x++) {
	    if(p.x + x >= isz.x) {return false;}
	    for (int y = 0; y < sz.y; y++) {
		if(p.y + y >= isz.y) {return false;}
		if(slots[p.x + x + isz.x * (p.y + y)]) return false;
	    }
	}
	return true;
    }
    
    public void enableDrops() {
        Window wnd = getparent(Window.class);
	if(wnd != null) {
	    canDropItems = true;
	    dropsCallback = this::doDrops;
	    ItemAutoDrop.addCallback(dropsCallback);
	    wnd.addtwdg(wnd.add(new ICheckBox("gfx/hud/btn-adrop", "", "-d", "-h")
		.changed(this::doEnableDrops)
		.rclick(this::showDropCFG)
		.settip("Left-click to toggle item dropping\nRight-click to open settings", true)
	    ));
	}
    }
    public void itemsChanged() {
	if(ext != null) {ext.itemsChanged();}
    }
    
    private void showDropCFG() {
	ItemAutoDrop.toggle(ui);
    }
    
    private void doEnableDrops(boolean v) {
	dropEnabled = v;
	doDrops();
    }
    
    public void doDrops() {
	if(dropEnabled) {
	    for (Widget wdg = lchild; wdg != null; wdg = wdg.prev) {
		if(wdg instanceof WItem) {
		    tryDrop(((WItem) wdg));
		}
	    }
	}
    }
    
    private void tryDrop(WItem w) {
	if(w != null) { w.tryDrop(); }
    }
    
    public static Coord invsz(Coord sz) {
	return invsq.sz().add(new Coord(-1, -1)).mul(sz).add(new Coord(1, 1));
    }

    public static Coord sqroff(Coord c){
	return c.div(invsq.sz());
    }

    public static Coord sqoff(Coord c){
	return c.mul(invsq.sz());
    }

    public void forEachItem(BiConsumer<GItem, WItem> consumer) {
	wmap.forEach(consumer);
    }
    
}
