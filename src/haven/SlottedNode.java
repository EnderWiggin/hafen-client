package haven;

import haven.render.RenderTree;

import java.util.ArrayList;
import java.util.Collection;

public class SlottedNode implements RenderTree.Node {
    protected Collection<RenderTree.Slot> slots;
    
    public void added(RenderTree.Slot slot) {
	if(slots == null)
	    slots = new ArrayList<>(1);
	slots.add(slot);
    }
    
    public void removed(RenderTree.Slot slot) {
	if(slots != null)
	    slots.remove(slot);
    }
    
}
