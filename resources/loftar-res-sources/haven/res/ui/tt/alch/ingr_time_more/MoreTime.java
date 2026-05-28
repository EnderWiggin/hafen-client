/* Preprocessed source code */
/* $use: ui/tt/alch/effect */
/* $use: ui/alchbook */
package haven.res.ui.tt.alch.ingr_time_more;

import haven.*;
import haven.res.ui.tt.alch.effect.*;
import haven.res.ui.alchbook.*;
import java.awt.image.BufferedImage;

/* >tt: MoreTime */
@haven.FromResource(name = "ui/tt/alch/ingr-time-more", version = 6)
public class MoreTime extends Effect implements EffectInfo {
    public MoreTime(Owner owner) {super(owner);}

    public static ItemInfo mkinfo(Owner owner, Raw raw, Object... args) {
	return(new MoreTime(owner));
    }

    public String alchtips() {
	return("Increase elixir duration");
    }

    public BufferedImage image() {
	return(Resource.classres(MoreTime.class).flayer(Resource.imgc).img);
    }

    public String desc() {
	return("Increase elixir duration");
    }
}
