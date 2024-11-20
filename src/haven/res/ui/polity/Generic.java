/* Preprocessed source code */
package haven.res.ui.polity;

import haven.*;
import java.util.*;
import static haven.BuddyWnd.width;

@FromResource(name = "ui/polity", version = 11)
public class Generic extends Polity {
    private final int my;

    public Generic(String name) {
	super("Polity", name);
	Widget prev = add(new Img(CharWnd.catf.i10n_label("Polity").tex()), 0, 0);
	prev = add(new Label.Untranslated(name, nmf), prev.pos("bl").adds(0, 5));
	prev = add(new AuthMeter(new Coord(width, 20)), prev.pos("bl").adds(0, 2));
	prev = add(new Label("Members:"), prev.pos("bl").adds(0, 5));
	prev = add(Frame.with(new MemberList(width - Window.wbox.bisz().x, 7), true), prev.pos("bl").adds(0, 2));
	pack();
	this.my = prev.pos("bl").adds(0, 10).y;
    }

    public static Widget mkwidget(UI ui, Object[] args) {
	String name = (String)args[0];
	return(new Generic(name));
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
