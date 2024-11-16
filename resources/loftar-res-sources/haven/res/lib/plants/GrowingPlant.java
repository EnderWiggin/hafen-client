/* Preprocessed source code */
package haven.res.lib.plants;

import haven.*;
import haven.render.*;
import haven.resutil.*;
import java.util.*;

@haven.FromResource(name = "lib/plants", version = 11)
public class GrowingPlant implements Sprite.Factory {
    public final int num;
    public final List<? extends List<RenderTree.Node>> var;

    public GrowingPlant(int num, List<? extends List<RenderTree.Node>> var) {
	this.num = num;
	this.var = var;
    }

    public GrowingPlant(Resource res, Object[] args) {
	this.num = (((Number)args[0]).intValue());
	ArrayList<ArrayList<RenderTree.Node>> var = new ArrayList<>();
	for(FastMesh.MeshRes mr : res.layers(FastMesh.MeshRes.class)) {
	    int st = mr.id / 10;
	    while(st >= var.size())
		var.add(new ArrayList<RenderTree.Node>());
	    var.get(st).add(mr.mat.get().apply(mr.m));
	}
	for(ArrayList<?> ls : var)
	    ls.trimToSize();
	var.trimToSize();
	this.var = var;
    }

    public Sprite create(Sprite.Owner owner, Resource res, Message sdt) {
	int st = sdt.uint8();
	if((st >= this.var.size()) || (this.var.get(st).size() < 1))
	    throw(new Sprite.ResourceException("No variants for grow stage " + st, res));
	List<RenderTree.Node> var = this.var.get(st);
	Random rnd = owner.mkrandoom();
	CSprite spr = new CSprite(owner, res);
	for(int i = 0; i < num; i++) {
	    RenderTree.Node v = var.get(rnd.nextInt(var.size()));
	    if(num > 1)
		spr.addpart((rnd.nextFloat() * 11f) - 5.5f, (rnd.nextFloat() * 11f) - 5.5f, Pipe.Op.nil, v);
	    else
		spr.addpart((rnd.nextFloat() * 4.4f) - 2.2f, (rnd.nextFloat() * 4.4f) - 2.2f, Pipe.Op.nil, v);
	}
	return(spr);
    }
}
