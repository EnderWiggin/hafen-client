/* Preprocessed source code */
package haven.res.lib.vmat;

import haven.*;
import haven.render.*;
import haven.ModSprite.*;
import java.util.*;
import java.util.function.Consumer;

@haven.FromResource(name = "lib/vmat", version = 38)
public abstract class Mapping extends GAttrib {
    public abstract Material mergemat(Material orig, int mid);

    public Mapping(Gob gob) {
	super(gob);
    }

    public RenderTree.Node[] apply(Resource res) {
	Collection<RenderTree.Node> rl = new LinkedList<RenderTree.Node>();
	for(FastMesh.MeshRes mr : res.layers(FastMesh.MeshRes.class)) {
	    String sid = mr.rdat.get("vm");
	    int mid = (sid == null)?-1:Integer.parseInt(sid);
	    if(mid >= 0) {
		rl.add(mergemat(mr.mat.get(), mid).apply(mr.m));
	    } else if(mr.mat != null) {
		rl.add(mr.mat.get().apply(mr.m));
	    }
	}
	return(rl.toArray(new RenderTree.Node[0]));
    }

    public final static Mapping empty = new Mapping(null) {
	    public Material mergemat(Material orig, int mid) {
		return(orig);
	    }
	};
}

/* >objdelta: Materials */
