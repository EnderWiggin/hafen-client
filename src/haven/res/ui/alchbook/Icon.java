/* Preprocessed source code */
package haven.res.ui.alchbook;

import java.util.*;
import java.util.function.*;
import haven.*;
import haven.MenuGrid.*;
import java.awt.Color;
import java.awt.image.BufferedImage;
import static haven.PType.*;

@FromResource(name = "ui/alchbook", version = 4)
public class Icon extends Widget {
    public final ItemSpec spec;
    public final Consumer<ItemSpec> action;
    private Tex tex;

    public Icon(int sz, ItemSpec spec, Consumer<ItemSpec> action) {
	super(Coord.of(sz, sz));
	this.spec = spec;
	this.action = action;
	this.tex = new TexI(Book.rowscalea(spec.image(), sz));
    }
    public Icon(int sz, ItemSpec spec) {
	this(sz, spec, null);
    }

    public void draw(GOut g) {
	g.aimage(tex, sz.div(2), 0.5, 0.5);
    }

    public Object tooltip(Coord c, Widget prev) {
	return(spec.name());
    }

    public boolean mousedown(MouseDownEvent ev) {
	if((action != null) && (ev.b == 1)) {
	    action.accept(spec);
	    return(true);
	}
	return(super.mousedown(ev));
    }
}
