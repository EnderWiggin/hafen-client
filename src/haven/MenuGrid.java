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

import haven.ItemInfo.AttrCache;
import haven.Resource.AButton;
import haven.render.Pipe;
import haven.rx.BuffToggles;
import me.ender.CustomPagina;
import me.ender.CustomPaginaAction;
import me.ender.GobInfoOpts;
import me.ender.GobInfoOpts.InfoPart;
import me.ender.minimap.Minesweeper;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.font.TextAttribute;
import java.awt.image.BufferedImage;
import java.util.List;
import java.util.*;
import java.util.function.Supplier;
import java.util.stream.Collectors;


public class MenuGrid extends Widget implements KeyBinding.Bindable {
    public final static Tex bg = Inventory.invsq;
    public final static Coord bgsz = Inventory.sqsz;
    public final static RichText.Foundry ttfnd = new RichText.Foundry(TextAttribute.FAMILY, "SansSerif", TextAttribute.SIZE, UI.scale(10f));
    private static Coord gsz = new Coord(4, 4);
    public final Set<Pagina> paginae = new HashSet<Pagina>();
    public Pagina cur;
    private final Map<Object, Pagina> pmap = new CacheMap<>(CacheMap.RefType.WEAK);
    public int pagseq = 0;
    private Pagina dragging;
    private Collection<PagButton> curbtns = Collections.emptyList();
    private PagButton pressed, layout[][] = new PagButton[gsz.x][gsz.y];
    private UI.Grab grab;
    private int curoff = 0;
    private boolean recons = true, showkeys = false;
    private double fstart;
    private Map<Character, PagButton> hotmap = new HashMap<>();
    public Pagina lastCraft = null;

    @RName("scm")
    public static class $_ implements Factory {
	public Widget create(UI ui, Object[] args) {
	    return(new MenuGrid());
	}
    }

    public static class Pagina {
	public final MenuGrid scm;
	public final Object id;
	public Indir<Resource> res;
	public byte[] sdt = null;
	public int anew, tnew;
	public Object[] rawinfo = {};

	public Pagina(MenuGrid scm, Object id, Indir<Resource> res) {
	    this.scm = scm;
	    this.id = id;
	    this.res = res;
	}
	public boolean isAction() {
	    Resource.AButton act = button().acts();
	    if(act == null) {return false;}
	    String[] ad = act.ad;
	    return ad != null && ad.length > 0;
	}
	
	public static String resname(Pagina p) {
	    String name = "";
	    if(p instanceof CustomPagina) {
		return ((CustomPagina) p).resName;
	    }
	    if(p.res instanceof Resource.Named) {
		name = ((Resource.Named) p.res).name;
	    } else {
		try {
		    name = p.res.get().name;
		} catch (Loading ignored) {}
	    }
	    return name;
	}
	
	public Resource res() {
	    return(res.get());
	}

	public Message data() {
	    return((sdt == null) ? Message.nil : new MessageBuf(sdt));
	}

	private void invalidate() {
	    button = null;
	}

	private PagButton button = null;
	public PagButton button() {
	    if(button == null) {
		Resource res = res();
		PagButton.Factory f = res.getcode(PagButton.Factory.class, false);
		if(f == null)
		    button = new PagButton(this);
		else
		    button = f.make(this);
	    }
	    return(button);
	}
	
	public void button(PagButton btn) {button = btn;}
	

	public Pagina parent() {
	    return(button().parent());
	}
    }

    public static class Interaction {
	public final int btn, modflags;
	public final Coord2d mc;
	public final ClickData click;

	public Interaction(int btn, int modflags, Coord2d mc, ClickData click) {
	    this.btn = btn;
	    this.modflags = modflags;
	    this.mc = mc;
	    this.click = click;
	}

	public Interaction(int btn, int modflags) {
	    this(btn, modflags, null, null);
	}

	public Interaction() {
	    this(1, 0);
	}
    }

    public static class PagButton implements ItemInfo.Owner, GSprite.Owner, RandomSource {
	public final Pagina pag;
	public final Resource res;
	public final KeyBinding bind;
	private GSprite spr;
	private AButton act;

	public PagButton(Pagina pag) {
	    this.pag = pag;
	    this.res = pag.res();
	    this.bind = binding();
	}

	public AButton act() {
	    if(act == null)
		act = res.flayer(Resource.action);
	    return(act);
	}
	
	public AButton acts() {
	    if(act == null)
		act = res.layer(Resource.action);
	    return(act);
	}

	private Pagina parent;
	public Pagina parent() {
	    if(parent == null)
		parent = pag.scm.paginafor(act().parent);
	    return(parent);
	}

	public GSprite spr() {
	    if(spr == null)
		spr = GSprite.create(this, res, Message.nil);
	    return(spr);
	}
	public String name() {return(act().name);}
	public String originalName() {return(res.flayer(Resource.action).original);}
	public KeyMatch hotkey() {
	    char hk = act().hk;
	    if(hk == 0)
		return(KeyMatch.nil);
	    //FIXME: I have no idea why exactly upper case keybinds continue to work properly.
	    return KeyMatch.forcode(KeyStroke.getKeyStroke(Character.toUpperCase(hk), 0).getKeyCode(), KeyMatch.MODS, 0);
	}
	public KeyBinding binding() {
	    return(KeyBinding.get("scm/" + res.name, hotkey()));
	}
	public void use() {
	    if(pag.scm.isCrafting(pag)) {
		pag.scm.lastCraft = pag;
	    }
	    pag.scm.wdgmsg("act", (Object[])res.flayer(Resource.action).ad);
	}
	public void use(Interaction iact) {
	    Object[] eact = new Object[] {pag.scm.ui.modflags()};
	    if(iact.mc != null) {
		eact = Utils.extend(eact, iact.mc.floor(OCache.posres));
		if(iact.click != null)
		    eact = Utils.extend(eact, iact.click.clickargs());
	    }
	    if(pag.id instanceof Indir)
		pag.scm.wdgmsg("act", Utils.extend(Utils.extend(new Object[0], act().ad), eact));
	    else
		pag.scm.wdgmsg("use", Utils.extend(new Object[] {pag.id}, eact));
	    if(pag.scm.isCrafting(pag)) {
		pag.scm.lastCraft = pag;
	    }
	}
	public void tick(double dt) {
	    if(spr != null)
		spr.tick(dt);
	}

	public BufferedImage img() {
	    GSprite spr = spr();
	    if(spr instanceof GSprite.ImageSprite)
		return(((GSprite.ImageSprite)spr).image());
	    return(null);
	}

	public final AttrCache<Pipe.Op> rstate = new AttrCache<>(this::info, info -> {
		ArrayList<GItem.RStateInfo> ols = new ArrayList<>();
		for(ItemInfo inf : info) {
		    if(inf instanceof GItem.RStateInfo)
			ols.add((GItem.RStateInfo)inf);
		}
		if(ols.size() == 0)
		    return(() -> null);
		if(ols.size() == 1) {
		    Pipe.Op op = ols.get(0).rstate();
		    return(() -> op);
		}
		Pipe.Op[] ops = new Pipe.Op[ols.size()];
		for(int i = 0; i < ops.length; i++)
		    ops[i] = ols.get(0).rstate();
		Pipe.Op cmp = Pipe.Op.compose(ops);
		return(() -> cmp);
	});
	public final AttrCache<GItem.InfoOverlay<?>[]> ols = new AttrCache<>(this::info, info -> {
		ArrayList<GItem.InfoOverlay<?>> buf = new ArrayList<>();
		for(ItemInfo inf : info) {
		    if(inf instanceof GItem.OverlayInfo)
			buf.add(GItem.InfoOverlay.create((GItem.OverlayInfo<?>)inf));
		}
		GItem.InfoOverlay<?>[] ret = buf.toArray(new GItem.InfoOverlay<?>[0]);
		return(() -> ret);
	});
	public final AttrCache<Double> meter = new AttrCache<>(this::info, AttrCache.map1(GItem.MeterInfo.class, minf -> minf::meter));

	public void drawmain(GOut g, GSprite spr) {
	    spr.draw(g);
	}
	public void draw(GOut g, GSprite spr) {
	    if(rstate.get() != null)
		g.usestate(rstate.get());
	    drawmain(g, spr);
	    g.defstate();
	    GItem.InfoOverlay<?>[] ols = this.ols.get();
	    if(ols != null) {
		for(GItem.InfoOverlay<?> ol : ols)
		    ol.draw(g);
	    }
	    Double meter = this.meter.get();
	    if((meter != null) && (meter > 0)) {
		g.chcolor(255, 255, 255, 64);
		Coord half = spr.sz().div(2);
		g.prect(half, half.inv(), half, meter * Math.PI * 2);
		g.chcolor();
	    }
	}

	public String sortkey() {
	    if((act().ad.length == 0) && (pag.id instanceof Indir))
		return("\0" + originalName());
	    return(originalName());
	}

	private char bindchr(KeyMatch key) {
	    if(key.modmatch != 0)
		return(0);
	    char vkey = key.chr;
	    if((vkey == 0) && (key.keyname.length() == 1))
		vkey = key.keyname.charAt(0);
	    return(vkey);
	}

	public static final Text.Foundry keyfnd = new Text.Foundry(Text.sans.deriveFont(Font.BOLD), 10);
	private Tex keyrend = null;
	private boolean haskeyrend = false;
	public Tex keyrend() {
	    if(!haskeyrend) {
		char vkey = bindchr(bind.key());
		if(vkey != 0)
		    keyrend = new TexI(Utils.outline2(keyfnd.render(Character.toString(vkey), Color.WHITE).img, Color.BLACK));
		else
		    keyrend = null;
		haskeyrend = true;
	    }
	    return(keyrend);
	}

	private List<ItemInfo> info = null;
	public List<ItemInfo> info() {
	    if(info == null) {
		info = ItemInfo.buildinfo(this, pag.rawinfo);
		Resource.Pagina pg = res.layer(Resource.pagina);
		if(pg != null)
		    info.add(new ItemInfo.Pagina(this, pg.text));
	    }
	    return(info);
	}
	private static final OwnerContext.ClassResolver<PagButton> ctxr = new OwnerContext.ClassResolver<PagButton>()
	    .add(PagButton.class, p -> p)
	    .add(MenuGrid.class, p -> p.pag.scm)
	    .add(Glob.class, p -> p.pag.scm.ui.sess.glob)
	    .add(UI.class, p -> p.pag.scm.ui)
	    .add(Session.class, p -> p.pag.scm.ui.sess);
	public <T> T context(Class<T> cl) {return(ctxr.context(cl, this));}
	public Random mkrandoom() {return(new Random());}
	public Resource getres() {return(res);}

	public BufferedImage rendertt(boolean withpg) {
	    String tt = name();
	    KeyMatch key = bind.key();
	    int pos = -1;
	    char vkey = bindchr(key);
	    if((vkey != 0) && (key.modmatch == 0))
		pos = tt.toUpperCase().indexOf(Character.toUpperCase(vkey));
	    if(pos >= 0)
		tt = tt.substring(0, pos) + "$b{$col[255,128,0]{" + tt.charAt(pos) + "}}" + tt.substring(pos + 1);
	    else if(key != KeyMatch.nil)
		tt += " [$b{$col[255,128,0]{" + key.name() + "}}]";

	    BufferedImage ret = ttfnd.render(tt, UI.scale(300)).img;
	    if(withpg) {
		List<ItemInfo> info = info();
		info.removeIf(el -> el instanceof ItemInfo.Name);
		if(!info.isEmpty())
		    ret = ItemInfo.catimgs(0, ret, ItemInfo.longtip(info));
	    }
	    return(ret);
	}

	public static class FactMaker extends Resource.PublishedCode.Instancer.Chain<Factory> {
	    public FactMaker() {
		super(Factory.class);
		add(new Direct<>(Factory.class));
		add(new StaticCall<>(Factory.class, "mkpagina", PagButton.class, new Class<?>[] {Pagina.class},
				     (make) -> (pagina) -> make.apply(new Object[] {pagina})));
		add(new Construct<>(Factory.class, PagButton.class, new Class<?>[] {Pagina.class},
				    (cons) -> (pagina) -> cons.apply(new Object[] {pagina})));
	    }
	}

	@Resource.PublishedCode(name = "pagina", instancer = FactMaker.class)
	public interface Factory {
	    public PagButton make(Pagina info);
	}
    }

    public final PagButton next = new PagButton(new Pagina(this, null, Resource.local().loadwait("gfx/hud/sc-next").indir())) {
	    {pag.button = this;}

	    public void use(Interaction iact) {
		int step = (gsz.x * gsz.y) - 2;
		if((curoff + step) >= curbtns.size())
		    curoff = 0;
		else
		    curoff += step;
		updlayout();
	    }

	    public String name() {return("More...");}

	    public KeyBinding binding() {return(kb_next);}
	};

    public final PagButton bk = new PagButton(new Pagina(this, null, Resource.local().loadwait("gfx/hud/sc-back").indir())) {
	    {pag.button = this;}

	    public void use(Interaction iact) {
		Pagina dst = pag.scm.cur.parent();
		pag.scm.change(dst);
		curoff = 0;
		selectCraft(dst);
	    }

	    public String name() {return("Back");}

	    public KeyBinding binding() {return(kb_back);}
	};

    public Pagina paginafor(Indir<Resource> res) {
	if(res == null)
	    return(null);
	synchronized(pmap) {
	    Pagina p = pmap.get(res);
	    if(p == null)
		pmap.put(res, p = new Pagina(this, res, res));
	    return(p);
	}
    }

    public Pagina paginafor(Object id, Indir<Resource> res) {
	synchronized(pmap) {
	    Pagina p = pmap.get(id);
	    if((p == null) && (res != null))
		pmap.put(id, p = new Pagina(this, id, res));
	    return(p);
	}
    }
    
    public Pagina paginafor(String name) {
	return paginafor(Resource.remote().load(name));
    }
    
    public boolean consb(Pagina p, Collection<PagButton> buf) {
        List<Pagina> pags = buf.stream().map(btn -> btn.pag).collect(Collectors.toList());
        boolean result = cons(p, pags);
        buf.clear();
        for(Pagina pag : pags) { buf.add(pag.button());	}
        return result;
    }
    
    public boolean cons(Pagina p, Collection<Pagina> buf) {
	Pagina[] cp = new Pagina[0];
	Collection<Pagina> open, close = new HashSet<Pagina>();
	synchronized(pmap) {
	    for(Pagina pag : pmap.values())
		pag.tnew = 0;
	}
	synchronized(paginae) {
	    open = new LinkedList<Pagina>();
	    for(Pagina pag : paginae) {
		open.add(pag);
		if(pag.anew > 0) {
		    try {
			for(Pagina npag = pag; npag != null; npag = npag.parent())
			    npag.tnew = Math.max(npag.tnew, pag.anew);
		    } catch(Loading l) {
		    }
		}
	    }
	}
	boolean ret = true;
	while(!open.isEmpty()) {
	    Iterator<Pagina> iter = open.iterator();
	    Pagina pag = iter.next();
	    iter.remove();
	    try {
		Pagina parent = pag.parent();
		if(parent == p)
		    buf.add(pag);
		else if((parent != null) && !close.contains(parent) && !open.contains(parent))
		    open.add(parent);
		close.add(pag);
	    } catch(Loading e) {
		ret = false;
	    }
	}
	return(ret);
    }

    private void announce(Pagina pag) {
	ui.loader.defer(() -> ui.msg("New discovery: " + pag.button().name(), Color.WHITE, null), null);
    }

    public MenuGrid() {
	super(bgsz.mul(gsz).add(1, 1));
	initCustomPaginae();
    }

    private void updlayout() {
	synchronized(paginae) {
	    List<PagButton> cur = new ArrayList<>();
	    recons = !consb(this.cur, cur);
	    Collections.sort(cur, Comparator.comparing(PagButton::sortkey));
	    this.curbtns = cur;
	    int i = curoff;
	    for(int y = 0; y < gsz.y; y++) {
		for(int x = 0; x < gsz.x; x++) {
		    PagButton btn = null;
		    if((this.cur != null) && (x == gsz.x - 1) && (y == gsz.y - 1)) {
			btn = bk;
		    } else if((cur.size() > ((gsz.x * gsz.y) - 1)) && (x == gsz.x - 2) && (y == gsz.y - 1)) {
			btn = next;
		    } else if(i < cur.size()) {
			btn = cur.get(i++);
		    }
		    layout[x][y] = btn;
		}
	    }
	    fstart = Utils.rtime();
	}
    }
    
    private static BufferedImage rendertt(Pagina pag, boolean withpg) {
	return rendertt(pag, withpg, true, false);
    }
    
    public static BufferedImage rendertt(Pagina pag, boolean withpg, boolean hotkey, boolean caption) {
	Resource.AButton ad = pag.res.get().layer(Resource.action);
	Resource.Pagina pg = pag.res.get().layer(Resource.pagina);
	String tt = ad.name;
	if(hotkey) {
	    int pos = tt.toUpperCase().indexOf(Character.toUpperCase(ad.hk));
	    if(pos >= 0)
		tt = tt.substring(0, pos) + "$b{$col[255,128,0]{" + tt.charAt(pos) + "}}" + tt.substring(pos + 1);
	    else if(ad.hk != 0)
		tt += " [" + ad.hk + "]";
	}
	if(caption){
	    tt = String.format("$b{$size[14]{%s}}", tt);
	}
	BufferedImage ret = ttfnd.render(tt, 300).img;
	if(withpg) {
	    List<ItemInfo> info = pag.button().info();
	    info.removeIf(el -> el instanceof ItemInfo.Name);
	    if(!info.isEmpty())
		ret = ItemInfo.catimgs(0, ret, ItemInfo.longtip(info));
	    if(pg != null)
		ret = ItemInfo.catimgs(0, ret, ttfnd.render("\n" + pg.text, 200).img);
	}
	return(ret);
    }

    public void draw(GOut g) {
	double now = Utils.rtime();
	for(int y = 0; y < gsz.y; y++) {
	    for(int x = 0; x < gsz.x; x++) {
		Coord p = bgsz.mul(new Coord(x, y));
		g.image(bg, p);
		PagButton btn = layout[x][y];
		if(btn != null) {
		    GSprite spr;
		    try {
			spr = btn.spr();
		    } catch(Loading l) {
			continue;
		    }
		    GOut g2 = g.reclip(p.add(1, 1), spr.sz());
		    Pagina info = btn.pag;
		    if(info.tnew != 0) {
			info.anew = 1;
			double a = 0.25;
			if(info.tnew == 2) {
			    double ph = (now - fstart) - (((x + (y * gsz.x)) * 0.15) % 1.0);
			    a = (ph < 1.25) ? (Math.cos(ph * Math.PI * 2) * -0.25) + 0.25 : 0.25;
			}
			g2.usestate(new ColorMask(new FColor(0.125f, 1.0f, 0.125f, (float)a)));
		    }
		    btn.draw(g2, spr);
		    g2.defstate();
		    if(showkeys) {
			Tex ki = btn.keyrend();
			if(ki != null)
			    g2.aimage(ki, Coord.of(bgsz.x - UI.scale(2), UI.scale(1)), 1.0, 0.0);
		    }
		    if(btn == pressed) {
			g.chcolor(new Color(0, 0, 0, 128));
			g.frect(p.add(1, 1), bgsz.sub(1, 1));
			g.chcolor();
		    }
		}
	    }
	}
	super.draw(g);
	if(dragging != null) {
	    GSprite ds = dragging.button().spr();
	    ui.drawafter(new UI.AfterDraw() {
		    public void draw(GOut g) {
			ds.draw(g.reclip(ui.mc.sub(ds.sz().div(2)), ds.sz()));
		    }
		});
	}
    }

    private PagButton curttp = null;
    private boolean curttl = false;
    private Tex curtt = null;
    private double hoverstart;
    public Object tooltip(Coord c, Widget prev) {
	PagButton pag = bhit(c);
	double now = Utils.rtime();
	if(pag != null) {
	    if(prev != this)
		hoverstart = now;
	    boolean ttl = (now - hoverstart) > 0.5;
	    if((pag != curttp) || (ttl != curttl)) {
		BufferedImage ti = pag.rendertt(ttl);
		curtt = (ti == null) ? null : new TexI(ti);
		curttp = pag;
		curttl = ttl;
	    }
	    return(curtt);
	} else {
	    hoverstart = now;
	    return(null);
	}
    }

    private PagButton bhit(Coord c) {
	Coord bc = c.div(bgsz);
	if((bc.x >= 0) && (bc.y >= 0) && (bc.x < gsz.x) && (bc.y < gsz.y))
	    return(layout[bc.x][bc.y]);
	else
	    return(null);
    }

    public boolean mousedown(MouseDownEvent ev) {
	PagButton h = bhit(ev.c);
	if((ev.b == 1) && (h != null)) {
	    pressed = h;
	    grab = ui.grabmouse(this);
	}
	return(true);
    }

    public void mousemove(MouseMoveEvent ev) {
	if((dragging == null) && (pressed != null)) {
	    PagButton h = bhit(ev.c);
	    if(h != pressed)
		dragging = pressed.pag;
	}
    }

    public void change(Pagina dst) {
	this.cur = dst;
	curoff = 0;
	if(dst == null)
	    showkeys = false;
	updlayout();
    }
    
    public void use(Pagina p, boolean reset) {
	if(p != null) { use(p.button(), new Interaction(), reset); }
    }

    public void use(PagButton r, Interaction iact, boolean reset) {
	Collection<PagButton> sub = new ArrayList<>();
	consb(r.pag, sub);
	selectCraft(r.pag);
	if(sub.size() > 0) {
	    change(r.pag);
	} else {
	    r.pag.anew = r.pag.tnew = 0;
	    r.use(iact);
	    if(reset)
		change(null);
	}
    }

    public void senduse(String... ad) {
	wdgmsg("act", (Object[]) ad);
    }

    public void tick(double dt) {
	if(recons)
	    updlayout();
	for(int y = 0; y < gsz.y; y++) {
	    for(int x = 0; x < gsz.x; x++) {
		if(layout[x][y] != null)
		    layout[x][y].tick(dt);
	    }
	}
    }

    public boolean mouseup(MouseUpEvent ev) {
	PagButton h = bhit(ev.c);
	if((ev.b == 1) && (grab != null)) {
	    if(dragging != null) {
		DropTarget.dropthing(ui.root, ui.mc, dragging);
		pressed = null;
		dragging = null;
	    } else if(pressed != null) {
		if(pressed == h)
		    use(h, new Interaction(1, ui.modflags()), false);
		pressed = null;
	    }
	    grab.remove();
	    grab = null;
	}
	return(true);
    }

    public void uimsg(String msg, Object... args) {
	if(msg == "goto") {
	    if(args[0] == null)
		change(null);
	    else
		change(paginafor(ui.sess.getresv(args[0])));
	} else if(msg == "fill") {
	    synchronized(paginae) {
		int a = 0;
		while(a < args.length) {
		    int fl = Utils.iv(args[a++]);
		    Pagina pag;
		    Object id;
		    if((fl & 2) != 0)
			pag = paginafor(id = args[a++], null);
		    else
			id = (pag = paginafor(ui.sess.getres(Utils.iv(args[a++]), -2))).res;
		    if((fl & 1) != 0) {
			if((fl & 2) != 0) {
			    Indir<Resource> res = ui.sess.getres(Utils.iv(args[a++]), -2);
			    if(pag == null) {
				pag = paginafor(id, res);
			    } else if(pag.res != res) {
				pag.res = res;
				pag.invalidate();
			    }
			}
			byte[] data = ((fl & 4) != 0) ? (byte[])args[a++] : null;
			if(!Arrays.equals(pag.sdt, data)) {
			    pag.sdt = data;
			    pag.invalidate();
			}
			if((fl & 8) != 0) {
			    pag.anew = 2;
			    announce(pag);
			}
			Object[] rawinfo = ((fl & 16) != 0) ? (Object[])args[a++] : new Object[0];
			if(!Arrays.deepEquals(pag.rawinfo, rawinfo)) {
			    pag.rawinfo = rawinfo;
			    pag.invalidate();
			}
			paginae.add(pag);
		    } else {
			paginae.remove(pag);
		    }
		}
		updlayout();
	    	pagseq++;
	    }
	} else {
	    super.uimsg(msg, args);
	}
    }

    public static final KeyBinding kb_root = KeyBinding.get("scm-root", KeyMatch.forcode(KeyEvent.VK_ESCAPE, 0));
    public static final KeyBinding kb_back = KeyBinding.get("scm-back", KeyMatch.forcode(KeyEvent.VK_BACK_SPACE, 0));
    public static final KeyBinding kb_next = KeyBinding.get("scm-next", KeyMatch.forchar('N', KeyMatch.S | KeyMatch.C | KeyMatch.M, KeyMatch.S));
    public boolean globtype(GlobKeyEvent ev) {
	if(kb_root.key().match(ev) && (this.cur != null)) {
	    change(null);
	    return(true);
	} else if(kb_back.key().match(ev) && (this.cur != null)) {
	    use(bk, new Interaction(), false);
	    return(true);
	} else if(kb_next.key().match(ev) && (layout[gsz.x - 2][gsz.y - 1] == next)) {
	    use(next, new Interaction(), false);
	    return(true);
	}
	int cp = -1;
	PagButton pag = null;
	for(PagButton btn : curbtns) {
	    if(btn.bind.key().match(ev)) {
		int prio = btn.bind.set() ? 1 : 0;
		if((pag == null) || (prio > cp)) {
		    pag = btn;
		    cp = prio;
		}
	    }
	}
	if(pag != null) {
	    use(pag, new Interaction(), (ev.mods & KeyMatch.S) == 0);
	    if(this.cur != null)
		showkeys = true;
	    return(true);
	}
	return(super.globtype(ev));
    }

    private void selectCraft(Pagina r) {
	if(r == null){
	    return;
	}
	if(ui.gui.craftwnd != null){
	    ui.gui.craftwnd.select(r, true);
	}
    }

    public boolean isCrafting(Pagina p) {
	return (p != null) && (Pagina.resname(p).startsWith("paginae/craft/") || isCrafting(p.res()) || isCrafting(getParent(p)));
    }

    public boolean isCrafting(Resource res){
	return res.name.contains("paginae/act/craft");
    }

    public Pagina getParent(Pagina p){
	if(p == null){
	    return null;
	}
	try {
	    Resource res = p.res();
	    Resource.AButton ad = res.layer(Resource.action);
	    if (ad == null)
		return null;
	    Pagina parent = paginafor(ad.parent);
	    return (parent == p) ? null : parent;
	} catch (Loading e){
	    return null;
	}
    }

    public boolean isChildOf(Pagina item, Pagina parent) {
	Pagina p;
	while((p = getParent(item)) != null){
	    if(p == parent){ return true; }
	    item = p;
	}
	return false;
    }

    public KeyBinding getbinding(Coord cc) {
	PagButton h = bhit(cc);
	return((h == null) ? null : h.bind);
    }
    
    @Override
    public void bound() {
	super.bound();
	BuffToggles.menuBound(this);
    }
    
    public Pagina findPagina(Indir<Resource> res) {
	if(res == null) {return null;}
	return pmap.get(res);
    }
    
    public Pagina findPagina(String name) {
	Collection<Pagina> open, close = new HashSet<>();
	synchronized (paginae) { open = new LinkedList<>(paginae); }
	while (!open.isEmpty()) {
	    Iterator<Pagina> iter = open.iterator();
	    Pagina pag = iter.next();
	    iter.remove();
	    try {
		if(name.equals(Pagina.resname(pag))) {
		    return pag;
		}
		AButton ad = pag.button().act();
		if(ad != null) {
		    Pagina parent = paginafor(ad.parent);
		    if((parent != null) && !close.contains(parent) && !open.contains(parent))
			open.add(parent);
		}
		close.add(pag);
	    } catch (Loading ignored) {
	    }
	}
	return null;
    }
    
    private void initCustomPaginae() {
	makeLocal("paginae/add/timer", Action.TOGGLE_TIMERS);
	makeLocal("paginae/add/clear_player_dmg", Action.CLEAR_PLAYER_DAMAGE);
	makeLocal("paginae/add/clear_all_dmg", Action.CLEAR_ALL_DAMAGE);
	makeLocal("paginae/add/craftdb", Action.OPEN_CRAFT_DB);
	makeLocal("paginae/add/actlist", Action.OPEN_QUICK_ACTION);
	makeLocal("paginae/add/buildlist", Action.OPEN_QUICK_BUILD);
	makeLocal("paginae/add/craftlist", Action.OPEN_QUICK_CRAFT);
	makeLocal("paginae/add/autobot", Action.BOT_PICK_ALL_HERBS);
	makeLocal("paginae/add/hide_trees", Action.TOGGLE_HIDE_TREES, CFG.HIDE_TREES::get);
	makeLocal("paginae/add/minesweeper", Minesweeper::paginaAction, CFG.SHOW_MINESWEEPER_OVERLAY::get);
	makeLocal("paginae/add/toggles/flat_terrain", CFG.FLAT_TERRAIN);
	makeLocal("paginae/add/toggles/flavor", CFG.DISPLAY_FLAVOR);
	makeLocal("paginae/add/refill_drinks", Action.ACT_REFILL_DRINKS);
	makeLocal("paginae/add/quest_help", Action.OPEN_QUEST_HELP);
	makeLocal("paginae/add/inspect", Action.TOGGLE_INSPECT);
	makeLocal("paginae/add/track", Action.TRACK_OBJECT);
	makeLocal("paginae/add/fsmelter9", Action.FUEL_SMELTER_9);
	makeLocal("paginae/add/fsmelter12", Action.FUEL_SMELTER_12);
	makeLocal("paginae/add/foven4", Action.FUEL_OVEN_4);
	makeLocal("paginae/add/auto/aggro_one", Action.AGGRO_ONE_PVE);
	makeLocal("paginae/add/auto/aggro_one_pvp", Action.AGGRO_ONE_PVP);
	makeLocal("paginae/add/auto/aggro_all", Action.AGGRO_ALL);
	makeLocal("paginae/add/auto/mount_horse", Action.BOT_MOUNT_HORSE);
	makeLocal("paginae/add/info/plant-growth", Action.TOGGLE_GOB_INFO_PLANTS, () -> GobInfoOpts.enabled(InfoPart.PLANT_GROWTH));
	makeLocal("paginae/add/info/tree-growth", Action.TOGGLE_GOB_INFO_TREE_GROWTH, () -> GobInfoOpts.enabled(InfoPart.TREE_GROWTH));
	makeLocal("paginae/add/info/tree-content", Action.TOGGLE_GOB_INFO_TREE_CONTENT, () -> GobInfoOpts.enabled(InfoPart.TREE_CONTENTS));
	makeLocal("paginae/add/info/animal-fleece", Action.TOGGLE_GOB_INFO_ANIMAL_FLEECE, () -> GobInfoOpts.enabled(InfoPart.ANIMAL_FLEECE));
	makeLocal("paginae/add/info/health", Action.TOGGLE_GOB_INFO_HEALTH, () -> GobInfoOpts.enabled(InfoPart.HEALTH));
	makeLocal("paginae/add/info/barrel", Action.TOGGLE_GOB_INFO_BARREL, () -> GobInfoOpts.enabled(InfoPart.BARREL));
	makeLocal("paginae/add/info/sign", Action.TOGGLE_GOB_INFO_SIGN, () -> GobInfoOpts.enabled(InfoPart.DISPLAY_SIGN));
	makeLocal("paginae/add/info/cheese", Action.TOGGLE_GOB_INFO_CHEESE, () -> GobInfoOpts.enabled(InfoPart.CHEESE_RACK));
	makeLocal("paginae/add/info/quality", Action.TOGGLE_GOB_INFO_QUALITY, () -> GobInfoOpts.enabled(InfoPart.QUALITY));
	makeLocal("paginae/add/info/timer", Action.TOGGLE_GOB_INFO_TIMER, () -> GobInfoOpts.enabled(InfoPart.TIMER));
	makeLocal("paginae/add/alchemy", Action.OPEN_ALCHEMY_DB);
	makeLocal("paginae/add/equip/sword-n-board", Action.EQUIP_SWORD_N_BOARD);
	makeLocal("paginae/add/equip/bow", Action.EQUIP_BOW);
	makeLocal("paginae/add/equip/spear", Action.EQUIP_SPEAR);
    }
    
    private void makeLocal(String path, CustomPaginaAction action, Supplier<Boolean> toggleState) {
	Resource.Named res = Resource.local().loadwait(path).indir();
	Pagina pagina = new CustomPagina(this, res, action, toggleState);
	synchronized (pmap) { pmap.put(res, pagina); }
	synchronized (paginae) { paginae.add(pagina); }
    }
    
    private void makeLocal(String path, Action action) {
	makeLocal(path, action, null);
    }

    private void makeLocal(String path, CFG<Boolean> cfg) {
	makeLocal(path, (ctx, iact) -> {
	    cfg.set(!cfg.get());
	    return true;
	}, cfg::get);
    }
    
    private void makeLocal(String path, Action action, Supplier<Boolean> toggleState) {
	makeLocal(path, (ctx, iact) -> {
	    action.run(ctx.context(UI.class).gui);
	    return true;
	}, toggleState);
    }
    
}
