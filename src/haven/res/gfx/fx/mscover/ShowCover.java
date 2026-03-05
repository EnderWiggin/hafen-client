/* Preprocessed source code */
/* $use: ui/pag/toggle */

package haven.res.gfx.fx.mscover;

import haven.*;
import haven.render.*;
import java.util.*;
import haven.res.ui.pag.toggle.*;
import haven.MenuGrid.Pagina;
import static haven.MCache.*;

/* >objdelta: Radius */
@FromResource(name = "gfx/fx/mscover", version = 1)
public class ShowCover extends MenuGrid.PagButton {
    public static boolean show;
    public final MapView map;

    public ShowCover(Pagina pag) {
	super(pag);
	MapView map = pag.scm.getparent(GameUI.class).map;
	try {
	    Global.get(map.ui.sess.glob).map = map;
	} catch(NoClassDefFoundError e) {
	    pag.scm.getparent(GameUI.class).error("Please update your client to restore mine-support coverage display.");
	    map = null;
	}
	this.map = map;
    }

    public static class Fac implements Factory {
	public MenuGrid.PagButton make(Pagina pag) {
	    return(new ShowCover(pag));
	}
    }

    public void use(MenuGrid.Interaction iact) {
	if(map == null) {
	    pag.scm.getparent(GameUI.class).error("Please update your client to restore mine-support coverage display.");
	    return;
	}

	show = !show;
	map.updateSupportOverlay();
	pag.scm.ui.msg("Mine-support display is now turned " + (show ? "on" : "off") + ".", null,
		       Audio.resclip(show ? Toggle.sfxon : Toggle.sfxoff));
    }

    public void drawmain(GOut g, GSprite spr) {
	super.drawmain(g, spr);
	g.image(show ? Toggle.on : Toggle.off, Coord.z);
    }
}
