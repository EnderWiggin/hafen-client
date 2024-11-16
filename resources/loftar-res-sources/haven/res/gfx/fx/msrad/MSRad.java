/* Preprocessed source code */
/* $use: gfx/fx/bprad */
/* $use: ui/pag/toggle */

package haven.res.gfx.fx.msrad;

import java.util.*;
import haven.*;
import haven.render.*;
import haven.MenuGrid.Pagina;
import haven.res.gfx.fx.bprad.*;
import haven.res.ui.pag.toggle.*;

/* >spr: MSRad */
@haven.FromResource(name = "gfx/fx/msrad", version = 16)
public class MSRad extends Sprite {
    public static boolean show = false;
    public static Collection<MSRad> current = new WeakList<>();
    final Sprite fx;
    final Collection<RenderTree.Slot> slots = new ArrayList<>(1);

    public MSRad(Owner owner, Resource res, Message sdt) {
	super(owner, res);
	fx = new BPRad(owner, res, Utils.hfdec((short)sdt.int16()) * 11);
    }

    public static void show(boolean show) {
	for(MSRad spr : current)
	    spr.show1(show);
	MSRad.show = show;
    }

    public void show1(boolean show) {
	if(show) {
	    Loading.waitfor(() -> RUtils.multiadd(slots, fx));
	} else {
	    for(RenderTree.Slot slot : slots)
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

    public void removed(RenderTree.Slot slot) {
	slots.remove(slot);
	if(slots.isEmpty())
	    current.remove(this);
    }
}

/* >pagina: ShowSupports$Fac */
