package me.ender;

import haven.*;
import haven.res.ui.tt.wpn.info.WeaponInfo;

import java.util.List;

public class DamageTip {
    
    public static void process(List<ItemInfo> tips, ItemInfo.Owner owner) {
	if(!CFG.IMPROVE_DAMAGE_TIP.get() || !(owner instanceof GItem)) {return;}
	GItem item = (GItem) owner;
	String name = item.resname();
	ItemInfo tip = tips.stream().filter(inf -> Reflect.is(inf, "Damage")).findFirst().orElse(null);
	boolean isMelee = name.endsWith("/cutthroatknuckles") || tips.stream().anyMatch(inf -> Reflect.is(inf, "Range"));
	boolean isRanged = name.endsWith("/sling") || name.endsWith("/huntersbow") || name.endsWith("/rangersbow"); 
	if(tip == null || (!isMelee && !isRanged)) {return;}
	
	tips.remove(tip);
	int dmg = Reflect.getFieldValueInt(tip, "dmg");
	tips.add(new Base(item, dmg));
	tips.add(new Real(item, dmg, isMelee ? "str" : "ranged"));
    }
    
    private static class Base extends WeaponInfo {
	private final int dmg;
	private final GItem item;
	
	
	public Base(GItem owner, int dmg) {
	    super(owner);
	    this.item = owner;
	    this.dmg = dmg;
	}
	
	@Override
	public String wpntips() {
	    return ("Base damage: " + damage());
	}
	
	public int damage() {
	    if(!(owner instanceof GItem)) {return dmg;}
	    
	    QualityList qlist = item.itemq.get();
	    if(qlist.isEmpty()) {return dmg;}
	    QualityList.Quality q = qlist.single();
	    if(q.value <= 0) {return dmg;}
	    return (int) Math.round(dmg / q.multiplier);
	}
	
	@Override
	public int order() {
	    return 50;
	}
    }
    
    private static class Real extends WeaponInfo {
	private final GItem item;
	private final int dmg;
	private final String aname;
	
	
	public Real(GItem owner, int dmg, String attr) {
	    super(owner);
	    this.item = owner;
	    this.dmg = dmg;
	    this.aname = attr;
	}
	
	@Override
	public String wpntips() {
	    return ("Damage: " + damage());
	}
	
	public int damage() {
	    if(!(owner instanceof GItem)) {return dmg;}
	    
	    QualityList q = item.itemq.get();
	    if(q.isEmpty()) {return dmg;}
	    
	    CharWnd charWdg = owner.context(Session.class).ui.gui.chrwdg;
	    Glob.CAttr attr = charWdg.findattr(aname);
	    if(attr == null) {return dmg;}
	    double value = q.single().value;
	    if(value <= 0) {return dmg;}
	    
	    return (int) Math.round(dmg * Math.pow(attr.comp / value, 0.25));
	}
	
	@Override
	public int order() {
	    return 51;
	}
    }
}
