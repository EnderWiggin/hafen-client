/* $use: ui/polity */

import haven.Coord;
import haven.Widget;
import haven.WindowX;

/* >wdg: Realm */
public class Hidewnd extends WindowX {
    Hidewnd(Coord sz, String cap, boolean lg) {
	super(sz, cap, lg);
    }
    
    Hidewnd(Coord sz, String cap) {
	super(sz, cap);
    }
    
    public void wdgmsg(Widget sender, String msg, Object... args) {
	if((sender == this) && msg.equals("close")) {
	    this.hide();
	    return;
	}
	super.wdgmsg(sender, msg, args);
    }
    
    public void show() {
	if(c.x < 0)
	    c.x = 0;
	if(c.y < 0)
	    c.y = 0;
	if(c.x + sz.x > parent.sz.x)
	    c.x = parent.sz.x - sz.x;
	if(c.y + sz.y > parent.sz.y)
	    c.y = parent.sz.y - sz.y;
	super.show();
    }
    
    public void cdestroy(Widget w) {
	reqdestroy();
    }
}
