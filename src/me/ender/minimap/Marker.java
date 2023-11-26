package me.ender.minimap;

import haven.*;

import java.util.Objects;

public abstract class Marker {
    public long seg;
    public Coord tc;
    public String nm;
    
    public Marker(long seg, Coord tc, String nm) {
	this.seg = seg;
	this.tc = tc;
	this.nm = nm;
    }
    
    public String name() {
	return nm;
    }
    
    public String tip(final UI ui) {
	return nm;
    }
    
    public abstract void draw(final GOut g, final Coord c, final Text tip, final float scale, final MapFile file);
    
    public abstract Area area();
    
    @Override
    public boolean equals(Object o) {
	if(this == o) return true;
	if(o == null || getClass() != o.getClass()) return false;
	Marker marker = (Marker) o;
	return seg == marker.seg && tc.equals(marker.tc) && nm.equals(marker.nm);
    }
    
    @Override
    public int hashCode() {
	return Objects.hash(seg, tc, nm);
    }
}
