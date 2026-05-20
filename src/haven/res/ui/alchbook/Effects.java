/* Preprocessed source code */
package haven.res.ui.alchbook;

import java.util.*;
import haven.*;
import haven.MenuGrid.*;
import java.awt.Color;
import java.awt.image.BufferedImage;
import static haven.PType.*;

@FromResource(name = "ui/alchbook", version = 1)
public class Effects extends Widget {
    public Effects(List<EffectInfo> effects) {
	Coord pos = Coord.z;
	for(EffectInfo eff : effects)
	    pos = add(new EffectIcon(eff), pos).pos("ur");
	pack();
    }
}

/* >wdg: BookFac */
