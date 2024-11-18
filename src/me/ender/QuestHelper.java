package me.ender;

import haven.*;
import me.ender.minimap.SMarker;

import java.awt.*;
import java.util.*;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static haven.MCache.*;

public class QuestHelper extends GameUI.Hidewnd {
    private static final Pattern patt = Pattern.compile("(Tell|Greet|to|at) (\\w+)");
    private final TaskList taskList;
    public HashMap<Integer, List<QuestListItem>> quests = new HashMap<>();
    
    public QuestHelper() {
	super(Coord.z, "Quest Helper");
	taskList = add(new TaskList(UI.scale(250), 15));
	pack();
    }
    
    public void processQuest(List<QuestWnd.Quest.Condition> conditions, int id, boolean isCredo) {
	long left = conditions.stream().filter(q -> q.done != 1).count();
	addConds(conditions, id, isCredo, left <= 1);
	if(!visible()) {return;}
	synchronized (taskList) {
	    taskList.tasks.removeIf(q -> q.id == id);
	    for (QuestWnd.Quest.Condition condition : conditions) {
		String name = condition.desc;
		TaskState status = TaskState.ACTIVE;
		
		if(isCredo) {name = "\uD83D\uDD6E " + name;}
		
		if(condition.done == 1) {//this task is done
		    continue;
		} else if(left <= 1) { //this is the last task
		    status = TaskState.LAST;
		    name = "★ " + name;
		}
		
		Matcher matcher = patt.matcher(condition.desc);
		SMarker marker = null;
		String markerName = null;
		if(matcher.find()) {
		    markerName = matcher.group(2);
		    marker = ui.gui.mapfile.findMarker(markerName);
		}
		
		taskList.tasks.add(new Task(name, status, id, isCredo, markerName, marker));
	    }
	    
	    taskList.tasks.sort(taskList.comp);
	    taskList.change(-1);
	    taskList.prevQuest = -2;
	}
    }
    
    @Override
    public void toggle() {
	super.toggle();
	if(visible()) {refresh();}
    }


    // TODO: We should merge refreshes
    public void refresh() {
	synchronized (taskList) {
	    taskList.tasks.clear();
	    taskList.refresh = true;
	}
	refreshQuests();
    }

    public void refreshQuests() {
	if(ui != null && ui.gui != null && ui.gui.chrwdg != null) {
	    for (Iterator<Map.Entry<Integer, List<QuestListItem>>> it = quests.entrySet().iterator(); it.hasNext();) {
		Map.Entry<Integer, List<QuestListItem>> entry = it.next();
		if (ui.gui.chrwdg.quest.cqst.get(entry.getKey()) == null) { // quest removed
		    for (QuestListItem qitem : entry.getValue()) {
			if (qitem.marker != null) {
			    qitem.marker.qitems.remove(qitem);
			}
		    }
		    it.remove();
		}
	    }
	    try {
		for (QuestWnd.Quest quest : ui.gui.chrwdg.quest.cqst.quests) {
		    if (!quests.containsKey(quest.id))
			ui.gui.chrwdg.wdgmsg("qsel", quest.id);
		}
	    } catch (NullPointerException var9) {
		var9.printStackTrace();
	    } catch (Loading var10) {
		System.out.println("loading...");
	    }
	}
    }

    public QuestListItem findqitem(Integer id, String name) {
	for(QuestListItem qitem : quests.get(id)) {
	    if(qitem.name.equals(name))
		return(qitem);
	}
	return(null);
    }

    public void addConds(List<QuestWnd.Quest.Condition> ncond, int id, boolean isCredo, boolean single) {
	if (ui.gui.chrwdg.quest.cqst.get(id) == null)
	    return;
	if (ui.gui.chrwdg.quest.cqst.get(id).done != QuestWnd.Quest.QST_PEND)
	    return;
	List<QuestListItem> condList = new ArrayList<>();
	boolean refreshAllConditions = false;
	for (int i = 0; i < ncond.size(); ++i) {
	    QuestListItem qitem = new QuestListItem(ncond.get(i).desc, ncond.get(i).done, (i == ncond.size() - 1) && !isCredo, single, id, isCredo);
	    if (!refreshAllConditions && quests.containsKey(id)) {
		QuestListItem oldqitem = findqitem(id, qitem.name);
		if (oldqitem != null) {
		    if (qitem.status != oldqitem.status)
			oldqitem.status = qitem.status;
		} else {
		    refreshAllConditions = true; // new condition, rewrite all conditions for this quest
		}
	    }
	    condList.add(qitem);
	}
	if (refreshAllConditions || !quests.containsKey(id)) {
	    if (quests.containsKey(id)) { // clear old conditions
		for (QuestListItem qitem: quests.get(id)) {
		    if (qitem.marker != null)
			if (qitem.marker.qitems.contains(qitem))
			    qitem.marker.qitems.remove(qitem);
		}
	    }
	    for (QuestListItem qitem: condList) {
		if (!qitem.questGiver.isEmpty())
		    qitem.AddMarker(ui);
	    }
	    quests.put(id, condList);
	}
    }
    
    private enum TaskState {
	ACTIVE, LAST
    }
    
    private static class TaskList extends Listbox<Task> {
	public static final int ITEM_H = UI.scale(20);
	public static final Coord TEXT_C = Coord.of(0, ITEM_H / 2);
	public static final Color BGCOLOR = new Color(0, 0, 0, 120);
	private final Coord DIST_C;
	public List<Task> tasks = new ArrayList<>(50);
	public boolean refresh = true;
	private long lastUpdateTime = System.currentTimeMillis();
	private final Comparator<Task> comp = Comparator.comparing(a -> a.name);
	private int prevQuest;
	
	public TaskList(int w, int h) {
	    super(w, h, ITEM_H);
	    bgcolor = BGCOLOR;
	    DIST_C = Coord.of(w - UI.scale(16), ITEM_H / 2);
	}
	
	@Override
	public void tick(double dt) {
	    if(!tvisible()) {return;}
	    GameUI gui = ui.gui;
	    if(gui == null || gui.chrwdg == null) {return;}
	    Optional<QuestWnd> optQuestWnd = Optional.ofNullable(gui.chrwdg.quest);
	    int currentQuest = optQuestWnd.map(q -> q.quest)
		.map(QuestWnd.Quest.Info::questid).orElse(-1);
	    
	    if(!refresh) {
		if(prevQuest != currentQuest) {
		    for (Task item : tasks) {
			item.current = currentQuest == item.id;
		    }
		    Collections.sort(tasks);
		    prevQuest = currentQuest;
		}
		return;
	    }
	    if(System.currentTimeMillis() - lastUpdateTime < 500) {return;}
	    
	    prevQuest = -2;
	    refresh = false;
	    lastUpdateTime = System.currentTimeMillis();
	    synchronized (this) {
		tasks.clear();
		if(!optQuestWnd.isPresent()) {return;}
		QuestWnd questWnd = optQuestWnd.get();
		boolean changed = false;
		List<QuestWnd.Quest> quests = optQuestWnd.map(w -> w.cqst).map(c -> c.quests).orElse(Collections.emptyList());
		for (QuestWnd.Quest quest : quests) {
		    //currently selected quest will be selected last
		    if(currentQuest == quest.id) {continue;}
		    questWnd.wdgmsg("qsel", quest.id);
		    changed = true;
		}
		if(currentQuest >= 0) {
		    questWnd.wdgmsg("qsel", currentQuest);
		    //if the only quest in the log is currently selected - send selection again to re-select it
		    if(!changed) {questWnd.wdgmsg("qsel", currentQuest);}
		} else {
		    questWnd.wdgmsg("qsel", (Object) null);
		}
		Collections.sort(tasks);
	    }
	}
	
	protected Task listitem(int idx) {
	    return tasks.get(idx);
	}
	
	@Override
	protected int listitems() {
	    return tasks.size();
	}
	
	@Override
	protected void drawitem(GOut g, Task item, int idx) {
	    Color color;
	    if(item.status == TaskState.LAST) {
		color = item.current ? Color.CYAN : Color.GREEN;
	    } else {
		color = item.current ? Color.WHITE : Color.LIGHT_GRAY;
	    }
	    g.chcolor(color);
	    g.atext(item.name, TEXT_C, 0, 0.5);
	    String distance = item.distance(ui.gui);
	    if(distance != null) {
		g.atext(distance, DIST_C, 1, 0.5);
	    }
	}
	
	public void change(Task item) {
	    if(item == null) {return;}
	    QuestWnd.Quest.Info quest = ui.gui.chrwdg.quest.quest;
	    if(quest != null && quest.questid() == item.id) {
		ui.gui.chrwdg.wdgmsg("qsel", (Object) null);
	    } else {
		ui.gui.chrwdg.wdgmsg("qsel", item.id);
	    }
	}
    }
    
    private static class Task implements Comparable<Task> {
	private final String name;
	private final TaskState status;
	private final int id;
	private final boolean credo;
	private final String markerName;
	private final SMarker marker;
	private boolean current = false;
	
	public Task(String name, TaskState status, int id, boolean credo, String markerName, SMarker marker) {
	    this.name = name;
	    this.status = status;
	    this.id = id;
	    this.credo = credo;
	    this.markerName = markerName;
	    this.marker = marker;
	}
	
	String distance(GameUI gui) {
	    if(markerName == null || gui == null || gui.map == null || gui.mapfile == null) {return null;}
	    
	    MiniMap.Location loc = gui.mapfile.playerLocation();
	    if(loc == null) {return null;}
	    
	    Gob player = gui.map.player();
	    if(player == null) {return null;}
	    
	    Coord2d pc = player.rc;
	    Coord tc = null;
	    
	    if(marker != null) {
		if(marker.seg == loc.seg.id) {tc = marker.tc.sub(loc.tc);}
	    } else {
		//TODO: cache pointer?
		tc = gui.findPointer(markerName)
		    .map(p -> p.tc(loc.seg.id).floor(tilesz))
		    .orElse(null);
	    }
	    
	    if(tc == null) {return null;}
	    
	    return String.format("%.0fm", tc.sub(pc.floor(tilesz)).abs());
	}
	
	public int compareTo(Task o) {
	    int result = -Boolean.compare(current, o.current);
	    if(result == 0) {
		result = status.compareTo(o.status);
	    }
	    if(result == 0) {
		result = -Boolean.compare(credo, o.credo);
	    }
	    if(result == 0) {
		result = name.compareTo(o.name);
	    }
	    return result;
	}
    }
}
