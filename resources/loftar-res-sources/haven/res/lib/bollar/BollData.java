/* Preprocessed source code */
package haven.res.lib.bollar;

import haven.*;
import haven.render.*;
import haven.render.sl.*;
import java.util.*;
import java.nio.*;
import static haven.render.sl.Type.*;
import static haven.render.sl.Cons.*;

@haven.FromResource(name = "lib/bollar", version = 3)
public class BollData implements Rendered, RenderTree.Node, Disposable {
    public final VertexArray.Layout fmt;
    public Model model = null;
    public VertexArray va = null;
    private final Collection<RenderTree.Slot> slots = new ArrayList<>(1);

    public BollData(VertexArray.Layout fmt) {
	this.fmt = fmt;
    }

    public void draw(Pipe state, Render out) {
	if(model != null)
	    out.draw(state, model);
    }

    public void dispose() {
	if(model != null) {
	    model.dispose();
	    model = null;
	}
	if(va != null) {
	    va.dispose();
	    va = null;
	}
    }

    public void update(Render d, int nb, DataBuffer.Filler<? super VertexArray.Buffer> fill) {
	if(nb < 1) {
	    dispose();
	    return;
	}
	if((va != null) && (va.bufs[0].size() < nb * fmt.inputs[0].stride)) {
	    va.dispose();
	    va = null;
	}
	if(va == null) {
	    int n = 3 * nb / 2;
	    va = new VertexArray(fmt, new VertexArray.Buffer(n * fmt.inputs[0].stride, DataBuffer.Usage.STREAM, null)).shared();
	}
	d.update(va.bufs[0], fill);
	if((model != null) && (model.n != nb)) {
	    model.dispose();
	    model = null;
	}
	if(model == null) {
	    model = new Model(Model.Mode.POINTS, va, null, 0, nb);
	    for(RenderTree.Slot slot : this.slots)
		slot.update();
	}
    }

    public void added(RenderTree.Slot slot) {
	slots.add(slot);
    }

    public void removed(RenderTree.Slot slot) {
	slots.remove(slot);
    }
}
