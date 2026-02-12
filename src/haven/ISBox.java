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

import me.ender.ui.ValueEntry;

import java.awt.Color;

public class ISBox extends Widget implements DTarget {
    public static final Color bgcol = new Color(43, 51, 44, 127);
    public static final IBox box = new IBox.Scaled("gfx/hud/bosq", "tl", "tr", "bl", "br", "el", "er", "et", "eb") {
	    public void draw(GOut g, Coord tl, Coord sz) {
		super.draw(g, tl, sz);
		g.chcolor(bgcol);
		g.frect(tl.add(ctloff()), sz.sub(cisz()));
		g.chcolor();
	    }
	};
    public static final Coord defsz = UI.scale(145, 42);
    public static final Text.Foundry lf = new Text.Foundry(Text.fraktur, 22, Color.WHITE).aa(true);
    private int first_Line = UI.scale(20);
    
    private final Indir<Resource> res;
    private Text label;

    private ValueEntry value;
    private Button take;

    private int rem;
    private int av;

    @RName("isbox")
    public static class $_ implements Factory {
	public Widget create(UI ui, Object[] args) {
	    Indir<Resource> res;
	    if(args[0] instanceof String)
		res = Resource.remote().load((String)args[0]);
	    else
		res = ui.sess.getresv(args[0]);
	    return(new ISBox(res, Utils.iv(args[1]), Utils.iv(args[2]), Utils.iv(args[3])));
	}
    }

    private void setlabel(int rem, int av, int bi) {
	if(bi < 0)
	    label = lf.renderf("%d/%d", rem, av);
	else
	    label = lf.renderf("%d/%d/%d", rem, av, bi);
    }
    
    @Override
    public void unlink() {
	ui.remInventory(this);
	super.unlink();
    }
    
    @Override
    protected void added() {
	if(parent instanceof Window) {
	    boolean isStockpile = "Stockpile".equals(((Window) parent).caption());
	    if(isStockpile) {
		value = new ValueEntry(UI.scale(50), "", this::take);
		add(value, UI.scale(45, 41));

		take = new Button(UI.scale(40), "Take", this::take);
		add(take, UI.scale(100, 39));

		sz = sz.add(0, UI.scale(25));
		
		ui.addInventory(this);
	    } else {
		first_Line = sz.y / 2;
	    }
	}
    }

    public ISBox(Indir<Resource> res, int rem, int av, int bi) {
	super(defsz);
	this.rem = rem;
	this.av = av;
	this.res = res;
	setlabel(rem, av, bi);
	tooltip = new PaginaTip(res, true);
    }

    public void draw(GOut g) {
	box.draw(g, Coord.z, sz);
	try {
            Tex t = res.get().flayer(Resource.imgc).tex();
	    Coord dc = Coord.of(UI.scale(6), first_Line - t.sz().y / 2);
            g.image(t, dc);
        } catch(Loading e) {}
	g.image(label.tex(), new Coord(UI.scale(40), first_Line - label.sz().y / 2));
	super.draw(g);
    }

    public boolean mousedown(MouseDownEvent ev) {
	if(ev.propagate(this)) {return true;}
	if (ev.b == 1) {
	    if (ui.modshift ^ ui.modctrl) {           //SHIFT or CTRL means pull
		int dir = ui.modctrl ? -1 : 1;        //CTRL means pull out, SHIFT pull in
		int all = (dir > 0) ? av - rem : rem; //count depends on direction
		int k = ui.modmeta ? all : 1;         //ALT means pull all
		transfer(dir, k);
	    } else {
		wdgmsg("click");
	    }
	    return (true);
	}
	return (super.mousedown(ev));
    }

    public void transfer(int dir, int amount) {
	for (int i = 0; i < amount; i++) {
	    wdgmsg("xfer2", dir, KeyMatch.S);
	}
    }
    
    public boolean mousewheel(MouseWheelEvent ev) {
	if(ev.a < 0)
	    wdgmsg("xfer2", -1, ui.modflags());
	if(ev.a > 0)
	    wdgmsg("xfer2", 1, ui.modflags());
	return(true);
    }

    public boolean drop(Coord cc, Coord ul) {
        wdgmsg("drop");
        return(true);
    }

    public boolean iteminteract(Coord cc, Coord ul) {
        wdgmsg("iact");
        return(true);
    }

    public void uimsg(String msg, Object... args) {
        if(msg == "chnum") {
            setlabel(Utils.iv(args[0]), Utils.iv(args[1]), Utils.iv(args[2]));
        } else {
            super.uimsg(msg, args);
        }
    }
    
    private void take() {
	int amount = value.value();
	if(amount > rem || amount <= 0) {
	    amount = rem;
	}
	if(amount > 0) {
	    transfer(-1, amount);
	}
    }

}
