package haven;

import haven.res.gfx.fx.msrad.MSRad;

import java.util.function.Function;

public class SquareRadiiOverlay {
    private Area area;
    private final MCache map;
    private final MCache.OverlayInfo safe;
    private final MCache.OverlayInfo danger;
    private MCache.Overlay ol;
    
    private final Gob gob;
    private final float radius;
    private final Function<Coord, Boolean> mask = this::mask;
    private float hp;
    
    public SquareRadiiOverlay(Gob gob, float radius, MCache.OverlayInfo safe) {
	this(gob, radius, safe, null);
    }
    
    public SquareRadiiOverlay(Gob gob, float radius, MCache.OverlayInfo safe, MCache.OverlayInfo danger) {
	this.map = gob.glob.map;
	this.safe = safe;
	this.danger = danger;
	
	this.gob = gob;
	this.radius = radius;
	
	hp = gob.hp();
	update();
    }
    
    private Boolean mask(Coord c) {
	boolean inRange = isInRange(c);
	if(!inRange || danger == null || !safe()) {return inRange;}
	
	for (SquareRadiiOverlay ol : gob.glob.oc.msols()) {
	    if(!ol.safe() && ol.isInRange(c)) {return false;}
	}
	
	return true;
    }

    private boolean isInRange(Coord c) {
	return c.mul(MCache.tilesz).add(MCache.tilesz.div(2, 2)).dist(gob.rc) <= radius;
    }
    
    public void add() {
	if(ol != null) {return;}
	ol = map.new Overlay(area, olid());
	ol.mask(mask);
    }
    
    public void rem() {
	if(ol == null) {return;}
	ol.destroy();
	ol = null;
    }
    
    public void update() {
	int k = (int) (radius / MCache.tilesz.x) + 1;//add 1 tile border
	Coord c = gob.rc.floor(MCache.tilesz);
	area = Area.sized(c.sub(k, k), Coord.of(2 * k + 1));
	
	gob.glob.oc.dirtyMSOls = true;
	if(ol != null) {ol.update(olid(), area);}
    }
    
    public void checkHP() {
	float chp = gob.hp();
	if(hp != chp) {
	    hp = chp;
	    update();
	}
    }
    
    private boolean safe() {
	return gob.hp() > MSRad.LOW_HP;
    }
    
    private MCache.OverlayInfo olid() {
	return (danger == null || safe()) ? safe : danger;
    }
}
