package auto;

import haven.*;
import haven.rx.Reactor;
import rx.functions.Action1;

import java.util.*;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;


public class Bot implements Defer.Callable<Void> {
    private static final Object lock = new Object();
    private static Bot current;
    private final List<Target> targets;
    private final BotAction[] actions;
    private Defer.Future<Void> task;
    private boolean cancelled = false;
    
    public Bot(List<Target> targets, BotAction... actions) {
	this.targets = targets;
	this.actions = actions;
    }
    
    @Override
    public Void call() throws InterruptedException {
	targets.forEach(Target::highlight);
	for (Target target : targets) {
	    for (BotAction action : actions) {
		if(target.disposed()) {break;}
		action.call(target);
		checkCancelled();
	    }
	}
	synchronized (lock) {
	    if(current == this) {current = null;}
	}
	return null;
    }
    
    private void run(Action1<String> callback) {
	task = Defer.later(this);
	task.callback(() -> callback.call(task.cancelled() ? "cancelled" : "complete"));
    }
    
    private void checkCancelled() throws InterruptedException {
	if(cancelled) {
	    throw new InterruptedException();
	}
    }
    
    private void markCancelled() {
	cancelled = true;
	task.cancel();
    }
    
    public static void cancel() {
	synchronized (lock) {
	    if(current != null) {
		current.markCancelled();
		current = null;
	    }
	}
    }
    
    private static void start(Bot bot, UI ui) {
	cancel();
	synchronized (lock) { current = bot; }
	bot.run((result) -> ui.message(String.format("Task is %s.", result), GameUI.MsgType.INFO));
    }
    
    public static void pickup(GameUI gui, String filter) {
	pickup(gui, filter, Integer.MAX_VALUE);
    }
    
    public static void pickup(GameUI gui, String filter, int limit) {
	List<Target> targets = gui.ui.sess.glob.oc.stream()
	    .filter(startsWith(filter))
	    .filter(gob -> distanceToPlayer(gob) <= CFG.AUTO_PICK_RADIUS.get())
	    .sorted(byDistance)
	    .limit(limit)
	    .map(Target::new)
	    .collect(Collectors.toList());
    
	start(new Bot(targets,
	    Target::rclick,
	    //selectFlower("Take branch"),
	    selectFlower("Pick"),
	    target -> target.gob.waitRemoval()
	), gui.ui);
    }
    
    public static void pickup_herbs(GameUI gui) {
	pickup(gui,
	    //"gfx/terobjs/items/branch",
	    //"gfx/terobjs/trees/",
	    "gfx/terobjs/herbs/"
	);
    }
    
    public static void selectFlower(GameUI gui, long gobid, String option) {
	List<Target> targets = gui.ui.sess.glob.oc.stream()
	    .filter(gob -> gob.id == gobid)
	    .map(Target::new)
	    .collect(Collectors.toList());
	
	start(new Bot(targets, Target::rclick, selectFlower(option)), gui.ui);
    }
    
    public static void drink(GameUI gui) {
	Collection<Supplier<List<WItem>>> everywhere = Arrays.asList(BELT(gui), HANDS(gui), INVENTORY(gui));
	Utils.chainOptionals(
	    () -> findFirstThatContains("Tea", everywhere),
	    () -> findFirstThatContains("Water", everywhere)
	).ifPresent(Bot::drink);
    }
    
    public static void drink(WItem item) {
	start(new Bot(Collections.singletonList(new Target(item)), Target::rclick, selectFlower("Drink")), item.ui);
    }
    
    private static List<WItem> items(Inventory inv) {
	return inv != null ? inv.children().stream()
	    .filter(widget -> widget instanceof WItem)
	    .map(widget -> (WItem) widget)
	    .collect(Collectors.toList()) : new LinkedList<>();
    }
    
    private static Optional<WItem> findFirstThatContains(String what, Collection<Supplier<List<WItem>>> where) {
	for (Supplier<List<WItem>> place : where) {
	    Optional<WItem> w = place.get().stream()
		.filter(contains(what))
		.findFirst();
	    if(w.isPresent()) {
		return w;
	    }
	}
	return Optional.empty();
    }
    
    private static Predicate<WItem> contains(String what) {
	return w -> w.contains.get().is(what);
    }
    
    
    private static Supplier<List<WItem>> INVENTORY(GameUI gui) {
	return () -> items(gui.maininv);
    }
    
    private static Supplier<List<WItem>> BELT(GameUI gui) {
	return () -> items(gui.beltinv);
    }
    
    private static Supplier<List<WItem>> HANDS(GameUI gui) {
	return () -> {
	    List<WItem> items = new LinkedList<>();
	    if(gui.equipory != null) {
		WItem slot = gui.equipory.slots[Equipory.SLOTS.HAND_LEFT.idx];
		if(slot != null) {
		    items.add(slot);
		}
		slot = gui.equipory.slots[Equipory.SLOTS.HAND_RIGHT.idx];
		if(slot != null) {
		    items.add(slot);
		}
	    }
	    return items;
	};
    }
    
    private static double distanceToPlayer(Gob gob) {
        try {
            Gob p = gob.glob.oc.getgob(gob.glob.sess.ui.gui.plid);
            return p.rc.dist(gob.rc);
        } catch (Exception e) {
            return(0);
        }
    }
    
    public static Comparator<Gob> byDistance = (o1, o2) -> {
	try {
	    Gob p = o1.glob.oc.getgob(o1.glob.sess.ui.gui.plid);
	    return Double.compare(p.rc.dist(o1.rc), p.rc.dist(o2.rc));
	} catch (Exception ignored) {}
	return Long.compare(o1.id, o2.id);
    };
    
    public static BotAction selectFlower(String option) {
	return target -> Reactor.FLOWER.first().subscribe(flowerMenu -> flowerMenu.forceChoose(option));
    }
    
    private static Predicate<Gob> startsWith(String text) {
	return gob -> {
	    try {
		return gob.getres().name.startsWith(text);
	    } catch (Exception ignored) {}
	    return false;
	};
    }
    
    private interface BotAction {
	void call(Target target) throws InterruptedException;
    }
    
    private static class Target {
	private final Gob gob;
	private final WItem item;
	
	public Target(Gob gob) {
	    this.gob = gob;
	    this.item = null;
	}
	
	public Target(WItem item) {
	    this.item = item;
	    this.gob = null;
	}
	
	public void rclick() {
	    if(!disposed()) {
		if(gob != null) {gob.rclick();}
		if(item != null) {item.rclick();}
	    }
	}
	
	public void highlight() {
	    if(!disposed()) {
		if(gob != null) {gob.highlight();}
	    }
	}
	
	public boolean disposed() {
	    return (item != null && item.disposed()) || (gob != null && gob.disposed());
	}
    }
}
