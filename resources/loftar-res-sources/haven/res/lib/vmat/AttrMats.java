/* Preprocessed source code */
package haven.res.lib.vmat;

import haven.*;
import haven.render.*;
import haven.ModSprite.*;
import java.util.*;
import java.util.function.Consumer;

@haven.FromResource(name = "lib/vmat", version = 38)
public class AttrMats extends VarMats {
    public final Map<Integer, Material> mats;

    public AttrMats(Gob gob, Map<Integer, Material> mats) {
	super(gob);
	this.mats = mats;
    }

    public Material varmat(int id) {
	return(mats.get(id));
    }
}
