/* Preprocessed source code */
package haven.res.ui.surv;

import haven.*;
import haven.render.*;
import haven.render.sl.*;
import java.util.*;
import java.nio.*;
import java.awt.Color;
import static haven.MCache.tilesz;
import static haven.render.sl.Cons.*;
import static haven.render.sl.Type.*;

@haven.FromResource(name = "ui/surv", version = 46)
public class Display implements RenderTree.Node, TickList.Ticking, TickList.TickNode {
    public static final Attribute v_elev = new Attribute(FLOAT);
    public static final Attribute v_flags = new Attribute(INT);
    public static final AutoVarying f_elev = new AutoVarying(FLOAT) {
	    public Expression root(VertexContext vctx) {return(v_elev.ref());}
	};
    public static final AutoVarying f_flags = new AutoVarying(INT) {
	    {ipol = Interpol.FLAT;}
	    public Expression root(VertexContext vctx) {return(v_flags.ref());}
	};
    public static final VertexArray.Layout fmt =
	new VertexArray.Layout(new VertexArray.Layout.Input(Homo3D.vertex, new VectorFormat(3, NumberFormat.FLOAT32), 0, 0, 16),
			       new VertexArray.Layout.Input(v_flags,       new VectorFormat(1, NumberFormat.UINT8),   0, 12, 16),
			       new VertexArray.Layout.Input(v_elev,        new VectorFormat(1, NumberFormat.UNORM8),  0, 13, 16));
    public final Data data;
    public final MapView mv;
    public final MCache map;
    public final VertexArray va;
    public final Model surface, vertices;
    public Area tarea;
    public Coord selected = null;
    public boolean active = false, update;
    private final Collection<RenderTree.Slot> slots = new ArrayList<>(1);
    private final Map<RenderTree.Slot, RenderTree.Slot> selslots = new IdentityHashMap<>();
    private Model selection = null;
    private int mapseq = -1, dataseq = -1;

    public Display(Data data, MapView mv) {
	this.data = data;
	this.mv = mv;
	this.map = mv.ui.sess.glob.map;
	Area varea = data.varea;
	this.va = new VertexArray(fmt, new VertexArray.Buffer(varea.area() * fmt.inputs[0].stride, DataBuffer.Usage.STREAM, this::vfill)).shared();
	this.tarea = Area.corn(varea.ul, varea.br.sub(1, 1));
	this.surface = new Model(Model.Mode.TRIANGLES, va, new Model.Indices(tarea.area() * 6, NumberFormat.UINT16, DataBuffer.Usage.STREAM, this::sfill));
	this.vertices = new Model(Model.Mode.POINTS, va, null);
    }

    private Coord3f orig() {
	return(Coord3f.of(data.varea.ul.x * (float)tilesz.x, data.varea.ul.y * (float)tilesz.y, 0));
    }

    private Coord3f vpos(Coord vc) {
	return(Coord3f.of((vc.x - data.varea.ul.x) * (float)tilesz.x,
			  (vc.y - data.varea.ul.y) * (float)tilesz.y,
			  data.dz[data.varea.ridx(vc)] / data.gran));
    }

    public float zsize(Coord vc) {
	Coord3f vpos = vpos(vc).add(orig());
	return(mv.screenxf(vpos).dist(mv.screenxf(vpos.add(0, 0, 1 / data.gran))));
    }

    private FillBuffer vfill(VertexArray.Buffer dst, Environment env) {
	float E = 0.001f;
	FillBuffer ret = env.fillbuf(dst);
	ByteBuffer buf = ret.push();
	Coord ul = data.varea.ul;
	data.eupdate();
	for(Coord vc : data.varea) {
	    float tz;
	    try {
		tz = (float)map.getfz(vc);
	    } catch(Loading l) {
		tz = 0;
	    }
	    float vz = data.dz[data.varea.ridx(vc)] / data.gran;
	    buf.putFloat((vc.x - ul.x) * (float)tilesz.x).putFloat(-(vc.y - ul.y) * (float)tilesz.y).putFloat(vz);
	    byte fl = 0;
	    if(vz - tz > E)
		fl |= 1;
	    else if(tz - vz > E)
		fl |= 2;
	    if(Utils.eq(vc, selected)) {
		fl |= 4;
		if(active)
		    fl |= 8;
	    }
	    buf.put(fl);
	    buf.put((byte)((255 * (vz - data.lo)) / (data.hi - data.lo)));
	    buf.put((byte)0).put((byte)0);
	}
	return(ret);
    }

    private FillBuffer sfill(Model.Indices dst, Environment env) {
	Area varea = data.varea;
	FillBuffer ret = env.fillbuf(dst);
	ShortBuffer buf = ret.push().asShortBuffer();
	float[] mz = new float[varea.area()];
	for(Coord vc : varea) {
	    try {
		mz[varea.ridx(vc)] = (float)map.getfz(vc);
	    } catch(Loading l) {}
	}
	for(Coord tc : tarea) {
	    if(Math.abs(mz[varea.ridx(tc.add(0, 0))] - mz[varea.ridx(tc.add(1, 1))]) > Math.abs(mz[varea.ridx(tc.add(1, 0))] - mz[varea.ridx(tc.add(0, 1))])) {
		buf.put((short)varea.ridx(tc.add(0, 0))); buf.put((short)varea.ridx(tc.add(0, 1))); buf.put((short)varea.ridx(tc.add(1, 0)));
		buf.put((short)varea.ridx(tc.add(1, 0))); buf.put((short)varea.ridx(tc.add(0, 1))); buf.put((short)varea.ridx(tc.add(1, 1)));
	    } else {
		buf.put((short)varea.ridx(tc.add(0, 0))); buf.put((short)varea.ridx(tc.add(0, 1))); buf.put((short)varea.ridx(tc.add(1, 1)));
		buf.put((short)varea.ridx(tc.add(0, 0))); buf.put((short)varea.ridx(tc.add(1, 1))); buf.put((short)varea.ridx(tc.add(1, 0)));
	    }
	}
	return(ret);
    }

    public static final Function vcol = new Function.Def(VEC4) {{
	code.add(new If(ne(bitand(f_flags.ref(), l(4)), l(0)),
			new Return(vec4(0.0, 0.0, 1.0, 1.0))));
	code.add(new If(ne(bitand(f_flags.ref(), l(1)), l(0)),
			new Return(vec4(0.0, 0.5, 1.0, 1.0))));
	code.add(new If(ne(bitand(f_flags.ref(), l(2)), l(0)),
			       new Return(vec4(1.0, 0.0, 1.0, 1.0))));
	code.add(new Return(vec4(0.0, 1.0, 0.0, 1.0)));
    }};
    public static final Function vsize = new Function.Def(FLOAT) {{
	code.add(new If(ne(bitand(v_flags.ref(), l(8)), l(0)),
			new Return(l(7.0))));
	code.add(new Return(l(3.0)));
    }};
    public static final ShaderMacro vshader = prog -> {
	FragColor.fragcol(prog.fctx).mod(in -> vcol.call(), 0);
	prog.vctx.ptsz.mod(in -> vsize.call(), 0);
	prog.vctx.ptsz.force();
    };
    public static final Pipe.Op vdraw = Pipe.Op.compose(new RUtils.AdHoc(vshader), States.Depthtest.none, States.maskdepth,
							new Rendered.Order.Default(20102));

    public static final Function scol = new Function.Def(VEC4) {{
	code.add(new If(lt(f_elev.ref(), l(0.5)),
			new Return(mix(vec4(1.0, 0.4, 0.0, 1.0), vec4(1.0, 0.0, 0.0, 1.0),
				       mul(f_elev.ref(), l(2.0)))),
			new Return(mix(vec4(1.0, 0.0, 0.0, 1.0), vec4(1.0, 0.0, 0.43, 1.0),
				       mul(sub(f_elev.ref(), l(0.5)), l(2.0))))));
    }};
    public static final ShaderMacro sshader = prog -> {
	FragColor.fragcol(prog.fctx).mod(in -> mul(in, scol.call()), 0);
    };
    public static final Pipe.Op sdraw1 = Pipe.Op.compose(FragColor.slot.nil, new States.DepthBias(-2, -2), new Rendered.Order.Default(20100), States.facecull.nil);
    public static final Pipe.Op sdraw2 = Pipe.Op.compose(new RUtils.AdHoc(sshader), new BaseColor(255, 255, 255, 128), new States.DepthBias(-2, -2), States.facecull.nil,
							 new Rendered.Order.Default(20101));

    public static final Pipe.Op seldraw = Pipe.Op.compose(new States.LineWidth(1), new BaseColor(0, 0, 255, 255), States.Depthtest.none, States.maskdepth,
							  new Rendered.Order.Default(20103));
    public void select(Area area) {
	if(selection != null) {
	    selslots.values().forEach(RenderTree.Slot::remove);
	    selslots.clear();
	    selection.dispose();
	    selection = null;
	}
	if(area != null) {
	    short[] ind = new short[(((area.br.x - area.ul.x) + (area.br.y - area.ul.y)) * 2) + 1];
	    int o = 0;
	    for(int x = area.ul.x; x < area.br.x; x++)
		ind[o++] = (short)data.varea.ridx(Coord.of(x, area.ul.y));
	    for(int y = area.ul.y; y < area.br.y; y++)
		ind[o++] = (short)data.varea.ridx(Coord.of(area.br.x, y));
	    for(int x = area.br.x; x > area.ul.x; x--)
		ind[o++] = (short)data.varea.ridx(Coord.of(x, area.br.y));
	    for(int y = area.br.y; y >= area.ul.y; y--)
		ind[o++] = (short)data.varea.ridx(Coord.of(area.ul.x, y));
	    selection = new Model(Model.Mode.LINE_STRIP, va, new Model.Indices(ind.length, NumberFormat.UINT16, DataBuffer.Usage.STATIC, DataBuffer.Filler.of(ind)));
	    for(RenderTree.Slot slot : slots)
		selslots.put(slot, slot.add(selection, seldraw));
	}
    }

    public void added(RenderTree.Slot slot) {
	slot.ostate(Location.xlate(Coord3f.of(data.varea.ul.x * (float)tilesz.x, -data.varea.ul.y * (float)tilesz.y, 0)));
	slot.add(surface, sdraw1);
	slot.add(surface, sdraw2);
	slot.add(vertices, vdraw);
	if(selection != null)
	    selslots.put(slot, slot.add(selection, seldraw));
	slots.add(slot);
    }

    public void removed(RenderTree.Slot slot) {
	slots.remove(slot);
	selslots.remove(slot);
    }

    public void autogtick(Render g) {
	if((mapseq != map.chseq) || (dataseq != data.seq) || update) {
	    g.update(va.bufs[0], this::vfill);
	    g.update(surface.ind, this::sfill);
	    mapseq = map.chseq;
	    dataseq = data.seq;
	    update = false;
	}
    }

    public TickList.Ticking ticker() {return(this);}

    public Coord mousetest(Coord mc, boolean any) {
	Coord sel = null;
	float minz = 0;
	float mind = 0;
	Coord3f orig = orig();
	for(Coord vc : data.varea) {
	    Coord3f sc = mv.screenxf(vpos(vc).add(orig));
	    if(!any) {
		if(Math.hypot(sc.x - mc.x, sc.y - mc.y) < UI.scale(10)) {
		    if((sel == null) || (sc.z < minz)) {
			sel = vc;
			minz = sc.z;
		    }
		}
	    } else {
		float d = (float)Math.hypot(sc.x - mc.x, sc.y - mc.y);
		if((sel == null) || (d < mind)) {
		    sel = vc;
		    mind = d;
		}
	    }
	}
	return(sel);
    }
}

/* >wdg: LandSurvey */
