package haven;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import haven.PathVisualizer.PathCategory;
import haven.rx.BuffToggles;
import me.ender.ClientUtils;
import me.ender.GobInfoOpts;
import me.ender.Reflect;

import java.awt.*;
import java.lang.reflect.Type;
import java.util.*;
import java.util.List;

public class CFG<T> {
    public static final CFG<String> VERSION = new CFG<>("version", "");
    public static final CFG<Boolean> VIDEO_FULL_SCREEN = new CFG<>("video.full_screen", false);
//    public static final CFG<Boolean> DISPLAY_KINNAMES = new CFG<>("display.kinnames", true);
    public static final CFG<Boolean> DISPLAY_KINSFX = new CFG<>("display.kinsfx", true);
    public static final CFG<Boolean> DISPLAY_FLAVOR = new CFG<>("display.flavor", true);
    public static final CFG<Boolean> DISPLAY_GOB_INFO = new CFG<>("display.gob_info", false);
    public static final CFG<Set<GobInfoOpts.InfoPart>> DISPLAY_GOB_INFO_DISABLED_PARTS = new CFG<>("display.gob_info_disabled_parts", new HashSet<>(), new TypeToken<Set<GobInfoOpts.InfoPart>>(){});
    public static final CFG<Set<GobInfoOpts.TreeSubPart>> DISPLAY_GOB_INFO_TREE_ENABLED_PARTS = new CFG<>("display.gob_info_tree_enabled_parts", new HashSet<>(Arrays.asList(GobInfoOpts.TreeSubPart.SEEDS, GobInfoOpts.TreeSubPart.LEAVES)), new TypeToken<Set<GobInfoOpts.TreeSubPart>>(){});
    public static final CFG<Boolean> DISPLAY_GOB_INFO_TREE_HIDE_GROWING_PARTS = new CFG<>("display.gob_info_tree_hide_growing_parts", true);
    public static final CFG<Boolean> DISPLAY_GOB_INFO_TREE_SHOW_BIG = new CFG<>("display.gob_info_tree_show_big", true);
    public static final CFG<Integer> DISPLAY_GOB_INFO_TREE_SHOW_BIG_THRESHOLD = new CFG<>("display.gob_info_show_big_threshold", 150);
    public static final CFG<Boolean> DISPLAY_GOB_INFO_SHORT = new CFG<>("display.gob_info_shorten_content", false);
    public static final CFG<Boolean> DISPLAY_GOB_HITBOX_FILLED = new CFG<>("display.gob_hitbox_filled", false);
    public static final CFG<Boolean> DISPLAY_GOB_HITBOX = new CFG<>("display.gob_hitbox", false);
    public static final CFG<Boolean> DISPLAY_GOB_HITBOX_TOP = new CFG<>("display.gob_hitbox_top", false);
    public static final CFG<Boolean> DISPLAY_GOB_PATHS = new CFG<>("display.gob_paths.show", false);
    public static final CFG<Set<PathCategory>> DISPLAY_GOB_PATHS_FOR = new CFG<>("display.gob_paths.categories", PathVisualizer.DEF_CATEGORIES, new TypeToken<Set<PathCategory>>(){});
    public static final CFG<Boolean> QUEUE_PATHS = new CFG<>("general.queue_path", false);
    public static final CFG<Boolean> HIDE_TREES = new CFG<>("display.hide_gobs", false);
    public static final CFG<Boolean> SKIP_HIDING_RADAR_TREES = new CFG<>("display.skip_hide_radar_gobs", false);
    public static final CFG<Boolean> DISPLAY_FOOD_CATEGORIES = new CFG<>("display.food_category", true);
    public static final CFG<Boolean> UI_INSTANT_LONG_TIPS = new CFG<>("ui.instant_long_tips", false);
    public static final CFG<Boolean> SHOW_GOB_RADIUS = new CFG<>("display.show_gob_radius", false);
    public static final CFG<Boolean> SHOW_MINESWEEPER_OVERLAY = new CFG<>("display.minesweeper_overlay", false);
    public static final CFG<Boolean> SHOW_MINE_SUPPORT_AS_OVERLAY = new CFG<>("display.mine_support_overlay", true);
    public static final CFG<Boolean> SHOW_CONTAINER_FULLNESS = new CFG<>("display.container_status", false);
    public static final CFG<Boolean> SHOW_PROGRESS_COLOR = new CFG<>("display.progress_coloring", false);
    public static final CFG<Boolean> SIMPLE_CROPS = new CFG<>("display.simple_crops", false);
    public static final CFG<Boolean> NO_TILE_TRANSITION = new CFG<>("display.no_tile_transition", false);
    public static final CFG<Boolean> FLAT_TERRAIN = new CFG<>("display.flat_terrain", false);
    public static final CFG<Boolean> DISPLAY_RIDGE_BOX = new CFG<>("display.ridge_box", false);
    public static final CFG<Boolean> COLORIZE_DEEP_WATER = new CFG<>("display.colored_deep_water", false);
    public static final CFG<Integer> DISPLAY_SCALE_CUPBOARDS = new CFG<>("display.scale.cupboards", 100);
    public static final CFG<Integer> DISPLAY_SCALE_WALLS = new CFG<>("display.scale.walls", 100);
    public static final CFG<Boolean> DISPLAY_DECALS_ON_TOP = new CFG<>("display.decals_on_top", false);
    public static final CFG<Boolean> DISPLAY_NO_MAT_CUPBOARDS = new CFG<>("display.no_mat.cupboards", false);
    public static final CFG<Boolean> DISPLAY_AURA_SPEED_BUFF = new CFG<>("display.aura.speed", false);
    public static final CFG<Boolean> DISPLAY_AURA_RABBIT = new CFG<>("display.aura.rabbit", false);
    public static final CFG<Boolean> DISPLAY_AURA_CRITTERS = new CFG<>("display.aura.critters", false);
    public static final CFG<Boolean> STORE_MAP = new CFG<>("general.storemap", false);
    public static final CFG<Boolean> SHOW_TOOLBELT_0 = new CFG<>("general.toolbelt0", true);
    public static final CFG<Boolean> SHOW_TOOLBELT_1 = new CFG<>("general.toolbelt1", false);
    public static final CFG<Boolean> ITEM_DROP_PROTECTION = new CFG<>("general.item_drop_protection", false);
    public static final CFG<Boolean> DECAL_SHIFT_PICKUP = new CFG<>("general.decal_shift_pickup", false);
    public static final CFG<Boolean> AUTO_PICK_ONLY_RADAR = new CFG<>("general.auto_pick_radar", true);
    public static final CFG<Integer> AUTO_PICK_RADIUS = new CFG<>("general.auto_pick_radius", 55);
    public static final CFG<Boolean> AUTO_DROP_RESPECT_FILTER = new CFG<>("general.auto_drop.filter", true);
    public static final CFG<Boolean> AUTO_DROP_PARASITES = new CFG<>("general.auto_drop_parasites", false);
    public static final CFG<Boolean> PRESERVE_SYMBEL = new CFG<>("general.preserve_symbel", false);
    
    public static final CFG<Theme> THEME = new CFG<>("ui.theme", Theme.Pretty);
    public static final CFG<Boolean> FORCE_HW_CURSOR = new CFG<>("ui.force_hw_cursor", false);
    public static final CFG<Boolean> DISABLE_UI_HIDING = new CFG<>("ui.disable_ui_hide", true);
    public static final CFG<Boolean> UI_DISABLE_CONTAINER_POS = new CFG<>("ui.disable_container_pos", false);
    public static final CFG<Boolean> UI_SHOW_EQPROXY_HAND = new CFG<>("ui.eq_proxy.hands", true);
    public static final CFG<Boolean> UI_SHOW_EQPROXY_POUCH = new CFG<>("ui.eq_proxy.pouch", false);
    public static final CFG<Boolean> ALT_COMBAT_UI = new CFG<>("ui.combat.alt_ui", true);
    public static final CFG<Boolean> SIMPLE_COMBAT_OPENINGS = new CFG<>("ui.combat.simple_openings", true);
    public static final CFG<Boolean> ALWAYS_MARK_COMBAT_TARGET = new CFG<>("ui.combat.always_mark_target", false);
    public static final CFG<Boolean> HIGHLIGHT_PARTY_IN_COMBAT = new CFG<>("ui.combat.highlight_party_in_combat", false);
    public static final CFG<Boolean> HIGHLIGHT_SELF_IN_COMBAT = new CFG<>("ui.combat.highlight_self_in_combat", false);
    public static final CFG<Boolean> HIGHLIGHT_ENEMY_IN_COMBAT = new CFG<>("ui.combat.highlight_enemies_in_combat", false);
    public static final CFG<Boolean> MARK_PARTY_IN_COMBAT = new CFG<>("ui.combat.mark_party_in_combat", false);
    public static final CFG<Boolean> MARK_SELF_IN_COMBAT = new CFG<>("ui.combat.mark_self_in_combat", false);
    public static final CFG<Boolean> MARK_ENEMY_IN_COMBAT = new CFG<>("ui.combat.mark_enemies_in_combat", false);
    public static final CFG<Boolean> SHOW_COMBAT_INFO = new CFG<>("ui.combat.show_info", false);
    public static final CFG<Integer> SHOW_COMBAT_INFO_HEIGHT = new CFG<>("ui.combat.show_info_height", 1);
    public static final CFG<Boolean> SHOW_FLOATING_STAT_WDGS = new CFG<>("ui.combat.show_floating_stat_wdgs", false);
    public static final CFG<Boolean> SHOW_FLOATING_STATS_COMBAT = new CFG<>("ui.combat.show_floating_stat_wdgs_combat", true);
    public static final CFG<Boolean> LOCK_FLOATING_STAT_WDGS = new CFG<>("ui.combat.lock_floating_stat_wdgs", false);
    public static final CFG<Boolean> DRAG_COMBAT_UI = new CFG<>("ui.combat.drag_combat_ui", false);
    public static final CFG<Boolean> SHOW_COMBAT_DMG = new CFG<>("ui.combat.show_dmg", true);
    public static final CFG<Boolean> CLEAR_PLAYER_DMG_AFTER_COMBAT = new CFG<>("ui.combat.clear_player_damage_after", true);
    public static final CFG<Boolean> CLEAR_ALL_DMG_AFTER_COMBAT = new CFG<>("ui.combat.clear_all_damage_after", false);
    public static final CFG<Boolean> SHOW_COMBAT_KEYS = new CFG<>("ui.combat.show_keys", true);
    public static final CFG<Boolean> COMBAT_AUTO_PEACE = new CFG<>("ui.combat.peace", false);
    public static final CFG<Boolean> COMBAT_RE_AGGRO = new CFG<>("ui.combat.reaggro", false);
    public static final CFG<Boolean> SHOW_CHAT_TIMESTAMP = new CFG<>("ui.chat.timestamp", true);
    public static final CFG<Boolean> STORE_CHAT_LOGS = new CFG<>("ui.chat.logs", false);
    public static final CFG<Boolean> LOCK_STUDY = new CFG<>("ui.lock_study", false);
    public static final CFG<Boolean> MMAP_LIST = new CFG<>("ui.mmap_list", true);
    public static final CFG<Boolean> MMAP_VIEW = new CFG<>("ui.mmap_view", false);
    public static final CFG<Boolean> MMAP_GRID = new CFG<>("ui.mmap_grid", false);
    public static final CFG<Boolean> MMAP_POINTER = new CFG<>("ui.mmap_pointer", false);
    public static final CFG<Boolean> MMAP_SHOW_BIOMES = new CFG<>("ui.mmap_biomes", true);
    public static final CFG<Boolean> MMAP_SHOW_PATH = new CFG<>("ui.mmap_path", false);
    public static final CFG<Boolean> MMAP_SHOW_MARKER_NAMES = new CFG<>("ui.mmap_mnames", false);
    public static final CFG<Boolean> MMAP_SHOW_PARTY_NAMES = new CFG<>("ui.mmap_party_names", false);
    public static final CFG<Integer> MMAP_SHOW_PARTY_NAMES_STYLE = new CFG<>("ui.mmap_party_names_style", 0);
    public static final CFG<Boolean> MENU_SINGLE_CTRL_CLICK = new CFG<>("ui.menu_single_ctrl_click", true);
    public static final CFG<UI.KeyMod> MENU_SKIP_AUTO_CHOOSE = new CFG<>("ui.menu_skip_auto_choose", UI.KeyMod.SHIFT);
    public static final CFG<Boolean> MENU_ADD_PICK_ALL = new CFG<>("ui.menu_add_pick_all", false);
    
    public static final CFG<Map<String, Map<String, Boolean>>> WARN_CONFIG = new CFG<>("general.warning", new HashMap<>());
    public static final CFG<Boolean> REAL_TIME_CURIO = new CFG<>("ui.real_time_curio", false);
    public static final CFG<Boolean> SHOW_CURIO_LPH = new CFG<>("ui.show_curio_lph", false);
    public static final CFG<Boolean> SHOW_CURIO_REMAINING_TT = new CFG<>("ui.show_curio_remaining_tt", true);
    public static final CFG<Boolean> SHOW_CURIO_REMAINING_METER = new CFG<>("ui.show_curio_remaining_meter", false);
    public static final CFG<Boolean> IMPROVE_DAMAGE_TIP = new CFG<>("ui.improve_damage_tip", false);
    public static final CFG<Boolean> SHOW_ITEM_DURABILITY = new CFG<>("ui.item_durability", false);
    public static final CFG<Boolean> SHOW_ITEM_WEAR_BAR = new CFG<>("ui.item_wear_bar", true);
    public static final CFG<Boolean> HIGHLIGHT_BROKEN_ITEMS = new CFG<>("ui.highlight_broken_items", true);
    public static final CFG<Boolean> SHOW_ITEM_ARMOR = new CFG<>("ui.item_armor", false);
    public static final CFG<Boolean> SWAP_NUM_AND_Q = new CFG<>("ui.swap_num_and_q", false);
    public static final CFG<Boolean> PROGRESS_NUMBER = new CFG<>("ui.progress_number", false);
    public static final CFG<Boolean> FEP_METER = new CFG<>("ui.fep_meter", false);
    public static final CFG<Boolean> HUNGER_METER = new CFG<>("ui.hunger_meter", false);
    public static final CFG<Boolean> DRINKS_METER = new CFG<>("ui.drinks_meter", false);
    public static final CFG<Boolean> SHOW_BOT_MESSAGES = new CFG<>("ui.hide_bot_messages", true);
    
    //Color settings
    public static final CFG<Color> COLOR_MINE_SUPPORT_OVERLAY = new CFG<>("colors.mine_support_overlay", new Color(149, 246, 194));
    public static final CFG<Color> COLOR_MINE_SUPPORT_DAMAGED_OVERLAY = new CFG<>("colors.damaged_mine_support_overlay", new Color(253, 44, 70));
    public static final CFG<Color> COLOR_TILE_GRID = new CFG<>("colors.tile_grid", new Color(255, 255, 255, 48));
    public static final CFG<Color> COLOR_HBOX_FILLED = new CFG<>("colors.hit_box_filled", new Color(178, 71, 178, 160));
    public static final CFG<Color> COLOR_HBOX_SOLID = new CFG<>("colors.hit_box_solid", new Color(178, 71, 178, 255));
    public static final CFG<Color> COLOR_HBOX_PASSABLE = new CFG<>("colors.hit_box_passable", new Color(105, 207, 124, 255));
    public static final CFG<Color> COLOR_RIDGE_BOX = new CFG<>("colors.ridge_box", new Color(200, 0, 0, 128));
    
    public static final CFG<Color> COLOR_GOB_READY = new CFG<>("colors.gob.ready", new Color(16, 255, 16, 128));
    public static final CFG<Color> COLOR_GOB_FULL = new CFG<>("colors.gob.full", new Color(215, 63, 250, 64));
    public static final CFG<Color> COLOR_GOB_EMPTY = new CFG<>("colors.gob.empty", new Color(104, 213, 253, 64));
    public static final CFG<Color> COLOR_GOB_PARTY = new CFG<>("colors.gob.party", new Color(16, 255, 16, 200));
    public static final CFG<Color> COLOR_GOB_LEADER = new CFG<>("colors.gob.leader", new Color(16, 64, 255, 200));
    public static final CFG<Color> COLOR_GOB_SELF = new CFG<>("colors.gob.self", new Color(2, 253, 177, 200));
    public static final CFG<Color> COLOR_GOB_IN_COMBAT = new CFG<>("colors.gob.in_combat", new Color(246, 86, 153, 200));
    public static final CFG<Color> COLOR_GOB_COMBAT_TARGET = new CFG<>("colors.gob.combat_target", new Color(255, 0, 0, 200));
    public static final CFG<Color> COLOR_GOB_RABBIT = new CFG<>("colors.gob.rabbit", new Color(0, 255, 119, 140));
    public static final CFG<Color> COLOR_GOB_CRITTERS = new CFG<>("colors.gob.critters", new Color(150, 230, 255, 140));
    public static final CFG<Color> COLOR_GOB_SPEED_BUFF = new CFG<>("colors.gob.speed_buff", new Color(200, 255, 230, 140));
    
    /**Show stack's hover inventory widgets only if SHIFT is pressed*/
    public static final CFG<Boolean> UI_STACK_SUB_INV_ON_SHIFT = new CFG<>("ui.stack.sub_inv_on_shift", false);
    /**Unpack stacks into single items for extra inventory's list*/
    public static final CFG<Integer> UI_EXT_INV_MIN_ROWS = new CFG<>("ui.stack.ext_inv_min_rows", 3);
    public static final CFG<Boolean> UI_STACK_EXT_INV_UNPACK = new CFG<>("ui.stack.ext_inv_unpack", true);

    public static final CFG<Float> CAMERA_BRIGHT = new CFG<>("camera.bright", 0f);
    public static final CFG<Boolean> CAMERA_INVERT_X = new CFG<>("camera.invert_x", false);
    public static final CFG<Boolean> CAMERA_INVERT_Y = new CFG<>("camera.invert_y", false);
    
    public static final CFG<Boolean> Q_SHOW_SINGLE = new CFG<>("ui.q.showsingle", true);
    
    public static final CFG<Boolean> AUTOMAP_UPLOAD = new CFG<>("automap.upload", false);
    public static final CFG<Boolean> AUTOMAP_TRACK = new CFG<>("automap.track", false);
    public static final CFG<Set<BuddyWnd.Group>> AUTOMAP_MARKERS = new CFG<>("automap.markers", new HashSet<>(), new TypeToken<Set<BuddyWnd.Group>>(){});
    public static final CFG<String> AUTOMAP_ENDPOINT = new CFG<>("automap.andpoint", "");

    public static final CFG<Boolean> ALWAYS_SHOW_DEWY_TIME = new CFG<>("addstg.always_show_dewy_time", false);
    public static final CFG<Boolean> SHOW_TIME = new CFG<>("addstg.show_time", false);
    public static final CFG<Boolean> SHOW_STATS = new CFG<>("addstg.show_stats", false);

    public static final CFG<Boolean> QUESTHELPER_HIGHLIGHT_QUESTGIVERS = new CFG<>("questhelper.highlight_questgivers", true);
    public static final CFG<Boolean> QUESTHELPER_SHOW_TASKS_IN_TOOLTIP = new CFG<>("questhelper.show_tasks_in_tooltip", true);

    private static final String CONFIG_JSON = "config.json";
    private static final Map<Object, Object> cfg;
    private static final Map<String, Object> cache = new HashMap<>();
    public static final Gson gson;
    private final String path;
    public final T def;
    private final Type t;
    private final List<Observer<T>> observers = new LinkedList<>();

    static {
	gson = new GsonBuilder().setPrettyPrinting()
	    .registerTypeAdapter(Color.class, new ClientUtils.ColorSerializer())
	    .create();
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
	    new CFG<>("display.buffs." + toggle.action, true),
	    new CFG<>("general.start_toggle." + toggle.action, false)
	));
    }

    public interface Observer<T> {
	void updated(CFG<T> cfg);
    }

    CFG(String path, T def, TypeToken<T> t) {
	this.path = path;
	this.def = def;
	this.t = t == null ? null : t.getType();
    }
    
    CFG(String path, T def) {
	this(path, def, null);
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

    public Disposable observe(Observer<T> observer) {
	this.observers.add(observer);
	return new ObserverHolder<>(this, observer);
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
		    } else if(name.t != null) {
			value = gson.fromJson(gson.toJson(data), name.t);
		    } else if(Map.class.isAssignableFrom(defClass) && Map.class.isAssignableFrom(data.getClass())) {
			value = (E) data;
		    } else if(Number.class.isAssignableFrom(defClass)) {
			Number n = (Number) data;
			value = (E) ClientUtils.num2value(n, (Class<? extends Number>)defClass);
		    } else if(Color.class.isAssignableFrom(defClass)) {
			String hex = data instanceof String ? (String) data : null;
			Color def = (Color) name.def;
			value = (E) ClientUtils.hex2color(hex, def);
		    } else if(Enum.class.isAssignableFrom(defClass)) {
			@SuppressWarnings("rawtypes") Class<? extends Enum> enumType = Reflect.getEnumSuperclass(defClass);
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

    @SuppressWarnings("rawtypes")
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
    
    private static class ObserverHolder<T> implements Disposable {
	private CFG<T> cfg;
	private Observer<T> observer;
	
	private ObserverHolder(CFG<T> cfg, Observer<T> observer) {
	    this.cfg = cfg;
	    this.observer = observer;
	}
	
	@Override
	public void dispose() {
	    if(cfg != null) {
		cfg.unobserve(observer);
		cfg = null;
		observer = null;
	    }
	}
    }
}
