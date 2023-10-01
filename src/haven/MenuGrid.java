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

import haven.Resource.AButton;

import javax.swing.*;
import java.awt.*;
import java.awt.Color;
import java.awt.Font;
import java.awt.event.KeyEvent;
import java.awt.font.TextAttribute;
import java.awt.image.BufferedImage;
import java.util.List;
import java.util.*;
import java.util.stream.Collectors;

public class MenuGrid extends Widget implements KeyBinding.Bindable {
    public final static Tex bg = Resource.loadtex("gfx/hud/invsq");
    public final static Coord bgsz = bg.sz().add(-UI.scale(1), -UI.scale(1));
    public final static RichText.Foundry ttfnd = new RichText.Foundry(TextAttribute.FAMILY, "SansSerif", TextAttribute.SIZE, UI.scale(10f));
    private static Coord gsz = new Coord(4, 4);
    public final Set<Pagina> paginae = new HashSet<Pagina>();
    public Pagina cur;
    public int pagseq = 0;
    private Pagina dragging;
    private Collection<PagButton> curbtns = Collections.emptyList();
    private PagButton pressed, layout[][] = new PagButton[gsz.x][gsz.y];
    private UI.Grab grab;
    private int curoff = 0;
    private boolean recons = true, showkeys = false;
    private Map<Character, PagButton> hotmap = new HashMap<>();
    public Pagina lastCraft = null;

    @RName("scm")
    public static class $_ implements Factory {
	public Widget create(UI ui, Object[] args) {
	    return(new MenuGrid());
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

    public static class PagButton implements ItemInfo.Owner {
	public final Pagina pag;
	public final Resource res;
	public final KeyBinding bind;

	public PagButton(Pagina pag) {
	    this.pag = pag;
	    this.res = pag.res();
	    this.bind = binding();
	}

	public BufferedImage img() {return(res.flayer(Resource.imgc).scaled());}
	public String name() {return(res.flayer(Resource.action).name);}
	public String originalName() {return(res.flayer(Resource.action).original);}
	public KeyMatch hotkey() {
	    char hk = res.flayer(Resource.action).hk;
	    if(hk == 0)
		return(KeyMatch.nil);
	    //FIXME: I have no idea why exactly upper case keybinds continue to work properly.
	    return KeyMatch.forcode(KeyStroke.getKeyStroke(Character.toUpperCase(hk), 0).getKeyCode(), KeyMatch.MODS, 0);
	}
	public KeyBinding binding() {
	    return(KeyBinding.get("scm/" + res.name, hotkey()));
	}
	@Deprecated public void use() {
	    pag.scm.wdgmsg("act", (Object[])res.flayer(Resource.action).ad);
	}
	public void use(Interaction iact) {
	    Object[] args = Utils.extend(new Object[0], res.flayer(Resource.action).ad);
	    args = Utils.extend(args, Integer.valueOf(pag.scm.ui.modflags()));
	    if(iact.mc != null) {
		args = Utils.extend(args, iact.mc.floor(OCache.posres));
		if(iact.click != null)
		    args = Utils.extend(args, iact.click.clickargs());
	    }
	    pag.scm.wdgmsg("act", args);
	}

	public String sortkey() {
	    AButton ai = pag.act();
	    if(ai.ad.length == 0)
		return("\0" + originalName());
	    return(name());
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
	    if(info == null)
		info = ItemInfo.buildinfo(this, pag.rawinfo);
	    return(info);
	}
	private static final OwnerContext.ClassResolver<PagButton> ctxr = new OwnerContext.ClassResolver<PagButton>()
	    .add(PagButton.class, p -> p)
	    .add(MenuGrid.class, p -> p.pag.scm)
	    .add(Glob.class, p -> p.pag.scm.ui.sess.glob)
	    .add(Session.class, p -> p.pag.scm.ui.sess);
	public <T> T context(Class<T> cl) {return(ctxr.context(cl, this));}

	public BufferedImage rendertt(boolean withpg) {
	    Resource.Pagina pg = res.layer(Resource.pagina);
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
		if(pg != null)
		    ret = ItemInfo.catimgs(0, ret, ttfnd.render("\n" + pg.text, UI.scale(200)).img);
	    }
	    return(ret);
	}

	@Resource.PublishedCode(name = "pagina")
	public interface Factory {
	    public PagButton make(Pagina info);
	}
    }

    public interface CustomPaginaAction {
	void perform(OwnerContext ctx);
    }
    
    public class CustomPagButton extends PagButton {

	private final CustomPaginaAction action;

	public CustomPagButton(Pagina pag, CustomPaginaAction action) {
	    super(pag);
	    this.action = action;
	}

	@Override
	public void use() {
	    action.perform(pag);
	}
    
	@Override
	public void use(Interaction iact) {
	    action.perform(pag);
	}
    }
    
    public final PagButton next = new PagButton(new Pagina(this, Resource.local().loadwait("gfx/hud/sc-next").indir())) {
	    {pag.button = this;}

	    public void use(Interaction iact) {
		if((curoff + 14) >= curbtns.size())
		    curoff = 0;
		else
		    curoff += (gsz.x * gsz.y) - 2;
		updlayout();
	    }

	    public String name() {return("More...");}

	    public KeyBinding binding() {return(kb_next);}
	};

    public final PagButton bk = new PagButton(new Pagina(this, Resource.local().loadwait("gfx/hud/sc-back").indir())) {
	    {pag.button = this;}

	    public void use(Interaction iact) {
		Pagina dst = paginafor(pag.scm.cur.act().parent);
		pag.scm.change(dst);
		curoff = 0;
		selectCraft(dst);
	    }

	    public String name() {return("Back");}

	    public KeyBinding binding() {return(kb_back);}
	};
    
    public static class Pagina implements ItemInfo.Owner {
	public final MenuGrid scm;
	public final Indir<Resource> res;
	public State st;
	public double meter, gettime, dtime, fstart;
	public Indir<Tex> img;
	public int newp;
	public Object[] rawinfo = {};

	public static enum State {
	    ENABLED, DISABLED {
		public Indir<Tex> img(Pagina pag) {
		    return(Utils.cache(() -> new TexI(PUtils.monochromize(PUtils.copy(pag.button().img()), Color.LIGHT_GRAY))));
		}
	    };

	    public Indir<Tex> img(Pagina pag) {
		return(Utils.cache(() -> new TexI(pag.button().img())));
	    }
	}

	public Pagina(MenuGrid scm, Indir<Resource> res) {
	    this.scm = scm;
	    this.res = res;
	    state(State.ENABLED);
	}

	public Resource res() {
	    return(res.get());
	}

	public Resource.AButton act() {
	    return(res().layer(Resource.action));
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

	public void state(State st) {
	    this.st = st;
	    this.img = st.img(this);
	}

	private List<ItemInfo> info = null;
	public List<ItemInfo> info() {
	    if(info == null)
		info = ItemInfo.buildinfo(this, rawinfo);
	    return(info);
	}
	private static final OwnerContext.ClassResolver<Pagina> ctxr = new OwnerContext.ClassResolver<Pagina>()
	    .add(Glob.class, p -> p.scm.ui.sess.glob)
	    .add(Session.class, p -> p.scm.ui.sess)
	    .add(UI.class, p -> p.scm.ui);
	public <T> T context(Class<T> cl) {return(ctxr.context(cl, this));}
 
	public boolean isAction() {
	    Resource.AButton act = act();
	    if(act == null) {return false;}
	    String[] ad = act.ad;
	    return ad != null && ad.length > 0;
	}
 
	public static String name(Pagina p) {
	    String name = "";
	    if(p.res instanceof Resource.Named) {
		name = ((Resource.Named) p.res).name;
	    } else {
		try {
		    name = p.res.get().name;
		} catch (Loading ignored) {}
	    }
	    return name;
	}
    }

    @Override
    public void bound() {
	super.bound();
	ui.gui.menuObservable.notifyObservers();
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
	makeLocal("paginae/add/hide_trees", Action.TOGGLE_HIDE_TREES);
	makeLocal("paginae/add/inspect", Action.TOGGLE_INSPECT);
	makeLocal("paginae/add/track", Action.TRACK_OBJECT);
	makeLocal("paginae/add/fsmelter9", Action.FUEL_SMELTER_9);
	makeLocal("paginae/add/fsmelter12", Action.FUEL_SMELTER_12);
	makeLocal("paginae/add/foven4", Action.FUEL_OVEN_4);
    }
    
    private void makeLocal(String path, CustomPaginaAction action) {
	Resource.Named res = Resource.local().loadwait(path).indir();
	Pagina pagina = new Pagina(this, res);
	pagina.button(new CustomPagButton(pagina, action));
	synchronized (pmap) { pmap.put(res, pagina); }
	synchronized (paginae) { paginae.add(pagina); }
    }
    
    private void makeLocal(String path, Action action) {
	makeLocal(path, ctx -> action.run(ctx.context(UI.class).gui));
    }

    public final Map<Indir<Resource>, Pagina> pmap = new WeakHashMap<Indir<Resource>, Pagina>();
    public Pagina paginafor(Indir<Resource> res) {
	if(res == null)
	    return(null);
	synchronized(pmap) {
	    Pagina p = pmap.get(res);
	    if(p == null)
		pmap.put(res, p = new Pagina(this, res));
	    return(p);
	}
    }
    
    public Pagina paginafor(Resource res) {
	if(res != null) {
	    synchronized (pmap) {
		for (Indir<Resource> key : pmap.keySet()) {
		    if(Objects.equals(key.get().name, res.name)) { return pmap.get(key); }
		}
	    }
	}
	return null;
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
	synchronized(paginae) {
	    open = new LinkedList<Pagina>();
	    for(Pagina pag : paginae) {
		if(pag.newp == 2) {
		    pag.newp = 0;
		    pag.fstart = 0;
		}
		open.add(pag);
	    }
	    for(Pagina pag : pmap.values()) {
		if(pag.newp == 2) {
		    pag.newp = 0;
		    pag.fstart = 0;
		}
	    }
	}
	boolean ret = true;
	while(!open.isEmpty()) {
	    Iterator<Pagina> iter = open.iterator();
	    Pagina pag = iter.next();
	    iter.remove();
	    try {
		AButton ad = pag.act();
		if(ad == null)
		    throw(new RuntimeException("Pagina in " + pag.res + " lacks action"));
		Pagina parent = paginafor(ad.parent);
		if((pag.newp != 0) && (parent != null) && (parent.newp == 0)) {
		    parent.newp = 2;
		    parent.fstart = (parent.fstart == 0)?pag.fstart:Math.min(parent.fstart, pag.fstart);
		}
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

    public MenuGrid() {
	super(bgsz.mul(gsz).add(UI.scale(1), UI.scale(1)));
	initCustomPaginae();
    }

    public static Comparator<Pagina> sorter = new Comparator<Pagina>() {
	public int compare(Pagina a, Pagina b) {
	    AButton aa = a.act(), ab = b.act();
	    if((aa.ad.length == 0) && (ab.ad.length > 0))
		return(-1);
	    if((aa.ad.length > 0) && (ab.ad.length == 0))
		return(1);
	    return(aa.name.compareTo(ab.name));
	}
    };

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
	    List<ItemInfo> info = pag.info();
	    info.removeIf(el -> el instanceof ItemInfo.Name);
	    if(!info.isEmpty())
		ret = ItemInfo.catimgs(0, ret, ItemInfo.longtip(info));
	    if(pg != null)
		ret = ItemInfo.catimgs(0, ret, ttfnd.render("\n" + pg.text, 200).img);
	}
	return(ret);
    }

    private static Map<PagButton, Tex> glowmasks = new WeakHashMap<>();
    private Tex glowmask(PagButton pag) {
	Tex ret = glowmasks.get(pag);
	if(ret == null) {
	    ret = new TexI(PUtils.glowmask(PUtils.glowmask(pag.img().getRaster()), 4, new Color(32, 255, 32)));
	    glowmasks.put(pag, ret);
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
		    Pagina info = btn.pag;
		    Tex btex;
		    try {
			btex = info.img.get();
			g.image(btex, p.add(UI.scale(1), UI.scale(1)), btex.sz());
		    } catch(NullPointerException e) {
			System.err.println(btn);
			System.err.println(info.scm == this);
			throw(e);
		    }
		    if(showkeys) {
			Tex ki = btn.keyrend();
			if(ki != null)
			    g.aimage(ki, p.add(bgsz.x - UI.scale(2), UI.scale(1)), 1.0, 0.0);
		    }
		    if(info.meter > 0) {
			double m = info.meter;
			if(info.dtime > 0)
			    m += (1 - m) * (now - info.gettime) / info.dtime;
			m = Utils.clip(m, 0, 1);
			g.chcolor(255, 255, 255, 128);
			g.fellipse(p.add(bgsz.div(2)), bgsz.div(2), Math.PI / 2, ((Math.PI / 2) + (Math.PI * 2 * m)));
			g.chcolor();
		    }
		    if(info.newp != 0) {
			if(info.fstart == 0) {
			    info.fstart = now;
			} else {
			    double ph = (now - info.fstart) - (((x + (y * gsz.x)) * 0.15) % 1.0);
			    Tex glow = glowmask(btn);
			    if(ph < 1.25) {
				g.chcolor(255, 255, 255, (int)(255 * ((Math.cos(ph * Math.PI * 2) * -0.5) + 0.5)));
			    } else {
				g.chcolor(255, 255, 255, 128);
			    }
			    g.image(glow, p.sub(4, 4));
			    g.chcolor();
			}
		    }
		    if(btn == pressed) {
			g.chcolor(new Color(0, 0, 0, 128));
			g.frect(p.add(UI.scale(1), UI.scale(1)), btex.sz());
			g.chcolor();
		    }
		}
	    }
	}
	super.draw(g);
	if(dragging != null) {
	    Tex dt = dragging.img.get();
	    ui.drawafter(new UI.AfterDraw() {
		    public void draw(GOut g) {
			g.image(dt, ui.mc.add(dt.sz().div(2).inv()));
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
		try {
		    BufferedImage ti = pag.rendertt(ttl);
		    curtt = (ti == null) ? null : new TexI(ti);
		} catch(Loading l) {
		    return("...");
		}
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

    public boolean mousedown(Coord c, int button) {
	PagButton h = bhit(c);
	if((button == 1) && (h != null)) {
	    pressed = h;
	    grab = ui.grabmouse(this);
	}
	return(true);
    }

    public void mousemove(Coord c) {
	if((dragging == null) && (pressed != null)) {
	    PagButton h = bhit(c);
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
	    r.pag.newp = 0;
	    r.use(iact);
	    if(isCrafting(r.pag)) {
		lastCraft = r.pag;
	    }
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
    }

    public boolean mouseup(Coord c, int button) {
	PagButton h = bhit(c);
	if((button == 1) && (grab != null)) {
	    if(dragging != null) {
		ui.dropthing(ui.root, ui.mc, dragging.res());
		pressed = null;
		dragging = null;
	    } else if(pressed != null) {
		if(pressed == h)
		    use(h, new Interaction(), false);
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
		change(paginafor(ui.sess.getres((Integer)args[0])));
	} else if(msg == "fill") {
	    synchronized(paginae) {
		int a = 0;
		while(a < args.length) {
		    int fl = (Integer)args[a++];
		    Pagina pag = paginafor(ui.sess.getres((Integer)args[a++], -2));
		    if((fl & 1) != 0) {
			pag.state(Pagina.State.ENABLED);
			pag.meter = 0;
			if((fl & 2) != 0)
			    pag.state(Pagina.State.DISABLED);
			if((fl & 4) != 0) {
			    pag.meter = ((Number)args[a++]).doubleValue() / 1000.0;
			    pag.gettime = Utils.rtime();
			    pag.dtime = ((Number)args[a++]).doubleValue() / 1000.0;
			}
			if((fl & 8) != 0)
			    pag.newp = 1;
			if((fl & 16) != 0)
			    pag.rawinfo = (Object[])args[a++];
			else
			    pag.rawinfo = new Object[0];
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
    public boolean globtype(char k, KeyEvent ev) {
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
	    use(pag, new Interaction(), (KeyMatch.mods(ev) & KeyMatch.S) == 0);
	    if(this.cur != null)
		showkeys = true;
	    return(true);
	}
	return(false);
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
	return (p != null) && (isCrafting(p.res()) || isCrafting(getParent(p)));
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
    
    public Pagina findPagina(String name) {
	Collection<Pagina> open, close = new HashSet<>();
	synchronized (paginae) { open = new LinkedList<>(paginae); }
	while (!open.isEmpty()) {
	    Iterator<Pagina> iter = open.iterator();
	    Pagina pag = iter.next();
	    iter.remove();
	    try {
		if(name.equals(Pagina.name(pag))) {
		    return pag;
		}
		AButton ad = pag.act();
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
}
