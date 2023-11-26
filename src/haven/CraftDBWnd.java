package haven;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import haven.MenuGrid.Pagina;
import haven.rx.Reactor;
import rx.Subscription;

import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.lang.reflect.Type;
import java.util.List;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static haven.CraftDBWnd.Mode.*;
import static haven.ItemFilter.*;

public class CraftDBWnd extends WindowX implements DTarget2 {
    private static final int PANEL_H = UI.scale(52);
    private static final Coord WND_SZ = UI.scale(635, 360).addy(PANEL_H);
    private static final Coord ICON_SZ = UI.scale(20, 20);
    private static final int LIST_SIZE = (WND_SZ.y - PANEL_H) / ICON_SZ.y;
    
    private static final String CONFIG_JSON = "favourites.json";
    private static final Gson gson;
    private static final Map<String, List<String>> config;
    private Subscription subscription;
    
    private RecipeListBox box;
    private Tex description;
    private Widget makewnd;
    private MenuGrid menu;
    private MenuGrid.Pagina CRAFT;
    private MenuGrid.Pagina HISTORY;
    private MenuGrid.Pagina FAVOURITES;
    private Breadcrumbs<Pagina> breadcrumbs;
    private static Pagina current = null;
    private Pagina descriptionPagina;
    private Pagina senduse = null;
    private ToggleButton btnFavourite;
    private List<String> favourites;
    
    TabStrip<Mode> tabStrip;
    private final Pattern category = Pattern.compile("paginae/craft/.+");
    private int pagseq = 0;
    private boolean needfilter = false;
    private final ReadLine filter = ReadLine.make(null, "");
    private Mode mode = All;
    
    static {
	gson = (new GsonBuilder()).setPrettyPrinting().create();
	Map<String, List<String>> cfg = null;
	try {
	    Type type = new TypeToken<Map<String, List<String>>>() {
	    }.getType();
	    cfg = gson.fromJson(Config.loadFile(CONFIG_JSON), type);
	} catch (Exception ignore) {}
	if(cfg == null) {
	    cfg = new HashMap<>();
	}
	config = cfg;
    }
    
    private static void save() {
	Config.saveFile(CONFIG_JSON, gson.toJson(config));
    }
    
    enum Mode {
        All("paginae/act/craft", true),
	Favourites("paginae/act/favourites", false),
	History("paginae/act/history", false);
	
        public final LinkedList<Pagina> items;
	public final boolean reparent;
	private final Resource.Named res;
 
	Mode(String name, boolean reparent) {
	    Resource.remote().loadwait(name);
	    this.res = Resource.remote().load(name);
	    this.reparent = reparent;
	    this.items = new LinkedList<>();
	}
    }
    
    public CraftDBWnd() {
	super(WND_SZ.add(UI.scale(0, 5)), "Craft window");
	CFG.REAL_TIME_CURIO.observe(cfg -> updateDescription(descriptionPagina));
	CFG.SHOW_CURIO_LPH.observe(cfg -> updateDescription(descriptionPagina));
    }
    
    private void loadFavourites(String player) {
	Favourites.items.clear();
	if(!config.containsKey(player) || config.get(player) == null) {
	    config.put(player, new LinkedList<>());
	}
	favourites = config.get(player);
	favourites.stream()
	    .map(ui.gui.menu::paginafor)
	    .filter(Objects::nonNull)
	    .collect(Collectors.toCollection(() -> Favourites.items));
    }

    @Override
    protected void attach(UI ui) {
	super.attach(ui);
	init();
    }

    @Override
    public void destroy() {
	subscription.unsubscribe();
	box.destroy();
	super.destroy();
    }

    private void init() {
	CRAFT = paginafor("paginae/act/craft");
	HISTORY = paginafor("paginae/act/history");
	FAVOURITES = paginafor("paginae/act/favourites");
    
	loadFavourites(Config.userpath());
	subscription = Reactor.PLAYER.subscribe(this::loadFavourites);
    
	tabStrip = add(new TabStrip<Mode>() {
	    @Override
	    protected void selected(Button<Mode> button) {
		changeMode(button.tag);
	    }
	}, 0, UI.scale(2));
	Mode[] modes = Mode.values();
	for (int i = 0; i < modes.length; i++) {
	    Resource res = modes[i].res.get();
	    tabStrip.insert(i,
		new TexI(PUtils.convolvedown(res.layer(Resource.imgc).img, ICON_SZ, CharWnd.iconfilter)),
		paginafor(modes[i].res).act().name, null).tag = modes[i];
	}
    
	box = add(new RecipeListBox(UI.scale(200), LIST_SIZE) {
	    @Override
	    protected void itemclick(Pagina item, int button) {
		if(button == 1) {
		    if(item == ui.gui.menu.bk.pag) {
			item = current;
			if(getPaginaChildren(current).size() == 0) {
			    item = menu.getParent(item);
			}
			item = menu.getParent(item);
		    }
		    if((mode.reparent && filter.line().isEmpty()) || !item.isAction()) {
			menu.use(item, false);
		    } else {
			select(item, true, true);
		    }
		}
	    }
	}, 0, PANEL_H + UI.scale(5));
	addtwdg(new IButton("gfx/hud/btn-help", "", "-d", "-h") {
	    @Override
	    public void click() {
		ItemFilter.showHelp(ui, HELP_SIMPLE, HELP_CURIO, HELP_FEP, HELP_ARMOR, HELP_SYMBEL, HELP_ATTR, HELP_INPUTS);
	    }
	});
	btnFavourite = add(new ToggleButton(
	    "gfx/hud/btn-star-e", "", "-d", "-h",
	    "gfx/hud/btn-star-f", "", "-d", "-h"
	), box.c.x + box.sz.x + UI.scale(10), box.c.y + UI.scale(3));
	btnFavourite.recthit = true;
	btnFavourite.action(this::toggleFavourite);
	
	menu = ui.gui.menu;
	breadcrumbs = add(new Breadcrumbs<Pagina>(new Coord(WND_SZ.x, ICON_SZ.y)) {
	    @Override
	    public void selected(Pagina data) {
	        if(data == HISTORY) {
		    showHistory();
		} else if(data == FAVOURITES) {
		    showFavourites();
		} else {
		    select(data, false, false);
		    ui.gui.menu.use(data, false);
		}
	    }
	}, UI.scale(0, 28));
	Pagina selected = current;
	if(selected == null) {
	    selected = menu.cur;
	    if(selected == null || !menu.isCrafting(selected)) {
		selected = CRAFT;
	    }
	}
	tabStrip.select(0);
	select(selected, true, false);
    }

    @Override
    public void cdestroy(Widget w) {
	if(w == makewnd) {
	    makewnd = null;
	}
	super.cdestroy(w);
    }

    @Override
    public void wdgmsg(Widget sender, String msg, Object... args) {
	if((sender == this) && msg.equals("close")) {
	    close();
	    return;
	} else if((sender == makewnd) && msg.equals("make")) {
	    addToHistory(current);
	}
	super.wdgmsg(sender, msg, args);
    }
    
    private void changeMode(Mode mode) {
        this.mode = mode;
        tabStrip.select(mode, true);
        switch (mode) {
	    case All:
	        select(CRAFT, false, false);
		break;
	    case History:
	        showHistory();
		break;
	    case Favourites:
		showFavourites();
		break;
	}
    }
    
    private void showHistory() {
	filter.setline("");
	updateBreadcrumbs(HISTORY);
	box.setitems(History.items);
    }
    
    private void showFavourites() {
	filter.setline("");
	updateBreadcrumbs(FAVOURITES);
	box.setitems(Favourites.items);
    }
    
    private void addToHistory(Pagina action) {
	History.items.remove(action);
	History.items.addFirst(action);
	if(History.items.size() > LIST_SIZE) {
	    History.items.removeLast();
	}
	if(mode == History) {
	    box.sort();
	}
    }
    
    private void toggleFavourite() {
	if(current == null) {
	    btnFavourite.state(false);
	    return;
	}
	if(Favourites.items.contains(current)) {
	    removeFromFavourites(current);
	} else {
	    addToFavourites(current);
	}
    }
    
    private void removeFromFavourites(Pagina action) {
	if(Favourites.items.contains(action)) {
	    Favourites.items.remove(action);
	    if(favourites.remove(action.res().name)) {
		save();
	    }
	}
    }
    
    private void addToFavourites(Pagina action) {
	if(!Favourites.items.contains(action)) {
	    Favourites.items.add(action);
	    favourites.add(action.res().name);
	    save();
	}
    }
    
    public void close() {
	if(makewnd != null) {
	    makewnd.wdgmsg("close");
	    makewnd = null;
	}
	ui.destroy(this);
	ui.gui.craftwnd = null;
    }

    private List<Pagina> getPaginaChildren(Pagina parent) {
	List<Pagina> buf = new LinkedList<>();
	if (parent != null) {
	    menu.cons(parent, buf);
	}
	return buf;
    }

    public void select(Pagina p, boolean use) {
	if(mode != All) {changeMode(All);}
	if(!filter.line().isEmpty()) {
	    filter.setline("");
	    changeMode(All);
	    if(p == ui.gui.menu.bk.pag) {
	        p = CRAFT;
	    }
	}
	select(p, use, false);
    }
    
    private void select(Pagina p, boolean use, boolean skipReparent) {
	Pagina BACK = ui.gui.menu.bk.pag;
	boolean isBack = p == BACK;
	if(!menu.isCrafting(p) && !isBack) {
	    return;
	}
	if(box != null) {
	    if(!p.isAction()){
	        closemake();
	    }
	    if(!skipReparent) {
		List<Pagina> children = getPaginaChildren(p);
		if(children.size() == 0) {
		    children = getPaginaChildren(menu.getParent(p));
		}
		children.sort(MenuGrid.sorter);
		if(p != CRAFT) {
		    children.add(0, BACK);
		}
	    	filter.setline("");
		box.setitems(children);
	    }
	    box.change(p);
	    if(isBack) {
		p = null;
		if(box.listitems() > 1) {
		    p = menu.getParent(box.listitem(1));
		}
		setCurrent(p != null ? p : CRAFT);
	    } else {
		setCurrent(p);
	    }
	}
	if(use && !isBack) {
	    this.senduse = p;
	}
    }

    private void closemake() {
	if(makewnd != null) {
	    makewnd.wdgmsg("close");
	}
	senduse = null;
    }

    @Override
    public void cdraw(GOut g) {
	super.cdraw(g);

	if(senduse != null) {
	    Pagina p = senduse;
	    closemake();
	    p.button().use();
	}
	drawDescription(g);
    }

    private void drawDescription(GOut g) {
	if(descriptionPagina == null) {
	    return;
	}
	if(description == null) {
	    try {
		description = ItemData.longtip(descriptionPagina, ui.sess, 20, 5);
	    } catch (Loading ignored) {}
	}
	if(description != null) {
	    g.image(description, new Coord(box.c.x + box.sz.x + UI.scale(10), PANEL_H + UI.scale(5)));
	}
    }

    private void setCurrent(Pagina current) {
	CraftDBWnd.current = current;
	updateBreadcrumbs(current);
	updateDescription(current);
	btnFavourite.state(Favourites.items.contains(current));
    }

    private void updateBreadcrumbs(Pagina p) {
	List<Breadcrumbs.Crumb<Pagina>> crumbs = new LinkedList<>();
	if (filter.line().isEmpty()) {
	    if(mode == All) {
		List<Pagina> parents = getParents(p);
		Collections.reverse(parents);
		for(Pagina item : parents) {
		    BufferedImage img = item.res().layer(Resource.imgc).img;
		    Resource.AButton act = item.act();
		    String name = "...";
		    if(act != null) {
			name = act.name;
		    }
		    crumbs.add(new Breadcrumbs.Crumb<>(img, name, item));
		}
	    } else {
		crumbs.add(Breadcrumbs.Crumb.fromPagina(paginafor(mode.res)));
	    }
	} else {
	    crumbs.add(Breadcrumbs.Crumb.fromPagina(paginafor(mode.res)));
	    BufferedImage img = Resource.remote().loadwait("paginae/act/inspect").layer(Resource.imgc).img;
	    crumbs.add(new Breadcrumbs.Crumb<>(img, filter.line(), CRAFT));
	}
	breadcrumbs.setCrumbs(crumbs);
    }

    private List<Pagina> getParents(Pagina p) {
	List<Pagina> list = new LinkedList<>();
	if(getPaginaChildren(p).size() > 0) {
	    list.add(p);
	}
	Pagina parent;
	while ((parent = menu.getParent(p)) != null) {
	    list.add(parent);
	    p = parent;
	}
	return list;
    }

    private void updateDescription(Pagina p) {
	if(description != null) {
	    description.dispose();
	    description = null;
	}
	descriptionPagina = p;
    }

    public void setMakewindow(Widget widget) {
	makewnd = add(widget, new Coord(box.c.x + box.sz.x + UI.scale(10), box.c.y + box.sz.y - widget.sz.y));
    }
    
    private Pagina paginafor(Resource.Named res) {
	return ui.gui.menu.paginafor(res);
    }
    
    private Pagina paginafor(String name) {
	return ui.gui.menu.findPagina(name);
    }

    private void updateInfo(WItem item){
	ItemData.actualize(item.item, current);
	updateDescription(current);
    }

    @Override
    public boolean drop(WItem target, Coord cc, Coord ul) {
	updateInfo(target);
	return true;
    }

    @Override
    public boolean iteminteract(WItem target, Coord cc, Coord ul) {
	updateInfo(target);
	return true;
    }
    
    @Override
    public void tick(double dt) {
	super.tick(dt);
    
	MenuGrid menu = ui.gui.menu;
	synchronized (menu.paginae) {
	    if(pagseq != menu.pagseq) {
		synchronized (All.items) {
		    All.items.clear();
		    All.items.addAll(
			menu.paginae.stream()
			    .filter(p -> category.matcher(Pagina.name(p)).matches())
			    .collect(Collectors.toList())
		    );
		    
		    pagseq = menu.pagseq;
		    needfilter();
		}
	    }
	}
	if(needfilter) {
	    filter();
	}
    }
    
    private void needfilter() {
	needfilter = true;
    }
    
    private void filter() {
	needfilter = false;
	String filter = this.filter.line().toLowerCase();
	if (filter.isEmpty()) {
	    return;
	}
	ItemFilter itemFilter = ItemFilter.create(filter);
	synchronized (mode.items) {
	    List<Pagina> filtered = mode.items.stream().filter(p -> {
		try {
		    Resource res = p.res.get();
		    String name = res.layer(Resource.action).name.toLowerCase();
		    return (name.contains(filter) || itemFilter.matches(p, ui.sess));
		} catch (Loading e) {
		    needfilter = true;
		}
		return false;
	    }).sorted(new ItemComparator(filter)).collect(Collectors.toList());
	    box.setitems(filtered);
	    if(filtered.isEmpty()) {
		if(!needfilter) {closemake();}
		box.change(null);
		setCurrent(null);
	    } else {
		select(filtered.get(0), true, true);
	    }
	}
    }
    
    @Override
    public boolean keydown(KeyEvent ev) {
	if(ignoredKey(ev)) {
	    return false;
	}
	switch (ev.getKeyCode()) {
	    case KeyEvent.VK_ESCAPE:
		if(!filter.line().isEmpty()) {
		    changeMode(mode);
		} else {
		    close();
		}
		return true;
	    case KeyEvent.VK_DOWN:
		if(box.listitems() > 0) {
		    select(box.listitem((box.selindex + 1) % box.listitems()), true, true);
		}
		return true;
	    case KeyEvent.VK_UP:
		if(box.listitems() > 0) {
		    select(box.listitem((Math.max(box.selindex, 0) - 1 + box.listitems()) % box.listitems()), true, true);
		}
		return true;
	    case KeyEvent.VK_ENTER:
		if(box.sel != null) {
		    if(!box.sel.isAction()) {
			box.itemclick(box.sel, 1);
		    } else if(makewnd != null) {
			makewnd.wdgmsg("make", ui.modctrl ? 1 : 0);
		    }
		}
		return true;
	}
 
	String before = filter.line();
	if(filter.key(ev) && !before.equals(filter.line())) {
	    needfilter();
	    if(filter.line().isEmpty()) {
		changeMode(mode);
	    }
	}
	return true;
    }
    
    private static boolean ignoredKey(KeyEvent ev){
	int code = ev.getKeyCode();
	int mods = ev.getModifiersEx();
	//any modifier except SHIFT pressed alone is ignored, TAB is also ignored
	return (mods != 0 && mods != KeyEvent.SHIFT_DOWN_MASK)
	    || code == KeyEvent.VK_CONTROL
	    || code == KeyEvent.VK_ALT
	    || code == KeyEvent.VK_META
	    || code == KeyEvent.VK_TAB;
    }
    
    private static class RecipeListBox extends Listbox<Pagina> {
	private static final Color BGCOLOR = new Color(0, 0, 0, 113);
	private List<Pagina> list;
	private final Map<Pagina, Tex> texes = new HashMap<>();
	
	public RecipeListBox(int w, int h) {
	    super(w, h, ICON_SZ.y);
	    bgcolor = BGCOLOR;
	}
	
	@Override
	protected Pagina listitem(int i) {
	    if(list == null) {
		return null;
	    }
	    return list.get(i);
	}

	public void setitems(List<Pagina> list) {
	    if(list.equals(this.list)) {
		return;
	    }
	    this.list = list;
	    sb.max = listitems() - h;
	    sb.val = 0;
	    if(!list.contains(sel)) {
		selindex = -1;
	    }
	}
	
	public void sort() {
	}
	
	@Override
	public void change(Pagina item) {
	    super.change(item);
	    int k = list.indexOf(item);
	    if(k >= 0) {
		if(k < sb.val) {
		    sb.val = k;
		}
		if(k >= sb.val + h) {
		    sb.val = Math.min(sb.max, k - h + 1);
		}
	    }
	}

	@Override
	protected int listitems() {
	    if(list == null) {
		return 0;
	    }
	    return list.size();
	}
	
	@Override
	protected void drawitem(GOut g, Pagina item, int i) {
	    if(item == null) {
		return;
	    }
	    Tex tex = tex(item);
	    if(tex != null) {
		g.image(tex, Coord.z);
	    }
	}
	
	private Tex tex(Pagina p) {
	    if(!texes.containsKey(p)) {
		Tex tex = null;
		Resource res = p.res();
		if(res != null) {
		    BufferedImage icon = PUtils.convolvedown(res.layer(Resource.imgc).img, ICON_SZ, CharWnd.iconfilter);
		    
		    Resource.AButton act = p.act();
		    String name = "...";
		    if(act != null) {
			name = act.name;
		    }
		    BufferedImage text = Text.render(name).img;
		    
		    tex = new TexI(ItemInfo.catimgsh(3, icon, text));
		}
		texes.put(p, tex);
	    }
	    return texes.get(p);
	}
    }
    
    private static class ItemComparator implements Comparator<Pagina> {
	private final String filter;
	
	public ItemComparator(String filter) {
	    this.filter = filter;
	}
	
	@Override
	public int compare(Pagina a, Pagina b) {
	    String an = a.act().name.toLowerCase();
	    String bn = b.act().name.toLowerCase();
	    if(filter != null && !filter.isEmpty()) {
		boolean ai = an.startsWith(filter);
		boolean bi = bn.startsWith(filter);
		if(ai && !bi) {return -1;}
		if(!ai && bi) {return 1;}
	    }
	    boolean ac = !a.isAction();
	    boolean bc = !b.isAction();
	    if(ac && !bc) {return -1;}
	    if(!ac && bc) {return 1;}
	    return an.compareTo(bn);
	}
    }
}
