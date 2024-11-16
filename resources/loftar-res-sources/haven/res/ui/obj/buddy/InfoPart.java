/* Preprocessed source code */
package haven.res.ui.obj.buddy;

import haven.*;
import haven.render.*;
import java.util.*;
import java.awt.Color;
import java.awt.Font;
import java.awt.image.BufferedImage;
import static haven.PUtils.*;

@haven.FromResource(name = "ui/obj/buddy", version = 4)
public interface InfoPart {
    public static final Text.Foundry fnd = new Text.Foundry(Text.sans.deriveFont(Font.BOLD, UI.scale(12f))).aa(true);

    public void draw(CompImage cmp, RenderContext ctx);
    public default int order() {return(0);}
    public default boolean auto() {return(false);}

    public static BufferedImage rendertext(String str, Color col) {
	return(rasterimg(blurmask2(fnd.render(str, col).img.getRaster(), UI.rscale(1.0), UI.rscale(1.0), Color.BLACK)));
    }
}

/* >objdelta: Buddy */
