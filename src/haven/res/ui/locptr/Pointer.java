/* Preprocessed source code */
package haven.res.ui.locptr;

import haven.*;
import haven.render.*;
import me.ender.ClientUtils;
import me.ender.minimap.Marker;
import me.ender.minimap.PMarker;
import me.ender.minimap.SMarker;

import java.awt.Color;
import java.util.Objects;

import static haven.MCache.*;
import static haven.OCache.*;
import static java.lang.Math.*;

/* >wdg: Pointer */
@FromResource(name = "ui/locptr", version = 21)
public class Pointer extends Widget implements MiniMap.IPointer, DTarget {
    private static final Color TRIANGULATION_COLOR = new Color(100, 100, 100);
    public static final BaseColor[] colors = new BaseColor[]{
	new BaseColor(new Color(241, 227, 157, 255)),
	new BaseColor(new Color(189, 157, 241, 255)),
	new BaseColor(new Color(209, 241, 157, 255)),
	new BaseColor(new Color(157, 212, 241, 255)),
	new BaseColor(new Color(241, 157, 196, 255)),
	new BaseColor(new Color(157, 241, 205, 255)),
	new BaseColor(new Color(241, 193, 157, 255)),
    };
    
    private BaseColor col = null;
    public Indir<Resource> icon;
    public Coord2d tc, mc;
    public Coord lc;
    public Marker marker;
    public long gobid = -1;
    public boolean click;
    private Tex licon;
    private String tip = null;
    private boolean triangulating = false;
    long lastseg = -1;
    Coord lastsegtc = null;

    public Pointer(Indir<Resource> icon) {
	super(Coord.z);
	this.icon = icon;
    }

    public Pointer(Marker marker) {
	super(Coord.z);
	this.marker = marker;
	tip = marker.nm;
	if(marker instanceof PMarker) {
	    col = new BaseColor(((PMarker) marker).color);
	} else if(marker instanceof SMarker) {
	    Resource.Saved spec = ((SMarker) marker).res;
	    icon = new Resource.Spec(spec.pool, spec.name, -1);
	    col = colors[0];
	} else if(marker instanceof MapWnd2.GobMarker) {
	    MapWnd2.GobMarker gobMarker = (MapWnd2.GobMarker) marker;
	    col = new BaseColor(gobMarker.col);
	    icon = gobMarker.res;
	}
    }
    
    public static Widget mkwidget(UI ui, Object... args) {
	int iconid = (Integer)args[0];
	Indir<Resource> icon = (iconid < 0) ? null : ui.sess.getres(iconid);
	return(new Pointer(icon));
    }
	
    public void presize() {
	resize(parent.sz);
    }

    protected void added() {
	presize();
	super.added();
    }

    private int signum(int a) {
	if(a < 0) return(-1);
	if(a > 0) return(1);
	return(0);
    }

    private Pair<Coord, Coord> screenp(Coord tc, Coord sz) {
	Coord hsz = sz.div(2);
	tc = tc.sub(hsz);
	if(tc.equals(Coord.z))
	    tc = new Coord(1, 1);
	double d = Coord.z.dist(tc);
	Coord sc = tc.mul((d - 25.0) / d);
	float ak = ((float)hsz.y) / ((float)hsz.x);
	if((abs(sc.x) > hsz.x) || (abs(sc.y) > hsz.y)) {
	    if(abs(sc.x) * ak < abs(sc.y)) {
		sc = new Coord((sc.x * hsz.y) / sc.y, hsz.y).mul(signum(sc.y));
	    } else {
		sc = new Coord(hsz.x, (sc.y * hsz.x) / sc.x).mul(signum(sc.x));
	    }
	}
	Coord ad = sc.sub(tc).norm(UI.scale(30.0));
	sc = sc.add(hsz);

	return new Pair<>(sc, ad);
    }
    
    private void drawarrow(GOut g, Coord tc) {
	Pair<Coord, Coord> sp = screenp(tc, sz);
	Coord sc = sp.a;
	Coord ad = sp.b;
	
	// gl.glEnable(GL2.GL_POLYGON_SMOOTH); XXXRENDER
	QuestWnd questWnd;
	if(tip != null && col == null && (questWnd = getQuestWnd()) != null) {
	    int i = questWnd.getObjectiveIndex(tip);
	    col = colors[i % colors.length];
	}
	g.usestate(col != null ? col : colors[0]);
	Coord tmp = sc;
	sc = sc.add(g.tx);
	g.drawp(Model.Mode.TRIANGLES, new float[] {
		sc.x, sc.y,
		sc.x + ad.x - (ad.y / 3), sc.y + ad.y + (ad.x / 3),
		sc.x + ad.x + (ad.y / 3), sc.y + ad.y - (ad.x / 3),
	    });
	sc = tmp.add(ad);
	if(icon != null) {
	    if(marker != null) {
		g.chcolor();
	    } else if(triangulating) {
		g.chcolor(TRIANGULATION_COLOR);
	    }
	    try {
		if(licon == null)
		    licon = icon.get().layer(Resource.imgc).tex();
		g.aimage(licon, sc, 0.5, 0.5);
	    } catch(Loading l) {
	    }
	}
	g.chcolor();
	this.lc = sc;
    }

    public void draw(GOut g) {
	this.lc = null;
	MiniMap.Location curloc = ui.gui.mapfile.playerLocation();
	if(curloc!= null && (lastseg != curloc.seg.id || !Objects.equals(lastsegtc, curloc.tc)) ) {
	    segmentChanged(curloc);
	}
	Coord2d tc = tc();
	if(tc == null)
	    return;
	Gob gob = getGob();
	Coord3f sl;
	if(gob != null) {
	    try {
		sl = getparent(GameUI.class).map.screenxf(gob.getc());
	    } catch(Loading l) {
		return;
	    }
	} else {
	    Coord3f map3d = getMap3d(tc);
	    MapView map = getparent(GameUI.class).map;
	    HomoCoord4f homo = map.clipxf(map3d, false);
	    if(homo.w < 0) {
		homo = map.clipxf(map3d, true);
	    }
	    sl = homo.toview(Area.sized(map.sz));
	}
	if(sl != null)
	    drawarrow(g, new Coord(sl));
    }

    Coord3f getMap3d(Coord2d mc) {
	float z = 0;
	MapView map = getparent(GameUI.class).map;
	Gob player = map == null ? null : map.player();
	if(player != null) {
	    try {
		Coord2d gsz = tilesz.mul(cmaps.x, cmaps.y);
		Coord pgc = player.rc.floor(gsz);
		Coord mgc = mc.floor(gsz);
		if(pgc.manhattan2(mgc) <= 1) {
		    Coord3f mp = ui.sess.glob.map.getzp(mc);
		    z = mp.z;
		} else {
		    z = player.getc().z;
		}
	    } catch (Loading ignored) {
		
	    }
	}
	return new Coord3f((float) mc.x, (float) mc.y, z);
    }
    
    public void update(Coord2d tc, long gobid) {
	this.tc = tc;
	triangulate(tc);
	this.gobid = gobid;
    }

    public boolean mousedown(Widget.MouseDownEvent ev) {
	if(lc != null && lc.dist(ev.c) < 20) {
	    if(ev.b == 1) {
		Gob gob = getGob();
		if(gob != null) {
		    ui.gui.map.click(gob, 1);
		} else {
		    ui.gui.map.click(tc(), 1);
		}
	    } else if(ev.b == 3) {
		if(ui.modctrl && marker != null) {
		    if(ui.modmeta && marker instanceof PMarker) {
			ui.gui.mapfile.removeMarker(marker);
		    } else {
			ui.gui.untrack(marker);
		    }
		} else {
		    Gob gob = getGob();
		    if(gob != null) {
			ui.gui.map.click(gob, 3);
		    }
		}
	    }
	    if(click) {wdgmsg("click", ev.b, ui.modflags());}
	    return(true);
	}
	return (super.mousedown(ev));
    }
    
    private QuestWnd getQuestWnd() {
	GameUI gui = getparent(GameUI.class);
	if(gui != null && gui.chrwdg != null && gui.chrwdg.quest != null) {
	    return gui.chrwdg.quest;
	}
	return null;
    }
    
    private Gob getGob() {
	if(marker instanceof MapWnd2.GobMarker) {
	    return ui.sess.glob.oc.getgob(((MapWnd2.GobMarker) marker).gobid);
	}
	return (gobid < 0) ? null : ui.sess.glob.oc.getgob(gobid);
    }

    public void uimsg(String name, Object... args) {
	if(name == "upd") {
	    if(args[0] == null)
		tc = null;
	    else
		tc = ((Coord)args[0]).mul(OCache.posres);
	    triangulate(tc);
	    if(args[1] == null)
		gobid = -1;
	    else
		gobid = Utils.uint32((Integer)args[1]);
	} else if(name == "icon") {
	    int iconid = (Integer)args[0];
	    Indir<Resource> icon = (iconid < 0) ? null : ui.sess.getres(iconid);
	    this.icon = icon;
	    licon = null;
	} else if(name == "cl") {
	    click = ((Integer)args[0]) != 0;
	} else if(name == "tip") {
	    Object tt = args[0];
	    if(tt instanceof String) {
		tip = (String) tt;
	    } else {
		super.uimsg(name, args);
	    }
	} else {
	    super.uimsg(name, args);
	}
    }
    
    public String name() {
	return tip;
    }

    public Object tooltip(Coord c, Widget prev) {
	if((lc != null) && (lc.dist(c) < 20))
	    return tooltip();
	return(null);
    }
    
    public Object tooltip() {
	if(tip != null) {
	    double d = getDistance();
	    if(d > 0) {
		return String.format("%s (%.1fm%s)", tip, d, triangulating ? "[?]" : "");
	    } else {
		return tip;
	    }
	} else return (tooltip);
    }
    
    double getDistance() {
	MapView map = getparent(GameUI.class).map;
	Gob target = getGob();
	Gob player = map == null ? null : map.player();
	if(player != null) {
	    if(target != null) {
		return player.rc.dist(target.rc) / 11.0;
	    } else {
		Coord2d tc = tc();
		if(tc != null) {
		    return player.rc.dist(tc) / 11.0;
		}
	    }
	}
	return -1;
    }
    
    Pair<Coord2d, Coord2d> firstLine = null;
    long firsSegment = -1;
    
    private void triangulate(Coord2d b) {
	if(b == null) {
	    firstLine = null;
	    return;
	}
	mc = null;
	tc();
	if(!triangulating) {return;}
	long curseg = ui.gui.mapfile.playerSegmentId();
	Gob player = ui.gui.map.player();
	if(player != null) {
	    Pair<Coord2d, Coord2d> line = new Pair<>(player.rc, b);
	    if(firstLine == null) {
		firsSegment = curseg;
		firstLine = line;
	    } else if(curseg == firsSegment) {
		mc = ClientUtils.intersect(firstLine, line).orElse(mc);
		//do not stop triangulation if calculated point diverges too far off from server one
		if(mc != null && Math.abs(player.rc.angle(mc) - player.rc.angle(b)) > PI / 3) {
		    mc = null;
		}
		triangulating = mc == null;
	    } else {
		firstLine = null;
	    }
	}
    }
    
    private void segmentChanged(MiniMap.Location curseg) {
	mc = null;
	firstLine = null;
	firsSegment = -1;
	triangulating = false;
	lastseg = curseg.seg.id;
	lastsegtc = curseg.tc;
    }
    
    public Coord2d tc() {return tc(ui.gui.mapfile.playerSegmentId());}
    
    public Coord2d tc(long id) {
	if(marker != null) {
	    triangulating = false;
	    MiniMap.Location loc = ui.gui.mapfile.view.sessloc;
	    if(id == marker.seg) {
		Coord2d tmp = null;
		if(marker instanceof MapWnd2.GobMarker) {
		    MapWnd2.GobMarker gobMarker = (MapWnd2.GobMarker) this.marker;
		    if(gobMarker.hide()) {return null;}
		    tmp = gobMarker.rc();
		}
		if(tmp == null) {
		    tmp = mc = marker.tc.sub(loc.tc).mul(tilesz).add(6, 6);
		}
		tc = mc = tmp;
		return mc;
	    } else {
		return null;
	    }
	} else if(tc == null) {
	    triangulating = false;
	    return null;
	} else if(mc == null) {
	    GameUI gui = getparent(GameUI.class);
	    Gob player = gui.map.player();
	    if(player != null) {
		double d = player.rc.dist(tc) / 11.0;
		if(d > 990) {
		    mc = gui.mapfile.findMarkerPosition(tip);
		    triangulating = mc == null;
		    if(mc != null) {
			return mc;
		    }
		}
	    }
	    mc = tc;
	    return mc;
	} else {
	    return mc;
	}
    }
    
    public long seg() {
	return marker != null ? marker.seg : ui.gui.mapfile.playerSegmentId();
    }
    
    public Coord sc(Coord c, Coord sz) {
	Pair<Coord, Coord> p = screenp(c, sz);
	return p.a.add(p.b);
    }
    
    public void drawmmarrow(GOut g, Coord tc, Coord sz) {
	Coord tsz = this.sz;
	Coord tlc = this.lc;
	this.sz = sz;
	drawarrow(g, tc);
	this.sz = tsz;
	this.lc = tlc;
    }
    
    @Override
    public boolean drop(Coord cc, Coord ul) {
	return false;
    }
    
    @Override
    public boolean iteminteract(Coord cc, Coord ul) {
	if((lc != null) && (lc.dist(cc) < 20)) {
	    Gob gob = getGob();
	    if(gob != null) {
		ui.gui.map.wdgmsg("itemact", ui.mc, Coord.z, ui.modflags(), 0, (int) gob.id, gob.rc.floor(posres), 0, -1);
	    }
	    return true;
	}
	return false;
    }
}
