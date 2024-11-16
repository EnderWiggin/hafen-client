/* Preprocessed source code */
package haven.res.ui.surv;

import haven.*;
import haven.render.*;
import java.awt.Color;
import java.nio.*;
import static haven.MCache.tilesz;

/* >wdg: LandSurvey */
@haven.FromResource(name = "ui/surv", version = 42)
public class LandSurvey extends Window {
    final Coord ul, br;
    MapView mv;
    Display dsp;
    RenderTree.Slot s_dsp, s_ol;
    final FastMesh ol;
    final Location dloc;
    final Label albl, zdlbl, wlbl, dlbl;
    final HSlider zset;
    final float gran;
    int tz;

    public LandSurvey(Coord ul, Coord br, float gran, int tz) {
	super(Coord.z, "Land survey", true);
	this.ul = ul;
	this.br = br;
	this.gran = gran;
	this.tz = tz;
	this.dloc = Location.xlate(new Coord3f(this.ul.x * (float)tilesz.x, -this.ul.y * (float)tilesz.y, 0));
	VertexBuf.VertexData olv = new VertexBuf.VertexData(FloatBuffer.wrap(new float[] {
		    0, 0, 0,
		    (br.x - ul.x) * (float)tilesz.x, 0, 0,
		    (br.x - ul.x) * (float)tilesz.x, -(br.y - ul.y) * (float)tilesz.y, 0,
		    0, -(br.y - ul.y) * (float)tilesz.y, 0,
		}));
	ol = new FastMesh(new VertexBuf(olv), ShortBuffer.wrap(new short[] {
		    0, 3, 1,
		    1, 3, 2,
		}));
	albl = add(new Label(String.format("Area: %d m\u00b2", (br.x - ul.x) * (br.y - ul.y))), 0, 0);
	zdlbl = add(new Label("..."), UI.scale(0, 15));
	wlbl = add(new Label("..."), UI.scale(0, 30));
	dlbl = add(new Label("..."), UI.scale(0, 45));
	zset = add(new HSlider(UI.scale(225), -1, 1, tz) {
		public void changed() {
		    LandSurvey.this.tz = val;
		    upd = true;
		    sendtz = Utils.rtime() + 0.5;
		}
	    }, UI.scale(0, 60));
	add(new Button(UI.scale(100), "Make level") {
		public void click() {
		    LandSurvey.this.wdgmsg("lvl", LandSurvey.this.tz / (gran * 11));
		}
	    }, UI.scale(0, 90));
	add(new Button(UI.scale(100), "Remove") {
		public void click() {
		    LandSurvey.this.wdgmsg("rm");
		}
	    }, UI.scale(125, 90));
	pack();
    }

    public static Widget mkwidget(UI ui, Object... args) {
	Coord ul = (Coord)args[0];
	Coord br = (Coord)args[1];
	float gran = ((Number)args[3]).floatValue() / 11;
	int tz = (args[2] == null) ? Integer.MIN_VALUE : Math.round(((Number)args[2]).floatValue() * gran * 11);
	return(new LandSurvey(ul, br, gran, tz));
    }

    protected void attached() {
	super.attached();
	this.mv = getparent(GameUI.class).map;
	this.dsp = new Display();
    }

    static final VertexArray.Layout pfmt =
	new VertexArray.Layout(new VertexArray.Layout.Input(Homo3D.vertex,     new VectorFormat(3, NumberFormat.FLOAT32), 0,  0, 16),
			       new VertexArray.Layout.Input(VertexColor.color, new VectorFormat(4, NumberFormat.UNORM8),  0, 12, 16));
    class Display implements Rendered, RenderTree.Node, TickList.Ticking, TickList.TickNode {
	final Pipe.Op ptsz = new PointSize(3);
	final MCache map;
	final int area;
	final Model model;
	boolean update = true;

	Display() {
	    map = mv.ui.sess.glob.map;
	    area = (br.x - ul.x + 1) * (br.y - ul.y + 1);
	    VertexArray va = new VertexArray(pfmt, new VertexArray.Buffer(area * pfmt.inputs[0].stride, DataBuffer.Usage.STATIC, this::initfill));
	    model = new Model(Model.Mode.POINTS, va, null);
	}

	public void draw(Pipe st, Render g) {
	    g.draw(st, model);
	}

	private FillBuffer initfill(VertexArray.Buffer dst, Environment env) {
	    try {
		return(fill(dst, env));
	    } catch(Loading l) {
		return(DataBuffer.Filler.zero().fill(dst, env));
	    }
	}

	private FillBuffer fill(VertexArray.Buffer dst, Environment env) {
	    float E = 0.001f;
	    FillBuffer ret = env.fillbuf(dst);
	    ByteBuffer buf = ret.push();
	    Coord c = new Coord();
	    float tz = LandSurvey.this.tz / gran;
	    for(c.y = ul.y; c.y <= br.y; c.y++) {
		for(c.x = ul.x; c.x <= br.x; c.x++) {
		    float z = (float)map.getfz(c);
		    buf.putFloat((c.x - ul.x) * (float)tilesz.x).putFloat(-(c.y - ul.y) * (float)tilesz.y).putFloat(tz);
		    if(Math.abs(tz - z) < E) {
			buf.put((byte)0).put((byte)255).put((byte)0).put((byte)255);
		    } else if(tz < z) {
			buf.put((byte)255).put((byte)0).put((byte)255).put((byte)255);
		    } else {
			buf.put((byte)0).put((byte)128).put((byte)255).put((byte)255);
		    }
		}
	    }
	    return(ret);
	}

	public void autogtick(Render g) {
	    if(update) {
		try {
		    g.update(model.va.bufs[0], this::fill);
		    update = false;
		} catch(Loading l) {
		}
	    }
	}

	public TickList.Ticking ticker() {return(this);}

	public void added(RenderTree.Slot slot) {
	    slot.ostate(Pipe.Op.compose(dloc, ptsz, new States.Depthtest(States.Depthtest.Test.TRUE), Rendered.last, VertexColor.instance));
	}
    }

    private int autoz() {
	MCache map = mv.ui.sess.glob.map;
	double zs = 0;
	int nv = 0;
	Coord c = new Coord();
	for(c.y = ul.y; c.y <= br.y; c.y++) {
	    for(c.x = ul.x; c.x <= br.x; c.x++) {
		zs += map.getfz(c);
		nv++;
	    }
	}
	return((int)Math.round(zs / nv));
    }

    private boolean upd = true;
    private void updmap() {
	MCache map = mv.ui.sess.glob.map;
	Coord c = new Coord();
	int min = Integer.MAX_VALUE, max = Integer.MIN_VALUE;
	int sd = 0, hn = 0;
	for(c.y = ul.y; c.y <= br.y; c.y++) {
	    for(c.x = ul.x; c.x <= br.x; c.x++) {
		int z = (int)Math.round(map.getfz(c) * gran);
		min = Math.min(min, z); max = Math.max(max, z);
		sd += tz - z;
		if(z > tz)
		    hn += z - tz;
	    }
	}
	zset.min = min - Math.round(11 * gran); zset.max = max + Math.round(11 * gran);
	zdlbl.settext(String.format("Peak to trough: %.1f m", (max - min) / 10.0));
	if(sd >= 0)
	    wlbl.settext(String.format("Units of soil required: %d", sd));
	else
	    wlbl.settext(String.format("Units of soil left over: %d", -sd));
	dlbl.settext(String.format("Units of soil to dig: %d", hn));
	dsp.update = true;
    }

    private double sendtz = 0;
    private static final Pipe.Op olmat = Pipe.Op.compose(new BaseColor(new Color(255, 0, 0, 64)),
							 Rendered.postfx, // Rendered.eyesort, XXXRENDER
							 new States.DepthBias(-2, -2));
    private int olseq = -1;
    public void tick(double dt) {
	if(tz == Integer.MIN_VALUE) {
	    try {
		zset.val = tz = autoz();
		olseq = mv.ui.sess.glob.map.olseq;
		upd = true;
	    } catch(Loading l) {}
	} else {
	    if(upd || (olseq != mv.ui.sess.glob.map.olseq)) {
		try {
		    updmap();
		    olseq = mv.ui.sess.glob.map.olseq;
		    upd = false;
		} catch(Loading l) {
		}
	    }
	    if((s_dsp == null) && (olseq != -1)) {
		s_dsp = mv.drawadd(dsp);
		s_ol = mv.drawadd(ol);
	    }
	    if(s_ol != null) {
		s_ol.cstate(Pipe.Op.compose(olmat, Location.xlate(new Coord3f(ul.x * (float)tilesz.x, -ul.y * (float)tilesz.y, tz))));
	    }
	}
	if((sendtz != 0) && (Utils.rtime() > sendtz)) {
	    wdgmsg("tz", tz / (gran * 11));
	    sendtz = 0;
	}
	super.tick(dt);
    }

    public void uimsg(String name, Object... args) {
	if(name == "tz") {
	    tz = Math.round(((Number)args[0]).floatValue() * gran);
	    zset.val = tz;
	    upd = true;
	} else {
	    super.uimsg(name, args);
	}
    }

    public void destroy() {
	if(s_dsp != null) {
	    s_dsp.remove();
	    s_ol.remove();
	}
	super.destroy();
    }
}
