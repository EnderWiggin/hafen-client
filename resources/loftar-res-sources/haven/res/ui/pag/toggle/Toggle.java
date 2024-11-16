/* Preprocessed source code */
package haven.res.ui.pag.toggle;

import haven.*;
import haven.MenuGrid.*;

@haven.FromResource(name = "ui/pag/toggle", version = 1)
public class Toggle extends PagButton {
    public static final Resource sfxon  = Loading.waitfor(Resource.classres(Toggle.class).pool.load("sfx/hud/on", 1)::get);
    public static final Resource sfxoff = Loading.waitfor(Resource.classres(Toggle.class).pool.load("sfx/hud/off", 1)::get);
    public static final Resource.Image on  = Resource.classres(Toggle.class).flayer(Resource.imgc, 0);
    public static final Resource.Image off = Resource.classres(Toggle.class).flayer(Resource.imgc, 1);
    public final boolean a;

    public Toggle(Pagina pag, boolean a) {
	super(pag);
	this.a = a;
    }

    public void drawmain(GOut g, GSprite spr) {
	super.drawmain(g, spr);
	g.image(a ? on : off, Coord.z);
    }
}

/* >pagina: Fac */
