package me.ender;

import haven.*;
import haven.rx.CharterBook;
import haven.rx.Reactor;
import me.ender.ui.CFGBox;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

public class WindowDetector {
    public static final String WND_STUDY = "Study";
    public static final String WND_TABLE = "Table";
    public static final String WND_CHARACTER_SHEET = "Character Sheet";
    public static final String WND_SMELTER = "Ore Smelter";
    public static final String WND_FINERY_FORGE = "Finery Forge";
    public static final String WND_STACK_FURNACE = "Stack furnace";
    
    private static final Object lock = new Object();
    private static final Set<Window> toDetect = new HashSet<>();
    private static final Set<Window> detected = new HashSet<>();
    
    static {
	Reactor.WINDOW.subscribe(WindowDetector::onWindowEvent);
    }
    
    public static void process(Widget wdg, Widget parent) {
	if(wdg instanceof Window) {
	    detect((Window) wdg);
	}
	untranslate(wdg, parent);
    }
    
    public static void detect(Window window) {
	synchronized (toDetect) {
	    toDetect.add(window);
	}
    }
    
    private static void onWindowEvent(Pair<Window, String> event) {
	synchronized (lock) {
	    Window window = event.a;
	    if(toDetect.contains(window)) {
		String eventName = event.b;
		switch (eventName) {
		    case Window.ON_DESTROY:
			toDetect.remove(window);
			detected.remove(window);
			break;
		    //Detect window on 'pack' message - this is last message server sends after constructing a window
		    case Window.ON_PACK:
			if(!detected.contains(window)) {
			    detected.add(window);
			    recognize(window);
			}
			break;
		}
	    }
	}
    }
    
    private static void recognize(Window window) {
	if(isWindowType(window, WND_TABLE)) {
	    extendTableWindow(window);
	} else {
	    AnimalFarm.processCattleInfo(window);
	}
    }
    
    private static void untranslate(Widget wdg, Widget parent) {
	Label lbl;
	if(parent instanceof Window) {
	    Window window = (Window) parent;
	    String caption = window.caption();
	    if("Milestone".equals(caption) && wdg instanceof Label) {
		lbl  = (Label) wdg;
		if(!lbl.original.equals("Make new trail:")) {
		    lbl.i10n(false);
		}
	    } else if(isProspecting(caption)) {
	        if(wdg instanceof Label) {
		    lbl = (Label) wdg;
		    ((ProspectingWnd) parent).text(lbl.original);
		} else if(wdg instanceof Button) {
	            ((Button) wdg).large(false);
		}
	    }
	}
    }
    
    public static Widget newWindow(Coord sz, String title, boolean lg) {
	if(isPortal(title)) {
	    return new CharterBook(sz, title, lg);
	} else if(isProspecting(title)) {
	    return new ProspectingWnd(sz, title);
	}
	return (new WindowX(sz, title, lg));
    }
    
    public static String getWindowName(Widget wdg) {
	Window wnd;
	if(wdg == null) {return null;}
	if(wdg instanceof Window) {
	    wnd = (Window) wdg;
	} else {
	    wnd = wdg.getparent(Window.class);
	}
	return wnd == null ? null : wnd.caption();
    }
    
    public static boolean isWindowType(Widget wdg, String... types) {
	if(types == null || types.length == 0) {return false;}
	String wnd = getWindowName(wdg);
	if(wnd == null) {return false;}
	for (String type : types) {
	    if(Objects.equals(type, wnd)) {return true;}
	}
	
	return false;
    }
    
    public static boolean isPortal(String title) {
	return "Sublime Portico".equals(title) || "Charter Stone".equals(title);
    }
    
    public static boolean isBelt(String title) {
	return "Belt".equals(title);
    }
    
    public static boolean isProspecting(String title) {
	return "Prospecting".equals(title);
    }

    private static void extendTableWindow(Window wnd) {
	Button btn = wnd.getchild(Button.class);
	if(btn == null) {return;}
	
	btn.c = wnd.add(new CFGBox("Preserve cutlery", CFG.PRESERVE_SYMBEL), btn.pos("ul"))
	    .settip("Prevent eating from this table if some of the cutlery is almost broken").pos("bl");//.adds(0, 5);
    }
}
