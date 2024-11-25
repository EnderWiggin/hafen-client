/* Preprocessed source code */
/* $use: gfx/fx/bprad */
/* $use: ui/pag/toggle */

package haven.res.gfx.fx.msrad;

import java.awt.*;
import java.util.*;

import haven.*;
import haven.render.*;
import me.ender.CFGOverlayId;

/* >spr: MSRad */
@haven.FromResource(name = "gfx/fx/msrad", version = 16)
public class MSRad extends Sprite {
    private static final double TICK_RATE = 0.1;
    public static final float LOW_HP = 0.25f;
    public static boolean show = false;
    public static Collection<MSRad> current = new WeakList<>();
    public static final String OL_TAG = "mine_support";
    final ColoredRadius circle;
    public final SquareRadiiOverlay overlay;
    final Collection<RenderTree.Slot> slots = new ArrayList<>(1);
    
    public static final MCache.OverlayInfo safeol = new CFGOverlayId(CFG.COLOR_MINE_SUPPORT_OVERLAY, OL_TAG);
    public static final MCache.OverlayInfo dangerol = new CFGOverlayId(CFG.COLOR_MINE_SUPPORT_DAMAGED_OVERLAY, OL_TAG);
    
    private double timer = TICK_RATE;
    
    public MSRad(Owner owner, Resource res, float r, Color color1, Color color2) {
	super(owner, res);
	Gob gob = (Gob) owner;
	circle = new ColoredRadius(gob, r, color1, color2);
	overlay = new SquareRadiiOverlay(gob, r, safeol, dangerol);
    }
    
    public MSRad(Owner owner, Resource res, float r, Color color) {
	this(owner, res, r, color, color);
    }
    
    public MSRad(Owner owner, Resource res, float r) {
	this(owner, res, r, new Color(128, 128, 128, 128), new Color(128, 192, 192));
    }
    
    public MSRad(Owner owner, Resource res, Message sdt) {
	this(owner, res, Utils.hfdec((short) sdt.int16()) * 11);
    }
    
    public MSRad(Owner owner, float r, Color color1, Color color2) {
	this(owner, null, r, color1, color2);
    }
    
    public static void show(boolean show) {
	if(MSRad.show == show) {return;}
	for (MSRad spr : current)
	    spr.show1(show);
	MSRad.show = show;
    }
    
    public void show1(boolean show) {
	if(show) {
	    if(useRadii()) {
		Loading.waitfor(() -> RUtils.multiadd(slots, circle));
	    }
	} else {
	    for (RenderTree.Slot slot : slots)
		slot.clear();
	}
    }
    
    public void added(RenderTree.Slot slot) {
	if(show) {
	    if(useRadii()) {
		slot.add(circle);
	    }
	}
	if(slots.isEmpty()) {
	    current.add(this);
	    if(!useRadii()) {overlay.add();}
	}
	slots.add(slot);
    }
    
    @Override
    public void gtick(Render g) {
	circle.gtick(g);
    }
    
    @Override
    public boolean tick(double dt) {
	timer -= dt;
	if(timer <= 0) {
	    timer = TICK_RATE;
	    overlay.checkHP();
	}
	return super.tick(dt);
    }
    
    public void removed(RenderTree.Slot slot) {
	slots.remove(slot);
	if(slots.isEmpty()) {
	    current.remove(this);
	    overlay.rem();
	}
    }
    
    private boolean useRadii() {
	String resid = ((Gob)owner).resid();
	if(resid == null || !CFG.SHOW_MINE_SUPPORT_AS_OVERLAY.get()) {return true;}
	switch (resid) {
	    case "gfx/terobjs/minesupport":
	    case "gfx/terobjs/column":
	    case "gfx/terobjs/trees/towercap":
	    case "gfx/terobjs/map/naturalminesupport":
	    case "gfx/terobjs/ladder":
	    case "gfx/terobjs/minebeam":
		return false;
	}
	
	return true;
    }
}

/* >pagina: ShowSupports$Fac */
