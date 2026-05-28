/* Preprocessed source code */
package haven.res.ui.alchbook;

import java.util.*;
import java.util.function.*;
import haven.*;
import haven.MenuGrid.*;
import java.awt.Color;
import java.awt.image.BufferedImage;
import static haven.PType.*;

@haven.FromResource(name = "ui/alchbook", version = 3)
public class EffectIcon extends Widget {
    public final EffectInfo eff;
    public final Consumer<EffectInfo> action;
    private final Tex tex;

    public EffectIcon(EffectInfo eff, Consumer<EffectInfo> action) {
	this.eff = eff;
	this.action = action;
	BufferedImage img = eff.image();
	tex = new TexI(Book.rowscaleh(img));
	resize(tex.sz());
    }

    public void draw(GOut g) {
	g.image(tex, Coord.z);
    }

    public String tooltip(Coord c, Widget prev) {
	return(eff.desc());
    }

    public boolean mousedown(MouseDownEvent ev) {
	if((action != null) && (ev.b == 1)) {
	    action.accept(eff);
	    return(true);
	}
	return(super.mousedown(ev));
    }
}
