package haven;

import auto.Actions;
import auto.Targets;
import haven.render.Pipe;
import haven.resutil.Curiosity;
import haven.rx.Reactor;
import me.ender.ClientUtils;
import me.ender.Reflect;
import rx.Subscription;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.List;
import java.util.*;
import java.util.stream.Collectors;

import static haven.Inventory.*;
import static haven.WItem.*;

public class ExtInventory extends Widget {
    private static final int margin = UI.scale(5);
    private static final int listw = UI.scale(150);
    private static final int itemh = UI.scale(20);
    private static final Color even = new Color(255, 255, 255, 16);
    private static final Color odd = new Color(255, 255, 255, 32);
    private static final String CFG_GROUP = "ext.group";
    private static final String CFG_SHOW = "ext.show";
    private static final String CFG_INV = "ext.inv";
    private static int curType = 0;
    private static final Set<String> EXCLUDES = Collections.unmodifiableSet(new HashSet<>(Arrays.asList("Steelbox", "Pouch", "Frame", "Tub", "Fireplace", "Rack", "Pane mold", "Table", "Purse")));
    public final Inventory inv;
    private final ItemGroupList list;
    private final Widget extension;
    private final Label space;
    private final TextButton type;
    private SortedMap<ItemType, List<WItem>> groups;
    private final Dropbox<Grouping> grouping;
    private boolean disabled = false;
    private boolean showInv = true;
    private boolean needUpdate = false;
    private double waitUpdate = 0;
    private boolean once = true;
    private WindowX wnd;
    private final ICheckBox chb_show = new ICheckBox("gfx/hud/btn-extlist", "", "-d", "-h");
    private final ICheckBox chb_repeat = new ICheckBox("gfx/hud/btn-repeat", "", "-d", "-h");
    
    public ExtInventory(Coord sz) {
	inv = new Inventory(sz);
	inv.ext = this;
	extension = new Extension();
	chb_repeat.settip("$b{Toggle repeat mode}\nApply any menu action to\nall items in the group.", true);
	chb_show
	    .rclick(this::toggleInventory)
	    .changed(this::setVisibility)
	    .settip("LClick to toggle extra info\nRClick to hide inventory when info is visible", true);
    
	Composer composer = new Composer(extension).hmrgn(margin).vmrgn(margin);
	composer.add(0);
	grouping = new Dropbox<Grouping>(UI.scale(75), 5, UI.scale(16)) {
	    {bgcolor = new Color(16, 16, 16, 128);}
	    
	    @Override
	    protected Grouping listitem(int i) {
		return Grouping.values()[i];
	    }
	    
	    @Override
	    protected int listitems() {
		return Grouping.values().length;
	    }
	    
	    @Override
	    protected void drawitem(GOut g, Grouping item, int i) {
		g.atext(item.name, UI.scale(3, 8), 0, 0.5);
	    }
	    
	    @Override
	    public Object itemtip(Grouping item) { return ""; }
	    
	    @Override
	    public void change(Grouping item) {
		if(item != sel && wnd != null) {
		    wnd.cfg.setValue(CFG_GROUP, item.name());
		    wnd.storeCfg();
		}
		needUpdate = true;
		super.change(item);
	    }
	};
	space = new Label("");
	type = new TextButton(DisplayType.values()[curType].name(), Coord.of(70, 0), this::changeDisplayType);
	grouping.sel = Grouping.NONE;
	composer.addr(
	    new Label("Group:"), 
	    grouping, 
	    chb_repeat, 
	    new IButton("gfx/hud/btn-help", "","-d","-h", this::showHelp).settip("Help")
	);
	list = new ItemGroupList(listw, (inv.sz.y - composer.y() - 2 * margin - space.sz.y) / itemh, itemh);
	composer.add(list);
	composer.addr(space, type);
	type.c.x = listw - type.sz.x - margin;
	extension.pack();
	composer = new Composer(this).hmrgn(margin);
	composer.addr2(inv, extension);
	pack();
	
    }
    
    private void showHelp() {
        HelpWnd.show(ui, "halp/extrainv");
    }
    
    public void hideExtension() {
	extension.hide();
	updateLayout();
    }
    
    public void showExtension() {
	extension.show();
	updateLayout();
    }
    
    public void disable() {
	hideExtension();
	disabled = true;
	chb_show.hide();
	if(wnd != null) {wnd.placetwdgs();}
    }
    
    private void minRowsChanged(CFG<Integer> cfg){
	updateLayout();
    }
    
    @Override
    public void unlink() {
	CFG.UI_EXT_INV_MIN_ROWS.unobserve(this::minRowsChanged);
	ui.remInventory(this);
	if(chb_show.parent != null) {
	    chb_show.unlink();
	}
	if(wnd != null) {
	    wnd.remtwdg(chb_show);
	}
	super.unlink();
    }
    
    @Override
    protected void attached() {
	CFG.UI_EXT_INV_MIN_ROWS.observe(this::minRowsChanged);
	ui.addInventory(this);
	super.attached();
    }
    
    @Override
    protected void added() {
	wnd = null;//just in case
	Window tmp;
	//do not try to add if we are in the contents window
	if(!(parent instanceof GItem.ContentsWindow)
	    //or in the item
	    && !(parent instanceof GItem)
	    //or if we have no window parent, 
	    && (tmp = getparent(Window.class)) != null
	    //or it is not WindowX for some reason
	    && tmp instanceof WindowX) {
	
	    wnd = (WindowX) tmp;
	    disabled = disabled || needDisableExtraInventory(wnd.caption());
	    boolean vis = !disabled && wnd.cfg.getValue(CFG_SHOW, false);
	    showInv = wnd.cfg.getValue(CFG_INV, true);
	    if(!disabled) {
		chb_show.a = vis;
		wnd.addtwdg(chb_show);
		grouping.sel = Grouping.valueOf(wnd.cfg.getValue(CFG_GROUP, Grouping.NONE.name()));
		needUpdate = true;
	    }
	}
	hideExtension();
    }
    
    private void setVisibility(boolean v) {
	if(wnd != null) {
	    wnd.cfg.setValue(CFG_SHOW, v);
	    wnd.storeCfg();
	}
	if(v) {
	    showExtension();
	} else {
	    hideExtension();
	}
    }
    
    private void toggleInventory() {
	showInv = !showInv;
	if(wnd != null) {
	    wnd.cfg.setValue(CFG_INV, showInv);
	    wnd.storeCfg();
	}
	updateLayout();
    }
    
    private void updateLayout() {
	inv.visible = showInv || !extension.visible;
    
	if(wnd == null) {
	    pack();
	    return;
	}
	
	int szx = 0;
	int szy = inv.pos("br").y;
	if(inv.visible && parent != null) {
	    szx = inv.sz.x;
	    for (Widget w : wnd.children()) {
		if(w != this && (wnd != parent || w != wnd.deco)) {
		    Position p = w.pos("br");
		    szx = Math.max(szx, p.x);
		    szy = Math.max(szy, p.y);
		}
	    }
	}
	extension.move(new Coord(szx + margin, extension.c.y));
	type.c.y = space.c.y = szy - space.sz.y;
	int list_h = space.c.y - grouping.sz.y - 2 * margin;
	int min_items = CFG.UI_EXT_INV_MIN_ROWS.get();
	if(list_h / itemh < min_items) {
	    list_h = min_items * itemh;
	    type.c.y = space.c.y = list.c.y + list_h;
	}
	list.resize(new Coord(list.sz.x, list_h));
	extension.pack();
	pack();
	if(wnd != null) {wnd.pack();}
	if(showInv) {
	    chb_show.setTex("gfx/hud/btn-extlist", "", "-d", "-h");
	} else {
	    chb_show.setTex("gfx/hud/btn-extlist2", "", "-d", "-h");
	}
    }
    
    private void updateSpace() {
	String value = String.format("%d/%d", inv.filled(), inv.size());
	if(!value.equals(space.texts)) {
	    space.settext(value);
	}
    }
    
    @Override
    public void addchild(Widget child, Object... args) {
	inv.addchild(child, args);
    }
    
    @Override
    public void cdestroy(Widget w) {
	super.cdestroy(w);
	inv.cdestroy(w);
    }
    
    public void itemsChanged() {
	waitUpdate = 0.05;
	needUpdate = true;
    }
    
    @Override
    public void wdgmsg(Widget sender, String msg, Object... args) {
	if(sender == inv) {
	    super.wdgmsg(this, msg, args);
	} else {
	    super.wdgmsg(sender, msg, args);
	}
    }
    
    @Override
    public void uimsg(String msg, Object... args) {
	boolean mask = msg.equals("mask");
	if(mask || msg.equals("sz") || msg.equals("mode")) {
	    int szx = inv.sz.x;
	    int szy = inv.sz.y;
	    inv.uimsg(msg, args);
	    if((szx != inv.sz.x) || (szy != inv.sz.y) || mask) {
		updateLayout();
	    }
	} else {
	    super.uimsg(msg, args);
	}
    }

    @Override
    public void tick(double dt) {
	if(waitUpdate > 0) {waitUpdate -= dt;}
	if(needUpdate && extension.visible && waitUpdate <= 0) {
	    needUpdate = false;
	    SortedMap<ItemType, List<WItem>> groups = new TreeMap<>();
	    inv.forEachItem((g, w) -> processItem(groups, w));
	    this.groups = groups;
	    list.changed();
	}
	if(once) {
	    once = false;
	    if(!disabled && chb_show.a) {
		showExtension();
	    }
	}
	if(extension.visible) {
	    updateSpace();
	    updateTypeText();
	}
	super.tick(dt);
    }
    
    private void changeDisplayType(Integer btn) {
	curType = (curType + (btn == 1 ? 1 : -1) + DisplayType.values().length) % DisplayType.values().length;
	updateTypeText();
    }
    
    private void updateTypeText() {
	String t = DisplayType.values()[curType].name();
	type.setText(t);
	type.c.x = listw - type.sz.x - margin;
    }
    
    private void processItem(SortedMap<ItemType, List<WItem>> groups, WItem witem) {
	try {
	    if(tryToUnpack(groups, witem)) {
		Double quality = quality(witem, grouping.sel);
		ItemType type = new ItemType(witem, quality);
		if(type.loading) {needUpdate = true;}
		groups.computeIfAbsent(type, k -> new ArrayList<>()).add(witem);
	    }
	} catch (Loading ignored) {
	    needUpdate = true;
	}
    }
    
    /** returns true if this item should be processed, false if skipped (e.g. stack that's unpacked) */
    private boolean tryToUnpack(SortedMap<ItemType, List<WItem>> groups, WItem wItem) {
	Widget inv = wItem.item.contents;
	if(inv == null || !CFG.UI_STACK_EXT_INV_UNPACK.get()) {
	    return true;
	}
	inv.children(WItem.class).forEach((w) -> processItem(groups, w));
	return !Reflect.is(inv, "haven.res.ui.stackinv.ItemStack"); //this is just a stack, hide it
    }
    
    private static String name(WItem item) {
	return item.name.get("???");
    }
    
    private static String resname(WItem item) {
	return item.item.resname();
    }
    
    private static Double quality(WItem item) {
	return quality(item, Grouping.Q);
    }
    
    private static Double quality(WItem item, Grouping g) {
	if(g == null || g == Grouping.NONE) {return null;}
	QualityList q = item.itemq.get();
	return (q == null || q.isEmpty()) ? null : quantifyQ(q.single().value, g);
    }
    
    private static Double quantifyQ(Double q, Grouping g) {
	if(q == null) {return null;}
	if(g == Grouping.Q1) {
	    q = Math.floor(q);
	} else if(g == Grouping.Q5) {
	    q = Math.floor(q);
	    q -= q % 5;
	} else if(g == Grouping.Q10) {
	    q = Math.floor(q);
	    q -= q % 10;
	}
	return q;
    }
    
    private static class ItemType implements Comparable<ItemType> {
	final String name;
	final String resname;
	final Double quality;
	final boolean matches;
	final boolean loading;
	final Color color;
	final Pipe.Op state;
	final String cacheId;

	public ItemType(WItem w, Double quality) {
	    this.name = name(w);
	    this.resname = resname(w);
	    this.quality = quality;
	    this.matches = w.item.matches();
	    this.color = w.olcol.get();
	    this.state = this.color != null ? new ColorMask(this.color) : null;
	    loading = name.startsWith("???");
	    cacheId = String.format("%s@%s", resname, name);
	}

	@Override
	public int compareTo(ItemType other) {
	    int byMatch = Boolean.compare(other.matches, matches);
	    if(byMatch != 0) { return byMatch; }
	    
	    int byOverlay = 0;
	    if(!Objects.equals(color, other.color)) {
		if(color == null) {
		    byOverlay = 1;
		} else if(other.color == null) {
		    byOverlay = -1;
		} else {
		    byOverlay = Integer.compare(color.getRGB(), other.color.getRGB());
		}
	    }
	    if(byOverlay != 0) { return byOverlay; }
	    
	    int byName = name.compareTo(other.name);
	    if(byName == 0) {
		byName = resname.compareTo(other.resname);
	    }
	    if((byName != 0) || (quality == null) || (other.quality == null)) {
		return(byName);
	    }
	    return(-Double.compare(quality, other.quality));
	}
    }
    
    private static class ItemsGroup extends Widget {
	private static final Map<String, Tex> cache = new WeakHashMap<>();
	private static final Color progc = new Color(31, 209, 185, 128);
	private static final BufferedImage def = WItem.missing.layer(Resource.imgc).img;
	private static final Text.Foundry fnd = new Text.Foundry(Text.sans, 10).aa(true);
	final ItemType type;
	final List<WItem> items;
	final WItem sample;
	private final Tex[] text = new Tex[3];
	private final Subscription flowerSubscription;
	private final ExtInventory extInventory;
	private Tex icon;
	
	public ItemsGroup(ExtInventory extInventory, ItemType type, List<WItem> items, UI ui, Grouping g) {
	    super(new Coord(listw, itemh));
	    this.extInventory = extInventory;
	    this.ui = ui;
	    this.type = type;
	    this.items = items;
	    items.sort(ExtInventory::byQuality);
	    this.sample = items.get(0);
	    double quality;
	    if(type.quality == null) {
		quality = items.stream().map(ExtInventory::quality).filter(Objects::nonNull).reduce(0.0, Double::sum)
		    / items.stream().map(ExtInventory::quality).filter(Objects::nonNull).count();
	    } else {
		quality = type.quality;
	    }
	    String quantity = ClientUtils.f2s(items.stream().map(wItem -> wItem.quantity.get()).reduce(0f, Float::sum));
	    this.text[DisplayType.Name.ordinal()] = fnd.render(String.format("×%s %s", quantity, type.name)).tex();
	    if(!Double.isNaN(quality)) {
		String avg = type.quality != null ? "" : "~";
		String sign = (g == Grouping.NONE || g == Grouping.Q) ? "" : "+";
		String q = String.format("%sq%s%s", avg, ClientUtils.f2s(quality, 1), sign);
		this.text[DisplayType.Quality.ordinal()] = fnd.render(String.format("×%s %s", quantity, q)).tex();
	    } else {
		this.text[DisplayType.Quality.ordinal()] = text[DisplayType.Name.ordinal()];
	    }
	    this.text[DisplayType.Info.ordinal()] = info(sample, quantity, text[DisplayType.Name.ordinal()]);
	    flowerSubscription = Reactor.FLOWER_CHOICE.subscribe(this::flowerChoice);
	}
    
	@Override
	public void dispose() {
	    flowerSubscription.unsubscribe();
	    super.dispose();
	}
    
	private void flowerChoice(FlowerMenu.Choice choice) {
	    if(extInventory.chb_repeat.a && !choice.forced && choice.opt != null && Targets.item(choice.target) == sample) {
		flowerSubscription.unsubscribe();
		List<WItem> targets = items.stream().filter(wItem -> wItem != sample).collect(Collectors.toList());
		Actions.selectFlowerOnItems(ui.gui, choice.opt, targets);
	    }
	}
	
    
	private static Tex info(WItem itm, String count, Tex def) {
	    Curiosity curio = itm.curio.get();
	    if(curio != null) {
	        int lph = Curiosity.lph(curio.lph);
	        return RichText.render(String.format("×%s lph: $col[192,255,255]{%d}  mw: $col[255,192,255]{%d}", count, lph, curio.mw), 0).tex();
	    }
	    return def;
	}

	@Override
	public void draw(GOut g) {
	    if(icon == null) {
		if(cache.containsKey(type.cacheId)) {
		    icon = cache.get(type.cacheId);
		} else if(!type.loading) {
		    try {
			GSprite sprite = sample.item.sprite();
			if(sprite instanceof GSprite.ImageSprite) {
			    icon = GobIcon.SettingsWindow.ListIcon.tex(((GSprite.ImageSprite) sprite).image());
			} else {
			    Resource.Image image = sample.item.resource().layer(Resource.imgc);
			    if(image == null) {
				icon = GobIcon.SettingsWindow.ListIcon.tex(def);
			    } else {
				icon = GobIcon.SettingsWindow.ListIcon.tex(image.img);
			    }
			}
			cache.put(type.cacheId, icon);
		    } catch (Loading ignored) {
		    }
		}
	    }
	    int mode = curType % text.length;
	    if(icon != null) {
		double meter = sample.meter();
		if(meter > 0) {
		    g.chcolor(progc);
		    g.frect(new Coord(itemh + margin, 0), new Coord((int) ((sz.x - itemh - margin) * meter), sz.y));
		    g.chcolor();
		}
		int sx = (itemh - icon.sz().x) / 2;
		if(type.state != null) {
		    g.usestate(type.state);
		}
		g.aimage(icon, new Coord(sx, itemh / 2), 0.0, 0.5);
		g.defstate();
		g.aimage(text[mode], new Coord(itemh + margin, itemh / 2), 0.0, 0.5);
	    } else {
		g.aimage(text[mode], new Coord(0, itemh / 2), 0.0, 0.5);
	    }
	    if(type.matches) {
		g.chcolor(MATCH_COLOR);
		g.rect(Coord.z, sz);
		g.chcolor();
	    }
	}

	@Override
	public boolean mousedown(MouseDownEvent ev) {
	    boolean properButton = ev.b == 1 || ev.b == 3;
	    boolean reverse = ev.b == 3;
	    if(ui.modshift && properButton) {
		Object[] args = extInventory.getTransferTargets();
		if(args == null) {
		    process(items, ui.modmeta, reverse, "transfer", sqsz.div(2), 1);
		} else {
		    process(items, ui.modmeta, reverse, "invxf2", args);
		}
		return true;
	    } else if(ui.modctrl && properButton) {
		process(items, ui.modmeta, reverse, "drop", sqsz.div(2), 1);
		return true;
	    } else {
		WItem item = items.get(0);
		if(!item.disposed()) {
		    item.mousedown(new MouseDownEvent(ev, sqsz.div(2)));
		}
	    }
	    return (false);
	}
    
	private static void process(final List<WItem> items, boolean all, boolean reverse, String action, Object... args) {
	    if(reverse) {
		items.sort(ExtInventory::byReverseQuality);
	    } else {
		items.sort(ExtInventory::byQuality);
	    }
	    if(!all) {
		WItem item = items.get(0);
		if(!item.disposed()) {
		    item.item.wdgmsg(action, args);
		}
	    } else {
		for (WItem item : items) {
		    if(!item.disposed()) {
			item.item.wdgmsg(action, args);
		    }
		}
	    }
	}
	
	@Override
	public Object tooltip(Coord c, Widget prev) {
	    return(sample.tooltip(c, sample));
	}
    }
    
    private static int byReverseQuality(WItem a, WItem b) {
	return byQuality(b, a);
    }
    
    private static int byQuality(WItem a, WItem b) {
	Double qa = quality(a, Grouping.Q);
	Double qb = quality(b, Grouping.Q);
	if(Objects.equals(qa, qb)) {return 0;}
	if(qa == null) {return 1;}
	if(qb == null) {return -1;}
	return Double.compare(qb, qa);
    }
    
    public static boolean needDisableExtraInventory(String title) {
	return EXCLUDES.contains(title);
    }
    
    private class Extension extends Widget implements DTarget {
	@Override
	public boolean drop(Drop ev) {
	    Coord c = inv.findPlaceFor(ev.src.lsz);
	    if(c != null) {
		c = c.mul(sqsz).add(sqsz.div(2));
		inv.drop(c, c);
	    } else {
		ui.message("Non enough space!", GameUI.MsgType.BAD);
	    }
	    return true;
	}
	
	@Override
	public boolean iteminteract(Interact ev) {
	    return false;
	}
    }
    
    private class ItemGroupList extends Listbox<ItemsGroup> implements DTarget {
	private List<ItemsGroup> groups = Collections.emptyList();
	private boolean needsUpdate = false;

	public ItemGroupList(int w, int h, int itemh) {
	    super(w, h, itemh);
	}
	
	@Override
	public boolean drop(Drop ev) {
	    return false;
	}
	
	@Override
	public boolean iteminteract(Interact ev) {
	    ItemsGroup item = itemat(ev.c);
	    if(item == null) {return false;}
	    if(item.items.isEmpty()) {return false;}
	    item.items.get(0).iteminteract(ev);
	    return false;
	}

	@Override
	protected ItemsGroup listitem(int i) {
	    return(groups.get(i));
	}

	@Override
	protected int listitems() {
	    return(groups.size());
	}

	@Override
	protected void drawitem(GOut g, ItemsGroup item, int i) {
	    g.chcolor(((i % 2) == 0) ? even : odd);
	    g.frect(Coord.z, g.sz());
	    g.chcolor();
	    item.draw(g);
	}
    
	@Override
	public void dispose() {
	    groups.forEach(ItemsGroup::dispose);
	    super.dispose();
	}
    
	public void changed() {needsUpdate = true;}

	@Override
	public void tick(double dt) {
	    if(needsUpdate) {
	        groups.forEach(ItemsGroup::dispose);
		if(ExtInventory.this.groups == null) {
		    groups = Collections.emptyList();
		} else {
		    groups = ExtInventory.this.groups.entrySet().stream()
			.map(v -> new ItemsGroup(ExtInventory.this, v.getKey(), v.getValue(), ui, grouping.sel)).collect(Collectors.toList());
		}
	    }
	    needsUpdate = false;
	    super.tick(dt);
	}
    
	@Override
	protected void drawbg(GOut g) {
	}
    
	@Override
	public Object tooltip(Coord c, Widget prev) {
	    int idx = idxat(c);
	    ItemsGroup item = (idx >= listitems()) ? null : listitem(idx);
	    if(item != null) {
		return item.tooltip(Coord.z, prev);
	    }
	    return super.tooltip(c, prev);
	}
    }
    
    public static Inventory inventory(Widget wdg) {
	if(wdg instanceof ExtInventory) {
	    return ((ExtInventory) wdg).inv;
	} else if(wdg instanceof Inventory) {
	    return (Inventory) wdg;
	} else {
	    return null;
	}
    }
    
    //TODO: should we sort inventories based on z-order of windows?
    private Object[] getTransferTargets() {
	//use default transfer logic if transferring not from main inventory
	if(inv != ui.gui.maininv) {
	    return null;
	}
	List<Widget> inventories = ui.EXT_INVENTORIES;
	if(inventories.isEmpty()) {
	    return null;
	}
	Object[] args = new Object[2 + inventories.size()];
	int i = 0;
	args[i++] = 0; //flags
	args[i++] = 1; //how many to transfer
	for (Widget wdg : inventories) {
	    args[i++] = wdg.wdgid();
	}
	return args;
    }
    
    enum DisplayType {
	Quality, Name, Info
    }
    
    enum Grouping {
	NONE("Type"),
	Q("Quality"),
	Q1("Quality 1"),
	Q5("Quality 5"),
	Q10("Quality 10");
	
	private final String name;
	
	Grouping(String name) {this.name = name; }
    }
}
