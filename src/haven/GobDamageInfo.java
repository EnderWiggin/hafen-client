package haven;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class GobDamageInfo extends GobInfo {
    private static final int SHP = 61455;
    private static final int HHP = 64527;
    private static final int ARM = 36751;
    private static final int PAD = UI.scale(3);
    
    private static final Color BG = new Color(0, 0, 0, 128);
    private static final Color SHP_C = Utils.col16(SHP);
    private static final Color HHP_C = Utils.col16(HHP);
    private static final Color ARM_C = Utils.col16(ARM);
    
    private static final Map<Long, DamageVO> gobDamage = new ConcurrentHashMap<>();
    
    private final DamageVO damage;
    
    public GobDamageInfo(Gob owner) {
	super(owner);
	up(12);
	center = new Pair<>(0.5, 1.0);
	if(gobDamage.containsKey(gob.id)) {
	    damage = gobDamage.get(gob.id);
	} else {
	    damage = new DamageVO();
	    gobDamage.put(gob.id, damage);
	}
    }
    
    @Override
    protected boolean enabled() {
	return CFG.SHOW_COMBAT_DMG.get();
    }
    
    @Override
    protected Tex render() {
	if(damage.isEmpty()) {return null;}
	
	BufferedImage hhp = null, shp = null, arm = null;
	if(damage.shp > 0) {
	    hhp = Text.std.render(String.format("%d", damage.shp), SHP_C).img;
	}
	if(damage.hhp > 0) {
	    shp = Text.std.render(String.format("%d", damage.hhp), HHP_C).img;
	}
	if(damage.armor > 0) {
	    arm = Text.std.render(String.format("%d", damage.armor), ARM_C).img;
	}
	return new TexI(ItemInfo.catimgsh(PAD, PAD, BG, hhp, shp, arm));
    }
    
    public void update(int c, int v) {
//	Debug.log.println(String.format("Number %d, c: %d", v, c));
	//35071 - Initiative
	if(c == SHP) {
	    damage.shp += v;
	    update();
	} else if(c == HHP) {
	    damage.hhp += v;
	    update();
	} else if(c == ARM) {
	    damage.armor += v;
	    update();
	}
    }
    
    private void update() {
	gobDamage.put(gob.id, damage);
	clean();
    }
    
    public static boolean has(Gob gob) {
	return gobDamage.containsKey(gob.id);
    }
    
    private static void clearDamage(Gob gob, long id) {
	if(gob != null) {
	    gob.clearDmg();
	}
	gobDamage.remove(id);
    }
    
    public static void clearPlayerDamage(GameUI gui) {
	clearDamage(gui.ui.sess.glob.oc.getgob(gui.plid), gui.plid);
    }
    
    public static void clearAllDamage(GameUI gui) {
	ArrayList<Long> gobIds = new ArrayList<>(gobDamage.keySet());
	for (Long id : gobIds) {
	    if(id == null) {continue;}
	    clearDamage(gui.ui.sess.glob.oc.getgob(id), id);
	}
    }
    
    private static class DamageVO {
	int shp = 0, hhp = 0, armor = 0;
	
	boolean isEmpty() {return shp == 0 && hhp == 0 && armor == 0;}
	
    }
}
