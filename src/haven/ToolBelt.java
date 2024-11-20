package haven;

import me.ender.CustomPagina;

import java.awt.*;
import java.awt.event.KeyEvent;

import static haven.Inventory.*;

public class ToolBelt extends DraggableWidget implements DTarget, DropTarget {
    private static final Text.Foundry fnd = new Text.Foundry(Text.sans, 12);
    public static final int GAP = 10;
    public static final int PAD = 2;
    public static final int BTNSZ = 17;
    public static final Coord INVSZ = invsq.sz();
    public static final Color BG_COLOR = new Color(43, 54, 35, 202);
    public static final int[] FKEYS = {KeyEvent.VK_F1, KeyEvent.VK_F2, KeyEvent.VK_F3, KeyEvent.VK_F4,
	KeyEvent.VK_F5, KeyEvent.VK_F6, KeyEvent.VK_F7, KeyEvent.VK_F8,
	KeyEvent.VK_F9, KeyEvent.VK_F10, KeyEvent.VK_F11, KeyEvent.VK_F12};
    private final int[] beltkeys;
    private final int group;
    private final int start;
    private final int size;
    private final ToggleButton btnLock;
    private final IButton btnFlip;
    private boolean vertical = false, over = false, locked = false;
    final Tex[] keys;
    private GameUI.BeltSlot last = null;
    private Tex ttip = null;
    
    public ToolBelt(String name, int start, int group, int[] beltkeys) {
        this(name, start, group, beltkeys.length, beltkeys);
    }
    
    public ToolBelt(String name, int start, int group, int size) {
	this(name, start, group, size, null);
    }
    
    public ToolBelt(String name, int start, int group, int size, int[] beltkeys) {
	super(name);
	this.start = start;
	this.group = group;
	this.beltkeys = beltkeys;
	this.size = size;
	keys = new Tex[size];
	if(beltkeys != null) {
	    for (int i = 0; i < size; i++) {
		if(beltkeys[i] != 0) {
		    keys[i] = Text.renderstroked(KeyEvent.getKeyText(beltkeys[i]), fnd).tex();
		}
	    }
	}
    
	btnLock = add(new ToggleButton("gfx/hud/btn-ulock", "", "-d", "-h", "gfx/hud/btn-lock", "", "-d", "-h"));
	btnLock.action(this::toggle);
	btnLock.recthit = true;
	
	btnFlip = add(new IButton("gfx/hud/btn-flip", "", "-d", "-h"));
	btnFlip.action(this::flip);
	btnFlip.recthit = true;
    }
    
    @Override
    protected void initCfg() {
	super.initCfg();
	locked = cfg.getValue("locked", locked);
	vertical = cfg.getValue("vertical", vertical);
	btnLock.state(locked);
	draggable(!locked);
	resize();
	update_buttons();
    }
    
    private void update_buttons() {
	btnFlip.visible = !locked;
	if(vertical) {
	    btnLock.c = new Coord(BTNSZ, 0);
	    btnFlip.c = Coord.z;
	} else {
	    btnLock.c = new Coord(0, BTNSZ);
	    btnFlip.c = Coord.z;
	}
    }
    
    private void resize() {
	sz = beltc(size - 1).add(INVSZ);
    }
    
    private void toggle(Boolean state) {
	locked = state != null ? state : false;
	draggable(!locked);
	update_buttons();
	cfg.setValue("locked", locked);
	storeCfg();
    }
    
    private void flip() {
	vertical = !vertical;
	resize();
	update_buttons();
	cfg.setValue("vertical", vertical);
	storeCfg();
    }
    
    public void act(int idx, MenuGrid.Interaction iact) {
	GameUI.BeltSlot slot = belt(idx);
	if(slot != null) {
	    slot.use(iact);
	}
    }
    
    private GameUI.BeltSlot belt(int slot) {
	if(slot < 0) {return null;}
	if(ui != null && ui.gui != null) {
	    return ui.gui.belt[slot];
	}
	return null;
    }
    
    private Coord beltc(int i) {
	return vertical ?
	    new Coord(0, BTNSZ + ((INVSZ.y + PAD) * i) + (GAP * (i / group))) :
	    new Coord(BTNSZ + ((INVSZ.x + PAD) * i) + (GAP * (i / group)), 0);
    }
    
    private int beltslot(Coord c) {
	for (int i = 0; i < size; i++) {
	    if(c.isect(beltc(i), invsq.sz())) {
		return slot(i);
	    }
	}
	return (-1);
    }
    
    private MenuGrid.Pagina getpagina(GameUI.BeltSlot slot) {
	if(slot instanceof GameUI.PagBeltSlot) {
	    return ((GameUI.PagBeltSlot) slot).pag;
	} else if(slot instanceof GameUI.ResBeltSlot) {
	    return ui.gui.menu.findPagina(((GameUI.ResBeltSlot) slot).rdt.res);
	}
	return null;
    }
    
    @Override
    public void draw(GOut g) {
	if(over) {
	    if(!locked) {
		g.chcolor(BG_COLOR);
		g.frect(Coord.z, sz);
		g.chcolor();
	    }
	    super.draw(g);
	}
	for (int i = 0; i < size; i++) {
	    Coord c = beltc(i);
	    int slot = slot(i);
	    g.image(invsq, c);
	    try {
		GameUI.BeltSlot item = belt(slot);
		if(item != null) {
		    item.draw(g.reclip(c.add(1, 1), invsq.sz().sub(2, 2)));
		}
	    } catch (Loading ignored) {}
	    if(keys[i] != null) {
		g.aimage(keys[i], c.add(INVSZ.sub(2, 0)), 1, 1);
	    }
	}
    }
    
    private int slot(int i) {return i + start;}
    
    @Override
    public boolean globtype(GlobKeyEvent ev) {
	//do we need to skip if CTRL (and only it) is held, like we do for normal tool belt?
	if(!visible || beltkeys == null) { return false;}
	for (int i = 0; i < beltkeys.length; i++) {
	    if(ev.code == beltkeys[i]) {
		keyact(slot(i));
		return true;
	    }
	}
	return false;
    }
    
    public void keyact(final int slot) {
	MapView map = ui.gui.map;
	if(map == null) {return;}
	Coord mvc = map.rootxlate(ui.mc);
	if(!mvc.isect(Coord.z, map.sz)) {return;}
	map.new Hittest(mvc) {
	    protected void hit(Coord pc, Coord2d mc, ClickData inf) {
		act(slot, new MenuGrid.Interaction(1, ui.modflags(), mc, inf));
	    }
	    
	    protected void nohit(Coord pc) {
		act(slot, new MenuGrid.Interaction(1, ui.modflags()));
	    }
	}.run();
    }
    
    @Override
    public boolean mousedown(MouseDownEvent ev) {
	//TODO: Make actions draggable if not locked
	int slot = beltslot(ev.c);
	if(slot != -1) {
	    if(ev.b == 1) {
		act(slot, new MenuGrid.Interaction(1, ui.modflags()));
	    } else if(ev.b == 3) {
		ui.gui.wdgmsg("setbelt", slot, null);
	    }
	    return true;
	}
	return super.mousedown(ev);
    }
    
    @Override
    public void mousemove(MouseMoveEvent ev) {
	over = ev.c.isect(Coord.z, sz);
	super.mousemove(ev);
    }
    
    @Override
    public Object tooltip(Coord c, Widget prev) {
	int slot = beltslot(c);
	if(slot < 0) {return super.tooltip(c, prev);}
	GameUI.BeltSlot item = belt(slot);
	if(item == null) {return super.tooltip(c, prev);}
	if(last != item) {
	    if(ttip != null) {ttip.dispose();}
	    ttip = null;
	    try {
		MenuGrid.Pagina p = getpagina(item);
		if(p != null) {
		    ttip = ItemData.longtip(p, false);
		} else if(item instanceof GameUI.ResBeltSlot) {
		    Resource.Tooltip tt = ((GameUI.ResBeltSlot) item).getres().layer(Resource.tooltip);
		    if(tt != null) {
			ttip = Text.render(tt.t).tex();
		    }
		}
		last = item;
	    } catch (Loading ignored) {}
	}
	return ttip;
    }
    
    public boolean drop(Coord c, Coord ul) {
	int slot = beltslot(c);
	if(slot != -1) {
	    ui.gui.wdgmsg("setbelt", slot, 0);
	    return true;
	}
	return false;
    }
    
    public boolean iteminteract(Coord c, Coord ul) {return false;}
    
    public boolean dropthing(Coord c, Object thing) {
	int slot = beltslot(c);
	if(slot != -1) {
	    if(thing instanceof MenuGrid.Pagina) {
		MenuGrid.Pagina pag = (MenuGrid.Pagina) thing;
		if(setCustomPagina(ui.gui, pag, slot)) {
		    return true;
		}
		try {
		    if(pag.id instanceof Indir)
			ui.gui.wdgmsg("setbelt", slot, pag.res().name);
		    else
			ui.gui.wdgmsg("setbelt", slot, "pag", pag.id);
		} catch (Loading ignored) {
		}
		return (true);
	    }
	}
	return false;
    }
    
    /**@return true if this was custom pagina*/
    public static boolean setCustomPagina(GameUI gui, MenuGrid.Pagina pag, int slot) {
	if(CustomPagina.isLocalPagina(pag)) {
	    String resName = MenuGrid.Pagina.resname(pag);
	    if(!resName.isEmpty()) {
		gui.wdgmsg("setbelt", slot, "str", "pag:" + resName);
	    }
	    return true;
	}
	return false;
    }
    
    public static GameUI.BeltSlot makeCustom(GameUI gui, int slot, String data) {
	if(data.startsWith("pag:") && gui.menu != null) {
	    MenuGrid.Pagina pagina = gui.menu.findPagina(data.substring(4));
	    if(pagina == null) {
		return null;
	    }
	    return new GameUI.PagBeltSlot(slot, pagina);
	}
	return null;
    }
    
}
