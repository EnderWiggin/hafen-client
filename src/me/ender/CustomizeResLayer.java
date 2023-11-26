package me.ender;

import haven.CFG;
import haven.Resource;
import haven.Skeleton;

import java.util.Objects;

public class CustomizeResLayer {
    public static <I, L extends Resource.IDLayer<I>> boolean needReturnNull(Resource res, Class<L> cl, I id) {
	
	//skip 'decal' bone offset for cupboards so decals would be positioned statically at (0,0,0) and not moving on the door
	if(CFG.DISPLAY_DECALS_ON_TOP.get() 
	    && cl == Skeleton.BoneOffset.class
	    && res.name.equals(ResName.CUPBOARD) 
	    && Objects.equals(id, "decal")) {
	    return true;
	}
	
	
	return false;
    }
}
