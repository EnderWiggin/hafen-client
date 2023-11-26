package me.ender;

import haven.*;
import haven.res.ui.tt.wpn.info.WeaponInfo;

import java.util.List;

public class DamageTip {
    
    public static void process(List<ItemInfo> tips) {
	if(!CFG.IMPROVE_DAMAGE_TIP.get()) {return;}
	ItemInfo tip = tips.stream().filter(inf -> Reflect.is(inf, "Damage")).findFirst().orElse(null);
	if(tip == null) {return;}
	
	tips.remove(tip);
	int dmg = Reflect.getFieldValueInt(tip, "dmg");
	tips.add(new Base(tip.owner, dmg));
	tips.add(new Real(tip.owner, dmg));
    }
    
    private static class Base extends WeaponInfo {
	private final int dmg;
	
	
	public Base(Owner owner, int dmg) {
	    super(owner);
	    this.dmg = dmg;
	}
	
	@Override
	public String wpntips() {
	    return ("Base damage: " + damage());
	}
	
	public int damage() {
	    if(!(owner instanceof GItem)) {return dmg;}
	    
	    GItem item = (GItem) owner;
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
	private final int dmg;
	
	
	public Real(Owner owner, int dmg) {
	    super(owner);
	    this.dmg = dmg;
	}
	
	@Override
	public String wpntips() {
	    return ("Damage: " + damage());
	}
	
	public int damage() {
	    if(!(owner instanceof GItem)) {return dmg;}
	    
	    GItem item = (GItem) owner;
	    QualityList q = item.itemq.get();
	    if(q.isEmpty()) {return dmg;}
	    
	    CharWnd charWdg = owner.context(Session.class).ui.gui.chrwdg;
	    Glob.CAttr attr = charWdg.findattr("str");
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
