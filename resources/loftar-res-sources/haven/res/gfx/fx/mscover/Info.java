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
public class Info implements OverlayInfo {
    public static final Collection<String> tags = Collections.singleton("mscover");
    public final Material mat;

    public Info(Material mat) {
	this.mat = mat;
    }

    public Collection<String> tags() {return(tags);}
    public Material mat() {return(mat);}
}
