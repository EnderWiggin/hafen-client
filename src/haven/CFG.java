package haven;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import haven.QualityList.SingleType;
import haven.rx.BuffToggles;
import me.ender.Reflect;

import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class CFG<T> {
    public static final CFG<String> VERSION = new CFG<>("version", "");
    public static final CFG<Boolean> DISPLAY_KINNAMES = new CFG<>("display.kinnames", true);
    public static final CFG<Boolean> DISPLAY_FLAVOR = new CFG<>("display.flavor", true);
    public static final CFG<Boolean> DISPLAY_GOB_INFO = new CFG<>("display.gob_info", false);
    public static final CFG<Boolean> DISPLAY_GOB_HITBOX = new CFG<>("display.gob_hitbox", false);
    public static final CFG<Boolean> DISPLAY_GOB_HITBOX_TOP = new CFG<>("display.gob_hitbox_top", false);
    public static final CFG<Boolean> HIDE_TREES = new CFG<>("display.hide_gobs", false);
    public static final CFG<Boolean> DISPLAY_FOD_CATEGORIES = new CFG<>("display.food_category", true);
    public static final CFG<Boolean> SHOW_GOB_RADIUS = new CFG<>("display.show_gob_radius", false);
    public static final CFG<Boolean> SIMPLE_CROPS = new CFG<>("display.simple_crops", false);
    public static final CFG<Boolean> STORE_MAP = new CFG<>("general.storemap", false);
    public static final CFG<Boolean> SHOW_TOOLBELT_0 = new CFG<>("general.toolbelt0", true);
    public static final CFG<Boolean> SHOW_TOOLBELT_1 = new CFG<>("general.toolbelt1", false);
    public static final CFG<Integer> AUTO_PICK_RADIUS = new CFG<>("general.auto_pick_radius", 55);

    public static final CFG<Boolean> ALT_COMBAT_UI = new CFG<>("ui.combat.alt_ui", true);
    public static final CFG<Boolean> SIMPLE_COMBAT_OPENINGS = new CFG<>("ui.combat.simple_openings", true);
    public static final CFG<Boolean> SHOW_COMBAT_DMG = new CFG<>("ui.combat.show_dmg", true);
    public static final CFG<Boolean> CLEAR_PLAYER_DMG_AFTER_COMBAT = new CFG<>("ui.combat.clear_player_damage_after", true);
    public static final CFG<Boolean> CLEAR_ALL_DMG_AFTER_COMBAT = new CFG<>("ui.combat.clear_all_damage_after", false);
    public static final CFG<Boolean> SHOW_COMBAT_KEYS = new CFG<>("ui.combat.show_keys", true);
    public static final CFG<Boolean> SHOW_CHAT_TIMESTAMP = new CFG<>("ui.chat.timestamp", true);
    public static final CFG<Boolean> STORE_CHAT_LOGS = new CFG<>("ui.chat.logs", false);
    public static final CFG<Boolean> LOCK_STUDY = new CFG<>("ui.lock_study", false);
    public static final CFG<Boolean> MMAP_LIST = new CFG<>("ui.mmap_list", true);
    public static final CFG<Boolean> MMAP_VIEW = new CFG<>("ui.mmap_view", false);
    public static final CFG<Boolean> MMAP_GRID = new CFG<>("ui.mmap_grid", false);
    public static final CFG<Boolean> MMAP_SHOW_BIOMES = new CFG<>("ui.mmap_biomes", true);
    public static final CFG<Boolean> MENU_SINGLE_CTRL_CLICK = new CFG<>("ui.menu_single_ctrl_click", true);
    public static final CFG<Boolean> MENU_ADD_PICK_ALL = new CFG<>("ui.menu_add_pick_all", false);

    public static final CFG<Boolean> REAL_TIME_CURIO = new CFG<>("ui.real_time_curio", false);
    public static final CFG<Boolean> SHOW_CURIO_LPH = new CFG<>("ui.show_curio_lph", false);
    public static final CFG<Boolean> SHOW_ITEM_DURABILITY = new CFG<>("ui.item_durability", false);
    public static final CFG<Boolean> SHOW_ITEM_WEAR_BAR = new CFG<>("ui.item_wear_bar", true);
    public static final CFG<Boolean> SHOW_ITEM_ARMOR = new CFG<>("ui.item_armor", false);
    public static final CFG<Boolean> SWAP_NUM_AND_Q = new CFG<>("ui.swap_num_and_q", false);
    public static final CFG<Boolean> PROGRESS_NUMBER = new CFG<>("ui.progress_number", false);
    public static final CFG<Boolean> FEP_METER = new CFG<>("ui.fep_meter", false);
    public static final CFG<Boolean> HUNGER_METER = new CFG<>("ui.hunger_meter", false);

    public static final CFG<Float> CAMERA_BRIGHT = new CFG<>("camera.bright", 0f);

    public static final CFG<Boolean> Q_SHOW_SINGLE = new CFG<>("ui.q.showsingle", true);
    public static final CFG<SingleType> Q_SINGLE_TYPE = new CFG<>("ui.q.singletype", SingleType.Average);
    public static final CFG<Boolean> Q_SHOW_SHIFT = new CFG<>("ui.q.showshift", true);
    public static final CFG<SingleType> Q_SHIFT_TYPE = new CFG<>("ui.q.shifttype", SingleType.All);
    public static final CFG<Boolean> Q_SHOW_ALT = new CFG<>("ui.q.showalt", true);
    public static final CFG<SingleType> Q_ALT_TYPE = new CFG<>("ui.q.alttype", SingleType.All);
    public static final CFG<Boolean> Q_SHOW_CTRL = new CFG<>("ui.q.showctrl", true);
    public static final CFG<SingleType> Q_CTRL_TYPE = new CFG<>("ui.q.ctrltype", SingleType.All);
    
    
    public static final CFG<Boolean> CAMERA_REVERSE_BAD_X = new CFG<>("camera.bad.reverse.x", false);
    public static final CFG<Boolean> CAMERA_REVERSE_BAD_Y = new CFG<>("camera.bad.reverse.y", false);

    private static final String CONFIG_JSON = "config.json";
    private static final Map<Object, Object> cfg;
    private static final Map<String, Object> cache = new HashMap<>();
    private static final Gson gson;
    private final String path;
    public final T def;
    private List<Observer<T>> observers = new LinkedList<>();

    static {
	gson = (new GsonBuilder()).setPrettyPrinting().create();
	Map<Object, Object> tmp = null;
	try {
	    Type type = new TypeToken<Map<Object, Object>>() {
	    }.getType();
	    tmp = gson.fromJson(Config.loadFile(CONFIG_JSON), type);
	} catch (Exception ignored) {
	}
	if(tmp == null) {
	    tmp = new HashMap<>();
	}
	cfg = tmp;

	BuffToggles.toggles.forEach(toggle -> toggle.cfg(
	    new CFG<Boolean>("display.buffs."+toggle.action, true),
	    new CFG<Boolean>("general.start_toggle."+toggle.action, false)
	));
    }

    public interface Observer<T> {
	void updated(CFG<T> cfg);
    }

    CFG(String path, T def) {
	this.path = path;
	this.def = def;
    }

    public T get() {
	return CFG.get(this);
    }

    public void set(T value) {
	CFG.set(this, value);
	observe();
    }

    public void set(T value, boolean observe) {
	set(value);
	if(observe) {observe();}
    }

    public void observe(Observer<T> observer) {
	this.observers.add(observer);
    }

    public void unobserve(Observer<T> observer) {
	this.observers.remove(observer);
    }

    private void observe() {
	for (Observer<T> observer : observers) {
	    observer.updated(this);
	}
    }

    @SuppressWarnings("unchecked")
    public static synchronized <E> E get(CFG<E> name) {
	E value = name.def;
	try {
	    if(cache.containsKey(name.path)) {
		return (E) cache.get(name.path);
	    } else {
		if(name.path != null) {
		    Object data = retrieve(name);
		    Class<?> defClass = name.def.getClass();
		    if(defClass.isAssignableFrom(data.getClass())) {
			value = (E) data;
		    } else if(Number.class.isAssignableFrom(defClass)) {
			Number n = (Number) data;
			value = (E) Utils.num2value(n, (Class<? extends Number>)defClass);
		    } else if(Enum.class.isAssignableFrom(defClass)) {
			Class<? extends Enum> enumType = Reflect.getEnumSuperclass(defClass);
			if(enumType != null) {
			    value = (E) Enum.valueOf(enumType, data.toString());
			}
		    }
		}
		cache.put(name.path, value);
	    }
	} catch (Exception ignored) {}
	return value;
    }

    @SuppressWarnings("unchecked")
    public static synchronized <E> void set(CFG<E> name, E value) {
	cache.put(name.path, value);
	if(name.path == null) {return;}
	String[] parts = name.path.split("\\.");
	int i;
	Object cur = cfg;
	for (i = 0; i < parts.length - 1; i++) {
	    String part = parts[i];
	    if(cur instanceof Map) {
		Map<Object, Object> map = (Map<Object, Object>) cur;
		if(map.containsKey(part)) {
		    cur = map.get(part);
		} else {
		    cur = new HashMap<String, Object>();
		    map.put(part, cur);
		}
	    }
	}
	if(cur instanceof Map) {
	    Map<Object, Object> map = (Map<Object, Object>) cur;
	    map.put(parts[parts.length - 1], value);
	}
	store();
    }

    private static synchronized void store() {
	Config.saveFile(CONFIG_JSON, gson.toJson(cfg));
    }

    private static Object retrieve(CFG name) {
	String[] parts = name.path.split("\\.");
	Object cur = cfg;
	for (String part : parts) {
	    if(cur instanceof Map) {
		Map map = (Map) cur;
		if(map.containsKey(part)) {
		    cur = map.get(part);
		} else {
		    return name.def;
		}
	    } else {
		return name.def;
	    }
	}
	return cur;
    }
}
