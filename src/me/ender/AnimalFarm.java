package me.ender;

import auto.Bot;
import haven.*;
import rx.functions.Func2;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static me.ender.AnimalFarm.AnimalActions.*;

public class AnimalFarm {
    private static final int BTN_W = UI.scale(50);
    private static final int PAD = UI.scale(3);
    public static final int BUTTONS_ON_LINE = 4;
    
    public static final Comparator<Widget> POSITIONAL_COMPARATOR = Comparator.comparingInt((Widget o) -> o.c.y).thenComparingInt(o -> o.c.x);
    
    public static void processCattleInfo(Window wnd) {
	Set<Avaview> avatars = wnd.children(Avaview.class);
	AnimalType type = AnimalType.getType(wnd.caption());
	if(type != null && avatars.size() == 1) {
	    Avaview ava = avatars.iterator().next();
	    TextEntry edit = wnd.children(TextEntry.class).stream().findFirst().orElse(null);
	    List<Label> labels = new ArrayList<>(wnd.children(Label.class));
	    labels.sort(POSITIONAL_COMPARATOR);
	    
	    if(edit != null) {
		labels.get(0).c.y -= UI.scale(10);
		edit.c.y = 0;
		edit.sz.x = BUTTONS_ON_LINE * BTN_W + (BUTTONS_ON_LINE - 1) * PAD;
		Coord c = new Coord(edit.c.x, edit.c.y + edit.sz.y + PAD);
		
		for (AnimalActions action : type.buttons) {
		    Button btn = wnd.add(new Button(BTN_W, action.name, action.make(wnd.ui.gui, ava.avagob)), c.x, c.y);
		    c.x += btn.sz.x + PAD;
		    if(c.x > edit.c.x + edit.sz.x) {
			c.x = edit.c.x;
			c.y += btn.sz.y + PAD;
		    }
		}
		wnd.pack();
	    }
	    
	    /*
	    int i = 0;
	    while (i < labels.size()) {
		Label label = labels.get(i);
		String labelText = label.gettext();
		AnimalStatType statType = AnimalStatType.parse(labelText);
		if(statType != null && i + 1 < labels.size()) {
		    AnimalStat stat = statType.make(labels.get(i + 1).gettext());
		    System.out.println(stat.toString());
		    i++;
		}
		i++;
	    }
	     */
	}
    }
    
    enum AnimalType {
	CATTLE(new String[]{"Bull", "Cow"}, Highlight, Shoo, Slaughter),
	HORSE(new String[]{"Stallion", "Mare"}, Highlight, Shoo, Slaughter, Ride),
	SHEEP(new String[]{"Ram", "Ewe"}, Highlight, Shoo, Slaughter, Shear),
	PIG(new String[]{"Hog", "Sow"}, Highlight, Shoo, Slaughter),
	GOAT(new String[]{"Billy", "Nanny"}, Highlight, Shoo, Slaughter);
	
	private final Set<String> names;
	private final List<AnimalActions> buttons;
	
	AnimalType(String[] names, AnimalActions... buttons) {
	    this.names = Stream.of(names).collect(Collectors.toSet());
	    this.buttons = Stream.of(buttons).collect(Collectors.toList());
	}
	
	public static AnimalType getType(String name) {
	    for (AnimalType type : values()) {
		if(type.names.contains(name)) {return type;}
	    }
	    return null;
	}
    }
    
    enum AnimalActions {
	Highlight("Show", (gui, id) -> () -> {
	    gui.ui.sess.glob.oc.stream()
		.filter(gob -> gob.id == id)
		.forEach(Gob::highlight);
	}),
	Shoo("Shoo", flower("Shoo")),
	Slaughter("Kill", flower("Slaughter")),
	Shear("Shear", flower("Shear wool")),
	Ride("Ride", flower("Giddyup!"));
	
	public final String name;
	private final Func2<GameUI, Long, Runnable> action;
	
	AnimalActions(String name, Func2<GameUI, Long, Runnable> action) {
	    this.name = name;
	    this.action = action;
	}
	
	public Runnable make(GameUI gui, long gob) {
	    return action.call(gui, gob);
	}
    
	private static Func2<GameUI, Long, Runnable> flower(final String option) {
	    return (gui, id) -> () -> Bot.selectFlower(gui, id, option);
	}
    }
    
    enum AnimalStatType {
	QUALITY("Quality:"),
	ENDURANCE("Endurance:", true),
	STAMINA("Stamina:"),
	METABOLISM("Metabolism:"),
	MEAT_QUANTITY("Meat quantity:"),
	MEAT_QUALITY("Meat quality:", true),
	MILK_QUANTITY("Milk quantity:"),
	MILK_QUALITY("Milk quality:", true),
	HIDE_QUALITY("Hide quality:", true),
	BREED_QUALITY("Breeding quality:");
	
	private final String name;
	private final boolean percent;
	
	AnimalStatType(String name, boolean percent) {
	    this.name = name;
	    this.percent = percent;
	}
	
	AnimalStatType(String name) {
	    this(name, false);
	}
    	
	AnimalStat make(String str) {
	    if(percent) {str = str.replaceAll("%", "");}
	    int value = 0;
	    try {value = Integer.parseInt(str);} catch (NumberFormatException ignored) {}
	    return new AnimalStat(this, value);
	}
	
	public static AnimalStatType parse(String name) {
	    for (AnimalStatType type : AnimalStatType.values()) {
		if(type.name.equals(name)) {
		    return type;
		}
	    }
	    return null;
	}
    }
    
    public static class AnimalStat {
	final AnimalStatType type;
	public final int value;
	
	AnimalStat(AnimalStatType type, int value) {
	    this.type = type;
	    this.value = value;
	}
	
	@Override
	public String toString() {
	    return String.format("%s\t%d%s", type.name, value, type.percent ? "%" : "");
	}
    }
}
