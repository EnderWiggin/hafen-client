/* Preprocessed source code */
/* $use: ui/polity */

package haven.res.ui.realm;

import haven.*;
import java.util.*;
import haven.res.ui.polity.*;
import static haven.BuddyWnd.width;

/* >wdg: Realm */
@FromResource(name = "ui/realm", version = 30)
public class Hidewnd extends Window {
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
