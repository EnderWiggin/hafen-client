package haven;

import java.awt.*;

import static haven.Equipory.*;
import static haven.Inventory.*;

public class EquipProxy extends DraggableWidget implements DTarget2 {
    public static final Color BG_COLOR = new Color(91, 128, 51, 202);
    private Equipory.SLOTS[] slots;
    
    public EquipProxy(Equipory.SLOTS... slots) {
	super("EquipProxy");
	setSlots(slots);
    }
    
    public void setSlots(Equipory.SLOTS... slots) {
	this.slots = slots;
	sz = invsz(new Coord(slots.length, 1));
    }
    
    private Equipory.SLOTS slot(Coord c) {
	int slot = sqroff(c).x;
	if(slot < 0) {slot = 0;}
	if(slot >= slots.length) {slot = slots.length - 1;}
	return slots[slot];
    }
    
    @Override
    public boolean mousehover(Coord c) {
	Equipory e = ui.gui.equipory;
	if(e != null) {
	    WItem w = e.slots[slot(c).idx];
	    if(w != null) {
	    	GItem g = w.item;
		g.hovering_pos = null;
		boolean wasNull = g.contentswdg == null;
		boolean hovered = w.mousehover(Coord.z);
		if(hovered && wasNull && (g.contents != null) && (g.contentswnd == null)) {
		    g.hovering_pos = parentpos(parent, sqroff(c).add(1, 1).mul(sqsz).sub(5, 5).sub(GItem.Contents.hovermarg));
		}
		return hovered;
	    }
	}
	return false;
    }
    
    @Override
    public boolean mousedown(Coord c, int button) {
	Equipory e = ui.gui.equipory;
	if(e != null) {
	    WItem w = e.slots[slot(c).idx];
	    if(w != null) {
		w.mousedown(Coord.z, button);
		return true;
	    }
	}
	return super.mousedown(c, button);
    }
    
    @Override
    public void draw(GOut g) {
	Equipory equipory = ui.gui.equipory;
	if(equipory != null) {
	    int k = 0;
	    g.chcolor(BG_COLOR);
	    g.frect(Coord.z, sz);
	    g.chcolor();
	    Coord c0 = new Coord(0, 0);
	    for (Equipory.SLOTS slot : slots) {
		c0.x = k;
		Coord c1 = sqoff(c0);
		g.image(invsq, c1);
		WItem w = equipory.slots[slot.idx];
		if(w != null) {
		    w.draw(g.reclipl(c1, invsq.sz()));
		} else if(ebgs[slot.idx] != null) {
		    g.image(ebgs[slot.idx], c1);
		}
		k++;
	    }
	}
    }
    
    @Override
    public Object tooltip(Coord c, Widget prev) {
	Equipory e = ui.gui.equipory;
	if(e != null) {
	    Equipory.SLOTS slot = slot(c);
	    WItem w = e.slots[slot.idx];
	    if(w != null) {
		return w.tooltip(c, (prev == this) ? w : prev);
	    } else {
		return etts[slot.idx];
	    }
	}
	return super.tooltip(c, prev);
    }
    
    @Override
    public boolean drop(WItem target, Coord cc, Coord ul) {
	Equipory e = ui.gui.equipory;
	if(e != null) {
	    e.wdgmsg("drop", slot(cc).idx);
	    return true;
	}
	return false;
    }
    
    @Override
    public boolean iteminteract(WItem target, Coord cc, Coord ul) {
	Equipory e = ui.gui.equipory;
	if(e != null) {
	    WItem w = e.slots[slot(cc).idx];
	    if(w != null) {
		return w.iteminteract(target, cc, ul);
	    }
	}
	return false;
    }
    
    public void activate(Equipory.SLOTS slot, int button) {
	for (int i = 0; i < slots.length; i++) {
	    if(slots[i] == slot) {
		activate(i, button);
		return;
	    }
	}
    }
    
    private void activate(int i, int button) {
	Coord mc = ui.mc;
	Coord c = sqoff(new Coord(i, 0)).add(rootpos());
	ui.mousedown(c, button);
	ui.mouseup(c, button);
	ui.mousemove(mc);
    }
}
