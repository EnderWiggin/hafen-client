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
public class Info extends GAttrib implements RenderTree.Node, PView.Render2D {
    public final List<InfoPart> parts = new ArrayList<>();
    private Tex rend = null;
    private boolean dirty;
    private double seen = 0;
    private boolean auto;

    public Info(Gob gob) {
	super(gob);
    }

    public void draw(GOut g, Pipe state) {
	Coord sc = Homo3D.obj2view(new Coord3f(0, 0, 15), state, Area.sized(g.sz())).round2();
	if(dirty) {
	    RenderContext ctx = state.get(RenderContext.slot);
	    CompImage cmp = new CompImage();
	    dirty = false;
	    auto = false;
	    synchronized(parts) {
		for(InfoPart part : parts) {
		    try {
			part.draw(cmp, ctx);
			auto |= part.auto();
		    } catch(Loading l) {
			dirty = true;
		    }
		}
	    }
	    rend = cmp.sz.equals(Coord.z) ? null : new TexI(cmp.compose());
	}
	if((rend != null) && sc.isect(Coord.z, g.sz())) {
	    double now = Utils.rtime();
	    if(seen == 0)
		seen = now;
	    double tm = now - seen;
	    Color show = null;
	    if(false) {
		/* XXX: QQ, RIP in peace until constant
		 * mouse-over checks can be had. */
		if(auto && (tm < 7.5)) {
		    show = Utils.clipcol(255, 255, 255, (int)(255 - ((255 * tm) / 7.5)));
		}
	    } else {
		show = Color.WHITE;
	    }
	    if(show != null) {
		g.chcolor(show);
		g.aimage(rend, sc, 0.5, 1.0);
		g.chcolor();
	    }
	} else {
	    seen = 0;
	}
    }

    public void dirty() {
	if(rend != null)
	    rend.dispose();
	rend = null;
	dirty = true;
    }

    public static Info add(Gob gob, InfoPart part) {
	Info info = gob.getattr(Info.class);
	if(info == null)
	    gob.setattr(info = new Info(gob));
	synchronized(info.parts) {
	    info.parts.add(part);
	    Collections.sort(info.parts, Comparator.comparing(InfoPart::order));
	}
	info.dirty();
	return(info);
    }

    public void remove(InfoPart part) {
	synchronized(parts) {
	    parts.remove(part);
	}
	dirty();
    }
}
