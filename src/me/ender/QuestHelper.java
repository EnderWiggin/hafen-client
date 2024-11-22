package me.ender;

import haven.*;

import java.util.List;

public class QuestHelper extends GameUI.Hidewnd {
    private final GameUI gui;
    private final QuestList questList;

    public QuestHelper(GameUI gui) {
	super(Coord.z, "Quest Helper");
	this.gui = gui;
	questList = add(new QuestList(UI.scale(250), 15, gui));
	pack();
    }

    public void ProcessQuest(List<QuestWnd.Quest.Condition> conditions, int id, String questTitle) {
	if (checkQuests()) {
	    boolean isLast = isLast(conditions);
	    for (int i = 0; i < conditions.size(); ++i) {
		QuestWnd.Quest.Condition condition = conditions.get(i);
		QuestWnd.Quest quest = gui.chrwdg.quest.cqst.get(id);
		if (quest == null || quest.done != 0) {
		    questList.RemoveQuest(id);
		    return;
		}

		boolean isEndpoint = (i == conditions.size() - 1);
		if(condition.done == 0)
		    questList.AddOrUpdateQuestCondition(id, condition.desc, isEndpoint, isLast, questTitle);
		else
		    questList.RemoveQuestCondition(id, condition.desc);
	    }
	}
    }

    public void Refresh() {
	if(checkQuests()) {
	    for (QuestWnd.Quest quest : gui.chrwdg.quest.cqst.quests)
		if(!questList.ContainsQuestId(quest.id) && quest.done == 0) {
		    gui.chrwdg.wdgmsg("qsel", (Object)null);
		    gui.chrwdg.wdgmsg("qsel", quest.id);
		}
	}
    }

    private boolean checkQuests() {return gui.chrwdg != null && gui.chrwdg.quest != null && gui.chrwdg.quest.cqst != null && gui.chrwdg.quest.cqst.quests != null;}

    private boolean isLast(List<QuestWnd.Quest.Condition> conditions) {return conditions.stream().filter(q -> q.done != 1).count() <= 1;}
}
