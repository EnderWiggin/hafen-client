package me.ender;

import haven.*;

import java.util.Set;

public class GobInfoOpts extends WindowX {
    
    public static final int PAD = UI.scale(5);
    
    public enum InfoPart {
	PLANT_GROWTH("Plant growth"),
	TREE_GROWTH("Tree growth"),
	HEALTH("Object health"),
	BARREL("Barrel contents"),
	DISPLAY_SIGN("Display sign contents"),
	CHEESE_RACK("Cheese rack contents"),
	QUALITY("Quality"),
	TIMER("Timer");
	
	public final String text;
	
	InfoPart(String text) {this.text = text;}
    }
    
    public GobInfoOpts() {
	super(Coord.z, "Gob info settings");
	justclose = true;
	
	Set<InfoPart> selected = CFG.DISPLAY_GOB_INFO_DISABLED_PARTS.get();
	Composer composer = new Composer(this).vmrgn(PAD);
	composer.add(new Label("Types of info to display:"));
	composer.hpad(2 * PAD);
	for (InfoPart cat : InfoPart.values()) {
	    CheckBox box = composer.add(new CheckBox(cat.text, false));
	    box.a = !selected.contains(cat);
	    box.changed(val -> {
		boolean changed;
		Set<InfoPart> categories = CFG.DISPLAY_GOB_INFO_DISABLED_PARTS.get();
		if(val) {
		    changed = categories.remove(cat);
		} else {
		    changed = categories.add(cat);
		}
		if(changed) {
		    CFG.DISPLAY_GOB_INFO_DISABLED_PARTS.set(categories);
		}
	    });
	}
	composer.y(0).hpad(UI.scale(150));
	composer.add(new Label("Options:"));
	composer.hpad(composer.hpad() + 2 * PAD);
	composer.add(new OptWnd.CFGBox("Shorten text of the object contents", CFG.DISPLAY_GOB_INFO_SHORT, "Will remove some not very relevant parts of the contents name", true));
	pack();
    }
    
    private static Window instance;
    
    public static void toggle(Widget parent) {
	if(instance == null) {
	    instance = parent.add(new GobInfoOpts(), 200, 100);
	} else {
	    instance.reqdestroy();
	    instance = null;
	}
    }
    
    @Override
    public void destroy() {
	super.destroy();
	instance = null;
    }
    
    public static boolean enabled(InfoPart part) {return !CFG.DISPLAY_GOB_INFO_DISABLED_PARTS.get().contains(part);}
    
    public static boolean disabled(InfoPart part) {return CFG.DISPLAY_GOB_INFO_DISABLED_PARTS.get().contains(part);}
}
