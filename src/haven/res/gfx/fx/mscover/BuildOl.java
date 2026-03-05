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
public class BuildOl extends Sprite {
    public BuildOl(Owner owner, Resource res, Message sdt) {
	super(owner, res);
	double r = (sdt.float16() * 11) - Radius.ε;
	Gob gob = owner.context(Gob.class);
	try {
	    gob.setattr(new Radius(gob, r, false));
	} catch(NoClassDefFoundError e) {
	}
    }
}
