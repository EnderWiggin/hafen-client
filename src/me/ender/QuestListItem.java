package me.ender;

import haven.CFG;
import haven.Coord2d;
import haven.UI;
import me.ender.minimap.SMarker;

import java.awt.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class QuestListItem implements Comparable<QuestListItem> {
    private static final Pattern patt = Pattern.compile("(Tell|Greet|to|at) (\\w+)");
    public String name;
    public String prefix = "";
    public Color color;
    public int status;
    public int parentid;
    public Coord2d coord = null;
    public String questGiver = "";
    public boolean last;
    public boolean single;
    public SMarker marker = null;
    public boolean isCredo;
    
    public QuestListItem(String name, int status, boolean last, boolean single, int parentid, boolean isCredo) {
	this.name = name;
	this.status = status;
	this.last = last;
	this.single = single;
	this.parentid = parentid;
	this.isCredo = isCredo;

	Matcher matcher = patt.matcher(name);
	if(matcher.find())
	    this.questGiver = matcher.group(2);
    }
    
    public String getDisplayName() {
        return prefix + " " + name;
    }
    
    public void AddMarker(UI ui) {
        if (questGiver.isEmpty())
            return;

	marker = ui.gui.mapfile.findMarker(questGiver);
	if(marker != null) {
	    if (!marker.qitems.contains(this))
	    	marker.qitems.add(this);
	}
    }
    
    public int compareTo(QuestListItem o) {
	return this.name.compareTo(o.name);
    }
}