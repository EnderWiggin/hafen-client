package me.ender;

import haven.*;
import me.ender.minimap.SMarker;

import java.awt.*;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static haven.MCache.*;

public class QuestCondition implements Comparable<QuestCondition> {
    private final GameUI gui;
    private static final Pattern pat = Pattern.compile("(Tell|Greet| to| at) (\\w+)");
    private String questGiver = "";
    private Optional<MiniMap.IPointer> questGiverPointer = Optional.empty();
    private SMarker questGiverMarker;
    private final String questTitle;
    private final String searchDescription;
    private boolean isEndpoint;
    private boolean isLast;

    public int questId;
    public String description;
    public boolean isCurrent = false;

    public QuestCondition(String description, boolean isEndpoint, boolean isLast, int questId, String questTitle, GameUI gui) {
	this.questId = questId;
	this.description = description;
	this.isEndpoint = isEndpoint;
	this.isLast = isLast;
	this.questTitle = questTitle;
	this.gui = gui;

	Matcher matcher = pat.matcher(description);
	if(matcher.find()) {
	    this.questGiver = matcher.group(2);
	}

	int i = description.lastIndexOf('(');
	searchDescription = i > 0 ? description.substring(0, i - 1) : description;

	addMarker();
	//addPointer();
    }

    public void UpdateQuestCondition(String description, boolean isEndpoint, boolean isLast)
    {
	this.isEndpoint = isEndpoint;
	this.isLast = isLast;
	this.description = description;

	addMarker();
	//addPointer();
    }

    public void RemoveMarker()
    {
	if (questGiverMarker != null)
	    questGiverMarker.questConditions.remove(this);
    }

    public String Name() {
	if(isCredo()) return "\uD83D\uDD6E " + description;
	else if(isLast) return "★ " + description;
	else return description;
    }

    public Color NameColor() {
	if(isLast && !isCredo()) return isCurrent ? Color.CYAN : Color.GREEN;
	else return isCurrent ? Color.WHITE : Color.LIGHT_GRAY;
    }

    public Color QuestGiverMarkerColor() {
	if(isEndpoint) {
	    if(isLast) return Color.GREEN;
	    else return Color.YELLOW;
	} else return Color.WHITE;
    }
    
    public String Distance() {
	if(questGiver == null || gui == null || gui.map == null || gui.mapfile == null) {return null;}

	MiniMap.Location loc = gui.mapfile.playerLocation();
	if(loc == null) {return null;}

	Gob player = gui.map.player();
	if(player == null) {return null;}

	Coord2d pc = player.rc;
	Coord tc = null;

	if(questGiverMarker != null)
	    if(questGiverMarker.seg == loc.seg.id) {tc = questGiverMarker.tc.sub(loc.tc);}
     	//else if (questGiverPointer.isPresent())
	    //tc = questGiverPointer.map(p -> p.tc(loc.seg.id).floor(tilesz)).orElse(null);

	if(tc == null) {return null;}

	return String.format("%.0fm", tc.sub(pc.floor(tilesz)).abs());
    }

    public boolean Equals(int questId, String questDescription) {
	int i = questDescription.lastIndexOf('(');
	String searchDescription = i > 0 ? questDescription.substring(0, i - 1) : questDescription;
	return this.questId == questId && Objects.equals(this.searchDescription, searchDescription);
    }

    public int compareTo(QuestCondition o) {
	int result = -Boolean.compare(isCredo(), o.isCredo());
	if(result == 0) {
	    result = -Boolean.compare(isCurrent, o.isCurrent);
	}
	if(result == 0) {
	    result = -Boolean.compare(questTitle != null, o.questTitle != null);
	}
	if(result == 0) {
	    result = Boolean.compare(isLast, o.isLast);
	}
	if(result == 0) {
	    result = description.compareTo(o.description);
	}
	return result;
    }

    private boolean checkMapFile() {return gui.mapfile != null;}

    private boolean isCredo() {return gui.chrwdg != null && gui.chrwdg.skill != null && gui.chrwdg.skill.credos != null && gui.chrwdg.skill.credos.pqid == questId;}

    private void addMarker()
    {
	if (questGiverMarker == null && !questGiver.isEmpty() && checkMapFile())
	    questGiverMarker = gui.mapfile.findMarker(questGiver);
	if (questGiverMarker != null) {
	    if (!questGiverMarker.questConditions.contains(this))
		questGiverMarker.questConditions.add(this);
	}
    }

    private void addPointer()
    {
	if (!questGiverPointer.isPresent() && !questGiver.isEmpty())
	    questGiverPointer = gui.findPointer(questGiver);
    }
}