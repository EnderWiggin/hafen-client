/* Preprocessed source code */
package haven.res.ui.alchbook;

import java.util.*;
import haven.*;
import haven.MenuGrid.*;
import java.awt.Color;
import java.awt.image.BufferedImage;
import static haven.PType.*;

@FromResource(name = "ui/alchbook", version = 1)
public class Icon extends Widget {
    public final ItemSpec spec;

    public Icon(int sz, ItemSpec spec) {
	super(Coord.of(sz, sz));
	this.spec = spec;
    }

    private Tex tex = null;
    public void draw(GOut g) {
	try {
	    if(tex == null)
		tex = new TexI(PUtils.uiscale(spec.image(), sz));
	    g.image(tex, Coord.z);
	} catch(Loading l) {}
    }

    public String tooltip(Coord c, Widget prev) {
	return(spec.name());
    }
}
