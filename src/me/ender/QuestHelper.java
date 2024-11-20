package me.ender;

import haven.*;

import java.util.*;
import java.util.List;

public class QuestHelper extends GameUI.Hidewnd {
    private final QuestList questList;
    private boolean refreshed = false;

    public QuestHelper() {
	super(Coord.z, "Quest Helper");
	questList = add(new QuestList(UI.scale(250), 15));
	pack();
    }
    
    public void processQuest(List<QuestWnd.Quest.Condition> conditions, int id, boolean isCredo, boolean noTitled) {
	if (!noTitled || isCredo)
	    for (int i = 0; i < conditions.size(); ++i) {
		long left = conditions.stream().filter(q -> q.done != 1).count();
		QuestWnd.Quest.Condition condition = conditions.get(i);

		QuestCondition questCondition = questList.questConditions.stream().filter(x -> x.questId == id && Objects.equals(x.description, condition.desc)).findFirst().orElse(null);
		if (questCondition == null && condition.done != 1){
		    questCondition = new QuestCondition(condition.desc, condition.done, (i == conditions.size() - 1), left <= 1, id, isCredo, ui.gui);
		    questList.questConditions.add(questCondition);
		} else {
		    if (questCondition != null) {
			questCondition.UpdateQuestCondition(condition.done, (i == conditions.size() - 1), left <= 1, ui.gui);
			if (condition.done == 1) {
			    questList.questConditions.remove(questCondition);
			}
		    }
		}
	    }
	questList.SelectedQuest(id);
    }

    public void selectAllQuestsOnce() {
	if(!refreshed && ui != null && ui.gui != null && ui.gui.chrwdg != null) {
	    for (QuestWnd.Quest quest : ui.gui.chrwdg.quest.cqst.quests)
		if (questList.questConditions.stream().noneMatch(x -> x.questId == quest.id))
		    ui.gui.chrwdg.wdgmsg("qsel", quest.id);
	    refreshed = true;
	}
    }
}
