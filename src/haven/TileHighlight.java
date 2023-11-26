package haven;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.WritableRaster;
import java.util.List;
import java.util.*;

import static haven.MCache.*;

public class TileHighlight {
    private static final Set<String> highlight = new HashSet<>();
    private static final Map<String, List<TileItem>> tiles = new HashMap<>();
    private static final List<String> categories = new ArrayList<>();
    public static final String TAG = "tileHighlight";
    private static boolean initialized = false;
    private static final String ALL = "All";
    
    private static String lastCategory = ALL;
    public static volatile long seq = 0;
    
    public static final Text.Foundry elf = CharWnd.attrf;
    public static final Color every = new Color(255, 255, 255, 16);
    public static final Color other = new Color(255, 255, 255, 32);
    public static final int elh = elf.height() + UI.scale(2);
    
    public static boolean isHighlighted(String name) {
	synchronized (highlight) {
	    return highlight.contains(name);
	}
    }
    
    public static void toggle(String name) {
	synchronized (highlight) {
	    if(highlight.contains(name)) {
		unhighlight(name);
	    } else {
		highlight(name);
	    }
	}
    }
    
    public static void highlight(String name) {
	synchronized (highlight) {
	    if(highlight.add(name)) {
		seq++;
	    }
	}
    }
    
    public static void unhighlight(String name) {
	synchronized (highlight) {
	    if(highlight.remove(name)) {
		seq++;
	    }
	}
    }
    
    public static BufferedImage olrender(MapFile.DataGrid grid) {
	TileHighlightOverlay ol = new TileHighlightOverlay(grid);
	WritableRaster buf = PUtils.imgraster(cmaps);
	Color col = ol.color();
	if(col != null) {
	    Coord c = new Coord();
	    for (c.y = 0; c.y < cmaps.y; c.y++) {
		for (c.x = 0; c.x < cmaps.x; c.x++) {
		    if(ol.get(c)) {
			buf.setSample(c.x, c.y, 0, ((col.getRed() * col.getAlpha()) + (buf.getSample(c.x, c.y, 1) * (255 - col.getAlpha()))) / 255);
			buf.setSample(c.x, c.y, 1, ((col.getGreen() * col.getAlpha()) + (buf.getSample(c.x, c.y, 1) * (255 - col.getAlpha()))) / 255);
			buf.setSample(c.x, c.y, 2, ((col.getBlue() * col.getAlpha()) + (buf.getSample(c.x, c.y, 2) * (255 - col.getAlpha()))) / 255);
			buf.setSample(c.x, c.y, 3, Math.max(buf.getSample(c.x, c.y, 3), col.getAlpha()));
		    }
		}
	    }
	}
	return (PUtils.rasterimg(buf));
    }
    
    public static void toggle(UI ui) {
	tryInit();
	if(ui.gui.tileHighlight == null) {
	    ui.gui.tileHighlight = ui.gui.add(new TileHighlightCFG(), ui.gui.mapfile.c);
	} else {
	    ui.gui.tileHighlight.destroy();
	}
    }
    
    private static void tryInit() {
	if(initialized) {return;}
	initialized = true;
	categories.add(ALL);
	ArrayList<TileItem> all = new ArrayList<>();
	tiles.put(ALL, all);
	Gson gson = new GsonBuilder().create();
	Map<String, List<String>> c = gson.fromJson(Config.loadJarFile("tile_highlight.json"), new TypeToken<Map<String, List<String>>>() {
	}.getType());
	for (Map.Entry<String, List<String>> entry : c.entrySet()) {
	    String category = entry.getKey();
	    categories.add(category);
	    List<String> tiles = entry.getValue();
	    List<TileItem> items = new ArrayList<>(tiles.size());
	    for (String tile : tiles) {
		items.add(new TileItem(tile));
	    }
	    items.sort(Comparator.comparing(item -> item.name));
	    TileHighlight.tiles.put(category, items);
	    all.addAll(items);
	}
	all.sort(Comparator.comparing(item -> item.name));
    }
    
    private static class TileItem {
	private final String name, res;
	private final Tex tex;
	
	private TileItem(String res) {
	    //TODO: add I10N support
	    this.res = res;
	    this.name = Utils.prettyResName(res);
	    this.tex = elf.render(this.name).tex();
	}
    }
    
    public static class TileHighlightCFG extends WindowX {
	public static final String FILTER_DEFAULT = "Start typing to filter";
	
	private boolean raised = false;
	private final TileList list;
	private final Label filter;
	private String category = lastCategory;
	
	public TileHighlightCFG() {
	    super(Coord.z, "Tile Highlight");
	    justclose = true;
	    
	    int h = add(new Label("Categories: "), Coord.z).sz.y;
	    add(new CheckBox("Select All") {
		@Override
		public void changed(boolean val) {
		    list.filtered.forEach(item -> {
			if(val) {
			    highlight(item.res);
			} else {
			    unhighlight(item.res);
			}
		    });
		}
	    }, UI.scale(135, 0));
	    h += UI.scale(5);
	    
	    add(new CategoryList(UI.scale(125), 8, elh), 0, h).sel = category;
	    
	    list = add(new TileList(UI.scale(220), UI.unscale(12)), UI.scale(135), h);
	    filter = adda(new Label(FILTER_DEFAULT), list.pos("ur").y(0), 1, 0);
	    pack();
	    setfocus(list);
	    list.setItems(tiles.get(category));
	}
	
	private void updateFilter(String text) {
	    filter.settext((text == null || text.isEmpty()) ? FILTER_DEFAULT : text);
	    filter.c = list.pos("ur").y(0).addx(-filter.sz.x);
	}
	
	@Override
	public void tick(double dt) {
	    super.tick(dt);
	    if(!raised) {
		parent.setfocus(this);
		raise();
		raised = true;
	    }
	}
	
	@Override
	public void destroy() {
	    super.destroy();
	    ui.gui.tileHighlight = null;
	}
	
	private class TileList extends FilteredListBox<TileItem> {
	    private Coord showc;
	    
	    public TileList(int w, int h) {
		super(w, h, elh);
		this.showc = showc();
		bgcolor = new Color(0, 0, 0, 84);
		showFilterText = false;
	    }
	    
	    private Coord showc() {
		return (new Coord(sz.x - (sb.vis() ? sb.sz.x : 0) - ((elh - CheckBox.sbox.sz().y) / 2) - CheckBox.sbox.sz().x,
		    ((elh - CheckBox.sbox.sz().y) / 2)));
	    }
	    
	    public void draw(GOut g) {
		this.showc = showc();
		super.draw(g);
	    }
	    
	    @Override
	    protected void filter() {
		super.filter();
		updateFilter(this.filter.line());
	    }
	    
	    @Override
	    protected boolean match(TileItem item, String filter) {
		if(filter.isEmpty()) {
		    return true;
		}
		if(item.name == null)
		    return (false);
		return (item.name.toLowerCase().contains(filter.toLowerCase()));
	    }
	    
	    public boolean keydown(java.awt.event.KeyEvent ev) {
		if(ev.getKeyCode() == java.awt.event.KeyEvent.VK_SPACE) {
		    if(sel != null) {
			toggle(sel.res);
		    }
		    return (true);
		}
		return (super.keydown(ev));
	    }
	    
	    @Override
	    protected void drawitem(GOut g, TileItem item, int idx) {
		g.chcolor(((idx % 2) == 0) ? every : other);
		g.frect(Coord.z, g.sz());
		g.chcolor();
		g.aimage(item.tex, new Coord(0, elh / 2), 0.0, 0.5);
		g.image(CheckBox.sbox, showc);
		if(isHighlighted(item.res))
		    g.image(CheckBox.smark, showc);
	    }
	    
	    @Override
	    public boolean mousedown(Coord c, int button) {
		int idx = idxat(c);
		if((idx >= 0) && (idx < listitems())) {
		    Coord ic = c.sub(idxc(idx));
		    TileItem item = listitem(idx);
		    if(ic.x < showc.x + CheckBox.sbox.sz().x) {
			toggle(item.res);
			return (true);
		    }
		}
		return (super.mousedown(c, button));
	    }
	}
	
	private class CategoryList extends Listbox<String> {
	    public CategoryList(int w, int h, int itemh) {
		super(w, h, itemh);
		bgcolor = new Color(0, 0, 0, 84);
	    }
	    
	    @Override
	    public void change(String item) {
		super.change(item);
		list.setItems(tiles.getOrDefault(item, Collections.emptyList()));
		list.sb.val = 0;
		category = lastCategory = item;
	    }
	    
	    @Override
	    protected String listitem(int i) {
		return categories.get(i);
	    }
	    
	    @Override
	    protected int listitems() {
		return categories.size();
	    }
	    
	    @Override
	    protected void drawitem(GOut g, String item, int i) {
		g.chcolor(((i % 2) == 0) ? every : other);
		g.frect(Coord.z, g.sz());
		g.chcolor();
		g.atext(item, new Coord(0, elh / 2), 0, 0.5);
	    }
	}
    }
    
    public static class TileHighlightOverlay {
	private final boolean[] ol;
	
	public TileHighlightOverlay(MapFile.DataGrid g) {
	    this.ol = new boolean[cmaps.x * cmaps.y];
	    fill(g);
	}
	
	private void fill(MapFile.DataGrid grid) {
	    if(grid == null) {return;}
	    Coord c = new Coord(0, 0);
	    for (c.x = 0; c.x < cmaps.x; c.x++) {
		for (c.y = 0; c.y < cmaps.y; c.y++) {
		    int tile = grid.gettile(c);
		    MapFile.TileInfo tileset = grid.tilesets[tile];
		    boolean v = isHighlighted(tileset.res.name);
		    set(c, v);
		    if(v) { setn(c, true); } //make 1 tile border around actual tiles
		}
	    }
	}
	
	public boolean get(Coord c) {
	    return (ol[c.x + (c.y * cmaps.x)]);
	}
	
	public void set(Coord c, boolean v) {
	    ol[c.x + (c.y * cmaps.x)] = v;
	}
	
	public void set(int x, int y, boolean v) {
	    if(x >= 0 && y >= 0 && x < cmaps.x && y < cmaps.y) {
		ol[x + (y * cmaps.x)] = v;
	    }
	}
	
	public void setn(Coord c, boolean v) {
	    set(c.x - 1, c.y - 1, v);
	    set(c.x - 1, c.y + 1, v);
	    set(c.x + 1, c.y - 1, v);
	    set(c.x + 1, c.y + 1, v);
	    set(c.x, c.y - 1, v);
	    set(c.x, c.y + 1, v);
	    set(c.x - 1, c.y, v);
	    set(c.x + 1, c.y, v);
	}
	
	public Color color() {
	    return Color.MAGENTA;
	}
    }
    
    
}
