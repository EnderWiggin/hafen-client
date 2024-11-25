package me.ender;

import haven.*;
import me.ender.ui.CFGBox;
import me.ender.ui.CFGSlider;

import static haven.OptWnd.*;

public class CustomOptPanels {
    private static final int STEP = UI.scale(25);
    private static final int H_STEP = UI.scale(10);
    private static final int COL_WIDTH = UI.scale(230);
    
    public static void initColorPanel(OptWnd wnd, OptWnd.Panel panel) {
	int START;
	int x, y;
	int my = 0, tx;
	Widget w;
	
	Widget title = panel.add(new Label("Color settings", LBL_FNT), 0, 0);
	START = title.sz.y + UI.scale(10);
	
	x = 0;
	y = START;
	panel.add(new CFGColorBtn(CFG.COLOR_MINE_SUPPORT_OVERLAY, "Mine support overlay", false), x, y);
	
	y += STEP;
	panel.add(new CFGColorBtn(CFG.COLOR_MINE_SUPPORT_DAMAGED_OVERLAY, "Damaged mine support overlay", true), x, y);
	
	y += STEP;
	panel.add(new CFGColorBtn(CFG.COLOR_TILE_GRID, "Tile grid", true), x, y);
	
	y += STEP;
	panel.add(new Label("Hit Box:"), x, y);
	
	y += STEP;
	tx = x;
	tx += panel.add(new CFGColorBtn(CFG.COLOR_HBOX_SOLID, "Solid", true), tx + H_STEP, y).sz.x;
	tx += H_STEP;
	panel.add(new CFGColorBtn(CFG.COLOR_HBOX_PASSABLE, "Passable", true), tx + H_STEP, y);
	
	y += STEP;
	panel.add(new CFGBox("Fill in solid hit boxes:", CFG.DISPLAY_GOB_HITBOX_FILLED), x, y);
	y += STEP;
	panel.add(new CFGColorBtn(CFG.COLOR_HBOX_FILLED, "With color", true), x + H_STEP, y);
	
	y += STEP;
	panel.add(new CFGColorBtn(CFG.COLOR_RIDGE_BOX, "Ridge highlight", true), x, y);
	
	y += STEP;
	panel.add(new Label("Aura colors:"), x, y);
	
	y += STEP;
	panel.add(new CFGColorBtn(CFG.COLOR_GOB_SPEED_BUFF, "Speed Buff", true), x + H_STEP, y);
	
	y += STEP;
	panel.add(new CFGColorBtn(CFG.COLOR_GOB_RABBIT, "Rabbits", true), x + H_STEP, y);
	
	y += STEP;
	panel.add(new CFGColorBtn(CFG.COLOR_GOB_CRITTERS, "Critters", true), x + H_STEP, y);
	
	my = Math.max(my, y);
	
	x += COL_WIDTH;
	y = START;
	
	panel.add(new CFGColorBtn(CFG.COLOR_GOB_READY, "Workstation ready", true), x, y);
	
	y += STEP;
	panel.add(new CFGColorBtn(CFG.COLOR_GOB_FULL, "Container: Full", true), x, y);
	
	y += STEP;
	panel.add(new CFGColorBtn(CFG.COLOR_GOB_EMPTY, "Container: Empty", true), x, y);
	
	y += STEP;
	
	//combat
	panel.add(new Label("Combat highlights:"), x, y);
	
	tx = x + H_STEP;
	y += STEP;
	panel.add(new CFGColorBtn(CFG.COLOR_GOB_SELF, "Self", true), tx, y);
	
	y += STEP;
	panel.add(new CFGColorBtn(CFG.COLOR_GOB_LEADER, "Leader", true), tx, y);
	
	y += STEP;
	panel.add(new CFGColorBtn(CFG.COLOR_GOB_PARTY, "Party", true), tx, y);
	
	y += STEP;
	panel.add(new CFGColorBtn(CFG.COLOR_GOB_IN_COMBAT, "Enemy", true), tx, y);
	
	y += STEP;
	panel.add(new CFGColorBtn(CFG.COLOR_GOB_COMBAT_TARGET, "Current target", true), tx, y);
	
	my = Math.max(my, y);
	
	w = panel.add(wnd.new PButton(UI.scale(200), "Back", 27, wnd.main), new Coord(0, my + UI.scale(35)));
	panel.pack();
	title.c.x = (panel.sz.x - title.sz.x) / 2;
	w.c.x = (panel.sz.x - w.sz.x) / 2;
    }
    
    public static void initCombatPanel(OptWnd wnd, OptWnd.Panel panel) {
	int START;
	int x, y;
	int my = 0, tx;
	Widget w;
	
	Widget title = panel.add(new Label("Combat settings", LBL_FNT), 0, 0);
	START = title.sz.y + UI.scale(10);
	
	x = 0;
	y = START;
	//first row
	panel.add(new CFGBox("Use new combat UI", CFG.ALT_COMBAT_UI), x, y);
	
	y += STEP;
	panel.add(new CFGBox("Allow dragging combat UI", CFG.DRAG_COMBAT_UI, "Drag by cooldown circle"), x, y);
	y += STEP;
	panel.add(new Button(UI.scale(150), "Reset combat UI position", false), x + H_STEP, y)
	    .action(() -> Fightsess.resetOffset(wnd.ui));
	y += STEP;
	
	y += STEP;
	panel.add(new CFGBox("Auto peace on combat start", CFG.COMBAT_AUTO_PEACE , "Automatically enter peaceful mode on combat start id enemy is aggressive - useful for taming"), x, y);
	
	y += STEP;
	panel.add(new CFGBox("Re-aggro animals", CFG.COMBAT_RE_AGGRO), x, y).settip("Automatically re-start combat with animals that dropped out of combat with you.\nOnly really useful if 'Auto peace on combat start' is enabled.", true);

	y += STEP;
	panel.add(new CFGBox("Always mark current target", CFG.ALWAYS_MARK_COMBAT_TARGET , "Usually current target only marked when there's more than one"), x, y);
	
	y = AddCombatHighlight(panel, x, y, "Highlight party members in combat", CFG.HIGHLIGHT_PARTY_IN_COMBAT, CFG.MARK_PARTY_IN_COMBAT);
	y = AddCombatHighlight(panel, x, y, "Highlight self in combat", CFG.HIGHLIGHT_SELF_IN_COMBAT, CFG.MARK_SELF_IN_COMBAT);
	y = AddCombatHighlight(panel, x, y, "Highlight enemies in combat", CFG.HIGHLIGHT_ENEMY_IN_COMBAT, CFG.MARK_ENEMY_IN_COMBAT);
	
	my = Math.max(my, y);
	
	//second row
	x += COL_WIDTH;
	y = START;
	
	panel.add(new CFGBox("Show combat info", CFG.SHOW_COMBAT_INFO, "Will display initiative points and openings over gobs that you are fighting"), x, y);
	
	y += STEP;
	Label label = panel.add(new Label(String.format("Combat info vertical offset: %d", CFG.SHOW_COMBAT_INFO_HEIGHT.get())), x + H_STEP, y);
	y += UI.scale(15);
	panel.add(new CFGSlider(UI.scale(150), 1, 35, CFG.SHOW_COMBAT_INFO_HEIGHT, label, "Combat info vertical offset: %d"), x + H_STEP, y);
	
	y += STEP;
	panel.add(new CFGBox("Simplified combat openings", CFG.SIMPLE_COMBAT_OPENINGS, "Show openings as solid colors with numbers"), x, y);
	
	y += STEP;
	panel.add(new CFGBox("Display combat keys", CFG.SHOW_COMBAT_KEYS), x, y);
	
	y += STEP;
	panel.add(new CFGBox("Show combat damage", CFG.SHOW_COMBAT_DMG), x, y);
	
	y += STEP;
	panel.add(new CFGBox("Clear player damage after combat", CFG.CLEAR_PLAYER_DMG_AFTER_COMBAT), x, y);
	
	y += STEP;
	panel.add(new CFGBox("Clear all damage after combat", CFG.CLEAR_ALL_DMG_AFTER_COMBAT), x, y);
	
	y += STEP;
	panel.add(new CFGBox("Show draggable HP/Stamina widgets", CFG.SHOW_FLOATING_STAT_WDGS), x, y);
	
	y += STEP;
	panel.add(new CFGBox("Only during combat", CFG.SHOW_FLOATING_STATS_COMBAT), x + H_STEP, y);
	
	y += STEP;
	panel.add(new CFGBox("Lock them to prevent clicks and dragging", CFG.LOCK_FLOATING_STAT_WDGS), x + H_STEP, y);
	
	
	my = Math.max(my, y);
	
	w = panel.add(wnd.new PButton(UI.scale(200), "Back", 27, wnd.main), new Coord(0, my + UI.scale(35)));
	panel.pack();
	title.c.x = (panel.sz.x - title.sz.x) / 2;
	w.c.x = (panel.sz.x - w.sz.x) / 2;
    }
    
    private static int AddCombatHighlight(OptWnd.Panel panel, int x, int y, String name, CFG<Boolean> highlight, CFG<Boolean> mark) {
	y += STEP;
	panel.add(new Label(name), x, y);
	
	y += STEP;
	panel.add(new CFGBox("By coloring", highlight), x + H_STEP, y);
	
	y += STEP;
	panel.add(new CFGBox("By ring", mark), x + H_STEP, y);
	
	return y;
    }
}
