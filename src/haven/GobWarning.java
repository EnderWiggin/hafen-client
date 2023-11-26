package haven;

import haven.render.RenderTree;

import java.awt.*;
import java.util.HashMap;
import java.util.Map;

import static haven.GameUI.*;
import static haven.GobWarning.WarnMethod.*;
import static haven.GobWarning.WarnTarget.*;

public class GobWarning extends GAttrib implements RenderTree.Node {
    private final ColoredRadius radius;
    private final WarnTarget tgt;
    
    public GobWarning(Gob gob) {
	super(gob);
	tgt = categorize(gob);
	if(tgt != null) {
	    if(WarnCFG.get(tgt, message)) {
		gob.glob.sess.ui.message(String.format("%s spotted!", tgt.message), tgt.mcol, errsfx);
	    }
	    radius = new ColoredRadius(gob, tgt.radius, tgt.scol, tgt.ecol);
	} else {
	    radius = null;
	}
    }
    
    @Override
    public void added(RenderTree.Slot slot) {
	super.added(slot);
	if(radius != null && WarnCFG.get(tgt, highlight)) {slot.add(radius);}
    }
    
    public static boolean needsWarning(Gob gob) {
	return categorize(gob) != null;
    }
    
    private static WarnTarget categorize(Gob gob) {
	if(gob.is(GobTag.FOE) && !gob.anyOf(GobTag.DEAD, GobTag.KO)) {
	    return player;
	} else if(gob.is(GobTag.AGGRESSIVE) && !gob.anyOf(GobTag.DEAD, GobTag.KO)) {
	    return animal;
	} else if(gob.is(GobTag.GEM)) {
	    return gem;
	} else if (gob.is(GobTag.MIDGES)) {
	    return midges;
	}
	return null;
    }
    
    public enum WarnTarget {
	player(50, "Player", Color.RED, new Color(192, 0, 0, 128), new Color(255, 224, 96)),
	animal(50, "Dangerous animal", Color.RED, new Color(192, 0, 0, 128), new Color(255, 224, 96)),
	gem(5, "Gem", Color.GREEN, new Color(0, 192, 122, 64), new Color(255, 90, 200, 128)),
	midges(15, "Midges", Color.MAGENTA, new Color(255, 255, 255, 64), new Color(128, 0, 255, 128));
	
	private final int radius;
	private final String message;
	private final Color mcol, scol, ecol;
	
	WarnTarget(int radius, String message, Color mcol, Color scol, Color ecol) {
	    this.radius = radius;
	    this.message = message;
	    this.mcol = mcol;
	    this.scol = scol;
	    this.ecol = ecol;
	}
    }
    
    public enum WarnMethod {
	highlight, message
    }
    
    private static class WarnCFG {
	
	static boolean get(WarnTarget target, WarnMethod method) {
	    if(target != null) {
		Map<String, Boolean> cfg = CFG.WARN_CONFIG.get().getOrDefault(target.name(), new HashMap<>());
		return cfg.getOrDefault(method.name(), false);
	    }
	    return false;
	}
	
	static void set(WarnTarget target, WarnMethod method, boolean value) {
	    Map<String, Map<String, Boolean>> cfg = CFG.WARN_CONFIG.get();
	    Map<String, Boolean> tcfg = cfg.getOrDefault(target.name(), new HashMap<>());
	    tcfg.put(method.name(), value);
	    cfg.put(target.name(), tcfg);
	    CFG.WARN_CONFIG.set(cfg);
	}
    }
    
    public static class WarnCFGWnd extends WindowX {
	private static Window instance;
	
	public static void toggle(Widget parent) {
	    if(instance == null) {
		instance = parent.add(new WarnCFGWnd());
	    } else {
		doClose();
	    }
	}
	
	private static void doClose() {
	    if(instance != null) {
		instance.reqdestroy();
		instance = null;
	    }
	}
	
	@Override
	public void destroy() {
	    super.destroy();
	    instance = null;
	}
	
	public WarnCFGWnd() {
	    super(Coord.z, "Warn settings");
	    justclose = true;
	    int y = 0;
	    
	    //TODO: Make this pretty
	    CheckBox box = add(new CheckBox("Highlight players", false), 0, y);
	    box.a = WarnCFG.get(player, highlight);
	    box.changed(val -> WarnCFG.set(player, highlight, val));
	    y += 25;
	    
	    box = add(new CheckBox("Warn about players", false), 0, y);
	    box.a = WarnCFG.get(player, message);
	    box.changed(val -> WarnCFG.set(player, message, val));
	    y += 35;
	    
	    box = add(new CheckBox("Highlight animals", false), 0, y);
	    box.a = WarnCFG.get(WarnTarget.animal, highlight);
	    box.changed(val -> WarnCFG.set(animal, highlight, val));
	    y += 25;
	    
	    box = add(new CheckBox("Warn about animals", false), 0, y);
	    box.a = WarnCFG.get(WarnTarget.animal, message);
	    box.changed(val -> WarnCFG.set(animal, message, val));
	    y += 35;
	    
	    box = add(new CheckBox("Highlight gems", false), 0, y);
	    box.a = WarnCFG.get(WarnTarget.gem, highlight);
	    box.changed(val -> WarnCFG.set(gem, highlight, val));
	    y += 25;
	    
	    box = add(new CheckBox("Warn about gems", false), 0, y);
	    box.a = WarnCFG.get(gem, message);
	    box.changed(val -> WarnCFG.set(gem, message, val));
	    y += 35;
	    
	    box = add(new CheckBox("Highlight midges", false), 0, y);
	    box.a = WarnCFG.get(WarnTarget.midges, highlight);
	    box.changed(val -> WarnCFG.set(midges, highlight, val));
	    y += 25;
	    
	    box = add(new CheckBox("Warn about midges", false), 0, y);
	    box.a = WarnCFG.get(midges, message);
	    box.changed(val -> WarnCFG.set(midges, message, val));
	    
	    pack();
	    Coord asz = ca().sz();
	    if(asz.x < 200) {
		resize(new Coord(200, asz.y));
	    }
	}
    }
}
