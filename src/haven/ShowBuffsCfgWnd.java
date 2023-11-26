package haven;

import haven.rx.BuffToggles;

public class ShowBuffsCfgWnd extends WindowX {
    private static Window instance;

    public static void toggle(Widget parent) {
	if(instance == null) {
	    instance = parent.add(new ShowBuffsCfgWnd());
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
    
    @Override
    public void close() {
	super.close();
	instance = null;
    }
    
    public ShowBuffsCfgWnd() {
	super(Coord.z, "Show as buffs");
	justclose = true;

	int y = 0;
	for (BuffToggles.Toggle toggle : BuffToggles.toggles) {
	    add(new OptWnd.CFGBox(toggle.name, toggle.show) {
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
