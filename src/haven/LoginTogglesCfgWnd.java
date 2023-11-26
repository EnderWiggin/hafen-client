package haven;

import haven.rx.BuffToggles;

public class LoginTogglesCfgWnd extends WindowX {
    private static Window instance;

    public static void toggle(Widget parent) {
	if(instance == null) {
	    instance = parent.add(new LoginTogglesCfgWnd());
	} else {
	    doClose();
	}
    }

    private static void doClose() {
	if(instance != null) {
	    instance.reqdestroy();
	    instance = null;
	}
    }

    public LoginTogglesCfgWnd() {
	super(Coord.z, "Toggle at login");
	justclose = true;

	int y = 0;
	for (BuffToggles.Toggle toggle : BuffToggles.toggles) {
	    add(new OptWnd.CFGBox(toggle.name, toggle.startup) {
		@Override
		protected boolean i10n() {
		    return false;
		}
	    }, 0, y);
	    y += 25;
	}

	pack();
	Coord asz = ca().sz();
	if(asz.x < 120) {
	    resize(new Coord(200, asz.y));
	}
    }

}
