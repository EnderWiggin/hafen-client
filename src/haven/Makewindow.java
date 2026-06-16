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

import auto.Actions;
import me.ender.Reflect;
import me.ender.ui.ICraftParent;
import me.ender.ui.ValueEntry;

import haven.render.*;
import java.util.*;
import java.awt.Font;
import java.awt.Color;
import java.awt.image.BufferedImage;
import static haven.PType.*;
import static haven.Inventory.invsq;

public class Makewindow extends Widget {
    public static final Text qmodl = Text.render(L10N.label("Quality:"));
    public static final Text tooll = Text.render(L10N.label("Tools:"));
    public static final Coord boff = UI.scale(new Coord(7, 9));
    public String rcpnm;
    public List<Input> inputs = Collections.emptyList();
    public List<SpecWidget> outputs = Collections.emptyList();
    public List<Indir<Resource>> qmod = Collections.emptyList();
    public List<Indir<Resource>> tools = new ArrayList<>();;
    private int xoff = UI.scale(45), qmy = UI.scale(38), outy = UI.scale(65);
    public static final Text.Foundry nmf = new Text.Foundry(Text.serif, 20).aa(true);
    private static double softcap = 0;
    private static Tex softTex = null;

    @RName("make")
    public static class $_ implements Factory {
	public Widget create(UI ui, Object[] args) {
	    return(new Makewindow((String)args[0]));
	}
    }

    private static final OwnerContext.ClassResolver<Makewindow> ctxr = new OwnerContext.ClassResolver<Makewindow>()
	.add(Makewindow.class, wdg -> wdg)
	.add(Glob.class, wdg -> wdg.ui.sess.glob)
	.add(Session.class, wdg -> wdg.ui.sess);
    public class Spec implements GSprite.Owner, ItemInfo.SpriteOwner, RandomSource {
	public ResData item, constraint;
	public int num;
	private GSprite spr;
	private Object[] rawinfo;
	private List<ItemInfo> info;

	public Spec(ResData item, int num, Object[] info) {
	    this.item = item;
	    this.num = num;
	    this.rawinfo = (info.length > 0) ? info : new Object[][] {{new ItemInfo.Name.Default()}};
	}

	private ResData display() {
	    if(constraint != null)
		return(constraint);
	    return(item);
	}

	public GSprite sprite() {
	    if(spr == null)
		spr = GSprite.create(this, display().res.get(), display().sdt.clone());;
	    return(spr);
	}

	public void draw(GOut g) {
	    try {
		sprite().draw(g);
	    } catch(Loading e) {}
	}

	private int opt = 0;
	public boolean opt() {
	    if(opt == 0)
		opt = (ItemInfo.find(Optional.class, info()) != null) ? 1 : 2;
	    return(opt == 1);
	}

	public class SpecTip implements Indir<Tex>, ItemInfo.InfoTip {
	    private final List<ItemInfo> info;
	    private final TexI tex;

	    public SpecTip(List<ItemInfo> info, BufferedImage img) {
		this.info = info;
		if(img == null)
		    throw(new Loading());
		tex = new TexI(img);
	    }

	    public List<ItemInfo> info() {return(info);}
	    public Tex get() {return(tex);}
	}

	public SpecTip shorttip() {
	    List<ItemInfo> info = info();
	    return(new SpecTip(info, ItemInfo.shorttip(info())));
	}
	public SpecTip longtip() {
	    List<ItemInfo> info = info();
	    BufferedImage img = ItemInfo.longtip(info);
	    Resource.Pagina pg = item.res.get().layer(Resource.pagina);
	    if(pg != null)
		img = ItemInfo.catimgs(0, img, RichText.render("\n" + pg.text, 200).img);
	    return(new SpecTip(info, img));
	}

	private Random rnd = null;
	public Random mkrandoom() {
	    if(rnd == null)
		rnd = new Random();
	    return(rnd);
	}
	public Resource getres() {return(display().res.get());}
	public <T> T context(Class<T> cl) {return(ctxr.context(cl, Makewindow.this));}

	public List<ItemInfo> info() {
	    if(info == null)
		info = ItemInfo.buildinfo(this, rawinfo);
	    return(info);
	}
	public Resource resource() {return(item.res.get());}
    }

    public static final KeyBinding kb_make = KeyBinding.get("make/one", KeyMatch.forcode(java.awt.event.KeyEvent.VK_ENTER, 0));
    public static final KeyBinding kb_makeall = KeyBinding.get("make/all", KeyMatch.forcode(java.awt.event.KeyEvent.VK_ENTER, KeyMatch.C));
    ValueEntry amount;
    public Makewindow(String rcpnm) {
	int inputW = add(new Label("Input:"), new Coord(0, UI.scale(8))).sz.x;
	int resultW = add(new Label("Result:"), new Coord(0, outy + UI.scale(8))).sz.x;
	xoff = Math.max(inputW, resultW) + UI.scale(10);
	Coord pos = UI.scale(235, 75);
	pos = add(new Button(UI.scale(65), "Craft"), pos).action(() -> wdgmsg("make", 0)).setgkey(kb_make).pos("ur").adds(5, 0);
	pos = add(new Button(UI.scale(65), "Craft…"), pos).action(this::craftMultiple).setgkey(kb_makeall).pos("ur").adds(5, 1);
	amount = add(new ValueEntry(UI.scale(30), this::craftMultiple), pos);
	amount.clearOnFocus = true;
	amount.update = this::amountChanged;
	pack();
	this.rcpnm = rcpnm;
    }

    private Spec parsespec(Object[] desc) {
	int a = 0;
	Indir<Resource> res = ui.sess.getresv(desc[a++]);
	Message sdt = BYTES.is(desc, a) ? new MessageBuf(BYTES.of(desc, a++)) : MessageBuf.nil;
	int num = INT.of(desc, a++);
	Object[] info = OBJS.is(desc, a) ? OBJS.of(desc, a++) : new Object[0];
	Spec ret = new Spec(new ResData(res, sdt), num, info);
	while(a < desc.length) {
	    Object[] arg = OBJS.of(desc[a++]);
	    switch(STR.of(arg[0])) {
	    case "constraint":
		ret.constraint = new ResData(ui.sess.getresv(arg[1]), Message.nil);
		if(BYTES.is(arg, 2))
		    ret.constraint.sdt = new MessageBuf(BYTES.of(arg, 2));
		break;
	    }
	}
	return(ret);
    }

    public void uimsg(String msg, Object... args) {
	if(msg == "inpop") {
	    List<Spec> inputs;
	    if(INT.is(args, 0)) {
		inputs = Arrays.asList(this.inputs.stream().map(w -> w.spec).toArray(Spec[]::new));
		for(int i = 0; i < args.length; i += 2)
		    inputs.set(INT.of(args, i), parsespec(OBJS.of(args, i + 1)));
	    } else {
		inputs = new ArrayList<>();
		for(int i = 0; i < args.length; i++)
		    inputs.add(parsespec(OBJS.of(args[i])));
	    }
	    List<Input> wdgs = new ArrayList<>();
	    int idx = 0;
	    for(Spec spec : inputs)
		wdgs.add(new Input(spec, idx++));
	    synchronized(ui) {
		for(Widget w : this.inputs)
		    w.destroy();
		Position pos = new Position(xoff, 0);
		SpecWidget prev = null;
		for(Input wdg : wdgs) {
		    if((prev != null) && (wdg.opt != false))
			pos = pos.adds(10, 0);
		    add(wdg, pos);
		    pos = pos.add(Inventory.sqsz.x, 0);
		    prev = wdg;
		}
		this.inputs = wdgs;
	    }
	} else if(msg == "opop") {
	    List<Spec> outputs;
	    if(INT.is(args, 0)) {
		outputs = Arrays.asList(this.outputs.stream().map(w -> w.spec).toArray(Spec[]::new));
		for(int i = 0; i < args.length; i += 2)
		    outputs.set(INT.of(args, i), parsespec(OBJS.of(args, i + 1)));
	    } else {
		outputs = new ArrayList<>();
		for(int i = 0; i < args.length; i++)
		    outputs.add(parsespec(OBJS.of(args[i])));
	    }
	    List<SpecWidget> wdgs = new ArrayList<>();
	    for(Spec spec : outputs)
		wdgs.add(new SpecWidget(spec));
	    synchronized(ui) {
		for(Widget w : this.outputs)
		    w.destroy();
		Position pos = new Position(xoff, outy);
		SpecWidget prev = null;
		for(SpecWidget wdg : wdgs) {
		    if((prev != null) && (wdg.opt != prev.opt))
			pos = pos.adds(10, 0);
		    add(wdg, pos);
		    pos = pos.add(Inventory.sqsz.x, 0);
		    prev = wdg;
		}
		this.outputs = wdgs;
	    }
	} else if(msg == "qmod") {
	    List<Indir<Resource>> qmod = new ArrayList<Indir<Resource>>();
	    for(Object arg : args)
		qmod.add(ui.sess.getresv(arg));
	    this.qmod = qmod;
	} else if(msg == "tool") {
	    tools.add(ui.sess.getresv(args[0]));
	} else if(msg == "use") {
	    inputs.get(INT.of(args[0])).using(INT.of(args[1]));
	} else if(msg == "inprcps") {
	    int idx = INT.of(args[0]);
	    List<MenuGrid.Pagina> rcps = new ArrayList<>();
	    GameUI gui = getparent(GameUI.class);
	    if((gui != null) && (gui.menu != null)) {
		for(int a = 1; a < args.length; a++)
		    rcps.add(gui.menu.paginafor(ui.sess.getresv(args[a])));
	    }
	    inputs.get(idx).recipes(rcps);
	} else {
	    super.uimsg(msg, args);
	}
    }

    public static final Coord qmodsz = UI.scale(20, 20);
    private static final Map<Indir<Resource>, Tex> qmicons = new WeakHashMap<>();
    private Tex qmicon(Indir<Resource> qm) {
        synchronized (qmicons) {
	    return qmicons.computeIfAbsent(qm, Makewindow.this::buildQTex);
	}
    }

    public static class SpecWidget extends Widget {
	public final Spec spec;
	public final boolean opt;
	public Tex num;

	public SpecWidget(Spec spec) {
	    super(invsq.sz());
	    this.spec = spec;
	    opt = spec.opt();
	    if(spec.num >= 0)
		this.num = new TexI(Utils.outline2(Text.render(Integer.toString(spec.num), Color.WHITE).img, Utils.contrast(Color.WHITE)));
	    else
		this.num = null;
	}

	public List<ItemInfo> info() {return(spec.info());}

	public void drawbg(GOut g) {
	    if(opt) {
		g.chcolor(0, 255, 0, 255);
		g.image(invsq, Coord.z);
		g.chcolor();
	    } else {
		g.image(invsq, Coord.z);
	    }
	}

	public final ItemInfo.AttrCache<Pipe.Op> rstate = new ItemInfo.AttrCache<>(this::info, GItem.RStateInfo.combine);
	public void drawicon(GOut g) {
	    if(rstate.get() != null)
		g.usestate(rstate.get());
	    spec.draw(g);
	    g.defstate();
	    if(num != null)
		g.aimage(num, Inventory.sqsz, 1.0, 1.0);
	}

	public void draw(GOut g) {
	    drawbg(g);
	    drawicon(g);
	}

	private double hoverstart;
	Object stip, ltip;
	public Object tooltip(Coord c, Widget prev) {
	    double now = Utils.rtime();
	    if(prev == this) {
	    } else if(prev instanceof SpecWidget) {
		double ps = ((SpecWidget)prev).hoverstart;
		hoverstart = (now - ps < 1.0) ? now : ps;
	    } else {
		hoverstart = now;
	    }
	    if(now - hoverstart < 1.0) {
		if(stip == null)
		    stip = spec.shorttip();
		return(stip);
	    } else {
		if(ltip == null)
		    ltip = spec.longtip();
		return(ltip);
	    }
	}

	public void tick(double dt) {
	    super.tick(dt);
	    if(spec.spr != null)
		spec.spr.tick(dt);
	}
    }

    public class Input extends SpecWidget implements DTarget {
	public final int idx;
	public int using = 0;
	private List<MenuGrid.Pagina> rpag = null;
	private Coord cc = null;

	public Input(Spec spec, int idx) {
	    super(spec);
	    this.idx = idx;
	}

	public void drawbg(GOut g) {
	    super.drawbg(g);
	    if(!opt && (using < spec.num)) {
		g.chcolor(255, 0, 0, 64);
		g.frect2(Coord.of(0, (sz.y * using) / spec.num), sz);
		g.chcolor();
	    }
	}

	public boolean mousedown(MouseDownEvent ev) {
	    if(ev.b == 1) {
		Makewindow.this.wdgmsg("choose", idx, ui.modflags());
		return(true);
	    } else if(ev.b == 3) {
		if(rpag == null)
		    Makewindow.this.wdgmsg("findrcps", idx);
		this.cc = ev.c;
		return(true);
	    }
	    return(super.mousedown(ev));
	}

	public void tick(double dt) {
	    super.tick(dt);
	    if((cc != null) && (rpag != null)) {
		if(!rpag.isEmpty()) {
		    SListMenu.of(UI.scale(250, 120), rpag,
				 pag -> pag.button().name(), pag -> pag.button().img(),
			    pag -> {
				pag.button().use(new MenuGrid.Interaction(1, ui.modflags()));
				CraftDBWnd db = getparent(CraftDBWnd.class);
				if(db != null) {db.select(pag, false);}
			    })
			.addat(this, cc.add(UI.scale(5, 5))).tick(dt);
		}
		cc = null;
	    }
	}

	public boolean drop(Coord cc, Coord ul) {
	    Makewindow.this.wdgmsg("itemact", idx, ui.modflags());
	    return(true);
	}

	public boolean iteminteract(Coord cc, Coord ul) {
	    Makewindow.this.wdgmsg("itemact", idx, ui.modflags());
	    return(true);
	}

	public void recipes(List<MenuGrid.Pagina> pag) {
	    rpag = pag;
	}

	public void using(int a) {
	    using = a;
	}
    }

    public void draw(GOut g) {
	int x = 0;
	if(!qmod.isEmpty()) {
	    g.aimage(qmodl.tex(), new Coord(x, qmy + (qmodsz.y / 2)), 0, 0.5);
	    x += qmodl.sz().x + UI.scale(5);
	    x = Math.max(x, xoff);
	    qmx = x;
	    int count = 0;
	    double product = 1.0;
	    for(Indir<Resource> qm : qmod) {
		try {
		    Tex t = qmicon(qm);
		    g.image(t, new Coord(x, qmy));
		    x += t.sz().x + UI.scale(1);
		    
		    Glob.CAttr attr = ui.gui.chrwdg.findattr(qm.get().basename());
		    if(attr != null) {
			count++;
			product = product * attr.comp;
		    }
		} catch(Loading l) {
		}
	    }
	    if(count > 0) {
		x += drawSoftcap(g, new Coord(x, qmy), product, count);
	    }
	    x += UI.scale(25);
	}
	if(!tools.isEmpty()) {
	    g.aimage(tooll.tex(), new Coord(x, qmy + (qmodsz.y / 2)), 0, 0.5);
	    x += tooll.sz().x + UI.scale(5);
	    x = Math.max(x, xoff);
	    toolx = x;
	    for(Indir<Resource> tool : tools) {
		try {
		    Tex t = qmicon(tool);
		    g.image(t, new Coord(x, qmy));
		    x += t.sz().x + UI.scale(1);
		} catch(Loading l) {
		}
	    }
	    x += UI.scale(25);
	}
	super.draw(g);
    }
    
    private int drawSoftcap(GOut g, Coord p, double product, int count) {
	if(count > 0) {
	    double current = Math.pow(product, 1.0 / count);
	    if(current != softcap || softTex == null) {
		softcap = current;
		String format = String.format("%s %.1f", L10N.label("Softcap:"), softcap);
		Text txt = Text.renderstroked(format, Color.WHITE, Color.BLACK, Glob.CAttr.fnd);
		if(softTex != null) {
		    softTex.dispose();
		}
		softTex = new TexI(txt.img);
	    }
	    g.image(softTex, p.add(UI.scale(5), 0));
	    return softTex.sz().x + UI.scale(6);
	}
	return 0;
    }
    
    private Tex buildQTex(Indir<Resource> res) {
	BufferedImage result = PUtils.convolve(res.get().layer(Resource.imgc).img, qmodsz, CharWnd.iconfilter);
	try {
	    Glob.CAttr attr = ui.gui.chrwdg.findattr(res.get().basename());
	    if(attr != null) {
		result = ItemInfo.catimgsh(1, result, attr.compline().img);
	    }
	} catch (Exception ignored) {
	}
	return new TexI(result);
    }
    
    public static void invalidate(String name) {
	synchronized (qmicons) {
	    LinkedList<Indir<Resource>> tmp = new LinkedList<>(qmicons.keySet());
	    tmp.forEach(res -> {
		if(name.equals(res.get().basename())) {
		    qmicons.remove(res);
		}
	    });
	}
    }
    
    private int qmx, toolx;
    public Object tooltip(Coord mc, Widget prev) {
	String name = null;
	Spec tspec = null;
	Coord c;
	if(!qmod.isEmpty()) {
	    c = new Coord(qmx, qmy);
	    for(Indir<Resource> qm : qmod) {
		Tex t = qmicon(qm);
		Coord sz = t.sz();
		if(mc.isect(c, sz))
		    return(qm.get().flayer(Resource.tooltip).t);
		c = c.add(sz.x + UI.scale(1), 0);
	    }
	}
	if(!tools.isEmpty()) {
	    c = new Coord(toolx, qmy);
	    for(Indir<Resource> tool : tools) {
		Coord tsz = qmicon(tool).sz();
		if(mc.isect(c, tsz))
		    return(tool.get().flayer(Resource.tooltip).t);
		c = c.add(tsz.x + UI.scale(1), 0);
	    }
	}
	return(super.tooltip(mc, prev));
    }

    private static String getDynamicName(GSprite spr) {
	if(spr != null) {
	    Class<? extends GSprite> sprClass = spr.getClass();
	    if(Reflect.hasInterface("haven.res.ui.tt.defn.DynName", sprClass)) {
		return (String) Reflect.invoke(spr, "name");
	    }
	}
	return null;
    }

    public boolean globtype(GlobKeyEvent ev) {
	if(ev.c == '\n') {
	    wdgmsg("make", ui.modctrl?1:0);
	    return(true);
	}
	return(super.globtype(ev));
    }

    public static class Optional extends ItemInfo.Tip {
	public static final Text text = RichText.render(String.format("$i{%s}", L10N.label("Optional")), 0);
	public Optional(Owner owner) {
	    super(owner);
	}

	public BufferedImage tipimg() {
	    return(text.img);
	}

	public Tip shortvar() {return(this);}
    }

    @Override
    protected void added() {
	super.added();
	Window wnd;
	if((wnd = getparent(Window.class)) instanceof ICraftParent) {
	    amount.value(((ICraftParent) wnd).getCraftAmount());
	}
    }

    private void amountChanged() {
	Window wnd;
	if((wnd = getparent(Window.class)) instanceof ICraftParent) {
	    ((ICraftParent) wnd).setCraftAmount(amount.value());
	}
    }

    private void craftMultiple() {
	int count = amount.value();
	if(count <= 0) {
	    wdgmsg("make", 1);
	} else {
	    Actions.craftCount(this, count);
	}
    }
}
