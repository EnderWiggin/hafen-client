/* Preprocessed source code */
package haven.res.ui.alchbook;

import java.util.*;
import haven.*;
import haven.MenuGrid.*;
import java.awt.Color;
import java.awt.image.BufferedImage;
import static haven.PType.*;

@FromResource(name = "ui/alchbook", version = 1)
public class EffectIcon extends Widget {
    public final EffectInfo eff;
    private final Tex tex;

    public EffectIcon(EffectInfo eff) {
	this.eff = eff;
	BufferedImage img = eff.image();
	Coord sz = PUtils.imgsz(img);
	sz = sz.mul(Book.HEIGHT).div(sz.y);
	tex = new TexI(PUtils.uiscale(img, sz));
	resize(sz);
    }

    public void draw(GOut g) {
	g.image(tex, Coord.z);
    }

    public String tooltip(Coord c, Widget prev) {
	return(eff.desc());
    }
}
