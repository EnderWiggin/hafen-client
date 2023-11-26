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

import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.util.*;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

import static haven.CharWnd.*;
import static haven.Inventory.*;
import static haven.Window.wbox;

public class FightWndEx extends Widget {
    public static final Text.Foundry fnd = new Text.Foundry(Text.sans.deriveFont(Font.BOLD), 14);
    private static final Text.Foundry cntfnd = new Text.Foundry(Text.serif, 14);
    public final int nsave;
    private final Button save;
    public int maxact;
    public final Actions actlist;
    public final ActionTypes acttypes;
    public final Savelist savelist;
    public final IButton accept, cancel, edit;
    public List<Action> ALL = new ArrayList<>();
    private List<Action> acts = ALL;
    private ActionType selectedType = ActionType.All;
    public final Action[] order;
    public int usesave;
    private final Text[] saves;
    private final FightWnd.ImageInfoBox info;
    private final Label count;
    private final Map<Indir<Resource>, Object[]> actrawinfo = new HashMap<>();
    private boolean needFilter = false;
    
    private static final OwnerContext.ClassResolver<FightWndEx> actxr = new OwnerContext.ClassResolver<FightWndEx>()
	.add(Glob.class, wdg -> wdg.ui.sess.glob)
	.add(Session.class, wdg -> wdg.ui.sess);
    public static final Text.Foundry namef = new Text.Foundry(Text.serif.deriveFont(java.awt.Font.BOLD, 16f)).aa(true);
    public class Action implements ItemInfo.ResOwner {
	public final Indir<Resource> res;
	private final int id;
	public int a, u;
	private Text rnm, ru, rc;
	private Tex ri;
	
	public Action(Indir<Resource> res, int id, int a, int u) {this.res = res; this.id = id; this.a = a; this.u = u;}
	
	public String rendertext() {
	    StringBuilder buf = new StringBuilder();
	    Resource res = this.res.get();
	    buf.append("$img[").append(res.name).append("]\n\n");
	    buf.append("$b{$font[serif,16]{").append(res.layer(Resource.tooltip).t).append("}}\n\n");
	    Resource.Pagina pag = res.layer(Resource.pagina);
	    if(pag != null)
		buf.append(pag.text);
	    return(buf.toString());
	}
	
	private void a(int a) {
	    if(this.a != a) {
		this.a = a;
		this.ru = null;
		this.rc = null;
	    }
	}
	
	private void u(int u) {
	    if(this.u != u) {
		this.u = u;
		this.ru = null;
		this.rc = null;
		recount();
	    }
	}
	
	public Resource resource() {return(res.get());}
	
	private List<ItemInfo> info = null;
	public List<ItemInfo> info() {
	    if(info == null) {
		Object[] rawinfo = actrawinfo.get(this.res);
		if(rawinfo != null)
		    info = ItemInfo.buildinfo(this, rawinfo);
		else
		    info = Collections.singletonList(new ItemInfo.Name(this, res.get().layer(Resource.tooltip).t));
	    }
	    return(info);
	}
	public <T> T context(Class<T> cl) {return(actxr.context(cl, FightWndEx.this));}
	
	public BufferedImage rendericon() {
	    BufferedImage ret = res.get().layer(Resource.imgc).img;
	    Graphics g = null;
	    for(ItemInfo inf : info()) {
		if(inf instanceof FightWnd.IconInfo) {
		    if(g == null) {
			BufferedImage buf = TexI.mkbuf(PUtils.imgsz(ret));
			g = buf.getGraphics();
			ret = buf;
		    }
		    ((FightWnd.IconInfo)inf).draw(ret, g);
		}
	    }
	    if(g != null)
		g.dispose();
	    return(ret);
	}
	
	private Tex icon = null;
	public Tex icon() {
	    if(icon == null)
		icon = new TexI(rendericon());
	    return(icon);
	}
	
	public BufferedImage renderinfo(int width) {
	    ItemInfo.Layout l = new ItemInfo.Layout();
	    l.width = width;
	    List<ItemInfo> info = info();
	    l.cmp.add(rendericon(), Coord.z);
	    ItemInfo.Name nm = ItemInfo.find(ItemInfo.Name.class, info);
	    assert nm != null;
	    l.cmp.add(namef.render(nm.str.text).img, new Coord(0, l.cmp.sz.y + UI.scale(10)));
	    l.cmp.sz = l.cmp.sz.addy(UI.scale(10));
	    for (ItemInfo inf : info) {
		if((inf != nm) && (inf instanceof ItemInfo.Tip)) {
		    l.add((ItemInfo.Tip) inf);
		}
	    }
	    Resource.Pagina pag = res.get().layer(Resource.pagina);
	    if(pag != null)
		l.add(new ItemInfo.Pagina(this, pag.text));
	    return (l.render());
	}
 
	private Tex usage() {
	    if(ru == null) {
		ru = cntfnd.render(String.format("%d/%d", u, a));
	    }
	    return ru.tex();
	}
 
 
	private Tex count() {
	    if(rc == null) {
		rc = cntfnd.render(String.format("%d", a - u));
	    }
	    return rc.tex();
	}
	
    }
    
    private void recount() {
	int u = 0;
	for (Action act : ALL)
	    u += act.u;
	count.settext(String.format("Used: %d/%d", u, maxact));
	count.setcolor((u > maxact) ? Color.RED : Color.WHITE);
    }

    private static final Tex[] add = {Resource.loadtex("gfx/hud/buttons/addu"),
	Resource.loadtex("gfx/hud/buttons/addd")};
    private static final Tex[] sub = {Resource.loadtex("gfx/hud/buttons/subu"),
	Resource.loadtex("gfx/hud/buttons/subd")};

    public class Actions extends Listbox<Action> implements DTarget {
	private boolean loading = false;
	private int da = -1, ds = -1;
	UI.Grab d = null;
	Action drag = null;
	Coord dp;

	public Actions(int w, int h) {
	    super(w, h, attrf.height() + UI.scale(2));
	}

	protected Action listitem(int n) {return (acts.get(n));}

	protected int listitems() {return (acts.size());}

	protected void drawbg(GOut g) {}

	protected void drawitem(GOut g, Action act, int idx) {
	    g.chcolor((idx % 2 == 0) ? CharWnd.every : CharWnd.other);
	    g.frect(Coord.z, g.sz());
	    g.chcolor();
	    try {
		if(act.ri == null)
		    act.ri = new TexI(PUtils.convolvedown(act.res.get().layer(Resource.imgc).img, new Coord(itemh, itemh), CharWnd.iconfilter));
		g.image(act.ri, Coord.z);
	    } catch (Loading l) {
		g.image(WItem.missing.layer(Resource.imgc).tex(), Coord.z, new Coord(itemh, itemh));
	    }
	    if(act.rnm != null) {
		int ty = (itemh - act.rnm.sz().y) / 2;
		g.image(act.rnm.tex(), new Coord(itemh + UI.scale(2), ty));
		g.aimage(act.count(), new Coord(sz.x - UI.scale(45), ty), 1.0, 0.0);
	    }
	    g.aimage(add[da == idx ? 1 : 0], new Coord(sz.x - UI.scale(10), itemh / 2), 1.0, 0.5);
	    g.aimage(sub[ds == idx ? 1 : 0], new Coord(sz.x - UI.scale(25), itemh / 2), 1.0, 0.5);
	}

	public void change(final Action act) {
	    if(act != null)
		info.set(() -> new TexI(act.renderinfo(info.sz.x - UI.scale(20))));
	    else if(sel != null)
		info.set((Tex) null);
	    super.change(act);
	}

	public boolean mousewheel(Coord c, int am) {
	    if(ui.modshift) {
		Action act = itemat(c);
		if(act != null)
		    setu(act, act.u - am);
		return (true);
	    }
	    return (super.mousewheel(c, am));
	}

	public void draw(GOut g) {
	    if(loading) {
		loading = false;
		for (Action act : acts) {
		    try {
			act.rnm = attrf.render(act.res.get().layer(Resource.tooltip).t);
		    } catch (Loading l) {
			act.rnm = attrf.render("...");
			loading = true;
		    }
		}
		acts.sort(Comparator.comparing(a -> a.rnm.text));
	    }
	    if((drag != null) && (dp == null)) {
		try {
		    final Tex dt = drag.res.get().layer(Resource.imgc).tex();
		    ui.drawafter(g1 -> g1.image(dt, ui.mc.add(dt.sz().div(2).inv())));
		} catch (Loading ignored) {}
	    }
	    super.draw(g);
	}

	private boolean onadd(Coord c, int idx) {
	    Coord ic = c.sub(0, (idx - sb.val) * itemh);
	    int by = (itemh - add[0].sz().y) / 2;
	    return (ic.isect(new Coord(sz.x - UI.scale(10) - add[0].sz().x, by), add[0].sz()));
	}

	private boolean onsub(Coord c, int idx) {
	    Coord ic = c.sub(0, (idx - sb.val) * itemh);
	    int by = (itemh - sub[0].sz().y) / 2;
	    return (ic.isect(new Coord(sz.x - UI.scale(25) - add[0].sz().x, by), add[0].sz()));
	}

	public void drag(Action act) {
	    if(d == null)
		d = ui.grabmouse(this);
	    drag = act;
	    dp = null;
	}

	public boolean mousedown(Coord c, int button) {
	    if(button == 1) {
		int idx = (c.y / itemh) + sb.val;
		if(idx < listitems()) {
		    if(onadd(c, idx)) {
			da = idx;
			d = ui.grabmouse(this);
			return (true);
		    } else if(onsub(c, idx)) {
			ds = idx;
			d = ui.grabmouse(this);
			return (true);
		    }
		}
		super.mousedown(c, button);
		if((sel != null) && (c.x < sb.c.x)) {
		    d = ui.grabmouse(this);
		    drag = sel;
		    dp = c;
		}
		return (true);
	    }
	    return (super.mousedown(c, button));
	}

	public void mousemove(Coord c) {
	    super.mousemove(c);
	    if((drag != null) && (dp != null)) {
		if(c.dist(dp) > 5)
		    dp = null;
	    }
	}

	private boolean setu(Action act, int u) {
	    u = Utils.clip(u, 0, act.a);
	    int s;
	    for (s = 0; s < order.length; s++) {
		if(order[s] == act)
		    break;
	    }
	    if(u > 0) {
		if(s == order.length) {
		    for (s = 0; s < order.length; s++) {
			if(order[s] == null)
			    break;
		    }
		    if(s == order.length)
			return (false);
		    order[s] = act;
		}
	    } else {
		if(s < order.length)
		    order[s] = null;
	    }
	    act.u(u);
	    return (true);
	}

	public boolean mouseup(Coord c, int button) {
	    if((d != null) && (button == 1)) {
		d.remove();
		d = null;
		if(drag != null) {
		    if(dp == null)
			ui.dropthing(ui.root, c.add(rootpos()), drag);
		    drag = null;
		}
		if(da >= 0) {
		    if(onadd(c, da)) {
			Action act = listitem(da);
			setu(act, act.u + 1);
		    }
		    da = -1;
		} else if(ds >= 0) {
		    if(onsub(c, ds)) {
			Action act = listitem(ds);
			setu(act, act.u - 1);
		    }
		    ds = -1;
		}
		return (true);
	    }
	    return (super.mouseup(c, button));
	}

	public boolean drop(Coord cc, Coord ul) {
	    return (false);
	}

	public boolean iteminteract(Coord cc, Coord ul) {
	    Action act = itemat(cc);
	    if(act != null)
		FightWndEx.this.wdgmsg("itemact", act.id, ui.modflags());
	    return (true);
	}
    }

    public int findorder(Action a) {
	for (int i = 0; i < order.length; i++) {
	    if(order[i] == a)
		return (i);
	}
	return (-1);
    }

    public class BView extends Widget implements DropTarget {
	private UI.Grab grab;
	private Action drag;
	private Coord dp;
	private final Coord[] animoff = new Coord[order.length];
	private final double[] animpr = new double[order.length];
	private boolean anim = false;

	private BView() {
	    super(invsq.sz().mul(order.length, 1).add(UI.scale(
		2 * (order.length - 1) + 10 * ((order.length - 1) / 5),
		16)));
	}

	private Coord itemc(int i) {
	    return (new Coord(((invsq.sz().x + UI.scale(2)) * i) + UI.scale(10 * (i / 5)), 0));
	}

	private int citem(Coord c) {
	    for (int i = 0; i < order.length; i++) {
		if(c.isect(itemc(i), invsq.sz()))
		    return (i);
	    }
	    return (-1);
	}

	final Tex[] keys = new Tex[10];

	{
	    listen(KeyBinder.COMBAT_KEYS_UPDATED, this::updateKeybinds);
	    updateKeybinds();
	}
 
	private void updateKeybinds() {
	    for (int i = 0; i < 10; i++) {
		if(this.keys[i] != null) {this.keys[i].dispose();}
		this.keys[i] = Text.renderstroked(Fightsess.keybinds[i].shortcut(true), fnd).tex();
	    }
	}

	public void draw(GOut g) {
	    int[] reo = null;
	    if(anim) {
		reo = new int[order.length];
		for (int i = 0, a = 0, b = order.length - 1; i < order.length; i++) {
		    if(animoff[i] == null)
			reo[a++] = i;
		    else
			reo[b--] = i;
		}
	    }
	    for (int io = 0; io < order.length; io++) {
		int i = (reo == null) ? io : reo[io];
		Coord c = itemc(i);
		g.image(invsq, c);
		Action act = order[i];
		try {
		    if(act != null) {
			Coord ic = c.add(1, 1);
			if(animoff[i] != null) {
			    ic = ic.add(animoff[i].mul(Math.pow(1.0 - animpr[i], 3)));
			}
			g.image(act.res.get().layer(Resource.imgc).tex(), ic);
			g.aimage(act.usage(), c.add(invsq.sz().sub(invsq.sz().x / 2, 0)), 0.5, 0);
		    }
		} catch (Loading ignored) {}
		g.aimage(keys[i], c.add(invsq.sz().sub(2, 0)), 1, 1);
		g.chcolor();
	    }
	}

	public boolean mousedown(Coord c, int button) {
	    if(button == 1) {
		int s = citem(c);
		if(s >= 0) {
		    Action act = order[s];
		    actlist.change(act);
		    actlist.display();
		    if(act != null) {
			grab = ui.grabmouse(this);
			drag = act;
			dp = c;
		    }
		    return (true);
		}
	    } else if(button == 3) {
		int s = citem(c);
		if(s >= 0) {
		    if(order[s] != null)
			order[s].u(0);
		    order[s] = null;
		    return (true);
		}
	    }
	    return (super.mousedown(c, button));
	}

	public void mousemove(Coord c) {
	    super.mousemove(c);
	    if(dp != null) {
		if(c.dist(dp) > 5) {
		    grab.remove();
		    actlist.drag(drag);
		    grab = null;
		    drag = null;
		    dp = null;
		}
	    }
	}

	public boolean mouseup(Coord c, int button) {
	    if(grab != null) {
		grab.remove();
		grab = null;
		drag = null;
		dp = null;
	    }
	    return (super.mouseup(c, button));
	}

	private void animate(int s, Coord off) {
	    animoff[s] = off;
	    animpr[s] = 0.0;
	    anim = true;
	}

	public boolean dropthing(Coord c, Object thing) {
	    if(thing instanceof Action) {
		Action act = (Action) thing;
		int s = citem(c);
		if(s < 0)
		    return (false);
		if(order[s] != act) {
		    if(order[s] != null) {
			int cp = findorder(act);
			if(cp >= 0) {
			    order[cp] = order[s];
			    animate(cp, itemc(s).sub(itemc(cp)));
			} else {
			    order[s].u(0);
			}
		    }
		    order[s] = act;
		    if(act.u < 1)
			act.u(1);
		}
		return (true);
	    }
	    return (false);
	}

	public void tick(double dt) {
	    if(anim) {
		boolean na = false;
		for (int i = 0; i < order.length; i++) {
		    if(animoff[i] != null) {
			if((animpr[i] += (dt * 3)) > 1.0)
			    animoff[i] = null;
			else
			    na = true;
		    }
		}
		anim = na;
	    }
	}
    }

    public class Savelist extends Dropbox<Integer> {
	private int edit = -1;
	private Text.Line redit = null;
	private ReadLine nmed;
	private long focusstart;

	public Savelist(int w, int h) {
	    super(w, h, attrf.height() + 2);
	    setcanfocus(true);
	    sel = 0;
	}

	public void edit() {
	    FightWndEx.this.save.hide();
	    FightWndEx.this.edit.hide();
	    FightWndEx.this.accept.show();
	    FightWndEx.this.cancel.show();
	    edit = sel;
	    nmed = ReadLine.make(new ReadLine.Owner() {
		@Override
		public void changed(ReadLine buf) {
		    redit = null;
		}
		
		@Override
		public void done(ReadLine buf) {
		    saveName();
		}
	    }, saves[sel].text);
	    redit = null;
	    parent.setfocus(this);
	    focusstart = System.currentTimeMillis();
	}

	public boolean isEditing() {
	    return nmed != null && edit != -1;
	}

	public void saveName() {
	    if(nmed != null && edit != -1) {
		saves[edit] = attrf.render(nmed.line());
		int tmp = edit;
		cancel();
		save(tmp);
		use(tmp);
	    }
	}

	public void cancel() {
	    FightWndEx.this.save.show();
	    FightWndEx.this.edit.show();
	    FightWndEx.this.accept.hide();
	    FightWndEx.this.cancel.hide();
	    edit = -1;
	    redit = null;
	    nmed = null;
	}

	protected Integer listitem(int idx) {return (idx);}

	protected int listitems() {return (nsave);}

	protected void drawitem(GOut g, Integer save, int n) {
	    if(edit == save) {
		if(redit == null)
		    redit = attrf.render(nmed.line());
		g.aimage(redit.tex(), new Coord(3, itemh / 2), 0.0, 0.5);
		if(hasfocus && (((System.currentTimeMillis() - focusstart) % 1000) < 500)) {
		    int cx = redit.advance(nmed.point());
		    g.chcolor(255, 255, 255, 255);
		    Coord co = new Coord(3 + cx + 1, (g.sz().y - redit.sz().y) / 2);
		    g.line(co, co.add(0, redit.sz().y), 1);
		    g.chcolor();
		}
	    } else {
		g.aimage(saves[save].tex(), new Coord(3, itemh / 2), 0.0, 0.5);
	    }
	}

	public void change(Integer sel) {
	    super.change(sel);
	    cancel();
	    load(sel);
	    use(sel);
	}

	public boolean keydown(KeyEvent ev) {
	    if(edit != -1) {
		if(ev.getKeyChar() == 27) {
		    cancel();
		    return (true);
		} else {
		    return (nmed.key(ev));
		}
	    }
	    return (super.keydown(ev));
	}

	public void change2(Integer sel) {
	    super.change(sel);
	}
    }

    public void load(int n) {
	wdgmsg("load", n);
    }

    public void save(int n) {
	List<Object> args = new LinkedList<>();
	args.add(n);
	if(saves[n] != unused)
	    args.add(saves[n].text);
	for(Action action : order) {
	    if(action == null) {
		args.add(null);
	    } else {
		args.add(action.id);
		args.add(action.u);
	    }
	}
	wdgmsg("save", args.toArray(new Object[0]));
    }

    public void use(int n) {
	wdgmsg("use", n);
    }

    private final Text unused = new Text.Foundry(attrf.font.deriveFont(java.awt.Font.ITALIC)).aa(true).i10n_label("Unused save");

    
    private static final Coord INFO_SZ = UI.scale(223, 0);
    public FightWndEx(int nsave, int nact, int max) {
	super(Coord.z);
	this.nsave = nsave;
	this.maxact = max;
	this.order = new Action[nact];
	this.saves = new Text[nsave];
	for (int i = 0; i < nsave; i++)
	    saves[i] = unused;
    
	Widget header = add(new Img(CharWnd.catf.i10n_label("Martial Arts & Combat Schools").tex()), 0, 0);
    
	Coord boxSz = wbox.btloff();
	info = add(new FightWnd.ImageInfoBox(INFO_SZ), header.pos("br").xs(5).addys(5).add(boxSz));
    
	acttypes = add(new ActionTypes(this::actionTypeSelected), info.pos("ur").addxs(5).addx(boxSz.x));
	acttypes.setSelectedColor(new Color(122, 191, 86, 153));
	acttypes.select(0);
    
	actlist = add(new Actions(UI.scale(250), 8), acttypes.pos("bl").add(boxSz).addys(-1));
	Frame.around(this, Collections.singletonList(actlist));
    
	info.resize(INFO_SZ.addy(actlist.sz.y + acttypes.sz.y + boxSz.y));
	Frame.around(this, Collections.singletonList(info));
    
	Widget mappedActions = add(new BView(), info.pos("bl").addys(10));
	count = add(new Label(""), mappedActions.pos("ur").adds(10, 5));
    
	savelist = add(new Savelist(UI.scale(370), 5), mappedActions.pos("bl").addys(5));
	
	save = adda(new Button(UI.scale(75), "Save", false) {
	    public void click() {
		if(savelist.sel == null || savelist.sel < 0) {
		    getparent(GameUI.class).error("No save entry selected.");
		} else {
		    if(savelist.isEditing()) {
			savelist.saveName();
		    } else {
			save(savelist.sel);
			use(savelist.sel);
		    }
		}
	    }
	}, savelist.pos("ur").x(actlist.pos("br").x), 1.0, 0.0);
	edit = add(new IButton("gfx/hud/btn-edit", "", "-d", "-h") {
	    {
		tooltip = L10N.button("Rename");
		recthit = true;
	    }
	
	    public void click() {
		if(savelist.sel == null || savelist.sel < 0) {
		    getparent(GameUI.class).error("No save entry selected.");
		} else {
		    savelist.edit();
		}
	    }
	}, savelist.pos("ur").addxs(5));
	accept = add(new IButton("gfx/hud/btn-check", "", "-d", "-h") {
	    {
		tooltip = L10N.button("Accept");
		recthit = true;
		hide();
	    }
	
	    public void click() {
		if(savelist.sel == null || savelist.sel < 0) {
		    getparent(GameUI.class).error("No save entry selected.");
		} else {
		    savelist.saveName();
		}
	    }
	}, edit.c);
	cancel = add(new IButton("gfx/hud/btn-x", "", "-d", "-h") {
	    {
		tooltip = L10N.button("Cancel");
		recthit = true;
		hide();
	    }
	
	    public void click() {
		if(savelist.sel == null || savelist.sel < 0) {
		    getparent(GameUI.class).error("No save entry selected.");
		} else {
		    savelist.cancel();
		}
	    }
	}, edit.pos("ur").addxs(20));
	pack();
    }

    private Void actionTypeSelected(TabStrip.Button<ActionType> button) {
	selectedType = button.tag;
	needFilter = true;
	return null;
    }


    @Override
    public void tick(double dt) {
	super.tick(dt);
	if(needFilter) {
	    doFilter();
	}
    }

    private void doFilter() {
	try {
	    if(ALL != null) {
		acts = ALL.stream().filter(selectedType::matches).collect(Collectors.toList());
		acts.sort(Comparator.comparing(a -> a.res.get().layer(Resource.tooltip).t));
		actlist.change(actlist.listitems() > 0 ? actlist.listitem(0) : null);
		actlist.showsel();
		needFilter = false;
	    }
	} catch (Resource.Loading ignored) {
	}
    }

    public Action findact(int resid) {
	for (Action act : ALL) {
	    if(act.id == resid)
		return (act);
	}
	return (null);
    }

    public void uimsg(String nm, Object... args) {
	if(Objects.equals(nm, "avail")) {
	    List<Action> acts = new ArrayList<>();
	    int a = 0;
	    while (true) {
		int resid = (Integer) args[a++];
		if(resid < 0)
		    break;
		int av = (Integer) args[a++];
		Action pact = findact(resid);
		if(pact == null) {
		    acts.add(new Action(ui.sess.getres(resid), resid, av, 0));
		} else {
		    acts.add(pact);
		    pact.a(av);
		}
	    }
	    this.ALL = acts;
	    actlist.loading = true;
	    needFilter = true;
	} else if(Objects.equals(nm, "tt")) {
	    Indir<Resource> res = ui.sess.getres((Integer)args[0]);
	    Object[] rawinfo = (Object[])args[1];
	    actrawinfo.put(res, rawinfo);
	} else if(Objects.equals(nm, "used")) {
	    int a = 0;
	    for (Action act : acts)
		act.u(0);
	    for (int i = 0; i < order.length; i++) {
		int resid = (Integer) args[a++];
		if(resid < 0) {
		    order[i] = null;
		    continue;
		}
		int us = (Integer) args[a++];
		(order[i] = findact(resid)).u(us);
	    }
	} else if(Objects.equals(nm, "saved")) {
	    int fl = (Integer) args[0];
	    for (int i = 0; i < nsave; i++) {
		if((fl & (1 << i)) != 0) {
		    if(args[i + 1] instanceof String)
			saves[i] = attrf.render((String) args[i + 1]);
		    else
			saves[i] = attrf.i10n_label(String.format("Saved school %d", i + 1));
		} else {
		    saves[i] = unused;
		}
	    }
	} else if(Objects.equals(nm, "use")) {
	    usesave = (Integer) args[0];
	    savelist.change2(usesave);
	} else if(Objects.equals(nm, "max")) {
	    maxact = (Integer) args[0];
	    recount();
	} else {
	    super.uimsg(nm, args);
	}
    }

    static class ActionTypes extends TabStrip<ActionType> {
	private final Function<Button<ActionType>, Void> selected;

	ActionTypes(Function<Button<ActionType>, Void> selected) {
	    super();
	    this.selected = selected;
	    ActionType[] types = ActionType.values();
	    for (int i = 0; i < types.length; i++) {
		insert(i, types[i].icon(), "", types[i].name()).tag = types[i];
	    }
	}

	@Override
	protected void selected(Button<ActionType> button) {
	    if(selected != null) {
		selected.apply(button);
	    }
	}
    }

    enum ActionType {
	All("gfx/hud/tab/combat/all", null),
	Attacks("gfx/hud/tab/combat/attack", new HashSet<>(Arrays.asList(
	    "paginae/atk/pow",
	    "paginae/atk/lefthook",
	    "paginae/atk/lowblow",
	    "paginae/atk/oppknock",
	    "paginae/atk/ripapart",
	    "paginae/atk/fullcircle",
	    "paginae/atk/cleave",
	    "paginae/atk/barrage",
	    "paginae/atk/sideswipe",
	    "paginae/atk/sting",
	    "paginae/atk/sos",
	    "paginae/atk/knockteeth",
	    "paginae/atk/kick",
	    "paginae/atk/haymaker",
	    "paginae/atk/chop",
	    "paginae/atk/gojug",
	    "paginae/atk/uppercut",
	    "paginae/atk/punchboth",
	    "paginae/atk/stealthunder",
	    "paginae/atk/ravenbite",
	    "paginae/atk/takedown"
	))),
	Defences("gfx/hud/tab/combat/restore", new HashSet<>(Arrays.asList(
	    "paginae/atk/regain",
	    "paginae/atk/dash",
	    "paginae/atk/zigzag",
	    "paginae/atk/yieldground",
	    "paginae/atk/watchmoves",
	    "paginae/atk/sidestep",
	    "paginae/atk/qdodge",
	    "paginae/atk/jump",
	    "paginae/atk/fdodge",
	    "paginae/atk/artevade",
	    "paginae/atk/flex"
	))),
	Maneuvers("gfx/hud/tab/combat/maneuver", new HashSet<>(Arrays.asList(
	    "paginae/atk/toarms",
	    "paginae/atk/shield",
	    "paginae/atk/parry",
	    "paginae/atk/oakstance",
	    "paginae/atk/dorg",
	    "paginae/atk/chinup",
	    "paginae/atk/bloodlust",
	    "paginae/atk/combmed"
	))),
	Moves("gfx/hud/tab/combat/move", new HashSet<>(Arrays.asList(
	    "paginae/atk/think",
	    "paginae/atk/takeaim"
	))),
	Other("gfx/invobjs/missing");

	private final String res;
	private final Set<String> list;
	private Tex icon;
	private boolean inverted = false;
 
	Tex icon() {
	    if(icon == null) {
		icon = new TexI(PUtils.convolvedown(Resource.loadimg(res), UI.scale(20, 20), CharWnd.iconfilter));
	    }
	    return icon;
	}
 
	boolean matches(Action action) {
	    if(inverted) {
		for(ActionType actionType : ActionType.values()) {
		    if(actionType.list != null && actionType.list.contains(action.res.get().name)) {
			return false;
		    }
		}
		return true;
	    } else {
		return list == null || list.contains(action.res.get().name);
	    }
	}

	ActionType(String res, Set<String> list) {
	    this.res = res;
	    this.list = list;
	}
 
	ActionType(String res) {
	    this.res = res;
	    this.list = null;
	    inverted = true;
	}
    }
}
