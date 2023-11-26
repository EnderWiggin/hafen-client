/* Preprocessed source code */
/* $use: gfx/fx/bprad */

package haven.res.gfx.fx.msrad;

import java.awt.*;
import java.util.*;

import haven.*;
import haven.render.*;

/* >spr: MSRad */
@haven.FromResource(name = "gfx/fx/msrad", version = 15)
public class MSRad extends Sprite {
    public static boolean show = false;
    public static Collection<MSRad> current = new WeakList<>();
    final ColoredRadius fx;
    final Collection<RenderTree.Slot> slots = new ArrayList<>(1);
    
    public MSRad(Owner owner, Resource res, float r, Color color1, Color color2) {
	super(owner, res);
	fx = new ColoredRadius((Gob) owner, r, color1, color2);
    }
    
    public MSRad(Owner owner, Resource res, float r, Color color) {
	this(owner, res, r, color, color);
    }
    
    public MSRad(Owner owner, Resource res, float r) {
	this(owner, res, r, new Color(128, 128, 128, 128), new Color(128, 192, 192));
    }
    
    public MSRad(Owner owner, Resource res, Message sdt) {
	this(owner, res, Utils.hfdec((short) sdt.int16()) * 11);
    }
    
    public MSRad(Owner owner, float r, Color color1, Color color2) {
	this(owner, null, r, color1, color2);
    }
    
    public static void show(boolean show) {
	for (MSRad spr : current)
	    spr.show1(show);
	MSRad.show = show;
    }
    
    public void show1(boolean show) {
	if(show) {
	    Loading.waitfor(() -> RUtils.multiadd(slots, fx));
	} else {
	    for (RenderTree.Slot slot : slots)
		slot.clear();
	}
    }
    
    public void added(RenderTree.Slot slot) {
	if(show)
	    slot.add(fx);
	if(slots.isEmpty())
	    current.add(this);
	slots.add(slot);
    }
    
    @Override
    public void gtick(Render g) {
	fx.gtick(g);
    }
    
    public void removed(RenderTree.Slot slot) {
	slots.remove(slot);
	if(slots.isEmpty())
	    current.remove(this);
    }
}

/* >pagina: ShowSupports$Fac */
