/* $use: ui/polity */

import haven.*;
import haven.res.ui.polity.GroupWidget;

import static haven.BuddyWnd.*;

/* >wdg: Village */
public class Village extends Polity {
    final BuddyWnd.GroupSelector gsel;
    private final int my;
    
    public Village(String name) {
	super("Village", name);
	Composer lay = new Composer(this).vmrgn(UI.scale(5));
	lay.add(new Img(CharWnd.catf.i10n_label("Village").tex()));
	
	lay.add(new Label.Untranslated(name, nmf));
	lay.add(new AuthMeter(new Coord(width, UI.scale(20))));
	lay.vmrgn(UI.scale(2)).add(new Label("Groups:"));
	gsel = lay.vmrgn(UI.scale(5)).add(new BuddyWnd.GroupSelector(-1) {
	    public void tick(double dt) {
		if(mw instanceof GroupWidget)
		    update(((GroupWidget) mw).id);
		else
		    update(-1);
	    }
	    
	    public void select(int group) {
		Village.this.wdgmsg("gsel", group);
	    }
	});
	lay.vmrgn(UI.scale(2)).add(new Label("Members:"));
	lay.vmrgn(UI.scale(5)).add(Frame.with(new MemberList(width, 7), true));
	pack();
	this.my = lay.y();
    }
    
    public static Widget mkwidget(UI ui, Object[] args) {
	String name = (String) args[0];
	return (new Village(name));
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
