package me.ender.minimap;

import haven.*;
import me.ender.QuestListItem;

import java.awt.*;
import java.util.Objects;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class SMarker extends Marker {
    public final long oid;
    public final Resource.Saved res;

    public List<QuestListItem> qitems = new ArrayList<>();
    public Iterator<QuestListItem> questiterator;

    public SMarker(long seg, Coord tc, String nm, long oid, Resource.Saved res) {
	super(seg, tc, nm);
	this.oid = oid;
	this.res = res;
	questiterator = Utils.circularIterator(qitems);
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
	    final Resource res = this.res.loadsaved();
	    final Resource.Image img = res.layer(Resource.imgc);
	    final Resource.Neg neg = res.layer(Resource.negc);
	    final Coord cc = neg != null ? neg.cc : img.ssz.div(2);
	    final Coord ul = c.sub(cc);
	    if(CFG.QUESTHELPER_HIGHLIGHT_QUESTGIVERS.get() && !qitems.isEmpty()) {
		for(QuestListItem item : qitems) {
		    if(!item.questGiver.isEmpty() && item.status == 0) {
			if(item.last) {
			    if (item.single) {
				g.chcolor(Color.GREEN);
				g.fellipse(c, img.ssz.div(2).sub(1, 1));
			    }
			    else if (CFG.QUESTHELPER_HIGHLIGHT_UNFINISHED.get()) {
				g.chcolor(Color.YELLOW);
				g.fellipse(c, img.ssz.div(2).sub(2, 1));
			    }
			}
			else
			{
			    g.chcolor(Color.WHITE);
			    g.fellipse(c, img.ssz.div(2).sub(1, 2));
			}
		    }
		}
		g.chcolor();
	    }
	    g.image(img, ul);
	    if(tip != null && CFG.MMAP_SHOW_MARKER_NAMES.get()) {
		g.aimage(tip.tex(), c.addy(UI.scale(3)), 0.5, 0);
	    }
	} catch (Loading ignored) {}
    }
    
    @Override
    public Area area() {
	try {
	    final Resource res = this.res.loadsaved();
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
