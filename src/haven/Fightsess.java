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

import haven.rx.Reactor;

import haven.render.*;

import java.awt.*;
import java.util.*;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;

import static haven.KeyBinder.*;

public class Fightsess extends Widget {
    private static final Coord off = new Coord(UI.scale(32), UI.scale(32));
    public static final Text.Foundry fnd = new Text.Foundry(Text.sans.deriveFont(Font.BOLD), 14);
    public static final Tex cdframe = Resource.loadtex("gfx/hud/combat/cool");
    public static final Tex actframe = Buff.frame;
    public static final Coord actframeo = Buff.imgoff;
    public static final Tex indframe = Resource.loadtex("gfx/hud/combat/indframe");
    public static final Coord indframeo = (indframe.sz().sub(off)).div(2);
    public static final Tex indbframe = Resource.loadtex("gfx/hud/combat/indbframe");
    public static final Coord indbframeo = (indframe.sz().sub(off)).div(2);
    public static final Tex useframe = Resource.loadtex("gfx/hud/combat/lastframe");
    public static final Coord useframeo = (useframe.sz().sub(off)).div(2);
    public static final int actpitch = UI.scale(50);
    public static final KeyBinder.KeyBind[] keybinds = new KeyBinder.KeyBind[]{
	new KeyBinder.KeyBind(KeyEvent.VK_1, NONE),
	new KeyBinder.KeyBind(KeyEvent.VK_2, NONE),
	new KeyBinder.KeyBind(KeyEvent.VK_3, NONE),
	new KeyBinder.KeyBind(KeyEvent.VK_4, NONE),
	new KeyBinder.KeyBind(KeyEvent.VK_5, NONE),
	new KeyBinder.KeyBind(KeyEvent.VK_1, SHIFT),
	new KeyBinder.KeyBind(KeyEvent.VK_2, SHIFT),
	new KeyBinder.KeyBind(KeyEvent.VK_3, SHIFT),
	new KeyBinder.KeyBind(KeyEvent.VK_4, SHIFT),
	new KeyBinder.KeyBind(KeyEvent.VK_5, SHIFT),
    };
    public final Action[] actions;
    public int use = -1, useb = -1;
    public Coord pcc;
    public int pho;
    private Fightview fv;

    public static class Action {
	public final Indir<Resource> res;
	public double cs, ct;

	public Action(Indir<Resource> res) {
	    this.res = res;
	}
    }

    @RName("fsess")
    public static class $_ implements Factory {
	public Widget create(UI ui, Object[] args) {
	    int nact = (Integer)args[0];
	    return(new Fightsess(nact));
	}
    }

    @SuppressWarnings("unchecked")
    public Fightsess(int nact) {
	pho = -UI.scale(40);
	this.actions = new Action[nact];
    }

    protected void added() {
	fv = parent.getparent(GameUI.class).fv;
	presize();
	ui.gui.calendar.hide();
    }

    public void presize() {
	resize(parent.sz);
	pcc = sz.div(2);
    }
    
    private void updatepos() {
	MapView map;
	Gob pl;
	if(((map = getparent(GameUI.class).map) == null) || ((pl = map.player()) == null))
	    return;
	Coord3f raw = pl.placed.getc();
	if(raw == null)
	    return;
	pcc = map.screenxf(raw).round2();
	pho = (int)(map.screenxf(raw.add(0, 0, UI.scale(20))).round2().sub(pcc).y) - UI.scale(20);
    }

    private static class Effect implements RenderTree.Node {
	Sprite spr;
	RenderTree.Slot slot;
	boolean used = true;

	Effect(Sprite spr) {this.spr = spr;}

	public void added(RenderTree.Slot slot) {
	    slot.add(spr);
	}
    }

    private static final Resource tgtfx = Resource.local().loadwait("gfx/hud/combat/trgtarw");
    private final Collection<Effect> curfx = new ArrayList<>();

    private Effect fxon(long gobid, Resource fx, Effect cur) {
	MapView map = getparent(GameUI.class).map;
	Gob gob = ui.sess.glob.oc.getgob(gobid);
	if((map == null) || (gob == null))
	    return(null);
	Pipe.Op place;
	try {
	    place = gob.placed.curplace();
	} catch(Loading l) {
	    return(null);
	}
	if((cur == null) || (cur.slot == null)) {
	    try {
		cur = new Effect(Sprite.create(null, fx, Message.nil));
		cur.slot = map.basic.add(cur.spr, place);
	    } catch(Loading l) {
		return(null);
	    }
	    curfx.add(cur);
	} else {
	    cur.slot.cstate(place);
	}
	cur.used = true;
	return(cur);
    }

    public void tick(double dt) {
	for(Iterator<Effect> i = curfx.iterator(); i.hasNext();) {
	    Effect fx = i.next();
	    if(!fx.used) {
		if(fx.slot != null) {
		    fx.slot.remove();
		    fx.slot = null;
		}
		i.remove();
	    } else {
		fx.used = false;
		fx.spr.tick(dt);
	    }
	}
    }

    public void destroy() {
	for(Effect fx : curfx) {
	    if(fx.slot != null)
		fx.slot.remove();
	}
	curfx.clear();
	ui.gui.calendar.show();
	if(CFG.CLEAR_PLAYER_DMG_AFTER_COMBAT.get()) {
	    haven.Action.CLEAR_PLAYER_DAMAGE.run(ui.gui);
	}
	if(CFG.CLEAR_ALL_DMG_AFTER_COMBAT.get()) {
	    haven.Action.CLEAR_ALL_DAMAGE.run(ui.gui);
	}
	super.destroy();
    }

    private static final Text.Furnace ipf = new PUtils.BlurFurn(new Text.Foundry(Text.serif, 18, new Color(128, 128, 255)).aa(true), 1, 1, new Color(48, 48, 96));
    private final Text.UText<?> ip = new Text.UText<Integer>(ipf) {
	public String text(Integer v) {return(CFG.ALT_COMBAT_UI.get()?v.toString():"IP: " + v);}
	public Integer value() {return(fv.current.ip);}
    };
    private final Text.UText<?> oip = new Text.UText<Integer>(ipf) {
	public String text(Integer v) {return(CFG.ALT_COMBAT_UI.get()?v.toString():"IP: " + v);}
	public Integer value() {return(fv.current.oip);}
    };

    private static Coord actc(int i) {
	int rl = 5;
	return(new Coord((actpitch * (i % rl)) - (((rl - 1) * actpitch) / 2), UI.scale(125) + ((i / rl) * actpitch)));
    }

    private static final Coord cmc = UI.scale(new Coord(0, 67));
    private static final Coord usec1 = UI.scale(new Coord(-65, 67));
    private static final Coord usec2 = UI.scale(new Coord(65, 67));
    private Indir<Resource> lastact1 = null, lastact2 = null;
    private Text lastacttip1 = null, lastacttip2 = null;
    private Effect curtgtfx;
    public void draw(GOut g) {
	updatepos();
        boolean altui = CFG.ALT_COMBAT_UI.get();
	int x0 = ui.gui.calendar.rootpos().x + ui.gui.calendar.sz.x / 2;
	int y0 = ui.gui.calendar.rootpos().y + ui.gui.calendar.sz.y / 2;
	int bottom = ui.gui.beltwdg.c.y - 40;
	double now = Utils.rtime();

	for(Buff buff : fv.buffs.children(Buff.class))
	    buff.draw(g.reclip(altui ? new Coord(x0 - buff.c.x - Buff.cframe.sz().x - UI.scale(80), y0) : pcc.add(-buff.c.x - Buff.cframe.sz().x - UI.scale(20), buff.c.y + pho - Buff.cframe.sz().y), buff.sz));
	if(fv.current != null) {
	    for(Buff buff : fv.current.buffs.children(Buff.class))
		buff.draw(g.reclip(altui ? new Coord(x0 + buff.c.x + UI.scale(80), y0) : pcc.add(buff.c.x + UI.scale(20), buff.c.y + pho - Buff.cframe.sz().y), buff.sz));

	    g.aimage(ip.get().tex(), altui ? new Coord(x0 - UI.scale(45), y0 - UI.scale(16)) : pcc.add(-UI.scale(75), 0), 1, 0.5);
	    g.aimage(oip.get().tex(), altui ? new Coord(x0 + UI.scale(45), y0 - UI.scale(16)) : pcc.add(UI.scale(75), 0), 0, 0.5);

	    if(fv.lsrel.size() > (CFG.ALWAYS_MARK_COMBAT_TARGET.get() ? 0 : 1))
		curtgtfx = fxon(fv.current.gobid, tgtfx, curtgtfx);
	}

	{
	    Coord cdc = altui ? new Coord(x0, y0) : pcc.add(cmc);
	    if(now < fv.atkct) {
		double a = (now - fv.atkcs) / (fv.atkct - fv.atkcs);
		g.chcolor(255, 0, 128, 224);
		g.fellipse(cdc, UI.scale(altui ? new Coord(24, 24) : new Coord(22, 22)), Math.PI / 2 - (Math.PI * 2 * Math.min(1.0 - a, 1.0)), Math.PI / 2);
		g.chcolor();
		FastText.aprintf(g, cdc, 0.5, 0.5, "%.1f", fv.atkct - now);
	    }
	    g.image(cdframe, altui ? new Coord(x0, y0).sub(cdframe.sz().div(2)) : cdc.sub(cdframe.sz().div(2)));
	}
	try {
	    Indir<Resource> lastact = fv.lastact;
	    if(lastact != this.lastact1) {
		this.lastact1 = lastact;
		this.lastacttip1 = null;
	    }
	    double lastuse = fv.lastuse;
	    if(lastact != null) {
		Tex ut = lastact.get().flayer(Resource.imgc).tex();
		Coord useul = altui ? new Coord(x0 - 69, y0) : pcc.add(usec1).sub(ut.sz().div(2));
		g.image(ut, useul);
		g.image(useframe, useul.sub(useframeo));
		double a = now - lastuse;
		if(a < 1) {
		    Coord off = new Coord((int)(a * ut.sz().x / 2), (int)(a * ut.sz().y / 2));
		    g.chcolor(255, 255, 255, (int)(255 * (1 - a)));
		    g.image(ut, useul.sub(off), ut.sz().add(off.mul(2)));
		    g.chcolor();
		}
	    }
	} catch(Loading l) {
	}
	if(fv.current != null) {
	    try {
		Indir<Resource> lastact = fv.current.lastact;
		if(lastact != this.lastact2) {
		    this.lastact2 = lastact;
		    this.lastacttip2 = null;
		}
		double lastuse = fv.current.lastuse;
		if(lastact != null) {
		    Tex ut = lastact.get().flayer(Resource.imgc).tex();
		    Coord useul = altui ? new Coord(x0 + 69 - ut.sz().x, y0) : pcc.add(usec2).sub(ut.sz().div(2));
		    g.image(ut, useul);
		    g.image(useframe, useul.sub(useframeo));
		    double a = now - lastuse;
		    if(a < 1) {
			Coord off = new Coord((int)(a * ut.sz().x / 2), (int)(a * ut.sz().y / 2));
			g.chcolor(255, 255, 255, (int)(255 * (1 - a)));
			g.image(ut, useul.sub(off), ut.sz().add(off.mul(2)));
			g.chcolor();
		    }
		}
	    } catch(Loading l) {
	    }
	}
	for(int i = 0; i < actions.length; i++) {
	    Coord ca = altui ? new Coord(x0 - 18, bottom - 150).add(actc(i)) : pcc.add(actc(i));
	    Action act = actions[i];
	    try {
		if(act != null) {
		    Tex img = act.res.get().flayer(Resource.imgc).tex();
		    Coord hsz = img.sz().div(2);
		    g.image(img, ca);
		    if(now < act.ct) {
			double a = (now - act.cs) / (act.ct - act.cs);
			g.chcolor(0, 0, 0, 132);
			g.prect(ca.add(hsz), hsz.inv(), hsz, (1.0 - a) * Math.PI * 2);
			g.chcolor();
			g.aimage(Text.renderstroked(String.format("%.1f", act.ct - now)).tex(), ca.add(hsz.x, 0), 0.5, 0);
		    }
		    if(CFG.SHOW_COMBAT_KEYS.get()) {g.aimage(keytex(i), ca.add(img.sz()), 1, 1);}
		    
		    if(i == use) {
			g.image(indframe, ca.sub(indframeo));
		    } else if(i == useb) {
			g.image(indbframe, ca.sub(indbframeo));
		    } else {
			g.image(actframe, ca.sub(actframeo));
		    }
		}
	    } catch(Loading l) {}
	}
    }
    
    public static final Tex[] keytex = new Tex[keybinds.length];
    
    static {
	Reactor.listen(COMBAT_KEYS_UPDATED, () ->
	{
	    for (int i = 0; i < keytex.length; i++) {
		if(keytex[i] != null) { keytex[i].dispose(); }
		keytex[i] = null;
	    }
	});
    }
    
    private Tex keytex(int i) {
	if(keytex[i] == null) {
	    keytex[i] = Text.renderstroked(keybinds[i].shortcut(true), fnd).tex();
	}
	return keytex[i];
    }
    
    private Widget prevtt = null;
    private Text acttip = null;
    
    public Object tooltip(Coord c, Widget prev) {
	boolean altui = CFG.ALT_COMBAT_UI.get();
	int x0 =  ui.gui.calendar.rootpos().x + ui.gui.calendar.sz.x / 2;
	int y0 =  ui.gui.calendar.rootpos().y + ui.gui.calendar.sz.y / 2;
	int bottom = ui.gui.beltwdg.c.y - 40;
	for(Buff buff : fv.buffs.children(Buff.class)) {
	    Coord dc = altui ? new Coord(x0 - buff.c.x - Buff.cframe.sz().x - UI.scale(80), y0) : pcc.add(-buff.c.x - Buff.cframe.sz().x - UI.scale(20), buff.c.y + pho - Buff.cframe.sz().y);
	    if(c.isect(dc, buff.sz)) {
		Object ret = buff.tooltip(c.sub(dc), prevtt);
		if(ret != null) {
		    prevtt = buff;
		    return(ret);
		}
	    }
	}
	if(fv.current != null) {
	    for(Buff buff : fv.current.buffs.children(Buff.class)) {
		Coord dc = altui ? new Coord(x0 + buff.c.x + UI.scale(80), y0) : pcc.add(buff.c.x + UI.scale(20), buff.c.y + pho - Buff.cframe.sz().y);
		if(c.isect(dc, buff.sz)) {
		    Object ret = buff.tooltip(c.sub(dc), prevtt);
		    if(ret != null) {
			prevtt = buff;
			return(ret);
		    }
		}
	    }
	}
	final int rl = 5;
	for(int i = 0; i < actions.length; i++) {
	    Coord ca = altui ? new Coord(x0 - 18, bottom - 150).add(actc(i)).add(16, 16) : pcc.add(actc(i));
	    Indir<Resource> act = (actions[i] == null) ? null : actions[i].res;
	    try {
		if(act != null) {
		    Tex img = act.get().flayer(Resource.imgc).tex();
		    ca = ca.sub(img.sz().div(2));
		    if(c.isect(ca, img.sz())) {
			String tip = act.get().flayer(Resource.tooltip).t + " ($b{$col[255,128,0]{" + keybinds[i].shortcut(true) + "}})";
			if((acttip == null) || !acttip.text.equals(tip))
			    acttip = RichText.render(tip, -1);
			return(acttip);
		    }
		}
	    } catch(Loading l) {}
	}
	try {
	    Indir<Resource> lastact = this.lastact1;
	    if(lastact != null) {
		Coord usesz = lastact.get().flayer(Resource.imgc).sz;
		Coord lac = altui ? new Coord(x0 - 69, y0).add(usesz.div(2)) : pcc.add(usec1);
		if(c.isect(lac.sub(usesz.div(2)), usesz)) {
		    if(lastacttip1 == null)
			lastacttip1 = Text.render(lastact.get().flayer(Resource.tooltip).t);
		    return(lastacttip1);
		}
	    }
	} catch(Loading l) {}
	try {
	    Indir<Resource> lastact = this.lastact2;
	    if(lastact != null) {
		Coord usesz = lastact.get().flayer(Resource.imgc).sz;
		Coord lac = altui ? new Coord(x0 + 69 - usesz.x, y0).add(usesz.div(2)) : pcc.add(usec2);
		if(c.isect(lac.sub(usesz.div(2)), usesz)) {
		    if(lastacttip2 == null)
			lastacttip2 = Text.render(lastact.get().flayer(Resource.tooltip).t);
		    return(lastacttip2);
		}
	    }
	} catch(Loading l) {}
	return(null);
    }

    public void uimsg(String msg, Object... args) {
	if(msg == "act") {
	    int n = (Integer)args[0];
	    if(args.length > 1) {
		Indir<Resource> res = ui.sess.getres((Integer)args[1]);
		actions[n] = new Action(res);
	    } else {
		actions[n] = null;
	    }
	} else if(msg == "acool") {
	    int n = (Integer)args[0];
	    double now = Utils.rtime();
	    actions[n].cs = now;
	    actions[n].ct = now + (((Number)args[1]).doubleValue() * 0.06);
	} else if(msg == "use") {
	    this.use = (Integer)args[0];
	    this.useb = (args.length > 1) ? ((Integer)args[1]) : -1;
	} else if(msg == "used") {
	} else {
	    super.uimsg(msg, args);
	}
    }

    public static final KeyBinding[] kb_acts = {
	KeyBinding.get("fgt/0", KeyMatchFake.forcode(KeyEvent.VK_1, 0)),
	KeyBinding.get("fgt/1", KeyMatchFake.forcode(KeyEvent.VK_2, 0)),
	KeyBinding.get("fgt/2", KeyMatchFake.forcode(KeyEvent.VK_3, 0)),
	KeyBinding.get("fgt/3", KeyMatchFake.forcode(KeyEvent.VK_4, 0)),
	KeyBinding.get("fgt/4", KeyMatchFake.forcode(KeyEvent.VK_5, 0)),
	KeyBinding.get("fgt/5", KeyMatchFake.forcode(KeyEvent.VK_1, KeyMatch.S)),
	KeyBinding.get("fgt/6", KeyMatchFake.forcode(KeyEvent.VK_2, KeyMatch.S)),
	KeyBinding.get("fgt/7", KeyMatchFake.forcode(KeyEvent.VK_3, KeyMatch.S)),
	KeyBinding.get("fgt/8", KeyMatchFake.forcode(KeyEvent.VK_4, KeyMatch.S)),
	KeyBinding.get("fgt/9", KeyMatchFake.forcode(KeyEvent.VK_5, KeyMatch.S)),
    };
    public static final KeyBinding kb_relcycle =  KeyBinding.get("fgt-cycle", KeyMatch.forcode(KeyEvent.VK_TAB, KeyMatch.C), KeyMatch.S);

    /* XXX: This is a bit ugly, but release message do need to be
     * properly sequenced with use messages in some way. */
    private class Release implements Runnable {
	final int n;

	Release(int n) {
	    this.n = n;
	    Environment env = ui.getenv();
	    Render out = env.render();
	    out.fence(this);
	    env.submit(out);
	}


	public void run() {
	    wdgmsg("rel", n);
	}
    }

    private UI.Grab holdgrab = null;
    private int held = -1;
    public boolean globtype(char key, KeyEvent ev) {
//	if((ev.getModifiersEx() & (InputEvent.CTRL_DOWN_MASK | KeyEvent.META_DOWN_MASK | KeyEvent.ALT_DOWN_MASK)) == 0) 
	{
	    int fn = getAction(ev);
	    if((fn >= 0) && (fn < actions.length)) {
		MapView map = getparent(GameUI.class).map;
		Coord mvc = map.rootxlate(ui.mc);
		if(held >= 0) {
		    new Release(held);
		    held = -1;
		}
		if(mvc.isect(Coord.z, map.sz)) {
		    map.new Maptest(mvc) {
			    protected void hit(Coord pc, Coord2d mc) {
				wdgmsg("use", fn, 1, ui.modflags(), mc.floor(OCache.posres));
			    }

			    protected void nohit(Coord pc) {
				wdgmsg("use", fn, 1, ui.modflags());
			    }
			}.run();
		}
		if(holdgrab == null)
		    holdgrab = ui.grabkeys(this);
		held = fn;
		return(true);
	    }
	}
	if(kb_relcycle.key().match(ev, KeyMatch.S)) {
	    if((ev.getModifiersEx() & KeyEvent.SHIFT_DOWN_MASK) == 0) {
		Fightview.Relation cur = fv.current;
		if(cur != null) {
		    fv.lsrel.remove(cur);
		    fv.lsrel.addLast(cur);
		}
	    } else {
		Fightview.Relation last = fv.lsrel.getLast();
		if(last != null) {
		    fv.lsrel.remove(last);
		    fv.lsrel.addFirst(last);
		}
	    }
	    fv.wdgmsg("bump", (int)fv.lsrel.get(0).gobid);
	    return(true);
	}
	return(super.globtype(key, ev));
    }

    public boolean keydown(KeyEvent ev) {
	return(false);
    }

    public boolean keyup(KeyEvent ev) {
//	if((holdgrab != null) && (kb_acts[held].key().match(ev, KeyMatch.MODS))) {
	if((holdgrab != null) && (keybinds[held].match(ev))) {
	    MapView map = getparent(GameUI.class).map;
	    new Release(held);
	    holdgrab.remove();
	    holdgrab = null;
	    held = -1;
	    return(true);
	}
	return(false);
    }
    
    private int getAction(KeyEvent ev) {
	for (int i = 0; i < actions.length && i < keybinds.length; i++) {
	    if(keybinds[i].match(ev)) {
		return i;
	    }
	}
	return -1;
    }
    
    public static void updateKeybinds(KeyBind[] combat) {
	if(combat != null) {
	    for (int i = 0; i < combat.length && i < keybinds.length; i++) {
		keybinds[i] = combat[i];
	    }
	}
    }
}
