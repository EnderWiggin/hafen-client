package haven;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.LinkedList;
import java.util.List;

public abstract class Breadcrumbs<T> extends Widget {
    private static final Coord border = UI.scale(2, 2);
    private static final BufferedImage separator = Resource.loadimg("gfx/hud/breadcrumb");
    private final Coord SZ;
    private List<Crumb<T>> crumbs;
    private List<IButton> buttons;
    
    public Breadcrumbs(Coord sz) {
	super(sz.add(0, border.x * 2));
	int d = sz.y;
	SZ = new Coord(d, d);
	buttons = new LinkedList<IButton>();
    }
    
    public void setCrumbs(List<Crumb<T>> crumbs) {
	this.crumbs = crumbs;
	cleanButtons();
	createButtons();
    }
    
    @Override
    public void wdgmsg(Widget sender, String msg, Object... args) {
	if(sender instanceof IButton && buttons.contains(sender)) {
	    if(msg.equals("activate")) {
		int k = buttons.indexOf(sender);
		selected(crumbs.get(k).data);
	    }
	    return;
	}
	super.wdgmsg(sender, msg, args);
    }
    
    public abstract void selected(T data);
    
    @Override
    public void draw(GOut g) {
	for (int i = 1; i < buttons.size(); i++) {
	    g.image(separator, buttons.get(i).c.sub(UI.scale(12, -6)));
	}
	super.draw(g);
    }
    
    private void createButtons() {
	int w = 0;
	for (Crumb item : crumbs) {
	    BufferedImage txt = Text.render(item.text).img;
	    
	    Coord isz = Utils.imgsz(txt).add(border.x * 3 + SZ.x, 0);
	    int ty = (sz.y - isz.y) / 2;
	    isz.y = sz.y;
	    
	    BufferedImage up = TexI.mkbuf(isz);
	    Graphics g = up.getGraphics();
	    if(item.img != null) {
		item.img = PUtils.convolvedown(item.img, SZ, CharWnd.iconfilter);
		g.drawImage(item.img, border.x, border.y, SZ.x, SZ.y, null);
	    }
	    g.drawImage(txt, SZ.x + border.x * 2, ty, null);
	    
	    BufferedImage down = highlight(up, new Color(0x44EFE40A, true));
	    BufferedImage over = highlight(up, new Color(0x44C5C3BD, true));
	    
	    IButton btn = new IButton(up, down, over);
	    btn.recthit = true;
	    buttons.add(btn);
	    add(btn, w, 0);
	    
	    w += isz.x + UI.scale(14);
	}
    }
    
    private BufferedImage highlight(BufferedImage img, Color color) {
	Coord imgsz = Utils.imgsz(img);
	BufferedImage ret = TexI.mkbuf(imgsz);
	Graphics g = ret.getGraphics();
	g.drawImage(img, 0, 0, color, null);
	return ret;
    }
    
    private void cleanButtons() {
	for (IButton btn : buttons) {
	    ui.destroy(btn);
	}
	buttons.clear();
    }
    
    public static class Crumb<T> {
	public BufferedImage img;
	public String text;
	public T data;
	
	public Crumb(BufferedImage img, String text, T data) {
	    this.img = img;
	    this.text = text;
	    this.data = data;
	}
	
	public static Crumb<MenuGrid.Pagina> fromPagina(MenuGrid.Pagina pagina) {
	    BufferedImage img = pagina.res().layer(Resource.imgc).img;
	    Resource.AButton act = pagina.act();
	    String name = "...";
	    if(act != null) {
		name = act.name;
	    }
	    return new Breadcrumbs.Crumb<>(img, name, pagina);
	}
	
	public Crumb(MenuGrid.Pagina pagina, T data) {
	    this.img = pagina.res().layer(Resource.imgc).img;
	    Resource.AButton act = pagina.act();
	    this.text = "...";
	    if(act != null) {
		this.text = act.name;
	    }
	    this.data = data;
	}
    }
}
