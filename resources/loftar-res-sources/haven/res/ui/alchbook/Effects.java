/* Preprocessed source code */
package haven.res.ui.alchbook;

import java.util.*;
import java.util.function.*;
import haven.*;
import haven.MenuGrid.*;
import java.awt.Color;
import java.awt.image.BufferedImage;
import static haven.PType.*;

@haven.FromResource(name = "ui/alchbook", version = 4)
public class Effects extends Widget {
    public Effects(List<EffectInfo> effects, Consumer<EffectInfo> action) {
	Coord pos = Coord.z;
	for(EffectInfo eff : effects)
	    pos = add(new EffectIcon(eff, action), pos).pos("ur").adds(2, 0);
	pack();
    }
}
