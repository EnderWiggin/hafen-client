package me.ender;

import haven.*;

import java.awt.*;
import java.util.*;
import java.util.List;


public class QuestList extends Listbox<QuestCondition> {
    private final GameUI gui;
    private final Coord DIST_C;
    private final List<QuestCondition> questConditions = new ArrayList<>();
    private static final int ITEM_H = UI.scale(20);
    private static final Coord TEXT_C = Coord.of(0, ITEM_H / 2);
    private static final Color BGCOLOR = new Color(0, 0, 0, 120);

    public QuestList(int w, int h, GameUI gui) {
	super(w, h, ITEM_H);
	this.gui = gui;
	bgcolor = BGCOLOR;
	DIST_C = Coord.of(w - UI.scale(16), ITEM_H / 2);
    }

    public void SelectedQuest(Integer questId){
	for (QuestCondition questCondition : questConditions)
	    questCondition.isCurrent = questCondition.questId == questId;
	Collections.sort(questConditions);
    }

    public QuestCondition GetQuestCondition(int questId, String questDescription) {
	return questConditions.stream().filter(x -> x.Equals(questId, questDescription)).findFirst().orElse(null);
    }

    public void AddOrUpdateQuestCondition(int questId, String questDescription, boolean isEndpoint, boolean isLast, String questTitle) {
	QuestCondition questCondition = GetQuestCondition(questId, questDescription);
	if (questCondition == null)
	    questConditions.add(new QuestCondition(questDescription, isEndpoint, isLast, questId, questTitle, gui));
	else
	    questCondition.UpdateQuestCondition(questDescription, isEndpoint, isLast);
	SelectedQuest(questId);
    }

    public void RemoveQuestCondition(int questId, String questDescription)
    {
	QuestCondition questCondition = GetQuestCondition(questId, questDescription);
	if (questCondition != null) {
	    questCondition.RemoveMarker();
	    questConditions.remove(questCondition);
	}
    }

    public void RemoveQuest(int questId)
    {
	for (QuestCondition questCondition: new ArrayList<>(questConditions))
	    if (questCondition.questId == questId) {
		questCondition.RemoveMarker();
		questConditions.remove(questCondition);
	}
	SelectedQuest(-2);
    }

    public boolean ContainsQuestId(int questId) {return questConditions.stream().anyMatch(x -> x.questId == questId);}

    @Override
    protected QuestCondition listitem(int i) {
	return questConditions.get(i);
    }

    @Override
    protected int listitems() {
	return questConditions.size();
    }

    @Override
    protected void drawitem(GOut g, QuestCondition questCondition, int i) {
	g.chcolor(questCondition.NameColor());
	g.atext(questCondition.Name(), TEXT_C, 0, 0.5);
	String distance = questCondition.Distance();
	if(distance != null) {
	    g.atext(distance, DIST_C, 1, 0.5);
	}
    }

    @Override
    public void change(QuestCondition questCondition) {
	if(questCondition == null) {return;}
	QuestWnd.Quest.Info quest = ui.gui.chrwdg.quest.quest;
	if(quest != null && quest.questid() == questCondition.questId) {
	    gui.chrwdg.wdgmsg("qsel", (Object) null);
	    SelectedQuest(-2);
	} else {
	    gui.chrwdg.wdgmsg("qsel", questCondition.questId);
	}
    }
}
