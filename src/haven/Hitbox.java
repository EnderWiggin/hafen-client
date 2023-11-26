package haven;

import haven.render.*;

import java.awt.*;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

public class Hitbox extends SlottedNode implements Rendered {
    private static final VertexArray.Layout LAYOUT = new VertexArray.Layout(new VertexArray.Layout.Input(Homo3D.vertex, new VectorFormat(3, NumberFormat.FLOAT32), 0, 0, 12));
    private Model model;
    private final Gob gob;
    private static final Map<Resource, Model> MODEL_CACHE = new HashMap<>();
    private static final float Z = 0.1f;
    private static final Color SOLID_COLOR = new Color(178, 71, 178, 255);
    private static final Color PASSABLE_COLOR = new Color(105, 207, 124, 255);
    private static final float PASSABLE_WIDTH = 1.5f;
    private static final float SOLID_WIDTH = 3f;
    private static final Pipe.Op TOP = Pipe.Op.compose(Rendered.last, States.Depthtest.none, States.maskdepth);
    private static final Pipe.Op SOLID = Pipe.Op.compose(new BaseColor(SOLID_COLOR), new States.LineWidth(SOLID_WIDTH));
    private static final Pipe.Op PASSABLE = Pipe.Op.compose(new BaseColor(PASSABLE_COLOR), new States.LineWidth(PASSABLE_WIDTH));
    private static final Pipe.Op SOLID_TOP = Pipe.Op.compose(SOLID, TOP);
    private static final Pipe.Op PASSABLE_TOP = Pipe.Op.compose(PASSABLE, TOP);
    private Pipe.Op state = SOLID;
    
    private Hitbox(Gob gob) {
	model = getModel(gob);
	this.gob = gob;
	updateState();
    }
    
    public static Hitbox forGob(Gob gob) {
	try {
	    return new Hitbox(gob);
	} catch (Loading ignored) { }
	return null;
    }
    
    @Override
    public void added(RenderTree.Slot slot) {
	super.added(slot);
	slot.ostate(state(state));
	updateState();
    }
    
    @Override
    public void draw(Pipe context, Render out) {
	if(model != null) {
	    out.draw(context, model);
	}
    }
    
    public void updateState() {
	if(model != null && slots != null) {
	    boolean top = CFG.DISPLAY_GOB_HITBOX_TOP.get();
	    Pipe.Op newState = passable() ? (top ? PASSABLE_TOP : PASSABLE) : (top ? SOLID_TOP : SOLID);
	    try {
	    	Model m = getModel(gob);
		if(m != null && m != model) {
	    	    model = m;
	    	    slots.forEach(RenderTree.Slot::update);
		}
	    }catch (Loading ignored) {}
	    if(newState != state) {
		state = newState;
		newState = state(state);
		for (RenderTree.Slot slot : slots) {
		    slot.ostate(newState);
		}
	    }
	}
    }
    
    private boolean passable() {
	try {
	    String name = gob.resid();
	    ResDrawable rd = (gob.drawable instanceof ResDrawable) ? (ResDrawable) gob.drawable : null;
	    
	    if(rd != null) {
		int state = gob.sdt();
		if(name.endsWith("gate") && name.startsWith("gfx/terobjs/arch")) {//gates
		    if(state == 1) { // gate is open
			return true;
		    }
		} else if(name.endsWith("/dng/antdoor")) {
		    return state == 1 || state == 13;
		} else if(name.endsWith("/pow[hearth]")) {//hearth fire
		    return true;
		} else if(name.equals("gfx/terobjs/arch/cellardoor") || name.equals("gfx/terobjs/fishingnet")) {
		    return true;
		}
	    }
	} catch (Loading ignored) {}
	return false;
    }
    
    private static Model getModel(Gob gob) {
	Model model;
	Resource res = getResource(gob);
	synchronized (MODEL_CACHE) {
	    model = MODEL_CACHE.get(res);
	    if(model == null) {
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
		    VertexArray va = new VertexArray(LAYOUT, vbo);
		
		    model = new Model(Model.Mode.LINES, va, null);
		
		    MODEL_CACHE.put(res, model);
		}
	    }
	}
	return model;
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
	Collection<RenderLink.Res> links = res.layers(RenderLink.Res.class);
	for (RenderLink.Res link : links) {
	    if(link.l instanceof RenderLink.MeshMat) {
		RenderLink.MeshMat mesh = (RenderLink.MeshMat) link.l;
		return mesh.mesh.get();
	    }
	}
	return res;
    }
    
    private Pipe.Op state(Pipe.Op state) {
	float scale = gob.scale();
	if(scale <= 0 || scale >= 1) {return state;}
	return Pipe.Op.compose(state, scale(scale));
    }
    
    private static Pipe.Op scale(float scale) {
	scale = 1 / scale;
	return new Location(new Matrix4f(
	    scale, 0, 0, 0,
	    0, scale, 0, 0,
	    0, 0, scale, 0,
	    0, 0, 0, 1));
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
