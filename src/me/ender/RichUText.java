package me.ender;

import haven.*;

import java.awt.*;
import java.awt.image.BufferedImage;

public abstract class RichUText<T> implements Indir<Tex> {
    public final RichText.Foundry fnd;
    private Tex cur = null;
    private final Color bg;
    private T cv = null;
    
    public RichUText(RichText.Foundry fnd, Color bg) {
	this.fnd = fnd;
	this.bg = bg;
    }
    
    public RichUText(RichText.Foundry fnd) {this(fnd, null);}
    
    protected Tex render(String text) {
	BufferedImage img = fnd.render(text).img;
	if(bg == null) {
	    return new TexI(img);
	}
	Coord sz = Coord.of(img.getWidth(), img.getHeight());
	BufferedImage ret = TexI.mkbuf(sz);
	Graphics g = ret.getGraphics();
	g.setColor(bg);
	g.fillRect(0, 0, sz.x, sz.y);
	g.drawImage(img, 0, 0, null);
	g.dispose();
	return new TexI(ret);
    }
    protected String text(T value) {return(String.valueOf(value));}
    protected abstract T value();
    
    public Tex get() {
	T value = value();
	if(!Utils.eq(value, cv)) {
	    if(cur != null) {cur.dispose();}
	    cur = render(text(cv = value));
	}
	return(cur);
    }
    
    public Indir<Tex> tex() {
	return(RichUText.this);
    }
}