/* Preprocessed source code */
/* $use: ui/pag/toggle */

package haven.res.gfx.fx.mscover;

import haven.*;
import haven.render.*;
import java.util.*;
import haven.res.ui.pag.toggle.*;
import haven.MenuGrid.Pagina;
import static haven.MCache.*;

/* >objdelta: Radius */
@FromResource(name = "gfx/fx/mscover", version = 1)
public class Data {
    public final Area area;
    public final short[] cc, vc, dc;

    public Data(Area area) {
	this.area = area;
	cc = new short[area.rsz()];
	vc = new short[area.rsz()];
	dc = new short[area.rsz()];
    }

    public void mod(Coord2d cc, double r, short[] buf, int m) {
	Area a = Area.corn(cc.sub(r, r).floor(tilesz),
			   cc.add(r, r).ceil(tilesz));
	if((a = a.overlap(area)) == null)
	    return;
	for(Coord tc : a) {
	    if(Coord2d.of(tc).add(0.5, 0.5).mul(tilesz).dist(cc) <= r) {
		buf[area.ridx(tc)] += m;
	    }
	}
    }

    public void mod(Coord2d oc, double r, int fl, int m) {
	mod(oc, r, cc, m);
	if((fl & 1) != 0)
	    mod(oc, r, vc, m);
	if((fl & 2) != 0)
	    mod(oc, r, dc, m);
    }
}
