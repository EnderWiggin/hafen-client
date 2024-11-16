/* Preprocessed source code */
/* $use: ui/tt/wear */

package haven.res.ui.tt.gast;

import haven.*;
import java.awt.image.BufferedImage;
import haven.res.ui.tt.wear.Wear;

/* >tt: Gast */
@haven.FromResource(name = "ui/tt/gast", version = 11)
public class Gast extends ItemInfo.Tip implements GItem.NumberInfo {
    public final double glut, fev;

    public Gast(Owner owner, double glut, double fev) {
	super(owner);
	this.glut = glut;
	this.fev = fev;
    }

    public static ItemInfo mkinfo(Owner owner, Object... args) {
	return(new Gast(owner, ((Number)args[1]).doubleValue(), ((Number)args[2]).doubleValue()));
    }

    public BufferedImage tipimg() {
	StringBuilder buf = new StringBuilder();
	if(glut != 0.0)
	    buf.append(String.format("Hunger reduction: %s%%\n", Utils.odformat2(100 * glut, 1)));
	if(fev != 0.0)
	    buf.append(String.format("Food event bonus: %s%%\n", Utils.odformat2(100 * fev, 1)));
	return(RichText.render(buf.toString(), 0).img);
    }

    public int itemnum() {
	Wear wear = find(Wear.class, owner.info());
	if(wear == null)
	    return(-1);
	return(wear.m - wear.d);
    }

    public Tex overlay() {
	return((itemnum() >= 0) ? GItem.NumberInfo.super.overlay() : null);
    }

    public void drawoverlay(GOut g, Tex tex) {
	if(tex != null)
	    GItem.NumberInfo.super.drawoverlay(g, tex);
    }
}
