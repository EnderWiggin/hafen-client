package me.ender;

import haven.*;
import me.ender.minimap.SMarker;

import java.awt.*;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static haven.MCache.*;

public class QuestCondition implements Comparable<QuestCondition> {
    private static final Pattern pat = Pattern.compile("(Tell|Greet| to| at) (\\w+)");

    public String name;
    public String description;
    public String questGiver = "";
    public Optional<MiniMap.IPointer> questGiverPointer = Optional.empty();
    public SMarker questGiverMarker;
    public Color questGiverMarkerColor;
    public int questId;
    public int statusId;
    public boolean isEndpoint;
    public boolean isLast;
    public boolean isCredo;
    public boolean isCurrent = false;

    public QuestCondition(String description, int statusId, boolean isEndpoint, boolean isLast, int questId, boolean isCredo, GameUI gui) {
	this.description = description;
	this.questId = questId;
	this.statusId = statusId;
	this.isEndpoint = isEndpoint;
	this.isLast = isLast;
	this.isCredo = isCredo;

	Matcher matcher = pat.matcher(description);
	if(matcher.find()) {
	    this.questGiver = matcher.group(2);
	}

	this.name = description;
	if(isCredo) {name = "\uD83D\uDD6E " + description;}
	if(isLast) {name = "★ " + name;}

	AddMarker(gui);
	AddPointer(gui);
    }

    public void UpdateQuestCondition(int statusId, boolean isEndpoint, boolean isLast, GameUI gui)
    {
	this.statusId = statusId;
	this.isEndpoint = isEndpoint;
	this.isLast = isLast;

	AddMarker(gui);
	AddPointer(gui);

	if (statusId == 1 && questGiverMarker != null)
	    questGiverMarker.questConditions.remove(this);
    }

    private void UpdateColor() {
	if(statusId == 0) {
	    if(isEndpoint) {
		if(isLast) {
		    questGiverMarkerColor = Color.GREEN;
		} else {
		    questGiverMarkerColor = Color.YELLOW;
		}
	    } else {
		questGiverMarkerColor = Color.WHITE;
	    }
	} else questGiverMarkerColor = null;
    }

    private void AddMarker(GameUI gui)
    {
	if (questGiverMarker == null && !questGiver.isEmpty())
	    questGiverMarker = gui.mapfile.findMarker(questGiver);
	if (questGiverMarker != null) {
	    UpdateColor();
	    if (!questGiverMarker.questConditions.contains(this))
	    	questGiverMarker.questConditions.add(this);
	}
    }

    private void AddPointer(GameUI gui)
    {
	if (!questGiverPointer.isPresent() && !questGiver.isEmpty())
	    questGiverPointer = gui.findPointer(questGiver);
    }

    public String distance(GameUI gui) {
	if(questGiver == null || gui == null || gui.map == null || gui.mapfile == null) {return null;}

	MiniMap.Location loc = gui.mapfile.playerLocation();
	if(loc == null) {return null;}

	Gob player = gui.map.player();
	if(player == null) {return null;}

	Coord2d pc = player.rc;
	Coord tc = null;

	if(questGiverMarker != null) {
	    if(questGiverMarker.seg == loc.seg.id) {tc = questGiverMarker.tc.sub(loc.tc);}
	} else {
	    tc = questGiverPointer
		.map(p -> p.tc(loc.seg.id).floor(tilesz))
		.orElse(null);
	}

	if(tc == null) {return null;}

	return String.format("%.0fm", tc.sub(pc.floor(tilesz)).abs());
    }

    public int compareTo(QuestCondition o) {
	int result = -Boolean.compare(isCurrent, o.isCurrent);
	if(result == 0) {
	    result = Boolean.compare(isLast, o.isLast);
	}
	if(result == 0) {
	    result = -Boolean.compare(isCredo, o.isCredo);
	}
	if(result == 0) {
	    result = description.compareTo(o.description);
	}
	return result;
    }
}