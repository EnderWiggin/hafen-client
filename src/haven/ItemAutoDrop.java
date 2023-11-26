package haven;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import rx.functions.Action0;

import java.awt.*;
import java.util.*;
import java.util.List;

import static haven.TileHighlight.*;

public class ItemAutoDrop {
    private static final List<Action0> updateCallbacks = new LinkedList<>();
    private static final Map<String, Boolean> cfg = new HashMap<>();
    private static final String CFG_NAME = "item_drop.json";
    private static Gson gson;
    private static CFGWnd wnd;
    
    public static void addCallback(Action0 callback) {
	updateCallbacks.add(callback);
    }
    
    public static void removeCallback(Action0 callback) {
	updateCallbacks.remove(callback);
    }
    
    public static void toggle(UI ui) {
	tryInit();
	if(wnd == null) {
	    wnd = ui.gui.add(new CFGWnd(), ui.gui.invwnd.c);
	} else {
	    wnd.destroy();
	}
    }
    
    public static boolean needDrop(String name) {
	tryInit();
	return cfg.getOrDefault(name, false);
    }
    
    public static String name(WItem target) {
	String result = target.name.get(null);
	target = target.item.contents != null
	    ? target.item.contents.getchild(WItem.class)
	    : null;
	
	if(target != null) {
	    result = name(target);
	}
	if(result != null && result.endsWith(", stack of")) {
	    result = result.substring(0, result.length() - 10);
	}
	return result;
    }
    
    private static void toggle(String name) {
	boolean value = !cfg.getOrDefault(name, false);
	if(cfg.put(name, value) == null) {
	    save();
	    if(wnd != null) {wnd.addItem(name);}
	}
	if(value) {
	    updateCallbacks();
	}
    }
    
    private static boolean add(String name) {
	if(cfg.put(name, true) == null) {
	    save();
	    updateCallbacks();
	    return true;
	}
	return false;
    }
    
    
    private static void remove(String name) {
	if(cfg.remove(name)) {
	    save();
	}
    }
    
    private static void updateCallbacks() {
	updateCallbacks.forEach(Action0::call);
    }
    
    
    private static void tryInit() {
	if(gson != null) {return;}
	gson = new GsonBuilder().create();
	try {
	    cfg.putAll(gson.fromJson(Config.loadFSFile(CFG_NAME), new TypeToken<Map<String, Boolean>>() {
	    }.getType()));
	} catch (Exception ignored) {}
    }
    
    private static void save() {
	if(gson != null) {
	    Config.saveFile(CFG_NAME, gson.toJson(cfg));
	}
    }
    
    private static class DropItem {
	private final String name;
	private final Tex tex;
	
	private DropItem(String name) {
	    //TODO: add I10N support
	    this.name = name;
	    this.tex = elf.render(this.name).tex();
	}
    }
    
    public static class CFGWnd extends WindowX implements DTarget2 {
	private static final String FILTER_DEFAULT = "Start typing to filter";
	private static final Comparator<DropItem> BY_NAME = Comparator.comparing(dropItem -> dropItem.name);
	
	private boolean raised = false;
	private final DropList list;
	private final Label filter;
	
	public CFGWnd() {
	    super(Coord.z, "Auto Drop");
	    justclose = true;
	    
	    int h = add(new CheckBox("Select All") {
		@Override
		public void changed(boolean val) {
		    list.filtered.forEach(item -> ItemAutoDrop.cfg.put(item.name, val));
		    save();
		    if(val) {
			updateCallbacks();
		    }
		}
	    }).pos("bl").y + UI.scale(3);
	    
	    list = add(new DropList(UI.scale(220), UI.unscale(12)), 0, h);
	    Position ur = list.pos("ur");
	    filter = adda(new Label(FILTER_DEFAULT), ur, 1, 1);
	    
	    Coord p = list.pos("bl").addys(10);
	    p = add(new OptWnd.CFGBox("Don't drop filtered items", CFG.AUTO_DROP_RESPECT_FILTER).set(CFGWnd::respectFilterChanged), p).pos("bl").addys(10);
	    p = add(new Label("Drop item on this window to add it to list"), p).pos("bl");
	    add(new Label("Right-click item to remove it"), p);
	    
	    pack();
	    setfocus(list);
	    populateList();
	}
    
	private static void respectFilterChanged(Boolean v) {
	    if(!v) {updateCallbacks();}
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
	
	private void populateList() {
	    List<DropItem> items = new ArrayList<>(ItemAutoDrop.cfg.size());
	    ItemAutoDrop.cfg.forEach((s, aBoolean) -> items.add(new DropItem(s)));
	    items.sort(BY_NAME);
	    list.setItems(items);
	}
	
	private void addItem(String name) {
	    list.items.add(new DropItem(name));
	    list.items.sort(BY_NAME);
	    list.needfilter();
	}
	
	private void updateFilter(String text) {
	    filter.settext((text == null || text.isEmpty()) ? FILTER_DEFAULT : text);
	    filter.c = list.pos("ur").sub(filter.sz).addys(-3);
	}
	
	@Override
	public void destroy() {
	    ItemAutoDrop.wnd = null;
	    super.destroy();
	}
	
	@Override
	public boolean drop(WItem target, Coord cc, Coord ul) {
	    String name = name(target);
	    if(name != null) {
		if(ItemAutoDrop.add(name)) {
		    addItem(name);
		}
	    }
	    return true;
	}
	
	@Override
	public boolean iteminteract(WItem target, Coord cc, Coord ul) {
	    return false;
	}
	
	private class DropList extends FilteredListBox<DropItem> {
	    private Coord showc;
	    
	    public DropList(int w, int h) {
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
	    protected boolean match(DropItem item, String filter) {
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
			toggle(sel.name);
		    }
		    return (true);
		}
		return (super.keydown(ev));
	    }
	    
	    @Override
	    protected void drawitem(GOut g, DropItem item, int idx) {
		g.chcolor(((idx % 2) == 0) ? every : other);
		g.frect(Coord.z, g.sz());
		g.chcolor();
		g.aimage(item.tex, new Coord(0, elh / 2), 0.0, 0.5);
		g.image(CheckBox.sbox, showc);
		if(needDrop(item.name))
		    g.image(CheckBox.smark, showc);
	    }
	    
	    @Override
	    public boolean mousedown(Coord c, int button) {
		int idx = idxat(c);
		if((idx >= 0) && (idx < listitems())) {
		    Coord ic = c.sub(idxc(idx));
		    DropItem item = listitem(idx);
		    if(ic.x < showc.x + CheckBox.sbox.sz().x) {
			if(button == 1) {
			    toggle(item.name);
			} else if(button == 3) {
			    ItemAutoDrop.remove(item.name);
			    list.items.remove(item);
			    list.needfilter();
			}
			return (true);
		    }
		}
		return (super.mousedown(c, button));
	    }
	}
    }
}
