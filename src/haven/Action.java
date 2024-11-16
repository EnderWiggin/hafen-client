package haven;

import auto.Actions;
import me.ender.CustomCursors;
import me.ender.GobInfoOpts;

public enum Action {
    TOGGLE_TIMERS(GameUI::toggleTimers, "Toggle Timers"),
    ACT_HAND_0(gui -> gui.eqproxyHandBelt.activate(Equipory.SLOTS.HAND_LEFT, 1), "Left hand", "Left click on left hand slot."),
    ACT_HAND_1(gui -> gui.eqproxyHandBelt.activate(Equipory.SLOTS.HAND_RIGHT, 1), "Right hand", "Left click on right hand slot."),
    ACT_BELT(gui -> gui.eqproxyHandBelt.activate(Equipory.SLOTS.BELT, 3), "Belt", "Right click on belt slot."),
    ACT_POUCH_0(gui -> gui.eqproxyPouchBack.activate(Equipory.SLOTS.POUCH_LEFT, 3), "Left pouch", "Right click on pouch slot."),
    ACT_POUCH_1(gui -> gui.eqproxyPouchBack.activate(Equipory.SLOTS.POUCH_RIGHT, 3), "Right pouch", "Right click on pouch slot."),
    ACT_DRINK(Actions::drink, "Drink", "Drinks water."),
    ACT_REFILL_DRINKS(Actions::refillDrinks, "Refill drinks", "Refills all water skins, flasks and jugs from nearby barrel or water tile."),
    OPEN_QUICK_CRAFT(GameUI::toggleCraftList, "Open craft list", "Opens list of items you can craft. Start typing to narrow the list. Press Enter or double-click to select recipe."),
    OPEN_QUICK_BUILD(GameUI::toggleBuildList, "Open building list", "Opens list of objects you can build. Start typing to narrow the list. Press Enter or double-click to select building."),
    OPEN_QUICK_ACTION(GameUI::toggleActList, "Open actions list", "Opens list of actions you can perform. Start typing to narrow the list. Press Enter or double-click to perform action."),
    OPEN_CRAFT_DB(GameUI::toggleCraftDB, "Open crafting DB"),
    OPEN_QUEST_HELP(GameUI::toggleQuestHelper, "Open quest helper", "Opens window with uncompleted tasks for all active quests."),
    TOGGLE_CURSOR(GameUI::toggleHand, "Toggle cursor item", "Hide/show item on a cursor. Allows you to walk with item on cursor when hidden."),
    TOGGLE_STUDY(GameUI::toggleStudy, "Toggle study window"),
    FILTER(GameUI::toggleFilter, "Show item filter"),
    TOGGLE_GOB_INFO(CFG.DISPLAY_GOB_INFO, "Display info", "Display crop/tree growth and object health overlay."),
    TOGGLE_GOB_HITBOX(Hitbox::toggle, "Display hitboxes"),
    TOGGLE_HIDE_TREES(CFG.HIDE_TREES, "Hide trees"),
    TOGGLE_GOB_RADIUS(CFG.SHOW_GOB_RADIUS, "Display radius", "Displays effective radius of beehives/mine supports etc."),
    TOGGLE_TILE_CENTERING(gui ->
    {
	Config.center_tile = !Config.center_tile;
	gui.ui.message(String.format("Tile centering turned %s", Config.center_tile ? "ON" : "OFF"), GameUI.MsgType.INFO);
    }, "Toggle tile centering"),
    TOGGLE_INSPECT(gui -> CustomCursors.toggleInspectMode(gui.map), "Toggle inspect mode"),
    TRACK_OBJECT(gui -> CustomCursors.toggleTrackingMode(gui.map), "Track object"),
    BOT_PICK_ALL_HERBS(Actions::pickup, "Auto-pick stuff", "Will automatically pickup all herbs/mussels/clay/frogs/grasshoppers etc. in radius that can be changed in Options->General."),
    BOT_MOUNT_HORSE(Actions::mountClosestHorse, "Mount nearest domestic horse", "Whistle at a closest domestic horse and mount it once it is close enough. If it is very close - mount without whistling."),
    BOT_OPEN_GATE(Actions::openGate, "Toggle closest gate", "Will right click on closest gate in 3 tile radius."),
    TOGGLE_PEACE(GameUI::togglePeace, "Toggle Peace", "Toggle peace for current target"),
    AGGRO_ONE_PVE(Actions::aggroOnePVE, "Aggro closest non-player to cursor", "Will try to aggro (or switch target to) one non-player target closest to cursor"),
    AGGRO_ONE_PVP(Actions::aggroOnePVP, "Aggro closest player to cursor", "Will try to aggro (or switch target to) one player closest to cursor"),
    AGGRO_ALL(Actions::aggroAll, "Aggro all creatures near player", "Will try to aggro all creatures near player that are not in party"),
    
    //Camera controls
    CAM_ZOOM_IN(gui -> gui.map.zoomCamera(-1), "Camera zoom in"),
    CAM_ZOOM_OUT(gui -> gui.map.zoomCamera(1), "Camera zoom out"),
    CAM_ROTATE_LEFT(gui -> gui.map.rotateCamera(Coord.left), "Camera move left"),
    CAM_ROTATE_RIGHT(gui -> gui.map.rotateCamera(Coord.right), "Camera move right"),
    CAM_ROTATE_UP(gui -> gui.map.rotateCamera(Coord.up), "Camera move up"),
    CAM_ROTATE_DOWN(gui -> gui.map.rotateCamera(Coord.down), "Camera move down"),
    CAM_SNAP_WEST(gui -> gui.map.snapCameraWest(), "Camera snap west"),
    CAM_SNAP_EAST(gui -> gui.map.snapCameraEast(), "Camera snap east"),
    CAM_SNAP_NORTH(gui -> gui.map.snapCameraNorth(), "Camera snap north"),
    CAM_SNAP_SOUTH(gui -> gui.map.snapCameraSouth(), "Camera snap south"),
    CAM_RESET(gui -> gui.map.resetCamera(), "Camera reset"),
    
    FUEL_SMELTER_9(gui -> Actions.fuelGob(gui, "terobjs/smelter", "Coal", 9)),
    FUEL_SMELTER_12(gui -> Actions.fuelGob(gui, "terobjs/smelter", "Coal", 12)),
    FUEL_OVEN_4(gui -> Actions.fuelGob(gui, "terobjs/oven", "Branch", 4)),
    TOGGLE_GOB_INFO_PLANTS(gui -> GobInfoOpts.toggle(GobInfoOpts.InfoPart.PLANT_GROWTH)),
    TOGGLE_GOB_INFO_TREE_GROWTH(gui -> GobInfoOpts.toggle(GobInfoOpts.InfoPart.TREE_GROWTH)),
    TOGGLE_GOB_INFO_TREE_CONTENT(gui -> GobInfoOpts.toggle(GobInfoOpts.InfoPart.TREE_CONTENTS)),
    TOGGLE_GOB_INFO_HEALTH(gui -> GobInfoOpts.toggle(GobInfoOpts.InfoPart.HEALTH)),
    TOGGLE_GOB_INFO_BARREL(gui -> GobInfoOpts.toggle(GobInfoOpts.InfoPart.BARREL)),
    TOGGLE_GOB_INFO_SIGN(gui -> GobInfoOpts.toggle(GobInfoOpts.InfoPart.DISPLAY_SIGN)),
    TOGGLE_GOB_INFO_CHEESE(gui -> GobInfoOpts.toggle(GobInfoOpts.InfoPart.CHEESE_RACK)),
    TOGGLE_GOB_INFO_QUALITY(gui -> GobInfoOpts.toggle(GobInfoOpts.InfoPart.QUALITY)),
    TOGGLE_GOB_INFO_TIMER(gui -> GobInfoOpts.toggle(GobInfoOpts.InfoPart.TIMER)),
    
    CLEAR_PLAYER_DAMAGE(GobDamageInfo::clearPlayerDamage, "Clear damage from player"),
    CLEAR_ALL_DAMAGE(GobDamageInfo::clearAllDamage, "Clear damage from everyone");
    
    
    public final String name;
    private final Do action;
    public final String description;
    
    //TODO: add possibility to use Paginae for name and description
    Action(Do action, String name, String description) {
	this.name = name;
	this.action = action;
	this.description = description;
    }
    
    Action(Do action) {
	this(action, null);
    }
    
    Action(Do action, String name) {
	this(action, name, null);
    }
    
    Action(CFG<Boolean> toggle, String name, String description) {
	this(gui -> toggle.set(!toggle.get(), true), name, description);
    }
    
    Action(CFG<Boolean> toggle, String name) {
	this(toggle, name, null);
    }
    
    public void run(GameUI gui) {
	action.run(gui);
    }
    
    interface Do {
	void run(GameUI gui);
    }
}
