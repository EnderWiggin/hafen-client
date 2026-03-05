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
@haven.FromResource(name = "gfx/fx/mscover", version = 1)
public class Radius extends GAttrib {
    public static final double ε = 0.01 * 11;
    public final Global gl;
    public final double r;
    public final boolean real;
    public Coord2d cc;
    public int cfl = 0, chseq = 1, useq = 0;
    public boolean removed = false;

    public Radius(Gob owner, double r, boolean real) {
	super(owner);
	this.r = r;
	this.real = real;
	this.gl = Global.get(gob.glob);
	gl.add(this);
    }

    public int fl() {
	GobHealth h = gob.getattr(GobHealth.class);
	return((real ? 0 : 1) | (((h != null) && (h.hp < 0.9)) ? 2 : 0));
    }

    public void dispose() {
	super.dispose();
	removed = true;
    }

    public void ctick(double dt) {
	if(!Utils.eq(gob.rc, cc) || (fl() != cfl))
	    gl.update = true;
    }

    public static void parse(Gob gob, Message dat) {
	try {
	    gob.setattr(new Radius(gob, (dat.float16() * 11) - ε, true));
	} catch(NoClassDefFoundError e) {
	}
    }
}

/* Only used for placement info */
/* >spr: BuildOl */
