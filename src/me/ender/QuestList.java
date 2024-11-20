package me.ender;

import haven.*;

import java.awt.*;
import java.util.*;
import java.util.List;


public class QuestList extends Listbox<QuestCondition> {
    public List<QuestCondition> questConditions = new ArrayList<>();
    public static final int ITEM_H = UI.scale(20);
    public static final Coord TEXT_C = Coord.of(0, ITEM_H / 2);
    public static final Color BGCOLOR = new Color(0, 0, 0, 120);
    private final Coord DIST_C;

    public QuestList(int w, int h) {
	super(w, h, ITEM_H);
	bgcolor = BGCOLOR;
	DIST_C = Coord.of(w - UI.scale(16), ITEM_H / 2);
    }

    protected QuestCondition listitem(int i) {
	return questConditions.get(i);
    }

    protected int listitems() {
	return questConditions.size();
    }

    protected void drawitem(GOut g, QuestCondition questCondition, int i) {
	Color color;
	if(questCondition.isLast) {
	    color = questCondition.isCurrent ? Color.CYAN : Color.GREEN;
	} else {
	    color = questCondition.isCurrent ? Color.WHITE : Color.LIGHT_GRAY;
	}
	g.chcolor(color);
	g.atext(questCondition.description, TEXT_C, 0, 0.5);
	if (!questCondition.questGiver.isEmpty()) {
	    String distance = questCondition.distance(ui.gui);
	    if(distance != null) {
		g.atext(distance, DIST_C, 1, 0.5);
	    }
	}
    }

    public void SelectQuest(Integer questId){
	for (QuestCondition questCondition : questConditions)
	    questCondition.isCurrent = questCondition.questId == questId;
	Collections.sort(questConditions);
    }

    public void change(QuestCondition questCondition) {
	if(questCondition == null) {return;}
	QuestWnd.Quest.Info quest = ui.gui.chrwdg.quest.quest;
	if(quest != null && quest.questid() == questCondition.questId) {
	    ui.gui.chrwdg.wdgmsg("qsel", (Object) null);
	    SelectQuest(-2);
	} else {
	    ui.gui.chrwdg.wdgmsg("qsel", questCondition.questId);
	}
    }
}
