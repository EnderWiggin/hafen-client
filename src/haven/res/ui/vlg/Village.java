/* Preprocessed source code */
/* $use: ui/polity */

package haven.res.ui.vlg;

import haven.*;
import java.util.*;
import haven.res.ui.polity.*;
import java.awt.Color;
import static haven.BuddyWnd.width;

/* >wdg: Village */
@FromResource(name = "ui/vlg", version = 37)
public class Village extends Polity {
    final BuddyWnd.GroupSelector gsel;
    private final int my;

    public Village(String name) {
	super("Village", name);
	Widget prev = add(new Img(CharWnd.catf.i10n_label("Village").tex()));
	
	prev = add(new Label.Untranslated(name, nmf), prev.pos("bl").adds(0, 5));
	prev = add(new AuthMeter(new Coord(width, UI.scale(20))), prev.pos("bl").adds(0, 2));
	prev = add(new Label("Groups:"), prev.pos("bl").adds(0, 15));
	gsel = add(new BuddyWnd.GroupSelector(-1) {
		public void tick(double dt) {
		    if(mw instanceof GroupWidget)
			update(((GroupWidget)mw).id);
		    else
			update(-1);
		}

		public void select(int group) {
		    Village.this.wdgmsg("gsel", group);
		}
	    }, prev.pos("bl").adds(0, 2));
	prev = add(new Label("Members:"), gsel.pos("bl").adds(0, 5));
	prev = add(Frame.with(new MemberList(width, 7), true), prev.pos("bl").adds(0, 2));
	pack();
	this.my = prev.pos("bl").adds(0, 5).y;
    }

    public static Widget mkwidget(UI ui, Object[] args) {
	String name = (String)args[0];
	return(new Village(name));
    }

    public void addchild(Widget child, Object... args) {
	if(args[0] instanceof String) {
	    String p = (String)args[0];
	    if(p.equals("m")) {
		mw = child;
		add(child, 0, my);
		pack();
		return;
	    }
	}
	super.addchild(child, args);
    }
}
