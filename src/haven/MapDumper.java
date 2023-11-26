package haven;

import haven.resutil.Ridges;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.text.SimpleDateFormat;
import java.util.Date;

public class MapDumper implements Defer.Callable<Object> {
    private static long start = 0;
    private static File sess;

    private static Type type;

    private final File file;
    private final MCache mCache;
    private final MCache.Grid grid;
    public static final Object sync = new Object();


    enum Type {
	NORMAL("map"), CAVE("cave"), HOUSE(null);

	public final String folder;

	Type(String folder) {
	    this.folder = folder;
	}
    }

    public static void dump(MCache mCache, MCache.Grid grid) {
	synchronized (sync) {
	    Type newType = gettype(mCache, grid);
	    if(newType != type) {
		newSession();
	    }
	    type = newType;
	    if(type.folder == null) {
		return;
	    }
	    checkSession();
	    Defer.later(new MapDumper(new File(sess, tileName(grid.gc)), mCache, grid));
	}
    }

    public static String tileName(Coord c) {return String.format("tile_%d_%d.png", c.x, c.y);}

    private static Type gettype(MCache mCache, MCache.Grid grid) {
	Coord c = new Coord(), sz = MCache.cmaps;
	for (c.y = 0; c.y < sz.y; c.y++) {
	    for (c.x = 0; c.x < sz.x; c.x++) {
		int t = grid.gettile(c);
		try {
		    Resource rq = mCache.tilesetr(t);
		    if(rq != null) {
			switch (rq.name) {
			    case "gfx/tiles/nil":
				return Type.HOUSE;
			    case "gfx/tiles/mine":
			    case "gfx/tiles/cave":
				return Type.CAVE;
			}
		    }
		} catch (Loading ignored) {
		}
	    }
	}
	return Type.NORMAL;
    }

    public static void newSession() {
	synchronized (sync) {
	    start = System.currentTimeMillis();
	    sess = null;
	}
    }

    private static void checkSession() {
	synchronized (sync) {
	    if(sess == null) {
		String date = new SimpleDateFormat("yyyy-MM-dd HH.mm.ss").format(new Date(start));
		sess = new File(String.format("%s/%s", type.folder, date));
		//noinspection ResultOfMethodCallIgnored
		sess.mkdirs();
		try {
		    Writer writer = new FileWriter(new File(sess.getParentFile(), "currentsession.js"));
		    writer.write(String.format("var currentSession = '%s';\n", date));
		    writer.close();
		} catch (IOException ignored) {}
	    }
	}
    }

    public static long session() {
	return start;
    }

    private MapDumper(File file, MCache mCache, MCache.Grid grid) {
	this.file = file;
	this.mCache = mCache;
	this.grid = grid;
    }

    @Override
    public Object call() throws InterruptedException {
	try {
	    BufferedImage img = drawmap();
	    store(img);
	} catch (Loading e) {
	    Defer.later(this);
	}
	return null;
    }

    private void store(BufferedImage img) {
	if(img == null) {
	    return;
	}
	try {
	    ImageIO.write(img, "png", file);
	} catch (IOException ignored) {
	}
    }

    private BufferedImage tileimg(int t, BufferedImage[] texes) {
	BufferedImage img = texes[t];
	if(img == null) {
	    Resource r = mCache.tilesetr(t);
	    if(r == null)
		return (null);
	    Resource.Image ir = r.layer(Resource.imgc);
	    if(ir == null)
		return (null);
	    img = ir.img;
	    texes[t] = img;
	}
	return (img);
    }

    public BufferedImage drawmap() {
	Coord sz = MCache.cmaps;

	BufferedImage[] texes = new BufferedImage[256];
	BufferedImage buf = TexI.mkbuf(sz);
	Coord c = new Coord();
	for (c.y = 0; c.y < sz.y; c.y++) {
	    for (c.x = 0; c.x < sz.x; c.x++) {
		int t = grid.gettile(c);
		BufferedImage tex = tileimg(t, texes);
		int rgb = 0;
		if(tex != null)
		    rgb = tex.getRGB(Utils.floormod(c.x, tex.getWidth()),
			Utils.floormod(c.y, tex.getHeight()));
		buf.setRGB(c.x, c.y, rgb);
	    }
	}
	for (c.y = 1; c.y < sz.y - 1; c.y++) {
	    for (c.x = 1; c.x < sz.x - 1; c.x++) {
		int t = grid.gettile(c);
		Tiler tl = mCache.tiler(t);
		if(tl instanceof Ridges.RidgeTile) {
		    if(Ridges.brokenp(mCache, grid, c)) {
			for (int y = c.y - 1; y <= c.y + 1; y++) {
			    for (int x = c.x - 1; x <= c.x + 1; x++) {
				Color cc = new Color(buf.getRGB(x, y));
				buf.setRGB(x, y, Utils.blendcol(cc, Color.BLACK, ((x == c.x) && (y == c.y)) ? 1 : 0.1).getRGB());
			    }
			}
		    }
		}
	    }
	}
	for (c.y = 1; c.y < sz.y - 1; c.y++) {
	    for (c.x = 1; c.x < sz.x - 1; c.x++) {
		try {
		    int t = grid.gettile(c);
		    Coord c0 = c.add(grid.ul);
		    if((mCache.gettile(c0.add(-1, 0)) > t) ||
			(mCache.gettile(c0.add(1, 0)) > t) ||
			(mCache.gettile(c0.add(0, -1)) > t) ||
			(mCache.gettile(c0.add(0, 1)) > t)) {
			buf.setRGB(c.x, c.y, Color.BLACK.getRGB());
		    }
		} catch (IndexOutOfBoundsException | Loading ignored) {}
	    }
	}
	return (buf);
    }
}
