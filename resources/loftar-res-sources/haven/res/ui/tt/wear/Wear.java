/* Preprocessed source code */
package haven.res.ui.tt.wear;

import haven.*;
import java.awt.image.BufferedImage;

/* >tt: Wear */
@haven.FromResource(name = "ui/tt/wear", version = 4)
public class Wear extends ItemInfo.Tip {
    public final int d, m;

    public Wear(Owner owner, int d, int m) {
	super(owner);
	this.d = d;
	this.m = m;
    }

    public static ItemInfo mkinfo(Owner owner, Object... args) {
	return(new Wear(owner, (Integer)args[1], (Integer)args[2]));
    }

    public BufferedImage tipimg() {
	if(d >= m)
	    return(RichText.render(String.format("Wear: $col[255,128,128]{%,d/%,d}", d, m), 0).img);
	return(RichText.render(String.format("Wear: %,d/%,d", d, m)).img);
    }
}
