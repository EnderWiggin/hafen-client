package me.ender.gob;

import haven.*;
import haven.render.Homo3D;
import haven.render.Pipe;
import haven.render.RenderTree;
import me.ender.RichUText;

import java.awt.*;
import java.awt.font.TextAttribute;
import java.text.AttributedCharacterIterator;
import java.util.HashMap;
import java.util.Map;

import static haven.Buff.*;

public class GobCombatInfo extends GAttrib implements RenderTree.Node, PView.Render2D {
    private final Fightview.Relation rel;
    private final Coord3f pos = new Coord3f(0, 0, 1);
    private static final Text.Furnace opIp = new PUtils.BlurFurn((new Text.Foundry(Text.mono, 14, new Color(255, 150, 80))).aa(false), 2, 2, Color.DARK_GRAY);
    private static final Text.Furnace myIp = new PUtils.BlurFurn((new Text.Foundry(Text.mono, 14, new Color(100, 220, 220))).aa(false), 2, 2, Color.DARK_GRAY);
    private static final RichText.Foundry fndOpen;
    
    //Slightly tweaked colors for better visibility
    private static final Map<String, Color> OPENINGS = new HashMap<String, Color>(4) {{
	put(OPEN_GREEN, new Color(89, 180, 62));
	put(OPEN_YELLOW, new Color(210, 210, 64));
	put(OPEN_BLUE, new Color(51, 129, 231));
	put(OPEN_RED, new Color(241, 62, 62));
    }};
    
    private static final Map<Long, Fightview.Relation> map = new HashMap<>();
    
    static {
	Map<AttributedCharacterIterator.Attribute, Object> a = new HashMap<>();
	a.put(TextAttribute.FAMILY, "monospaced");
	a.put(TextAttribute.SIZE, UI.scale(12.0f));
	a.put(TextAttribute.WEIGHT, TextAttribute.WEIGHT_BOLD);
	fndOpen = new RichText.Foundry(new RichText.Parser(a));
    }
    
    private final Text.UText<?> oip;
    private final Text.UText<?> mip;
    private final RichUText<String> openings;
    
    public GobCombatInfo(Gob owner, Fightview.Relation rel) {
	super(owner);
	this.rel = rel;
	
	this.oip = new Text.UText<Integer>(opIp) {
	    public String text(Integer v) {
		return v.toString();
	    }
	    
	    public Integer value() {
		if(rel == null || rel.invalid)
		    return 0;
		return rel.oip;
	    }
	};
	
	
	this.mip = new Text.UText<Integer>(myIp) {
	    public String text(Integer v) {
		return v.toString();
	    }
	    
	    public Integer value() {
		if(rel == null || rel.invalid)
		    return 0;
		return rel.ip;
	    }
	};
	
	this.openings = new RichUText<String>(fndOpen, new Color(0, 0, 0, 84)) {
	    @Override
	    protected String value() {
		return getOpeningText();
	    }
	};
    }
    
    @Override
    public void ctick(double dt) {
	super.ctick(dt);
    }
    
    protected boolean enabled() {
	return CFG.SHOW_COMBAT_INFO.get() && rel != null;
    }
    
    @Override
    public void draw(GOut g, Pipe state) {
	if(!enabled() || rel.invalid) {return;}
	
	Coord3f c3d = Homo3D.obj2view2(pos, state, Area.sized(g.sz()));
	if(c3d == null) {return;}
	
	Coord sc = c3d.round2();
	if(!sc.isect(Coord.z, g.sz())) {return;}
	
	g.aimage(mip.get().tex(), sc.addx(-2), 1f, 0f);
	g.aimage(oip.get().tex(), sc.addx(2), 0f, 0f);
	
	g.aimage(openings.get(), sc.addy(UI.scale(20)), 0.5f, 0f);
    }
    
    private String getOpeningText() {
	String y = "-", g = "-", b = "-", r = "-";
	
	for (Buff buff : rel.buffs.children(Buff.class)) {
	    int value = buff.ameter();
	    if(value <= 0) {continue;}
	    try {
		String name = buff.res.get().name;
		if(OPEN_YELLOW.equals(name)) {
		    y = String.format("%2d", value);
		} else if(OPEN_GREEN.equals(name)) {
		    g = String.format("%2d", value);
		} else if(OPEN_BLUE.equals(name)) {
		    b = String.format("%2d", value);
		} else if(OPEN_RED.equals(name)) {
		    r = String.format("%2d", value);
		}
	    } catch (Loading ignore) {}
	}
	
	y = RichText.color(y, OPENINGS.get(OPEN_YELLOW));
	g = RichText.color(g, OPENINGS.get(OPEN_GREEN));
	b = RichText.color(b, OPENINGS.get(OPEN_BLUE));
	r = RichText.color(r, OPENINGS.get(OPEN_RED));
	
	return String.join("$col[150,150,150]{|}", y, g, b, r);
    }
    
    public static void check(Gob gob) {
	Fightview.Relation rel = map.getOrDefault(gob.id, null);
	if(rel != null) {
	    gob.addCombatInfo(rel);
	}
    }
    
    public static void add(Fightview.Relation rel, UI ui) {
	map.put(rel.gobid, rel);
	if(ui == null) {return;}
	Gob gob = ui.sess.glob.oc.getgob(rel.gobid);
	if(gob != null) {gob.addCombatInfo(rel);}
    }
    
    public static void del(Fightview.Relation rel, UI ui) {
	map.remove(rel.gobid);
	if(ui == null) {return;}
	Gob gob = ui.sess.glob.oc.getgob(rel.gobid);
	if(gob != null) {
	    gob.delattr(GobCombatInfo.class);
	}
    }
    
    public static void clear() {
	map.clear();
    }
    
}
