/* Preprocessed source code */
package haven.res.lib.plants;

import haven.*;
import haven.render.*;
import haven.resutil.*;
import me.ender.ClientUtils;

import java.util.*;

@haven.FromResource(name = "lib/plants", version = 11)
public class TrellisPlant implements Sprite.Factory {
    public final int num;
    public final List<? extends List<RenderTree.Node>> var;
    
    public TrellisPlant(int num, List<? extends List<RenderTree.Node>> var) {
	this.num = num;
	this.var = var;
    }
    
    public TrellisPlant(List<? extends List<RenderTree.Node>> var) {
	this(2, var);
    }
    
    public TrellisPlant(Resource res, Object[] args) {
	this.num = ((Number)args[0]).intValue();
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
	double a = ClientUtils.owner2gob(owner).a;
	float ac = (float) Math.cos(a), as = -(float) Math.sin(a);
	int st = sdt.uint8();
	if((st >= this.var.size()) || (this.var.get(st).size() < 1))
	    throw (new Sprite.ResourceException("No variants for grow stage " + st, res));
	List<RenderTree.Node> var = this.var.get(st);
	Random rnd = owner.mkrandoom();
	CSprite spr = new CSprite(owner, res);
	float d = 11f / num;
	float c = -5.5f + (d / 2);
	if(CFG.SIMPLE_CROPS.get()) {
	    spr.addpart(0, 0, Pipe.Op.nil, var.get(0));
	} else {
	    for (int i = 0; i < num; i++) {
		RenderTree.Node v = var.get(rnd.nextInt(var.size()));
		spr.addpart(c * as, c * ac, Pipe.Op.nil, v);
		c += d;
	    }
	}
	return(spr);
    }
}
