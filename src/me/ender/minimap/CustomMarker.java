package me.ender.minimap;

import haven.*;

import java.awt.*;
import java.awt.image.WritableRaster;
import java.util.Map;
import java.util.Objects;
import java.util.WeakHashMap;

import static haven.MapWnd.MarkerType.*;

// Simple custom icons that are a combo of PMarker (color) and SMarker (Custom res)
public class CustomMarker extends Marker {
    private static final Map<String, Image> cache = new WeakHashMap<>();
    
    public Color color;
    public final Resource.Spec res;
    
    public CustomMarker(final long seq, final Coord tc, final String nm,
			final Color color, final Resource.Spec res) {
	super(seq, tc, nm);
	this.color = color;
	this.res = res;
    }
    
    public char identifier() {
	return 'r';
    }
    
    public int version() {
	return 1;
    }
    
    @Override
    public boolean equals(Object o) {
	if(this == o) return true;
	if(o == null || getClass() != o.getClass()) return false;
	if(!super.equals(o)) return false;
	CustomMarker that = (CustomMarker) o;
	return color.equals(that.color) && res.equals(that.res);
    }
    
    @Override
    public void draw(GOut g, Coord c, Text tip, final float scale, final MapFile file) {
	final Image img = image(res, color);
	if(img != null && img.tex != null) {
	    final Coord ul = c.sub(img.cc);
	    g.image(img.tex, ul);
	    if(tip != null && CFG.MMAP_SHOW_MARKER_NAMES.get()) {
		g.aimage(tip.tex(), c.addy(UI.scale(5)), 0.5, 0);
	    }
	}
    }
    
    @Override
    public Area area() {
	final Image img = image(res, Color.WHITE);
	if(img == null) {return null;}
	Coord sz = img.tex.sz();
	return Area.sized(sz.div(2).inv(), sz);
    }
    
    public static Image image(Resource.Spec spec, Color col) {
	String cacheId = String.format("%s:c[%d]", spec.name, col.getRGB());
	Image image = cache.get(cacheId);
	if(image == null) {
	    try {
		image = new Image(Resource.loadsaved(Resource.remote(), spec), col);
		cache.put(cacheId, image);
	    } catch (Loading ignored) {}
	}
	return image;
    }
    
    @Override
    public int hashCode() {
	return Objects.hash(super.hashCode(), color, res);
    }
    
    public static boolean equals(CustomMarker a, CustomMarker b) {
	return a.seg == b.seg
	    && a.tc.equals(b.tc)
	    && a.res.name.equals(b.res.name);
    }
    
    public static class Image {
	public final Tex tex;
	public final Coord cc;
	
	public Image(Resource res, Color col) {
	    Resource.Image bg = res.layer(Resource.imgc, 0);
	    Resource.Image fg = res.layer(Resource.imgc, 1);
	    if(bg == null) {bg = res.layer(Resource.imgc);}
	    if(bg == null) {
		throw new IllegalArgumentException(String.format("res '%s' has no image layers!", res.name));
	    }
	    Coord sz;
	    if(fg == null) {
		sz = new Coord(bg.o.x + bg.sz.x, bg.o.y + bg.sz.y);
	    } else {
		sz = new Coord(Math.max(bg.o.x + bg.sz.x, fg.o.x + fg.sz.x),
		    Math.max(bg.o.y + bg.sz.y, fg.o.y + fg.sz.y));
	    }
	    
	    WritableRaster buf = PUtils.imgraster(sz);
	    PUtils.blit(buf, PUtils.coercergba(bg.img).getRaster(), bg.o);
	    PUtils.colmul(buf, col);
	    if(fg != null) {
		PUtils.alphablit(buf, PUtils.coercergba(fg.img).getRaster(), fg.o);
	    }
	    
	    this.tex = new TexI(PUtils.uiscale(PUtils.rasterimg(buf), new Coord(iconsz, iconsz)));
	    this.cc = tex.sz().div(2);
	}
    }
}
