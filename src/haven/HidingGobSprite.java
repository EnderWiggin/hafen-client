package haven;

import haven.render.RenderTree;

import java.util.ArrayList;
import java.util.Collection;

public class HidingGobSprite<T extends RenderTree.Node> extends Sprite {
    private boolean visible;
    final Collection<RenderTree.Slot> slots = new ArrayList<>(1);
    public final T fx;
    
    protected HidingGobSprite(Gob gob, T fx) {
	this(gob, fx, true);
    }
    
    protected HidingGobSprite(Gob gob, T fx, boolean visible) {
	super(gob, null);
	this.fx = fx;
	this.visible = visible;
    }
    
    /**returns true if visibility actually changed*/
    public boolean show(boolean show) {
	if(show == visible) {return false;}
	visible = show;
	if(show) {
	    Loading.waitfor(() -> RUtils.multiadd(slots, fx));
	} else {
	    for (RenderTree.Slot slot : slots)
		slot.clear();
	}
	return true;
    }
    
    public void added(RenderTree.Slot slot) {
	if(visible)
	    slot.add(fx);
	slots.add(slot);
    }
    
    public void removed(RenderTree.Slot slot) {
	slots.remove(slot);
    }
}
