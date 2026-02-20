package haven;

import haven.render.*;

import java.awt.*;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

public class Hitbox extends SlottedNode implements Rendered {
    private static final VertexArray.Layout LAYOUT = new VertexArray.Layout(new VertexArray.Layout.Input(Homo3D.vertex, new VectorFormat(3, NumberFormat.FLOAT32), 0, 0, 12));
    private VertexArray mesh;
    private Model model;
    private final Gob gob;
    private final boolean filled;
    private static final Map<Resource, VertexArray> VERTEX_CACHE = new HashMap<>();
    private static final float Z = 0.1f;
    private static final float PASSABLE_WIDTH = 1.5f;
    private static final float SOLID_WIDTH = 3f;
    private static final Pipe.Op NO_CULL = new States.Facecull(States.Facecull.Mode.NONE);
    private static final Pipe.Op TOP = Pipe.Op.compose(States.Depthtest.none, States.maskdepth);
    private static Pipe.Op FILLED;
    private static Pipe.Op SOLID;
    private static Pipe.Op PASSABLE;
    private static Pipe.Op SOLID_TOP;
    private static Pipe.Op PASSABLE_TOP;
    private Pipe.Op state = SOLID;
    
    static {
	CFG.COLOR_HBOX_FILLED.observe(Hitbox::updateColors);
	CFG.COLOR_HBOX_SOLID.observe(Hitbox::updateColors);
	CFG.COLOR_HBOX_PASSABLE.observe(Hitbox::updateColors);
	updateColors(null);
    }
    
    private static void updateColors(CFG<Color> cfg) {
	FILLED = Pipe.Op.compose(new BaseColor(CFG.COLOR_HBOX_FILLED.get()), NO_CULL, MixColor.slot.nil, MapMesh.postmap, Location.noscale);
	SOLID = Pipe.Op.compose(new BaseColor(CFG.COLOR_HBOX_SOLID.get()), new States.LineWidth(SOLID_WIDTH), MixColor.slot.nil, Rendered.last, Location.noscale);
	PASSABLE = Pipe.Op.compose(new BaseColor(CFG.COLOR_HBOX_PASSABLE.get()), new States.LineWidth(PASSABLE_WIDTH), MixColor.slot.nil, Rendered.last);
	SOLID_TOP = Pipe.Op.compose(SOLID, TOP);
	PASSABLE_TOP = Pipe.Op.compose(PASSABLE, TOP);
    }
    
    private Hitbox(Gob gob, boolean filled) {
	mesh = getMesh(gob);
	this.gob = gob;
	this.filled = filled;
	updateState();
    }
    
    public static Hitbox forGob(Gob gob, boolean filled) {
	try {
	    return new Hitbox(gob, filled);
	} catch (Loading ignored) { }
	return null;
    }
    
    @Override
    public void added(RenderTree.Slot slot) {
	super.added(slot);
	slot.ostate(state);
	updateState();
    }
    
    @Override
    public void draw(Pipe context, Render out) {
	if(model != null) {
	    out.draw(context, model);
	}
    }
    
    public void updateState() {
	if(mesh != null && slots != null) {
	    boolean top = CFG.DISPLAY_GOB_HITBOX_TOP.get();
	    boolean passable = passable();
	    Pipe.Op newState = filled
		? FILLED
		: passable ? (top ? PASSABLE_TOP : PASSABLE) : (top ? SOLID_TOP : SOLID);
	    try {
		VertexArray m = getMesh(gob);
		boolean changed = false;
		if(m != null && m != mesh) {
		    changed = true;
		    mesh = m;
		}
		
		if(filled) {
		    boolean needFilled = mesh != null && !passable && CFG.DISPLAY_GOB_HITBOX_FILLED.get();
		    if(needFilled) {
			if(changed || model == null) {
			    changed = true;
			    model = new Model(Model.Mode.TRIANGLE_FAN, mesh, null);
			}
		    } else if(model != null) {
			changed = true;
			model = null;
		    }
		} else {
		    if(mesh == null && model != null) {
			changed = true;
			model = null;
		    } else if(mesh != null && (model == null || changed)) {
			changed = true;
			model = new Model(Model.Mode.LINES, mesh, null);
		    }
		}
		
		if(changed) {slots.forEach(RenderTree.Slot::update);}
	    } catch (Loading ignored) {}
	    if(newState != state) {
		state = newState;
		for (RenderTree.Slot slot : slots) {
		    slot.ostate(newState);
		}
	    }
	}
    }
    
    private boolean passable() {
	try {
	    String name = gob.resid();
	    if(name == null) {return false;}
	    ResDrawable rd = (gob.drawable instanceof ResDrawable) ? (ResDrawable) gob.drawable : null;
	    
	    if(rd == null) {return false;}
	    int state = gob.sdt();
	    if(gob.is(GobTag.GATE)) {//gates
		if(state != 1) {return false;}// gate is not open 
		return !gob.isVisitorGate() // not visitor gate or not in combat 
		    || !gob.contextopt(GameUI.class).map(GameUI::isInCombat).orElse(false);
	    } else if(name.contains("/dng/") && (name.endsWith("door") || name.endsWith("gate"))) {
		return (state & 1) != 0;
	    } else if(name.endsWith("/pow[hearth]")) {//hearth fire
		return true;
	    } else if(name.equals("gfx/terobjs/arch/cellardoor") || name.equals("gfx/terobjs/fishingnet")) {
		return true;
	    }
	} catch (Loading ignored) {}
	return false;
    }
    
    private static VertexArray getMesh(Gob gob) {
	Resource res = getResource(gob);
	VertexArray mesh;
	synchronized (VERTEX_CACHE) {
	    mesh = VERTEX_CACHE.get(res);
	    if(mesh == null) {
		List<List<Coord3f>> polygons = new LinkedList<>();
	    
		Collection<Resource.Neg> negs = res.layers(Resource.Neg.class);
		if(negs != null) {
		    for (Resource.Neg neg : negs) {
			List<Coord3f> box = new LinkedList<>();
			box.add(new Coord3f(neg.ac.x, -neg.ac.y, Z));
			box.add(new Coord3f(neg.bc.x, -neg.ac.y, Z));
			box.add(new Coord3f(neg.bc.x, -neg.bc.y, Z));
			box.add(new Coord3f(neg.ac.x, -neg.bc.y, Z));
		    
			polygons.add(box);
		    }
		}
	    
		Collection<Resource.Obstacle> obstacles = res.layers(Resource.Obstacle.class);
		if(obstacles != null) {
		    for (Resource.Obstacle obstacle : obstacles) {
			if("build".equals(obstacle.id)) {continue;}
			for (Coord2d[] polygon : obstacle.p) {
			    polygons.add(Arrays.stream(polygon)
				.map(coord2d -> new Coord3f((float) coord2d.x, (float) -coord2d.y, Z))
				.collect(Collectors.toList()));
			}
		    }
		}
	    
		if(!polygons.isEmpty()) {
		    List<Float> vertices = new LinkedList<>();
		
		    for (List<Coord3f> polygon : polygons) {
			addLoopedVertices(vertices, polygon);
		    }
		
		    float[] data = convert(vertices);
		    VertexArray.Buffer vbo = new VertexArray.Buffer(data.length * 4, DataBuffer.Usage.STATIC, DataBuffer.Filler.of(data));
		    mesh = new VertexArray(LAYOUT, vbo);
		
		    VERTEX_CACHE.put(res, mesh);
		}
	    }
	}
	return mesh;
    }
    
    private static float[] convert(List<Float> list) {
	float[] ret = new float[list.size()];
	int i = 0;
	for (Float value : list) {
	    ret[i++] = value;
	}
	return ret;
    }
    
    private static void addLoopedVertices(List<Float> target, List<Coord3f> vertices) {
	int n = vertices.size();
	for (int i = 0; i < n; i++) {
	    Coord3f a = vertices.get(i);
	    Coord3f b = vertices.get((i + 1) % n);
	    Collections.addAll(target, a.x, a.y, a.z);
	    Collections.addAll(target, b.x, b.y, b.z);
	}
    }
    
    private static Resource getResource(Gob gob) {
	Resource res = gob.getres();
	if(res == null) {throw new Loading();}
	res = fix(gob, res);
	Collection<RenderLink.Res> links = res.layers(RenderLink.Res.class);
	for (RenderLink.Res link : links) {
	    if(link.l instanceof RenderLink.MeshMat) {
		RenderLink.MeshMat mesh = (RenderLink.MeshMat) link.l;
		return mesh.mesh.get();
	    }
	}
	return res;
    }
    
    public static Resource fix(Gob gob, Resource res) {
	if(gob.is(GobTag.HORSE)) {
	    return Resource.remote().loadwait("gfx/kritter/horse/horse");
	}
	if(gob.is(GobTag.GOAT)) {
	    return Resource.remote().loadwait("gfx/kritter/goat/goat");
	}
	if(gob.is(GobTag.CALF)) {
	    return Resource.remote().loadwait("gfx/kritter/cattle/cattle");
	}
	if(gob.is(GobTag.LAMB)) {
	    return Resource.remote().loadwait("gfx/kritter/sheep/sheep");
	}
	if(gob.is(GobTag.PIG)) {
	    return Resource.remote().loadwait("gfx/kritter/pig/pig");
	}
	if(res.name.startsWith("gfx/kritter/reindeer")) {
	    return Resource.remote().loadwait("gfx/kritter/reindeer/reindeer");
	}
	if(res.name.startsWith("gfx/terobjs/producesack")) {
	    return Resource.remote().loadwait("gfx/terobjs/producesack");
	}
	return res;
    }
    
    public static void toggle(GameUI gui) {
	boolean shown = CFG.DISPLAY_GOB_HITBOX.get();
	boolean top = CFG.DISPLAY_GOB_HITBOX_TOP.get();
	if(!shown) {
	    CFG.DISPLAY_GOB_HITBOX.set(true);
	} else if(!top) {
	    CFG.DISPLAY_GOB_HITBOX_TOP.set(true);
	} else {
	    CFG.DISPLAY_GOB_HITBOX.set(false);
	    CFG.DISPLAY_GOB_HITBOX_TOP.set(false);
	}
    }
}
