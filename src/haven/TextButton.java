package haven;

import rx.functions.Action1;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.Objects;

public class TextButton extends SIWidget {
    private String text;
    public BufferedImage up, down, hover;
    public boolean h = false;
    boolean a = false;
    UI.Grab d = null;
    Action1<Integer> action;
    
    private final Coord minSz;
    
    public TextButton(String text, Coord minSz, Action1<Integer> action) {
	super(Coord.z);
	this.action = action;
	this.minSz = minSz;
	setText(text);
    }
    
    public TextButton(String text) {
	this(text, Coord.z, null);
    }
    
    public void setText(String text) {
	if(Objects.equals(text, this.text)) {
	    return;
	}
	this.text = text;
	makeImages(text);
	
	redraw();
    }
    
    private void makeImages(String text) {
	BufferedImage txt = Text.render(text).img;
	
	Coord tsz = Utils.imgsz(txt);
	sz = tsz.max(minSz);
	
	int tx = (sz.x - tsz.x) / 2;
	int ty = (sz.y - tsz.y) / 2;
	
	up = TexI.mkbuf(sz);
	Graphics g = up.getGraphics();
	g.drawImage(txt, tx, ty, null);
	g.dispose();
	
	down = highlight(up, new Color(0x44EFE40A, true));
	hover = highlight(up, new Color(0x44C5C3BD, true));
    }
    
    public TextButton action(Action1<Integer> action) {
	this.action = action;
	return (this);
    }
    
    public void draw(BufferedImage buf) {
	Graphics g = buf.getGraphics();
	BufferedImage img;
	if(a)
	    img = down;
	else if(h)
	    img = hover;
	else
	    img = up;
	g.drawImage(img, 0, 0, null);
	g.dispose();
    }
    
    public boolean checkhit(Coord c) {
	return c.isect(Coord.z, sz);
    }
    
    public void click(int button) {
	if(action != null) {
	    action.call(button);
	}
    }
    
    public boolean gkeytype(java.awt.event.KeyEvent ev) {
	click(1);
	return (true);
    }
    
    protected void depress() {
    }
    
    protected void unpress() {
    }
    
    @Override
    public boolean mousedown(Coord c, int button) {
	if(!checkhit(c))
	    return (false);
	a = true;
	d = ui.grabmouse(this);
	depress();
	redraw();
	return (true);
    }
    
    @Override
    public boolean mouseup(Coord c, int button) {
	if(d == null) {return (false);}
	d.remove();
	d = null;
	mousemove(c);
	if(checkhit(c)) {
	    unpress();
	    click(button);
	}
	return (true);
    }
    
    public void mousemove(Coord c) {
	boolean h = checkhit(c);
	boolean a = false;
	if(d != null) {
	    a = h;
	    h = true;
	}
	if((h != this.h) || (a != this.a)) {
	    this.h = h;
	    this.a = a;
	    redraw();
	}
    }
    
    public Object tooltip(Coord c, Widget prev) {
	if(!checkhit(c))
	    return (null);
	return (super.tooltip(c, prev));
    }
    
    private static BufferedImage highlight(BufferedImage img, Color color) {
	Coord imgsz = Utils.imgsz(img);
	BufferedImage ret = TexI.mkbuf(imgsz);
	Graphics g = ret.getGraphics();
	g.drawImage(img, 0, 0, color, null);
	g.dispose();
	return ret;
    }
}
