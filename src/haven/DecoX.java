package haven;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.Collection;
import java.util.LinkedList;
import java.util.Objects;

import static haven.PUtils.*;

public class DecoX extends Window.DefaultDeco {
    private final CFG.Observer<Theme> updateDecorator = this::updateDecorator;
    protected final Collection<Widget> twdgs = new LinkedList<>();
    private DecoTheme theme;
    
    public DecoX(boolean large) {
	super(large);
    }
    
    @Override
    protected void added() {
	super.added();
	initTheme();
    }
    
    @Override
    public void destroy() {
	CFG.THEME.unobserve(updateDecorator);
	super.destroy();
    }
    
    private WindowX wndx() {
	return (WindowX) parent;
    }
    
    private void initTheme() {
	setTheme(CFG.THEME.get().deco);
	CFG.THEME.observe(updateDecorator);
    }
    
    private void updateDecorator(CFG<Theme> theme) {
	setTheme(theme.get().deco);
    }
    
    private void setTheme(DecoThemeType type) {
	this.theme = DecoTheme.fromType(type);
	WindowX wnd = wndx();
	if(theme != null) {
	    theme.apply(wnd, this);
	} else {
	    wnd.resize(wnd.contentsz());
	}
    }
    
    public void addtwdg(Widget wdg) {
	twdgs.add(add(wdg));
	placetwdgs();
    }
    
    public void remtwdg(Widget wdg) {
	twdgs.remove(wdg);
	placetwdgs();
    }
    
    protected void placetwdgs() {
	int x = cbtn.c.x;
	int y = cbtn.c.y + cbtn.sz.y / 2;
	for (Widget ch : twdgs) {
	    if(ch.visible) {
		x -= ch.sz.x + UI.scale(3);
		ch.c = new Coord(x, y - ch.sz.y / 2);
	    }
	}
    }
    
    @Override
    public void iresize(Coord isz) {
	if(theme == null) {
	    super.iresize(isz);
	} else {
	    theme.iresize(isz, this);
	}
	placetwdgs();
    }
    
    @Override
    protected void drawbg(GOut g) {
	if(theme == null) {
	    super.drawbg(g);
	} else {
	    theme.drawbg(g, this);
	}
    }
    
    @Override
    protected void drawframe(GOut g) {
	if(theme == null) {
	    super.drawframe(g);
	} else {
	    theme.drawframe(g, this);
	}
    }
    
    @Override
    public boolean checkhit(Coord c) {
	if(theme == null) {
	    return super.checkhit(c);
	}
	
	return theme.checkhit(c, this);
    }
    
    @Override
    protected boolean hitSizer(Coord c) {
	if(theme == null) {
	    return super.hitSizer(c);
	}
	
	return theme.hitSizer(c, this);
    }
    
    public void siresize(Coord isz) {
	super.iresize(isz);
    }
    
    public void sdrawbg(GOut g) {
	super.drawbg(g);
    }
    
    public void sdrawframe(GOut g) {
	super.drawframe(g);
    }
    
    public boolean scheckhit(Coord c) {
	return super.checkhit(c);
    }
    
    public boolean shitSizer(Coord c) {
	return super.hitSizer(c);
    }
    
    public enum DecoThemeType {
	Big, Small
    }
    
    public interface DecoTheme {
	DecoTheme BIG = new Pretty();
	DecoTheme SMALL = new Slim();
	
	static DecoTheme fromType(DecoThemeType type) {
	    switch (type) {
		case Big:
		    return BIG;
		case Small:
		    return SMALL;
		default:
		    throw new IllegalArgumentException(String.format("Unknown theme type: '%s'", type));
	    }
	}
	
	default void apply(WindowX wndX, DecoX decoX) {
	    wndX.resize2(wndX.contentsz());
	}
	
	void iresize(Coord isz, DecoX decoX);
	
	void drawbg(GOut g, DecoX decoX);
	
	void drawframe(GOut g, DecoX decoX);
	boolean checkhit(Coord c, DecoX decoX);
	
	boolean hitSizer(Coord c, DecoX decoX);
    }
    
    private static class Slim implements DecoTheme {
	private static final Tex cl = Resource.loadtex("gfx/hud/wnd/cleft");
	private static final TexI cm = new TexI(Resource.loadsimg("gfx/hud/wnd/cmain"));
	private static final Tex cr = Resource.loadtex("gfx/hud/wnd/cright");
	private static final int capo = UI.scale(2), capio = UI.scale(1);
	private static final Coord mrgn = UI.scale(1, 1);
	private static final double cay = 0.5;
	private static final Text.Furnace cf = new Text.Imager(new PUtils.TexFurn(new Text.Foundry(Text.serif.deriveFont(Font.BOLD, UI.scale(14))).aa(true), WindowX.ctex)) {
	    protected BufferedImage proc(Text text) {
		return (rasterimg(blurmask2(text.img.getRaster(), UI.rscale(0.75), UI.rscale(1.0), Color.BLACK)));
	    }
	};
	
	public static final BufferedImage[] cbtni = new BufferedImage[]{
	    Resource.loadsimg("gfx/hud/btn-close"),
	    Resource.loadsimg("gfx/hud/btn-close-d"),
	    Resource.loadsimg("gfx/hud/btn-close-h")
	};
	
	private static final IBox wbox = new IBox("gfx/hud/wnd", "tl", "tr", "bl", "br", "extvl", "extvr", "extht", "exthb") {
	    final Coord co = UI.scale(3, 3), bo = UI.scale(2, 2);
	    
	    public Coord btloff() {return (super.btloff().sub(bo));}
	    
	    public Coord ctloff() {return (super.ctloff().sub(co));}
	    
	    public Coord bisz() {return (super.bisz().sub(bo.mul(2)));}
	    
	    public Coord cisz() {return (super.cisz().sub(co.mul(2)));}
	};
	
	@Override
	public void apply(WindowX wndX, DecoX decoX) {
	    decoX.cbtn.images(cbtni[0], cbtni[1], cbtni[2]);
	    DecoTheme.super.apply(wndX, decoX);
	}
	
	@Override
	public void iresize(Coord isz, DecoX decoX) {
	    Coord asz = isz;
	    Coord csz = asz.add(mrgn.mul(2));
	    
	    decoX.cptl = Coord.of(0, wbox.ctl.sz().y);
	    Coord wsz = csz.add(wbox.bisz()).addy(cm.sz().y / 2).add(decoX.cptl);
	    decoX.resize(wsz);
	    
	    decoX.ca = Area.sized(decoX.cptl.add(wbox.btloff()).add(0, cm.sz().y / 2), wsz);
	    decoX.aa = Area.sized(decoX.ca.ul.add(mrgn), asz);
	    
	    decoX.cbtn.c = Coord.of(wsz.x, decoX.aa.ul.y).sub(decoX.cbtn.sz);
	}
	
	@Override
	public void drawbg(GOut g, DecoX decoX) {
	    g.chcolor(new Color(55, 64, 32, 200));
	    g.frect(decoX.cptl.add(mrgn.mul(2)), decoX.sz.sub(mrgn.mul(2)));
	    g.chcolor();
	}
	
	@Override
	public void drawframe(GOut g, DecoX decoX) {
	    Window wnd = decoX.wndx();
	    Text cap = decoX.cap;
	    if((cap == null) || (!Objects.equals(cap.text, wnd.cap))) {
		cap = (wnd.cap == null) ? null : cf.render(wnd.cap);
		decoX.cmw = (cap == null) ? 0 : cap.sz().x;
		decoX.cpsz = Coord.of(cl.sz().x + decoX.cmw + cr.sz().x, cm.sz.y);
		decoX.cmw = decoX.cmw - (cl.sz().x) - UI.scale(5);
	    }
	    
	    wbox.draw(g, decoX.cptl, decoX.sz.sub(decoX.cptl));
	    
	    if(decoX.dragsize) {
		Coord sub = decoX.sz.sub(Window.sizer_sz);
		g.image(Window.sizer, sub);
	    }
	    
	    if(cap != null) {
		int w = cap.sz().x;
		int y = decoX.cptl.y + capo;
		g.aimage(cl, new Coord(decoX.cptl.x, y), 0, cay);
		g.aimage(cm, new Coord(decoX.cptl.x + cl.sz().x, y), 0, cay, new Coord(w, cm.sz().y));
		g.aimage(cr, new Coord(decoX.cptl.x + w + cl.sz().x, y), 0, cay);
		g.aimage(cap.tex(), new Coord(decoX.cptl.x + cl.sz().x, y - capo - capio), 0, cay);
	    }
	}
	
	@Override
	public boolean checkhit(Coord c, DecoX decoX) {
	    return c.isect(decoX.cptl, decoX.sz)
		|| c.isect(decoX.cptl.addy(-cm.sz.y), decoX.cpsz);
	}
	
	@Override
	public boolean hitSizer(Coord c, DecoX decoX) {
	    return c.x > decoX.sz.x - Window.sizer_sz.x 
		&& c.y > decoX.sz.y - Window.sizer_sz.y;
//	    return c.isect(decoX.sz.sub(Window.sizer_sz), Window.sizer_sz);
	}
    }
    
    private static class Pretty implements DecoTheme {
	@Override
	public void apply(WindowX wndX, DecoX decoX) {
	    decoX.cbtn.images(Window.cbtni[0], Window.cbtni[1], Window.cbtni[2]);
	    DecoTheme.super.apply(wndX, decoX);
	}
	
	@Override
	public void iresize(Coord isz, DecoX decoX) {
	    decoX.siresize(isz);
	}
	
	@Override
	public void drawbg(GOut g, DecoX decoX) {
	    decoX.sdrawbg(g);
	}
	
	@Override
	public void drawframe(GOut g, DecoX decoX) {
	    decoX.sdrawframe(g);
	}
	
	@Override
	public boolean checkhit(Coord c, DecoX decoX) {
	    return decoX.scheckhit(c);
	}
	
	@Override
	public boolean hitSizer(Coord c, DecoX decoX) {
	    return decoX.shitSizer(c);
	}
    }
}
