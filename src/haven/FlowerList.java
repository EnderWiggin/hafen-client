package haven;

import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

import java.io.IOException;
import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static haven.FlowerMenu.*;

public class FlowerList extends WidgetList<FlowerList.Item> {
    
    @Override
    protected void drawbg(GOut g) {
	super.drawbg(g);
    }
    
    public FlowerList() {
	super(UI.scale(235, 25), 10);
	
	for (AutoChooseCFG.Opt entry : AUTOCHOOSE.options) {
	    additem(new Item(entry.name));
	}
	
	update();
    }

    @Override
    public void wdgmsg(Widget sender, String msg, Object... args) {
	switch(msg) {
	    case "changed": {
		String name = (String) args[0];
		boolean val = (Boolean) args[1];
		synchronized(AUTOCHOOSE) {
		    AUTOCHOOSE.put(L10N.revertFlower(name), val);
		}
		FlowerMenu.saveAutochoose();
		break;
	    }
	    case "delete": {
		String name = ((Item) sender).name;
		synchronized (AUTOCHOOSE) {
		    AUTOCHOOSE.remove(L10N.revertFlower(name));
		}
		FlowerMenu.saveAutochoose();
		removeitem((Item) sender, true);
		update();
		break;
	    }
	    case "up": {
		String name = ((Item) sender).name;
		synchronized (AUTOCHOOSE) {
		    AUTOCHOOSE.up(L10N.revertFlower(name));
		}
		FlowerMenu.saveAutochoose();
		update();
		break;
	    }
	    case "down": {
		String name = ((Item) sender).name;
		synchronized (AUTOCHOOSE) {
		    AUTOCHOOSE.down(L10N.revertFlower(name));
		}
		FlowerMenu.saveAutochoose();
		update();
		break;
	    }
	    default:
		super.wdgmsg(sender, msg, args);
		break;
	}
    }

    public void add(String name) {
	if(name != null && !name.isEmpty() && !AUTOCHOOSE.has(name)) {
            String originalName = L10N.revertFlower(name);
	    synchronized (AUTOCHOOSE) {
		AUTOCHOOSE.put(L10N.revertFlower(originalName), true);
	    }
	    FlowerMenu.saveAutochoose();
	    additem(new Item(originalName));
	    update();
	}
    }

    private void update() {
	list.sort(Comparator.comparingInt(o -> AUTOCHOOSE.index(o.name)));
	updateChildPositions();
    }

    protected static class Item extends Widget {

	public final String name;
	private final CheckBox cb;
	private boolean a = false;
	private UI.Grab grab;

	public Item(String name) {
	    super(UI.scale(235, 25));
	    this.name = name;

	    cb = adda(new CheckBox.Untranslated(L10N.flower(name)), UI.scale(3, 12), 0, 0.5);
	    cb.a = FlowerMenu.autochoose(name);
	    cb.canactivate = true;

	    add(new Button(UI.scale(24), "X", false), UI.scale(210, 0)).action(() -> wdgmsg("delete"));
	    add(new Button(UI.scale(24), "⇑", false), UI.scale(180, 0)).action(() -> wdgmsg("up"));
	    add(new Button(UI.scale(24), "⇓", false), UI.scale(156, 0)).action(() -> wdgmsg("down"));
	}

	@Override
	public boolean mousedown(MouseDownEvent ev) {
	    if(ev.propagate(this)) {
		return true;
	    }
	    if(ev.b != 1)
		return (false);
	    a = true;
	    grab = ui.grabmouse(this);
	    return (true);
	}

	@Override
	public boolean mouseup(MouseUpEvent ev) {
	    if(a && ev.b == 1) {
		a = false;
		if(grab != null) {
		    grab.remove();
		    grab = null;
		}
		if(ev.c.isect(new Coord(0, 0), sz))
		    click();
		return (true);
	    }
	    return (false);
	}

	private void click() {
	    cb.a = !cb.a;
	    wdgmsg("changed", name, cb.a);
	}

	@Override
	public void wdgmsg(Widget sender, String msg, Object... args) {
	    switch(msg) {
		case "ch":
		    wdgmsg("changed", name, (int) args[0] > 0);
		    break;
		case "activate":
		    wdgmsg("delete", name);
		    break;
		default:
		    super.wdgmsg(sender, msg, args);
		    break;
	    }
	}
    }
    
    public static class AutoChooseCFG {
	private static class Opt {
	    public static final Opt empty = new Opt(null, false);
	    
	    final String name;
	    private boolean enabled;
	    
	    public Opt(String name, boolean enabled) {
		this.name = name;
		this.enabled = enabled;
	    }
	}
	
	private final List<Opt> options = new LinkedList<>();
	
	public void put(String name) {put(name, false);}
	
	public void put(String name, boolean enabled) {
	    Optional<Opt> found = find(name);
	    if(!found.isPresent()) {
		options.add(new Opt(name, enabled));
	    } else {
		found.get().enabled = enabled;
	    }
	}
	
	public void remove(String name) {
	    options.removeIf(opt -> Objects.equals(name, opt.name));
	}
	
	public void up(String name) {
	    Iterator<Opt> iter = options.iterator();
	    int i = 0;
	    while (iter.hasNext()) {
		Opt opt = iter.next();
		if(Objects.equals(opt.name, name)) {
		    if(i > 0) {
			iter.remove();
			options.add(i - 1, opt);
		    }
		    return;
		}
		i++;
	    }
	}
	
	public void down(String name) {
	    Iterator<Opt> iter = options.iterator();
	    int i = 0;
	    int max = options.size() - 1;
	    while (iter.hasNext()) {
		Opt opt = iter.next();
		if(Objects.equals(opt.name, name)) {
		    if(i < max) {
			iter.remove();
			options.add(i + 1, opt);
		    }
		    return;
		}
		i++;
	    }
	}
    
	public int index(String name) {
	    Predicate<Opt> check = byName(name);
	    for (int i = 0; i < options.size(); i++) {
		if(check.test(options.get(i))) {
		    return i;
		}
	    }
	    return -1;
	}
	
	public Optional<Opt> find(String name) {
	    return options.stream().filter(byName(name)).findFirst();
	}
	
	public boolean has(String name) {
	    return options.stream().anyMatch(byName(name));
	}
	
	public boolean active(String name) {
	    return find(name).orElse(Opt.empty).enabled;
	}
	
	public List<String> active() {
	    return options.stream().filter(opt -> opt.enabled).map(opt -> opt.name).collect(Collectors.toList());
	}
	
	public int choose(String[] options) {
	    List<String> active = active();
	    for (String s : active) {
		for (int i = 0; i < options.length; i++) {
		    if(Objects.equals(s, options[i])) {
			return i;
		    }
		}
	    }
	    return -1;
	}
	
	private static Predicate<Opt> byName(String name) {
	    return opt -> opt.name.equals(name) || opt.name.equals(L10N.flower(name));
	}
    }
    
    public static class AutoChooseCFGAdapter extends TypeAdapter<AutoChooseCFG> {
	
	@Override
	public void write(JsonWriter out, AutoChooseCFG cfg) throws IOException {
	    out.beginObject();
	    for (AutoChooseCFG.Opt option : cfg.options) {
		out.name(option.name);
		out.value(option.enabled);
	    }
	    out.endObject();
	}
	
	@Override
	public AutoChooseCFG read(JsonReader in) throws IOException {
	    AutoChooseCFG cfg = new AutoChooseCFG();
	    in.beginObject();
	    while (in.hasNext()) {
		cfg.put(in.nextName(), in.nextBoolean());
	    }
	    in.endObject();
	    return cfg;
	}
    }
}
