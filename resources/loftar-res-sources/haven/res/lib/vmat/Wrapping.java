/* Preprocessed source code */
package haven.res.lib.vmat;

import haven.*;
import haven.render.*;
import haven.ModSprite.*;
import java.util.*;
import java.util.function.Consumer;

@haven.FromResource(name = "lib/vmat", version = 38)
public class Wrapping extends Pipe.Op.Wrapping {
    public final int mid;

    public Wrapping(RenderTree.Node r, Pipe.Op st, int mid) {
	super(r, st, true);
	this.mid = mid;
    }

    public String toString() {
	return(String.format("#<vmat %s %s>", mid, op));
    }
}

/* >spr: VarSprite */
