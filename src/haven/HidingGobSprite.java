package haven;

import haven.render.RenderTree;

import java.util.ArrayList;
import java.util.Collection;

public class HidingGobSprite<T extends RenderTree.Node> extends Sprite {
    private boolean visible = true;
    final Collection<RenderTree.Slot> slots = new ArrayList<>(1);
    public final Collection<T> fxs;
    
    protected HidingGobSprite(Gob gob, Collection<T> fxs) {
	super(gob, null);
	this.fxs = fxs;
    }
    
    /**returns true if visibility actually changed*/
    public boolean show(boolean show) {
	if(show == visible) {return false;}
	visible = show;
	if(show) {
	    for (T fx : fxs) {
		Loading.waitfor(() -> RUtils.multiadd(slots, fx));
	    }
	} else {
	    for (RenderTree.Slot slot : slots)
		slot.clear();
	}
	return true;
    }
    
    public void added(RenderTree.Slot slot) {
	if(visible)
	    for (T fx : fxs) {
		slot.add(fx);
	    }
	slots.add(slot);
    }
    
    public void removed(RenderTree.Slot slot) {
	slots.remove(slot);
    }
}
