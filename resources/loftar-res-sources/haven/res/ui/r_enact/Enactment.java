/* Preprocessed source code */
package haven.res.ui.r_enact;

import haven.*;
import haven.CharWnd.LoadingTextBox;
import java.util.*;
import java.awt.Color;

@haven.FromResource(name = "ui/r-enact", version = 64)
public class Enactment {
    public final int id;
    public final Indir<Resource> res;
    public int lvl, mlvl;
    public Cost cost, dcost, icost;
    String sortkey = "\uffff";

    public Enactment(int id, Indir<Resource> res) {
	this.id = id;
	this.res = res;
    }

    public String rendertext() {
	StringBuilder buf = new StringBuilder();
	Resource res = this.res.get();
	buf.append("$img[" + res.name + "]\n\n");
	buf.append("$b{$font[serif,16]{" + res.layer(Resource.tooltip).t + "}}\n\n\n");
	buf.append(res.layer(Resource.pagina).text);
	return(buf.toString());
    }
}

/* >wdg: Enactments */
