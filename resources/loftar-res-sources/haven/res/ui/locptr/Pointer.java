/* Preprocessed source code */
package haven.res.ui.locptr;

import haven.*;
import haven.render.*;
import java.awt.Color;
import static java.lang.Math.*;

/* >wdg: Pointer */
@haven.FromResource(name = "ui/locptr", version = 21)
public class Pointer extends Widget {
    public static final BaseColor col = new BaseColor(new Color(241, 227, 157, 255));
    public Indir<Resource> icon;
    public Coord2d tc;
    public Coord lc;
    public long gobid = -1;
    public boolean click;
    private Tex licon;

    public Pointer(Indir<Resource> icon) {
	super(Coord.z);
	this.icon = icon;
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

    private void drawarrow(GOut g, Coord tc) {
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

	// gl.glEnable(GL2.GL_POLYGON_SMOOTH); XXXRENDER
	g.usestate(col);
	g.drawp(Model.Mode.TRIANGLES, new float[] {
		sc.x, sc.y,
		sc.x + ad.x - (ad.y / 3), sc.y + ad.y + (ad.x / 3),
		sc.x + ad.x + (ad.y / 3), sc.y + ad.y - (ad.x / 3),
	    });

	if(icon != null) {
	    try {
		if(licon == null)
		    licon = icon.get().layer(Resource.imgc).tex();
		g.aimage(licon, sc.add(ad), 0.5, 0.5);
	    } catch(Loading l) {
	    }
	}
	this.lc = sc.add(ad);
    }

    public void draw(GOut g) {
	this.lc = null;
	if(tc == null)
	    return;
	Gob gob = (gobid < 0) ? null : ui.sess.glob.oc.getgob(gobid);
	Coord3f sl;
	if(gob != null) {
	    try {
		sl = getparent(GameUI.class).map.screenxf(gob.getc());
	    } catch(Loading l) {
		return;
	    }
	} else {
	    sl = getparent(GameUI.class).map.screenxf(tc);
	}
	if(sl != null)
	    drawarrow(g, new Coord(sl));
    }

    public void update(Coord2d tc, long gobid) {
	this.tc = tc;
	this.gobid = gobid;
    }

    public boolean mousedown(Coord c, int button) {
	if(click && (lc != null)) {
	    if(lc.dist(c) < 20) {
		wdgmsg("click", button, ui.modflags());
		return(true);
	    }
	}
	return(super.mousedown(c, button));
    }

    public void uimsg(String name, Object... args) {
	if(name == "upd") {
	    if(args[0] == null)
		tc = null;
	    else
		tc = ((Coord)args[0]).mul(OCache.posres);
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
	} else {
	    super.uimsg(name, args);
	}
    }

    public Object tooltip(Coord c, Widget prev) {
	if((lc != null) && (lc.dist(c) < 20))
	    return(tooltip);
	return(null);
    }
}
