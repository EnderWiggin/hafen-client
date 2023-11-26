package haven.rx;

import haven.*;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class BuffToggles {
    public static final Collection<Toggle> toggles = new ArrayList<>();
    private static final Collection<String> filters;

    static {
	toggles.add(new Toggle("Tracking", "paginae/act/tracking", "tracking", "Tracking is now turned on.", "Tracking is now turned off."));
	toggles.add(new Toggle("Criminal Acts", "paginae/act/crime", "crime", "Criminal acts are now turned on.", "Criminal acts are now turned off."));
	toggles.add(new Toggle("Swimming", "paginae/act/swim", "swim", "Swimming is now turned on.", "Swimming is now turned off."));

	filters = toggles.stream()
	    .flatMap(toggle -> Stream.of(toggle.msgOn, toggle.msgOff))
	    .collect(Collectors.toCollection(HashSet::new));
    }

    public static void init(GameUI gameUI) {
	Reactor.IMSG.filter(filters::contains)
	    .subscribe(BuffToggles::toggle);

	toggles.forEach(toggle -> toggle.setup(gameUI));
    }

    private static void toggle(String msg) {
	toggles.stream().filter(t -> t.matches(msg)).findFirst().ifPresent(toggle -> toggle.update(msg));
    }

    public static class Toggle implements CFG.Observer<Boolean>, Observer {

	public final String name;
	private final String resname;
	public final String action;
	private final String msgOn;
	private final String msgOff;
	private boolean state = false;
	private Buff buff;
	public CFG<Boolean> show;
	public CFG<Boolean> startup;
	private GameUI gui;
	private boolean toggled = false;

	public Toggle(String name, String resname, String action, String msgOn, String msgOff) {
	    this.name = L10N.tooltip(resname, name);
	    this.resname = resname;

	    this.action = action;
	    this.msgOn = msgOn;
	    this.msgOff = msgOff;
	}

	public boolean matches(String msg) {
	    return msg.equals(msgOn) || msg.equals(msgOff);
	}

	public void update(String msg) {
	    state = msg.equals(msgOn);
	    update();
	}

	private void update() {
	    if(gui == null) {return;}
	    if(state && show.get()) {
		if(buff == null) {
		    buff = new TBuff(this);
		    gui.buffs.addchild(buff);
		}
	    } else {
		if(buff != null) {
		    gui.ui.destroy(buff);
		    buff = null;
		}
	    }
	}

	public void act() {
	    if(gui.menu != null) {
		gui.menu.senduse(action);
	    } else {
		toggled = !toggled;
	    }
	}

	public void cfg(CFG<Boolean> show, CFG<Boolean> startup) {
	    this.show = show;
	    this.startup = startup;

	    show.observe(this);
	}

	@Override
	public void updated(CFG<Boolean> cfg) {
	    update();
	}

	public void setup(GameUI gameUI) {
	    gui = gameUI;
	    if(gui.menu == null) {
		gui.menuObservable.addObserver(this);
	    }
	    if(startup.get()) {
		act();
	    }
	}

	@Override
	public void update(Observable o, Object arg) {
	    gui.menuObservable.deleteObserver(this);
	    if(toggled) {
		act();
	    }
	}
    }

    private static class TBuff extends Buff {
	private final Toggle toggle;

	public TBuff(Toggle toggle) {
	    super(Resource.remote().load(toggle.resname));
	    this.toggle = toggle;
	}

	@Override
	public boolean mousedown(Coord c, int btn) {
	    toggle.act();
	    return true;
	}
    }
}
