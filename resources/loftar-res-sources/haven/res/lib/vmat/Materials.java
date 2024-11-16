/* Preprocessed source code */
package haven.res.lib.vmat;

import haven.*;
import haven.render.*;
import haven.ModSprite.*;
import java.util.*;
import java.util.function.Consumer;

@haven.FromResource(name = "lib/vmat", version = 38)
public class Materials extends Mapping {
    public static final Map<Integer, Material> empty = Collections.<Integer, Material>emptyMap();
    public final Map<Integer, Material> mats;

    public static Map<Integer, Material> decode(Resource.Resolver rr, Message sdt) {
	Map<Integer, Material> ret = new IntMap<Material>();
	int idx = 0;
	while(!sdt.eom()) {
	    Indir<Resource> mres = rr.getres(sdt.uint16());
	    int mid = sdt.int8();
	    Material.Res mat;
	    if(mid >= 0)
		mat = mres.get().layer(Material.Res.class, mid);
	    else
		mat = mres.get().layer(Material.Res.class);
	    ret.put(idx++, mat.get());
	}
	return(ret);
    }

    private static final Collection<Pair<TexRender.TexDraw, TexRender.TexClip>> warned = new HashSet<>();
    public static Material stdmerge(Material orig, Material var) {
	return(new Material(Pipe.Op.compose(orig.states, var.states),
			    Pipe.Op.compose(orig.dynstates, var.dynstates)));
    }

    public Material mergemat(Material orig, int mid) {
	if(!mats.containsKey(mid))
	    return(orig);
	Material var = mats.get(mid);
	return(stdmerge(orig, var));
    }

    public Materials(Gob gob, Map<Integer, Material> mats) {
	super(gob);
	this.mats = mats;
    }

    public static void parse(Gob gob, Message dat) {
	Map<Integer, Material> mats = decode(gob.context(Resource.Resolver.class), dat);
	gob.setattr(new Materials(gob, mats));
	try {
	    gob.setattr((GAttrib)Utils.construct(Class.forName("haven.res.lib.vmat.AttrMats").getConstructor(new Class[]{Gob.class, Map.class}), gob, mats));
	} catch(ClassNotFoundException | NoSuchMethodException | LinkageError e) {
	    new Warning(e, "could not create mod-sprite varmats; update client").issue();
	}
    }
}
