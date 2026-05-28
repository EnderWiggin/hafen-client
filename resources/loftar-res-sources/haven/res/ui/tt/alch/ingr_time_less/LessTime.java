/* Preprocessed source code */
/* $use: ui/tt/alch/effect */
/* $use: ui/alchbook */
package haven.res.ui.tt.alch.ingr_time_less;

import haven.*;
import haven.res.ui.tt.alch.effect.*;
import haven.res.ui.alchbook.*;
import java.awt.image.BufferedImage;

/* >tt: LessTime */
@haven.FromResource(name = "ui/tt/alch/ingr-time-less", version = 6)
public class LessTime extends Effect implements EffectInfo {
    public LessTime(Owner owner) {super(owner);}

    public static ItemInfo mkinfo(Owner owner, Raw raw, Object... args) {
	return(new LessTime(owner));
    }

    public String alchtips() {
	return("Decrease elixir duration");
    }

    public BufferedImage image() {
	return(Resource.classres(LessTime.class).flayer(Resource.imgc).img);
    }

    public String desc() {
	return("Decrease elixir duration");
    }
}
