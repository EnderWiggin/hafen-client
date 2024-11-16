package haven;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import haven.rx.Reactor;
import rx.functions.Action2;

import java.awt.*;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.lang.reflect.Type;
import java.util.List;
import java.util.*;

import static haven.Action.*;
import static haven.WidgetList.*;

public class KeyBinder {
    private static final String CONFIG_JSON = "keybindings.json";
    
    public static final int NONE = 0;
    public static final int ALT = 1;
    public static final int CTRL = 2;
    public static final int SHIFT = 4;
    public static final int MODS = ALT | CTRL | SHIFT;
    public static final String COMBAT_KEYS_UPDATED = "KeyBinder.COMBAT_KEYS_UPDATED";
    
    private static final Gson gson;
    private static final Map<Action, KeyBind> binds;
    private static final List<Action> order;
    private static final KeyBind EMPTY = new KeyBind(0, 0, null);
    
    enum KeyBindType {GENERAL, COMBAT}
    
    static {
	gson = (new GsonBuilder()).setPrettyPrinting().create();
	String json = Config.loadFile(CONFIG_JSON);
	Map<Action, KeyBind> tmpGeneralCFG = null;
    
	try {
	    ConfigBean configBean = gson.fromJson(json, ConfigBean.class);
	    tmpGeneralCFG = configBean.general;
	    Fightsess.updateKeybinds(configBean.combat);
	} catch (Exception ignore) {}
    
	if(tmpGeneralCFG == null) {
	    try {
		Type type = new TypeToken<Map<Action, KeyBind>>() {
		}.getType();
		tmpGeneralCFG = gson.fromJson(json, type);
	    } catch (Exception ignored) {}
	}
    
	if(tmpGeneralCFG == null) {
	    tmpGeneralCFG = new HashMap<>();
	}
	binds = tmpGeneralCFG;
	binds.forEach((action, keyBind) -> keyBind.action = action);
	order = Arrays.asList(Action.values());
	defaults();
    }
    
    private static class ConfigBean {
	ConfigBean(Map<Action, KeyBind> general, KeyBind[] combat) {
	    this.general = general;
	    this.combat = combat;
	}
	
	final Map<Action, KeyBind> general;
	final KeyBind[] combat;
    }
    
    private static void defaults() {
	add(KeyEvent.VK_1, CTRL,  ACT_HAND_0);
	add(KeyEvent.VK_2, CTRL,  ACT_HAND_1);
	add(KeyEvent.VK_3, CTRL,  ACT_BELT);
	add(ACT_POUCH_0);
	add(ACT_POUCH_1);
    	add(KeyEvent.VK_D, ALT,   ACT_DRINK);
    	add(ACT_REFILL_DRINKS);
	add(KeyEvent.VK_C, ALT,   OPEN_QUICK_CRAFT);
	add(KeyEvent.VK_B, ALT,   OPEN_QUICK_BUILD);
	add(KeyEvent.VK_A, ALT,   OPEN_QUICK_ACTION);
	add(KeyEvent.VK_X, ALT,   OPEN_CRAFT_DB);
    	add(OPEN_QUEST_HELP);
	add(KeyEvent.VK_H, ALT,   TOGGLE_CURSOR);
	add(KeyEvent.VK_S, ALT,   TOGGLE_STUDY);
	add(KeyEvent.VK_F, ALT,   FILTER);
	add(KeyEvent.VK_I, ALT,   TOGGLE_GOB_INFO);
	add(KeyEvent.VK_H, CTRL,  TOGGLE_GOB_HITBOX);
	add(KeyEvent.VK_R, ALT,   TOGGLE_GOB_RADIUS);
	add(KeyEvent.VK_Z, CTRL,  TOGGLE_TILE_CENTERING);
	add(KeyEvent.VK_Q, ALT,   BOT_PICK_ALL_HERBS);
	add(KeyEvent.VK_W, ALT,   BOT_OPEN_GATE);
	add(KeyEvent.VK_Q, CTRL,  BOT_MOUNT_HORSE);
	add(AGGRO_ONE_PVE);
	add(AGGRO_ONE_PVP);
	add(AGGRO_ALL);
	
	//Camera controls
	add(KeyEvent.VK_ADD, NONE, CAM_ZOOM_IN);
	add(KeyEvent.VK_SUBTRACT, NONE, CAM_ZOOM_OUT);
	add(KeyEvent.VK_LEFT, NONE, CAM_ROTATE_LEFT);
	add(KeyEvent.VK_RIGHT, NONE, CAM_ROTATE_RIGHT);
	add(KeyEvent.VK_UP, NONE, CAM_ROTATE_UP);
	add(KeyEvent.VK_DOWN, NONE, CAM_ROTATE_DOWN);
	add(KeyEvent.VK_LEFT, CTRL, CAM_SNAP_WEST);
	add(KeyEvent.VK_RIGHT, CTRL, CAM_SNAP_EAST);
	add(KeyEvent.VK_UP, CTRL, CAM_SNAP_NORTH);
	add(KeyEvent.VK_DOWN, CTRL, CAM_SNAP_SOUTH);
	add(KeyEvent.VK_HOME, NONE, CAM_RESET);
	
	add(TOGGLE_HIDE_TREES);
	add(TOGGLE_INSPECT);
	add(TOGGLE_PEACE);
    }
    
    private static synchronized void store() {
	Config.saveFile(CONFIG_JSON, gson.toJson(new ConfigBean(binds, Fightsess.keybinds)));
    }
    
    public static boolean handle(UI ui, GlobKeyEvent e) {
	return get(e).execute(ui);
    }
    
    public static int getAWTModFlags(int modflags) {
	modflags = ((modflags & InputEvent.ALT_DOWN_MASK) != 0 ? ALT : 0)
	    | ((modflags & InputEvent.META_DOWN_MASK) != 0 ? ALT : 0)
	    | ((modflags & InputEvent.CTRL_DOWN_MASK) != 0 ? CTRL : 0)
	    | ((modflags & InputEvent.SHIFT_DOWN_MASK) != 0 ? SHIFT : 0);
	return modflags;
    }
    
    public static int getLoftarModFlags(int modflags) {
	modflags = ((modflags & KeyMatch.M) != 0 ? ALT : 0)
	    | ((modflags & KeyMatch.SUPER) != 0 ? ALT : 0) //not sure about this one
	    | ((modflags & KeyMatch.C) != 0 ? CTRL : 0)
	    | ((modflags & KeyMatch.S) != 0 ? SHIFT : 0);
	return modflags;
    }
    
    public static Action add(Action action, KeyBind bind) {
	if(!binds.containsKey(action)) {
	    binds.put(action, bind);
	}
	return action;
    }
    
    public static Action add(int code, int mods, Action action) {
	return add(action, new KeyBind(code, mods, action));
    }
    
    public static Action add(Action action) {
	return add(action, new KeyBind(0, 0, action));
    }
    
    public static KeyBind get(Action action) {
	return binds.get(action);
    }

    public static KeyBind get(final GlobKeyEvent e) {
	return binds.values().stream().filter(b -> b.match(e)).findFirst().orElse(EMPTY);
    }
    
    public static KeyBind make(KeyEvent e, Action action) {
	return new KeyBind(e.getKeyCode(), getAWTModFlags(e.getModifiersEx()), action);
    }
    
    private static boolean change(KeyBind to) {
	boolean conflicts = false;
	if(!to.isEmpty()) {
	    for(Map.Entry<Action, KeyBind> entry : binds.entrySet()) {
		KeyBind bind = entry.getValue();
		Action action = entry.getKey();
		if(to.action != action && to.code == bind.code && to.mods == bind.mods) {
		    binds.put(action, new KeyBind(0, 0, action));
		    conflicts = true;
		}
	    }
	}
	binds.put(to.action, to);
        store();
	return conflicts;
    }
    
    public static List<ShortcutWidget> makeWidgets(KeyBindType type) {
	switch (type) {
	    case GENERAL:
		return makeGeneralWidgets();
	    case COMBAT:
		return makeCombatWidgets();
	}
	throw new IllegalArgumentException(String.format("Unknown KeyBindType: %s", type));
    }
    
    private static List<ShortcutWidget> makeGeneralWidgets() {
	final List<ShortcutWidget> list = new ArrayList<>(binds.size());
	for (Action action : order) {
	    if(binds.containsKey(action)) {
		list.add(new ShortcutWidget(binds.get(action), (from, to) -> {
		    if(change(to)) {
			list.forEach(wdg -> wdg.update(get(wdg.keyBind.action)));
		    }
		}));
	    }
	}
	return list;
    }
    
    private static List<ShortcutWidget> makeCombatWidgets() {
	final List<ShortcutWidget> list = new ArrayList<>(Fightsess.keybinds.length);
	for (int k = 0; k < Fightsess.keybinds.length; k++) {
	    list.add(new ShortcutWidget(Fightsess.keybinds[k], (from, to) -> {
		if(to.equals(from)) {return;}
		for (int i = 0; i < Fightsess.keybinds.length; i++) {
		    if(Fightsess.keybinds[i].equals(from)) {
			Fightsess.keybinds[i] = to;
		    } else if(Fightsess.keybinds[i].equals(to)) {
			Fightsess.keybinds[i] = new KeyBind(0, 0);
			list.get(i).update(Fightsess.keybinds[i]);
		    }
		}
		Reactor.event(COMBAT_KEYS_UPDATED);
		store();
	    }, String.format("Action %02d", k + 1)));
	}
	return list;
    }
    
    public static class KeyBind {
	private final int code;
	private final int mods;
	transient private Action action;
    
	public KeyBind(int code, int mods) {
	    this(code, mods, null);
	}
	
	public KeyBind(int code, int mods, Action action) {
	    this.code = code;
	    this.mods = mods;
	    this.action = action;
	}
    
	public boolean match(KeyEvent e) {
	    return match(e.getKeyCode(), getAWTModFlags(e.getModifiersEx()), 0);
	}
	
	public boolean match(KbdEvent e) {
	    return match(e, 0);
	}
	
	public boolean match(KbdEvent e, int modign) {
	    return match(e.code, getLoftarModFlags(e.mods), modign);
	}
	
	public boolean match(int code, int mods, int modign) {
	    return !isEmpty() && code == this.code && ((mods & MODS & ~modign) == (this.mods & ~modign));
	}

	public boolean execute(UI ui) {
	    boolean canRun = ui.gui != null && action != null;
	    if(canRun) { action.run(ui.gui); }
	    return canRun;
	}
	
	public String shortcut() {
	    return shortcut(false);
	}
    
	public String shortcut(boolean shortened) {
	    if(isEmpty()) {return shortened ? "" : "<UNBOUND>";}
	    String key = KeyEvent.getKeyText(code);
	    if ((mods & SHIFT) != 0) {
		key = (shortened ? "⇧" : "SHIFT+") + key;
	    }
	    if ((mods & ALT) != 0) {
		key = (shortened ? "A" : "ALT+") + key;
	    }
	    if ((mods & CTRL) != 0) {
		key = (shortened ? "C" : "CTRL+") + key;
	    }
	    return key;
	}
    
	public boolean isEmpty() {
	    return code == 0 && mods == 0;
	}
    
	@Override
	public boolean equals(Object obj) {
	    if(obj instanceof KeyBind) {
		KeyBind other = (KeyBind) obj;
		return action == other.action && code == other.code && mods == other.mods;
	    }
	    return super.equals(obj);
	}
 
	@Override
	public String toString() {
	    return shortcut();
	}
    }
    
    public static class KeyBindTip implements Indir<Tex> {
	private final String text;
	private final Action action;
	private KeyBind bind;
	private Tex rendered = null;
	
	public KeyBindTip(String text, Action action) {
	    this.text = text;
	    this.action = action;
	    this.bind = KeyBinder.get(action);
	}
	
	@Override
	public Tex get() {
	    KeyBinder.KeyBind bind = KeyBinder.get(action);
	    if(this.bind != bind) {
		this.bind = bind;
		if(rendered != null) {
		    rendered.dispose();
		    rendered = null;
		}
	    }
	    if(rendered == null) {
		String tt = text;
		if(this.bind != null && !this.bind.isEmpty()) {
		    tt = String.format("%s ($col[255,255,0]{%s})", text, this.bind.shortcut());
		}
		rendered = RichText.render(tt, 0).tex();
	    }
	    return rendered;
	}
    }
    
    public static class ShortcutWidget extends Widget implements ShortcutSelectorWdg.Result {
    
	private final Button btn;
	private KeyBind keyBind;
	private final Action2<KeyBind, KeyBind> update;
    
	public ShortcutWidget(KeyBind bind, Action2<KeyBind, KeyBind> update) {
	    this(bind, update, bind.action != null ? bind.action.name : "<EMPTY ACTION>");
	}
 
	public ShortcutWidget(KeyBind bind, Action2<KeyBind, KeyBind> update, String label) {
	    btn = add(new Button(UI.scale(75), bind.shortcut()) {
		  @Override
		  protected boolean i10n() { return false; }
	    
		  @Override
		  public void click() {
		      ui.root.add(new ShortcutSelectorWdg(keyBind, ShortcutWidget.this), ui.mc.sub(50, 20));
		  }
    
		  @Override
		  public boolean mouseup(MouseUpEvent ev) {
		      //FIXME: a little hack, because WidgetList does not pass correct click coordinates if scrolled
		      return super.mouseup(new MouseUpEvent(ev, Coord.z));
		  }
	      },
	    UI.scale(225), 0);
	    this.keyBind = bind;
	    this.update = update;
	    if(bind.action != null && bind.action.description != null) {
		tooltip = RichText.render(bind.action.description, UI.scale(200));
	    }
	    btn.autosize(true);
	    btn.c.x = UI.scale(300) - btn.sz.x;
	    sz = UI.scale(300, 24);
	    add(new Label(label), UI.scale(5, 5));
	}
    
	@Override
	public void keyBindChanged(KeyBind from, KeyBind to) {
	    update.call(from, to);
	    update(to);
	}
    
	public void update(KeyBind to) {
	    keyBind = to;
	    btn.change(keyBind.shortcut());
	    btn.c.x = UI.scale(300) - btn.sz.x;
	}
    }
    
    private static class ShortcutSelectorWdg extends Widget {
	private static final Color BGCOLOR = new Color(32, 64, 32, 196);
	private static final Coord PAD = new Coord(5, 5);
	private final KeyBind bind;
	private final Result listener;
	private final Tex label;
    
	private UI.Grab keygrab;
	private UI.Grab mousegrab;
	
	public ShortcutSelectorWdg(KeyBind bind, Result listener) {
	    this.bind = bind;
	    this.listener = listener;
	    label = RichText.render("Press any key...\nOr DELETE to unbind", 0).tex();
	    sz = label.sz().add(PAD.mul(2));
	}
    
	@Override
	public boolean keydown(KeyDownEvent ev) {
	    int code = ev.code;
	    if(    code != 0
		&& code != KeyEvent.VK_CONTROL
		&& code != KeyEvent.VK_SHIFT
		&& code != KeyEvent.VK_ALT
		&& code != KeyEvent.VK_META) {
		if(code == KeyEvent.VK_DELETE) {
		    listener.keyBindChanged(bind, new KeyBind(0, 0, bind.action));
		} else if(code != KeyEvent.VK_ESCAPE) {
		    listener.keyBindChanged(bind, make(ev.awt, bind.action));
		}
		reqdestroy();
	    }
	    return true;
	}
    
	@Override
	protected void attach(UI ui) {
	    super.attach(ui);
	    keygrab = ui.grabkeys(this);
	    mousegrab = ui.grabmouse(this);
	}
	
	@Override
	public boolean mousedown(MouseDownEvent ev) {
	    reqdestroy();
	    return true;
	}
	
	public void reqdestroy() {
	    mousegrab.remove();
	    keygrab.remove();
	    super.reqdestroy();
	}
	
	@Override
	public void draw(GOut g) {
	    g.chcolor(BGCOLOR);
	    g.frect(Coord.z, sz);
	    g.chcolor();
	    BOX.draw(g, Coord.z, sz);
	    g.image(label, PAD);
	    super.draw(g);
	}
	
	public interface Result {
	    void keyBindChanged(KeyBind from, KeyBind to);
	}
    }
}
