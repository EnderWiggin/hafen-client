package me.ender;

import haven.CFG;
import haven.Coord3f;
import haven.StaticSprite;
import haven.render.Location;
import haven.render.RenderTree;

public class CustomizeStaticSprite {
    public static void added(StaticSprite sprite, RenderTree.Slot slot) {
	try {
	    if (CFG.DISPLAY_DECALS_ON_TOP.get() 
		&& sprite.res.name.equals(ResName.DECAL) 
		&& sprite.owner.getres().name.equals(ResName.CUPBOARD))
	    {
		slot.cstate(Location.xlate(new Coord3f(-5,-5,17.5f)));
	    }
	} catch (Exception ignored) {}
    }
}
