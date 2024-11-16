/* Preprocessed source code */
package haven.res.lib.vmat;

import haven.*;
import haven.render.*;
import haven.ModSprite.*;
import java.util.*;
import java.util.function.Consumer;

@haven.FromResource(name = "lib/vmat", version = 38)
public abstract class VarMats extends GAttrib implements Mod {
    public VarMats(Gob gob) {
	super(gob);
    }

    public abstract Material varmat(int id);

    public void operate(Cons cons) {
	for(Part part : cons.parts) {
	    if(part.obj instanceof FastMesh.ResourceMesh) {
		FastMesh.ResourceMesh m = (FastMesh.ResourceMesh)part.obj;
		String sid = m.info.rdat.get("vm");
		int mid = (sid == null) ? -1 : Integer.parseInt(sid);
		if(mid >= 0) {
		    Material vm = varmat(mid);
		    if(vm != null)
			part.wraps.addFirst(vm);
		}
	    }
	}
    }

    public int order() {return(100);}
}
