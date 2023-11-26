/* Preprocessed source code */
package haven.res.lib.plants;

import haven.*;
import haven.resutil.*;
import java.util.*;

@haven.FromResource(name = "lib/plants", version = 9)
public class GrowingPlant implements Sprite.Factory {
    public final int num;

    public GrowingPlant(int num) {
	this.num = num;
    }

    public GrowingPlant(Object[] args) {
	this(((Number)args[0]).intValue());
    }

    public Sprite create(Sprite.Owner owner, Resource res, Message sdt) {
	int st = sdt.uint8();
	ArrayList<FastMesh.MeshRes> var = new ArrayList<FastMesh.MeshRes>();
	for(FastMesh.MeshRes mr : res.layers(FastMesh.MeshRes.class)) {
	    if((mr.id / 10) == st)
		var.add(mr);
	}
	if(var.size() < 1)
	    throw(new Sprite.ResourceException("No variants for grow stage " + st, res));
	Random rnd = owner.mkrandoom();
	CSprite spr = new CSprite(owner, res);
	if(CFG.SIMPLE_CROPS.get()) {
	    FastMesh.MeshRes mesh = var.get(0);
	    spr.addpart(0, 0, mesh.mat.get(), mesh.m);
	} else {
	    for (int i = 0; i < num; i++) {
		FastMesh.MeshRes v = var.get(rnd.nextInt(var.size()));
		if(num > 1)
		    spr.addpart((rnd.nextFloat() * 11f) - 5.5f, (rnd.nextFloat() * 11f) - 5.5f, v.mat.get(), v.m);
		else
		    spr.addpart((rnd.nextFloat() * 4.4f) - 2.2f, (rnd.nextFloat() * 4.4f) - 2.2f, v.mat.get(), v.m);
	    }
	}
	return(spr);
    }
}
