package haven;


import me.ender.minimap.*;

import java.awt.*;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Random;

import static haven.MCache.*;

public class MapWnd2 extends MapWnd {
    private boolean switching = true;
    private final Map<Long, GobMarker> markers = new HashMap<>();
    
    public MapWnd2(MapFile file, MapView mv, Coord sz, String title) {
	super(file, mv, sz, title);
    }
    
    @Override
    protected String cfgName(String cap) {
	return super.cfgName(cap) + (compact() ? ".compact" : "");
    }
    
    private boolean compact() {
	return tool != null && !tool.visible;
    }
    
    @Override
    public void compact(boolean a) {
	switching = true;
	storeCfg();
	super.compact(a);
	String name = cfgName(caption());
	cfg = WidgetCfg.get(name);
	initCfg();
	switching = false;
    }
    
    @Override
    public void resize(Coord sz) {
	super.resize(sz);
	if(cfg != null) {
	    cfg.sz = ca().sz();
	}
	if(!switching) {storeCfg();}
    }
    
    @Override
    protected void initCfg() {
	if(cfg != null && cfg.sz != null) {
	    sz = cfg.sz;
	    super.resize(sz);
	}
	super.initCfg();
    }
    
    @Override
    protected void setCfg() {
	super.setCfg();
	cfg.sz = sz;
    }
    
    public void addMarker(Gob gob) {
	addMarker(gob.rc.floor(tilesz), gob.tooltip());
    }
    
    public void addMarker(Coord at) {
	addMarker(at, "New marker");
    }
    
    public void addMarker(Coord at, String name) {
	at = at.add(view.sessloc.tc);
	Marker nm = new PMarker(view.sessloc.seg.id, at, name, BuddyWnd.gc[new Random().nextInt(BuddyWnd.gc.length)]);
	file.add(nm);
	focus(nm);
	if(ui.modctrl) {
	    ui.gui.track(nm);
	}
	domark = false;
    }
    
    public void removeMarker(Marker marker) {
	if(tool.list.sel != null && tool.list.sel.mark == marker) {
	    if(mremove != null) {
		mremove.click();
	    } else {
		view.file.remove(marker);
		ui.gui.untrack(marker);
	    }
	}
    }
    
    public void updateGobMarkers() {
	Map<Long, GobMarker> markers;
	synchronized (this.markers) {
	    markers = new HashMap<>(this.markers);
	}
	markers.values().forEach(GobMarker::update);
    }
    
    public void track(Gob gob) {
	GobMarker marker;
	synchronized (this.markers) {
	    if(markers.containsKey(gob.id)) {
		marker = markers.get(gob.id);
	    } else {
		marker = new GobMarker(gob);
		markers.put(gob.id, marker);
	    }
	}
	ui.gui.track(marker);
	domark = false;
    }
    
    public void untrack(long gobid) {
	synchronized (markers) {
	    markers.remove(gobid);
	}
    }
    
    public void markobj(String icon, String name, Coord2d mc) {
	synchronized (deferred) {
	    deferred.add(() -> {
		final Coord tc = mc.floor(tilesz);
		MCache.Grid obg = ui.sess.glob.map.getgrid(tc.div(cmaps));
		if(!view.file.lock.writeLock().tryLock())
		    throw (new Loading());
		try {
		    MapFile.GridInfo info = view.file.gridinfo.get(obg.id);
		    if(info == null)
			throw (new Loading());
		    Coord sc = tc.add(info.sc.sub(obg.gc).mul(cmaps));
		    //Check for duplicate
		    for (final Marker mark : view.file.markers) {
			if(mark instanceof CustomMarker) {
			    if(mark.seg == info.seg && sc.equals(mark.tc) && icon.equals(((CustomMarker) mark).res.name)) {
				return;
			    }
			}
		    }
		    
		    final Marker mark = new CustomMarker(info.seg, sc, name, Color.WHITE, new Resource.Spec(Resource.remote(), icon));
		    view.file.add(mark);
		} finally {
		    view.file.lock.writeLock().unlock();
		}
	    });
	}
    }
    
    public void markobj(AutoMarkers.Mark mark, Coord2d mc) {
	markobj(mark.res, mark.name, mc);
    }
    
    public class GobMarker extends Marker {
	public final long gobid;
	public final Indir<Resource> res;
	private Coord2d rc = null;
	public final Color col;
	
	public GobMarker(Gob gob) {
	    super(0, gob.rc.floor(tilesz), gob.tooltip());
	    this.gobid = gob.id;
	    GobIcon icon = gob.getattr(GobIcon.class);
	    res = (icon == null) ? null : icon.res;
	    col = color(gob);
	}
	
	private Color color(Gob gob) {
	    if(gob.anyOf(GobTag.FOE, GobTag.AGGRESSIVE)) {
		return new Color(220, 100, 100);
	    } else if(gob.is(GobTag.FRIEND)) {
		return new Color(100, 220, 100);
	    } else if(gob.is(GobTag.ANIMAL)) {
		return new Color(100, 200, 220);
	    }
	    return Color.LIGHT_GRAY;
	}
    
	public void update() {
	    Gob gob = ui.sess.glob.oc.getgob(gobid);
	    if(gob != null) {
		seg = view.sessloc.seg.id;
		try {
		    rc = gob.rc.add(view.sessloc.tc.mul(tilesz));
		    tc = rc.floor(tilesz);
		} catch (Exception ignore) {}
	    }
	}
    
	public Coord2d rc() {
	    try {
		return rc.sub(view.sessloc.tc.mul(tilesz));
	    } catch (Exception ignore) {}
	    return null;
	}
    
	public boolean hide() {
	    Gob gob = ui.sess.glob.oc.getgob(gobid);
	    Gob player = ui.gui.map.player();
	    if(gob != null && player != null) {
		if(gob.drivenByPlayer) {
		    return true;
		}
		if(gob.moving instanceof Following) {
		    return ((Following) gob.moving).tgt == gob.glob.sess.ui.gui.map.plgob;
		} else if(gob.moving instanceof Homing) {
		    Homing homing = (Homing) gob.moving;
		    return homing.dist < 30 && homing.tgt == gob.glob.sess.ui.gui.map.plgob;
		}
		return gob.is(GobTag.VEHICLE) && gob.rc.dist(player.rc) < 25;
	    }
	    return false;
	}
    
	@Override
	public void draw(GOut g, Coord c, Text tip, float scale, MapFile file) {
	
	}
    
	@Override
	public Area area() {
	    return null;
	}
    
	@Override
	public int hashCode() {
	    return Objects.hash(gobid);
	}
    }
}
