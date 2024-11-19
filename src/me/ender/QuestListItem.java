package me.ender;

import haven.CFG;
import haven.UI;
import me.ender.minimap.SMarker;

import java.awt.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class QuestListItem implements Comparable<QuestListItem> {
    private static final Pattern pat = Pattern.compile("(Tell|Greet| to| at) (\\w+)");
    public String name;
    public Color color;
    public int status;
    public int parent;
    public boolean last;
    public boolean single;
    public String questGiver = "";
    public SMarker marker = null;
    public boolean isCredo;
    
    public QuestListItem(String name, int status, boolean last, boolean single, int parent, boolean isCredo) {
	this.name = name;
	this.status = status;
	this.parent = parent;
	this.last = last;
	this.single = single;
	this.isCredo = isCredo;

	Matcher matcher = pat.matcher(name);
	if(matcher.find())
	    this.questGiver = matcher.group(2);
    }
    
    public void AddMarker(UI ui) {
	marker = ui.gui.mapfile.findMarker(questGiver);
	if(marker != null) {
	    if (!marker.qitems.contains(this))
	    	marker.qitems.add(this);
	}
	UpdateColor();
    }

    public void UpdateColor() {
	if(marker != null) {
	    if(status == 0) {
		if(last) {
		    if(single) {
			color = Color.GREEN;
		    } else if(CFG.QUESTHELPER_HIGHLIGHT_UNFINISHED.get()) {
			color = Color.YELLOW;
		    }
		} else {
		    color = Color.WHITE;
		}
	    } else color = null;
	}
    }
    
    public int compareTo(QuestListItem o) {
	return this.name.compareTo(o.name);
    }
}