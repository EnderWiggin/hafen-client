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

import haven.QualityList.SingleType;
import me.ender.Reflect;

import java.awt.*;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.awt.image.BufferedImage;
import haven.ItemInfo.AttrCache;
import rx.functions.Action1;
import rx.functions.Action3;

import static haven.Inventory.sqsz;
import static haven.Text.*;

public class WItem extends Widget implements DTarget2 {
    public static final Resource missing = Resource.local().loadwait("gfx/invobjs/missing");
    public static final Coord TEXT_PADD_TOP = new Coord(0, -3), TEXT_PADD_BOT = new Coord(0, 2);
    public static final Color DURABILITY_COLOR = new Color(214, 253, 255);
    public static final Color ARMOR_COLOR = new Color(255, 227, 191);
    public static final Color MATCH_COLOR = new Color(255, 32, 255, 255);
    public final GItem item;
    private Resource cspr = null;
    private Message csdt = Message.nil;
    private final List<Action3<WItem, Coord, Integer>> rClickListeners = new LinkedList<>();

    public WItem(GItem item) {
	super(sqsz);
	this.item = item;
	this.item.onBound(widget -> this.bound());
	CFG.REAL_TIME_CURIO.observe(cfg -> longtip = null);
	CFG.SHOW_CURIO_LPH.observe(cfg -> longtip = null);
    }

    public void drawmain(GOut g, GSprite spr) {
	spr.draw(g);
    }

    public static BufferedImage shorttip(List<ItemInfo> info) {
	return(ItemInfo.shorttip(info));
    }

    public static BufferedImage longtip(GItem item, List<ItemInfo> info) {
	BufferedImage img = ItemInfo.longtip(info);
	Resource.Pagina pg = item.res.get().layer(Resource.pagina);
	if(pg != null)
	    img = ItemInfo.catimgs(0, img, RichText.render("\n" + pg.text, UI.scale(200)).img);
	return(img);
    }

    public BufferedImage longtip(List<ItemInfo> info) {
	return(longtip(item, info));
    }

    public class ItemTip implements Indir<Tex> {
	private final TexI tex;

	public ItemTip(BufferedImage img) {
	    if(img == null)
		throw(new Loading());
	    tex = new TexI(img);
	}

	public GItem item() {
	    return(item);
	}

	public Tex get() {
	    return(tex);
	}
    }

    public class ShortTip extends ItemTip {
	public ShortTip(List<ItemInfo> info) {super(shorttip(info));}
    }

    public class LongTip extends ItemTip {
	public LongTip(List<ItemInfo> info) {super(longtip(info));}
    }

    private double hoverstart;
    private ItemTip shorttip = null, longtip = null;
    private List<ItemInfo> ttinfo = null;
    public Object tooltip(Coord c, Widget prev) {
	double now = Utils.rtime();
	if(prev == this) {
	} else if(prev instanceof WItem) {
	    double ps = ((WItem)prev).hoverstart;
	    if(now - ps < 1.0)
		hoverstart = now;
	    else
		hoverstart = ps;
	} else {
	    hoverstart = now;
	}
	try {
	    List<ItemInfo> info = item.info();
	    if(info.size() < 1)
		return(null);
	    if(info != ttinfo) {
		shorttip = longtip = null;
		ttinfo = info;
	    }
	    if(now - hoverstart < 1.0) {
		if(shorttip == null)
		    shorttip = new ShortTip(info);
		return(shorttip);
	    } else {
		if(longtip == null)
		    longtip = new LongTip(info);
		return(longtip);
	    }
	} catch(Loading e) {
	    return("...");
	}
    }

    private List<ItemInfo> info() {return(item.info());}
    public final AttrCache<Color> olcol = new AttrCache<>(this::info, AttrCache.cache(info -> {
	Color ret = null;
	for(ItemInfo inf : info) {
	    if(inf instanceof GItem.ColorInfo) {
		Color c = ((GItem.ColorInfo) inf).olcol();
		if(c != null)
		    ret = (ret == null) ? c : Utils.preblend(ret, c);
	    }
	}
	return ret;
    }));
    public final AttrCache<GItem.InfoOverlay<?>[]> itemols = new AttrCache<>(this::info, info -> {
	    ArrayList<GItem.InfoOverlay<?>> buf = new ArrayList<>();
	    for(ItemInfo inf : info) {
		if(inf instanceof GItem.OverlayInfo)
		    buf.add(GItem.InfoOverlay.create((GItem.OverlayInfo<?>)inf));
	    }
	    GItem.InfoOverlay<?>[] ret = buf.toArray(new GItem.InfoOverlay<?>[0]);
	    return(() -> ret);
	});
    public final AttrCache<Double> itemmeter = new AttrCache<>(this::info, AttrCache.map1(GItem.MeterInfo.class, minf -> minf::meter));
    
    public final AttrCache<QualityList> itemq = new AttrCache<QualityList>(this::info, AttrCache.cache(info -> {
	List<ItemInfo.Contents> contents = ItemInfo.findall(ItemInfo.Contents.class, info);
	List<ItemInfo> qualities = null;
	if(!contents.isEmpty()) {
	    for(ItemInfo.Contents content : contents) {
		List<ItemInfo> tmp = ItemInfo.findall(QualityList.classname, content.sub);
		if(!tmp.isEmpty()) {
		    qualities = tmp;
		}
	    }
	}
	if(qualities == null || qualities.isEmpty()) {
	    qualities = ItemInfo.findall(QualityList.classname, info);
	}
	
	QualityList qualityList = new QualityList(qualities);
	return !qualityList.isEmpty() ? qualityList : null;
    }));
    
    public final AttrCache<ItemInfo.Contents.Content> contains = new AttrCache<>(this::info, AttrCache.cache(ItemInfo::getContent), ItemInfo.Contents.Content.EMPTY); 

    public final AttrCache<Tex> heurnum = new AttrCache<Tex>(this::info, AttrCache.cache(info -> {
	String num = ItemInfo.getCount(info);
	if(num == null) return null;
	return Text.renderstroked(num, Color.WHITE, Color.BLACK).tex();
    }));
    
    public final AttrCache<Tex> durability = new AttrCache<Tex>(this::info, AttrCache.cache(info -> {
	Pair<Integer, Integer> wear = ItemInfo.getWear(info);
	if(wear == null) return (null);
	return Text.renderstroked(String.valueOf(wear.b - wear.a), DURABILITY_COLOR, Color.BLACK).tex();
    })) {
	@Override
	public Tex get() {
	    return CFG.SHOW_ITEM_DURABILITY.get() ? super.get() : null;
	}
    };
    
    public final AttrCache<Pair<Integer, Integer>> wear = new AttrCache<Pair<Integer, Integer>>(this::info, AttrCache.cache(ItemInfo::getWear));
    
    public final AttrCache<Tex> armor = new AttrCache<Tex>(this::info, AttrCache.cache(info -> {
	Pair<Integer, Integer> armor = ItemInfo.getArmor(info);
	if(armor == null) return (null);
	return Text.renderstroked(String.format("%d/%d", armor.a, armor.b), ARMOR_COLOR, Color.BLACK).tex();
    })) {
	@Override
	public Tex get() {
	    return CFG.SHOW_ITEM_ARMOR.get() ? super.get() : null;
	}
    };
    
    public final AttrCache<List<ItemInfo>> gilding = new AttrCache<List<ItemInfo>>(this::info, AttrCache.cache(info -> ItemInfo.findall("Slotted", info)));
    
    public final AttrCache<List<ItemInfo>> slots = new AttrCache<List<ItemInfo>>(this::info, AttrCache.cache(info -> ItemInfo.findall("ISlots", info)));

    public final AttrCache<Boolean> gildable = new AttrCache<Boolean>(this::info, AttrCache.cache(info -> {
	List<ItemInfo> slots = ItemInfo.findall("ISlots", info);
	for(ItemInfo slot : slots) {
	    if(Reflect.getFieldValueInt(slot, "left") > 0) {
		return true;
	    }
	}
	return false;
    }));
    
    public final AttrCache<String> name = new AttrCache<>(this::info, AttrCache.cache(info -> {
	ItemInfo.Name name = ItemInfo.find(ItemInfo.Name.class, info);
	return (name != null && name.str != null && name.str.text != null) ? name.str.text : "";
    }));

    private GSprite lspr = null;
    public void tick(double dt) {
	/* XXX: This is ugly and there should be a better way to
	 * ensure the resizing happens as it should, but I can't think
	 * of one yet. */
	GSprite spr = item.spr();
	if((spr != null) && (spr != lspr)) {
	    Coord sz = new Coord(spr.sz());
	    if((sz.x % sqsz.x) != 0)
		sz.x = sqsz.x * ((sz.x / sqsz.x) + 1);
	    if((sz.y % sqsz.y) != 0)
		sz.y = sqsz.y * ((sz.y / sqsz.y) + 1);
	    resize(sz);
	    lspr = spr;
	}
    }

    public void draw(GOut g) {
	GSprite spr = item.spr();
	if(spr != null) {
	    Coord sz = spr.sz();
	    g.defstate();
	    drawbars(g, sz);
	    if(olcol.get() != null)
		g.usestate(new ColorMask(olcol.get()));
	    if(item.matches) {
		g.chcolor(MATCH_COLOR);
		g.rect(Coord.z, sz);
		g.chcolor();
	    }
	    drawmain(g, spr);
	    g.defstate();
	    GItem.InfoOverlay<?>[] ols = itemols.get();
	    if(ols != null) {
		for(GItem.InfoOverlay<?> ol : ols)
		    ol.draw(g);
	    }
	    drawnum(g, sz);
	    drawmeter(g, sz);
	    drawq(g);
	} else {
	    g.image(missing.layer(Resource.imgc).tex(), Coord.z, sz);
	}
    }

    private void drawmeter(GOut g, Coord sz) {
	Double meter = (item.meter > 0) ? (Double) (item.meter / 100.0) : itemmeter.get();
	if(meter != null && meter > 0) {
	    if(CFG.PROGRESS_NUMBER.get()) {
		Tex tex = Text.renderstroked(String.format("%d%%", Math.round(100*meter))).tex();
		g.aimage(tex, sz.div(2), 0.5, 0.5);
		tex.dispose();
	    } else {
		GItem itm = WItem.this.item;
		if (itm.studytime > 0 && meter > 0) {
		    int timeleft = (int) (itm.studytime * (1.0 - meter));
		    int hoursleft = timeleft / 60;
		    int minutesleft = timeleft - hoursleft * 60;
		    Tex tex = Text.renderstroked(String.format("%d:%02d", hoursleft, minutesleft)).tex();
		    g.aimage(tex, TEXT_PADD_BOT.add(sz), 1, 1);
		    tex.dispose();
		}
		g.chcolor(255, 255, 255, 64);
		Coord half = sz.div(2);
		g.prect(half, half.inv(), half, meter * Math.PI * 2);
		g.chcolor();
	    }
	}
    }

    private void drawbars(GOut g, Coord sz) {
	float bar = 0f;

	if(CFG.SHOW_ITEM_WEAR_BAR.get() && this.wear.get() != null) {
	    Pair<Integer, Integer> wear = this.wear.get();
	    if(wear.a > 0) {
		bar = (float) (wear.b - wear.a) / wear.b;
	    }
	}

	if(bar > 0) {
	    g.chcolor(Utils.blendcol(Color.RED, Color.GREEN, bar));
	    int h = (int) (sz.y * bar);
	    g.frect(new Coord(0, sz.y - h), new Coord(4, h));
	    g.chcolor();
	}

    }

    private void drawnum(GOut g, Coord sz) {
	Tex tex;
	if(item.num >= 0) {
	    tex = Text.render(Integer.toString(item.num)).tex();
	} else {
	    tex = chainattr(/*itemnum, */heurnum, armor, durability);
	}
 
	if(tex != null) {
	    if(CFG.SWAP_NUM_AND_Q.get()) {
		g.aimage(tex, TEXT_PADD_TOP.add(sz.x, 0),1 , 0);
	    } else {
		g.aimage(tex, TEXT_PADD_BOT.add(sz), 1, 1);
	    }
	}
    }

    @SafeVarargs //actually, method just assumes you'll feed it correctly typed var args
    private static Tex chainattr(AttrCache<Tex> ...attrs){
	for(AttrCache<Tex> attr : attrs){
	    Tex tex = attr.get();
	    if(tex != null){
		return tex;
	    }
	}
	return null;
    }

    private void drawq(GOut g) {
	QualityList quality = itemq.get();
	if(quality != null) {
	    Tex tex = null;
	    SingleType qtype = getQualityType();
	    if(qtype != null) {
		QualityList.Quality single = quality.single(qtype);
		if(single != null) {
		    tex = single.tex();
		}
	    }

	    if(tex != null) {
		if(CFG.SWAP_NUM_AND_Q.get()) {
		    g.aimage(tex, TEXT_PADD_BOT.add(sz), 1, 1);
		} else {
		    g.aimage(tex, TEXT_PADD_TOP.add(sz.x, 0), 1, 0);
		}
	    }
	}
    }

    private SingleType getQualityType() {
	return CFG.Q_SHOW_SINGLE.get() ? SingleType.Quality : null;
    }

    public boolean mousedown(Coord c, int btn) {
	if(checkXfer(btn)) {
	    return true;
	} else if(btn == 1) {
	    item.wdgmsg("take", c);
	    return true;
	} else if(btn == 3) {
	    synchronized (rClickListeners) {
		if(rClickListeners.isEmpty()) {
		    item.wdgmsg("iact", c, ui.modflags());
		} else {
		    rClickListeners.forEach(action -> action.call(this, c, ui.modflags()));
		}
	    }
	    return(true);
	}
	return(false);
    }
    
    public void onRClick(Action3<WItem, Coord, Integer> action) {
	synchronized (rClickListeners) {
	    rClickListeners.add(action);
	}
    }
    
    public void rclick() {
	rclick(Coord.z, 0);
    }
    
    
    public void rclick(Coord c, int flags) {
	item.wdgmsg("iact", c, flags);
    }

    private boolean checkXfer(int button) {
	boolean inv = parent instanceof Inventory;
	if(ui.modshift) {
	    if(ui.modmeta) {
		if(inv) {
		    wdgmsg("transfer-same", item, button == 3);
		    return true;
		}
	    } else if(button == 1) {
		item.wdgmsg("transfer", c);
		return true;
	    }
	} else if(ui.modctrl) {
	    if(ui.modmeta) {
		if(inv) {
		    wdgmsg("drop-same", item, button == 3);
		    return true;
		}
	    } else if(button == 1) {
		item.wdgmsg("drop", c);
		return true;
	    }
	}
	return false;
    }
    
    @Override
    public void dispose() {
	synchronized (rClickListeners) {rClickListeners.clear();}
	super.dispose();
    }
    
    public boolean drop(WItem target, Coord cc, Coord ul) {
	return(false);
    }

    public boolean iteminteract(WItem target, Coord cc, Coord ul) {
	if(!GildingWnd.processGilding(ui,this, target)) {
	    item.wdgmsg("itemact", ui.modflags());
	}
	return(true);
    }

}
