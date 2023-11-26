package me.ender.minimap;

import haven.*;

import java.util.Objects;

public class SMarker extends Marker {
    public final long oid;
    public final Resource.Spec res;
    
    public SMarker(long seg, Coord tc, String nm, long oid, Resource.Spec res) {
	super(seg, tc, nm);
	this.oid = oid;
	this.res = res;
    }
    
    @Override
    public boolean equals(Object o) {
	if(this == o) return true;
	if(o == null || getClass() != o.getClass()) return false;
	if(!super.equals(o)) return false;
	SMarker sMarker = (SMarker) o;
	return oid == sMarker.oid && res.equals(sMarker.res);
    }
    
    @Override
    public void draw(GOut g, Coord c, Text tip, final float scale, final MapFile file) {
	try {
	    final Resource res = this.res.loadsaved(Resource.remote());
	    final Resource.Image img = res.layer(Resource.imgc);
	    final Resource.Neg neg = res.layer(Resource.negc);
	    final Coord cc = neg != null ? neg.cc : img.ssz.div(2);
	    final Coord ul = c.sub(cc);
	    g.image(img, ul);
	    if(tip != null && CFG.MMAP_SHOW_MARKER_NAMES.get()) {
		g.aimage(tip.tex(), c.addy(UI.scale(3)), 0.5, 0);
	    }
	} catch (Loading ignored) {}
    }
    
    @Override
    public Area area() {
	try {
	    final Resource res = this.res.loadsaved(Resource.remote());
	    final Resource.Image img = res.layer(Resource.imgc);
	    final Resource.Neg neg = res.layer(Resource.negc);
	    final Coord cc = neg != null ? neg.cc : img.ssz.div(2);
	    return Area.sized(cc.inv(), img.ssz);
	} catch (Loading ignored) {
	    return null;
	}
    }
    
    @Override
    public int hashCode() {
	return Objects.hash(super.hashCode(), oid, res);
    }
}
