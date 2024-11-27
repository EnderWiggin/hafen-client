package me.ender;

import haven.*;
import haven.render.*;
import haven.resutil.WaterTile;

import java.nio.FloatBuffer;
import java.nio.ShortBuffer;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

public class MeshUtils {
    private static final States.Facecull NoFacecull = new States.Facecull(States.Facecull.Mode.NONE);
    
    private static final Map<String, FastMesh> cache = new HashMap<>();
    
    public static Supplier<FastMesh> Circle(float radius) {
	return () -> getCircle(radius, !CFG.FLAT_TERRAIN.get());
    }
    
    public static Supplier<FastMesh> Ring(float radius, float width) {
	return () -> getRing(radius, width, !CFG.FLAT_TERRAIN.get());
    }
    
    private static FastMesh getCircle(float radius, boolean dynamic) {
	if(dynamic) {return makeDynamicCircle(radius);}
	
	String key = String.format("circle:%f", radius);
	if(cache.containsKey(key)) {
	    return cache.get(key);
	}
	
	FastMesh circle = makeStaticCircle(radius);
	cache.put(key, circle);
	return circle;
    }
    
    private static FastMesh getRing(float radius, float width, boolean dynamic) {
	if(dynamic) {return makeRing(radius, width, true);}
	
	String key = String.format("ring:%f:%f", radius, width);
	if(cache.containsKey(key)) {
	    return cache.get(key);
	}
	
	FastMesh circle = makeRing(radius, width, false);
	cache.put(key, circle);
	return circle;
    }
    
    private static FastMesh makeStaticCircle(float radius) {
	float h = 0.3f;
	int total = 64; //TODO: calculate based on radius?
	
	FloatBuffer vert = Utils.mkfbuf(3 * (total + 1));
	ShortBuffer ind = Utils.mksbuf(3 * total);
	
	vert.put(0).put(0).put(h);
	for (int i = 1; i <= total; i++) {
	    float rad = (float) (Math.PI * 2 * i / total);
	    float x = (float) Math.cos(rad) * radius;
	    float y = (float) Math.sin(rad) * radius;
	    
	    vert.put(x).put(y).put(h);
	    
	    short a = (short) 0;
	    short b = (short) ((i % total) + 1);
	    short c = (short) (((i + 1) % total) + 1);
	    
	    ind.put(a).put(b).put(c);
	}
	
	return new StateFastMesh(Pipe.Op.compose(MapMesh.postmap, NoFacecull, Location.nullrot, Location.nullz),
	    new VertexBuf(new VertexBuf.VertexData(vert), new VertexBuf.NormalData(vert)), ind);
    }
    
    private static FastMesh makeDynamicCircle(float radius) {
	float h = 0.6f;
	int total = 32; //TODO: calculate based on radius?
	float step = MCache.tilesz2.x / 6f;
	int steps = radius > 25 ? (int) (radius / step) : 0; //extra steps to make
	if(radius - steps * step < step / 2) {steps -= 1;}
	
	FloatBuffer vert = Utils.mkfbuf(3 * (total * (1 + steps) + 1));
	ShortBuffer ind = Utils.mksbuf(3 * total * (1 + 2 * steps));
	
	vert.put(0).put(0).put(h);
	for (int k = 0; k <= steps; k++) {
	    float r0 = radius - step * (steps - k);
	    for (int i = 1; i <= total; i++) {
		float rad = (float) (Math.PI * 2 * i / total);
		float x = (float) Math.cos(rad) * r0;
		float y = (float) Math.sin(rad) * r0;
		
		vert.put(x).put(y).put(h);
		
		short a = (short) (k == 0 ? 0 : ((k - 1) * total + (i % total) + 1));
		short b = (short) (k * total + (i % total) + 1);
		short c = (short) (k * total + ((i + 1) % total) + 1);
		
		ind.put(a).put(b).put(c);
		if(k > 0) {
		    b = (short) ((k - 1) * total + ((i + 1) % total) + 1);
		    ind.put(a).put(b).put(c);
		}
	    }
	}
	
	return new HeightFastMesh(Pipe.Op.compose(MapMesh.postmap, NoFacecull, Location.nullrot, MixColor.nil),
	    h, new VertexBuf(new VertexBuf.VertexData(vert), new VertexBuf.NormalData(vert)), ind);
    }
    
    private static final Pipe.Op RING_DYNAMIC = Pipe.Op.compose(MapMesh.postmap, NoFacecull, Clickable.No, Location.nullrot);
    private static final Pipe.Op RING_STATIC = Pipe.Op.compose(MapMesh.postmap, NoFacecull, Clickable.No, Location.nullrot, Location.nullz);
    
    private static FastMesh makeRing(float r0, float width, boolean dynamic) {
	float h = 0.25f;
	int total = 32; //TODO: calculate based on radius?
	float r1 = r0 + width;
	int MAX_IND = 2 * total;
	
	FloatBuffer vert = Utils.mkfbuf(3 * total * 2);
	ShortBuffer ind = Utils.mksbuf(3 * total * 2);
	
	for (int i = 0; i < total; i++) {
	    float angle = (float) (Math.PI * 2 * i / total);
	    float cos = (float) Math.cos(angle);
	    float sin = (float) Math.sin(angle);
	    float x0 = cos * r0;
	    float y0 = sin * r0;
	    float x1 = cos * r1;
	    float y1 = sin * r1;
	    
	    vert.put(x0).put(y0).put(h); //i
	    vert.put(x1).put(y1).put(h); //i+1
	    
	    short a = (short) (2 * i);
	    short b = (short) ((2 * i + 1) % MAX_IND);
	    short c = (short) ((2 * (i + 1) + 1) % MAX_IND);
	    
	    ind.put(a).put(b).put(c);
	    b = (short) ((2 * (i + 1)) % MAX_IND);
	    ind.put(a).put(b).put(c);
	    
	    /*
	    //This produces a 'sun'
	    short a = (short) (2 * i);
	    short b = (short) ((2 * i + 1) % (2 * total));
	    short c = (short) ((2 * i + 2) % (2 * total));
	    
	    ind.put(a).put(b).put(c);
	    b = (short) ((2 * i + 3) % (2 * total));
	    ind.put(a).put(b).put(c);
	    */
	}
	
	VertexBuf buf = new VertexBuf(new VertexBuf.VertexData(vert), new VertexBuf.NormalData(vert));
	
	if(dynamic) {
	    return new HeightFastMesh(RING_DYNAMIC, h, buf, ind);
	} else {
	    return new StateFastMesh(RING_STATIC, buf, ind);
	}
    }
    
    public static class StateFastMesh extends FastMesh {
	private final Pipe.Op state;
	protected boolean disposable = false;
	
	public StateFastMesh(Pipe.Op state, VertexBuf vert, ShortBuffer ind) {
	    super(vert, ind);
	    this.state = state;
	}
	
	@Override
	public void dispose() {
	    if(disposable) {super.dispose();}
	}
	
	@Override
	public void added(RenderTree.Slot slot) {
	    slot.ostate(state);
	}
    }
    
    public static class HeightFastMesh extends StateFastMesh implements IUpdatingMesh {
	
	private final float h;
	
	public HeightFastMesh(Pipe.Op state, float h, VertexBuf vert, ShortBuffer ind) {
	    super(state, vert, ind);
	    this.h = h;
	    disposable = true;
	}
	
	@Override
	public boolean update(Render g, Gob gob) {
	    Coord2d c = gob.rc;
	    MCache map = gob.glob.map;
	    VertexBuf.VertexData buf = (VertexBuf.VertexData) vert.bufs[0];
	    int n = buf.size();
	    FloatBuffer points = buf.data;
	    
	    try {
		DrawOffset dro = gob.getattr(DrawOffset.class);
		Tiler t = map.tiler(map.gettile(c.floor(MCache.tilesz)));
		float waterHeight = 0.0F;
		if(t instanceof WaterTile) {
		    waterHeight = map.getzp(gob.rc).z - gob.getrc().z;
		}
		
		float bz = (float) map.getcz(c.x, c.y);
		
		for (int i = 0; i < n; ++i) {
		    float z = (float) map.getcz(c.x + (double) points.get(i * 3), c.y - (double) points.get(i * 3 + 1)) - bz;
		    if(dro != null) {
			points.put(3 * i + 2, z + h - dro.off.z);
		    } else {
			points.put(3 * i + 2, z + h + waterHeight);
		    }
		}
	    } catch (Loading loading) {
		return false;
	    }
	    
	    this.vert.update(g);
	    return true;
	}
    }
    
    interface IUpdatingMesh {
	boolean update(Render g, Gob gob);
    }
    
}
