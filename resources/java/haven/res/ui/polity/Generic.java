package haven.res.ui.polity;

import haven.*;

import static haven.BuddyWnd.*;

public class Generic extends Polity {
    private final int my;
    
    public Generic(String name) {
	super("Polity", name);
	Composer lay = new Composer(this).vmrgn(UI.scale(5));
	lay.add(new Img(CharWnd.catf.i10n_label("Polity").tex()));
	lay.add(new Label.Untranslated(name, nmf));
	lay.add(new AuthMeter(new Coord(width, 20)));
	lay.vmrgn(UI.scale(2)).add(new Label("Members:"));
	lay.vmrgn(UI.scale(5)).add(Frame.with(new MemberList(width - Window.wbox.bisz().x, 7), true));
	pack();
	this.my = lay.y();
    }
    
    public static Widget mkwidget(UI ui, Object[] args) {
	String name = (String) args[0];
	return (new Generic(name));
    }
    
    public void addchild(Widget child, Object... args) {
	if(args[0] instanceof String) {
	    String p = (String) args[0];
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
