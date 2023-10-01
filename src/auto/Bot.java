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
    private static final Object waiter = new Object();
    
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
	bot.run((result) -> {
	    if (CFG.SHOW_BOT_MESSAGES.get())
	    	ui.message(String.format("Task is %s.", result), GameUI.MsgType.INFO);
	});
    }
    
    public static void pickup(GameUI gui, String filter) {
	pickup(gui, filter, Integer.MAX_VALUE);
    }
    
    public static void pickup(GameUI gui, String filter, int limit) {
	pickup(gui, startsWith(filter), limit);
    }
    
    public static void pickup(GameUI gui, Predicate<Gob> filter) {
	pickup(gui, filter, Integer.MAX_VALUE);
    }
    
    public static void pickup(GameUI gui, Predicate<Gob> filter, int limit) {
	List<Target> targets = gui.ui.sess.glob.oc.stream()
	    .filter(filter)
	    .filter(gob -> distanceToPlayer(gob) <= CFG.AUTO_PICK_RADIUS.get())
	    .filter(Bot::isOnRadar)
	    .sorted(byDistance)
	    .limit(limit)
	    .map(Target::new)
	    .collect(Collectors.toList());
	
	start(new Bot(targets,
	    Target::rclick,
	    selectFlower("Pick"),
	    target -> target.gob.waitRemoval()
	), gui.ui);
    }
    
    public static void pickup(GameUI gui) {
	pickup(gui, has(GobTag.PICKUP));
    }
    
    public static void selectFlower(GameUI gui, long gobid, String option) {
	List<Target> targets = gui.ui.sess.glob.oc.stream()
	    .filter(gob -> gob.id == gobid)
	    .map(Target::new)
	    .collect(Collectors.toList());
	
	selectFlower(gui, option, targets);
    }
    
    public static void selectFlowerOnItems(GameUI gui, String option, List<WItem> items) {
	List<Target> targets = items.stream()
	    .map(Target::new)
	    .collect(Collectors.toList());
    
	selectFlower(gui, option, targets);
    }
    
    public static void selectFlower(GameUI gui, String option, List<Target> targets) {
	start(new Bot(targets, Target::rclick, selectFlower(option)), gui.ui);
    }
    
    public static void drink(GameUI gui) {
	Collection<Supplier<List<WItem>>> everywhere = Arrays.asList(HANDS(gui), INVENTORY(gui), BELT(gui));
	Utils.chainOptionals(
	    () -> findFirstThatContains("Tea", everywhere),
	    () -> findFirstThatContains("Water", everywhere)
	).ifPresent(Bot::drink);
    }
    
    public static void drink(WItem item) {
	start(new Bot(Collections.singletonList(new Target(item)), Target::rclick, selectFlower("Drink")), item.ui);
    }
    
    public static void fuelGob(GameUI gui, String name, String fuel, int count) {
	List<Target> targets = getNearestTargets(gui, name, 1);
	
	if(!targets.isEmpty()) {
	    start(new Bot(targets, fuelWith(gui, fuel, count)), gui.ui);
	}
    }
    
    private static List<Target> getNearestTargets(GameUI gui, String name, int limit) {
	return gui.ui.sess.glob.oc.stream()
	    .filter(gobIs(name))
	    .filter(gob -> distanceToPlayer(gob) <= CFG.AUTO_PICK_RADIUS.get())
	    .sorted(byDistance)
	    .limit(limit)
	    .map(Target::new)
	    .collect(Collectors.toList());
    }
    
    private static BotAction fuelWith(GameUI gui, String fuel, int count) {
	return target -> {
	    Supplier<List<WItem>> inventory = INVENTORY(gui);
	    float has = countItems(fuel, inventory);
	    if(has >= count) {
		for (int i = 0; i < count; i++) {
		    Optional<WItem> w = findFirstItem(fuel, inventory);
		    if(w.isPresent()) {
			w.get().take();
			if(!waitHeld(gui, fuel)) {
			    cancel();
			    return;
			}
			target.interact();
			if(!waitHeld(gui, null)) {
			    cancel();
			    return;
			}
		    } else {
			cancel();
			return;
		    }
		}
	    } else {
		cancel();
	    }
	};
    }
    
    private static boolean isHeld(GameUI gui, String what) throws Loading {
	GameUI.DraggedItem drag = gui.hand();
	if(drag == null && what == null) {
	    return true;
	}
	if(drag != null && what != null) {
	    return drag.item.is2(what);
	}
	return false;
    }
    
    private static boolean waitHeld(GameUI gui, String what) {
	if(Boolean.TRUE.equals(doWaitLoad(() -> isHeld(gui, what)))) {
	    return true;
	}
	if(waitHeldChanged(gui)) {
	    return Boolean.TRUE.equals(doWaitLoad(() -> isHeld(gui, what)));
	}
	return false;
    }
    
    private static boolean waitHeldChanged(GameUI gui) {
	boolean result = true;
	try {
	    synchronized (gui.heldNotifier) {
		gui.heldNotifier.wait(5000);
	    }
	} catch (InterruptedException e) {
	    result = false;
	}
	return result;
    }
    
    private static <T> T doWaitLoad(Supplier<T> action) {
	T result = null;
	boolean ready = false;
	while (!ready) {
	    try {
		result = action.get();
		ready = true;
	    } catch (Loading e) {
		pause(100);
	    }
	}
	return result;
    }
    
    private static void pause(long ms) {
	synchronized (waiter) {
	    try {
		waiter.wait(ms);
	    } catch (InterruptedException ignore) {
	    }
	}
    }
    
    private static void unpause() {
	synchronized (waiter) { waiter.notifyAll(); }
    }
    
    private static List<WItem> items(Widget inv) {
	return inv != null ? new ArrayList<>(inv.children(WItem.class)) : new LinkedList<>();
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
    
    private static Predicate<Gob> gobIs(String what) {
	return g -> {
	    if(g == null) { return false; }
	    String id = g.resid();
	    if(id == null) {return false;}
	    return id.contains(what);
	};
    }
    
    private static float countItems(String what, Supplier<List<WItem>> where) {
	return where.get().stream()
	    .filter(wItem -> wItem.is(what))
	    .map(wItem -> wItem.quantity.get())
	    .reduce(0f, Float::sum);
    }
    
    private static Optional<WItem> findFirstItem(String what, Supplier<List<WItem>> where) {
	return where.get().stream()
	    .filter(wItem -> wItem.is(what))
	    .findFirst();
    }
    
    
    private static Supplier<List<WItem>> INVENTORY(GameUI gui) {
	return () -> items(gui.maininv);
    }
    
    private static Supplier<List<WItem>> BELT(GameUI gui) {
	return () -> {
	    Equipory e = gui.equipory;
	    if(e != null) {
		WItem w = e.slots[Equipory.SLOTS.BELT.idx];
		if(w != null) {
		    return items(w.item.contents);
		}
	    }
	    return new LinkedList<>();
	};
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
    
    private static boolean isOnRadar(Gob gob) {
	if(!CFG.AUTO_PICK_ONLY_RADAR.get()) {return true;}
	GobIcon icon = gob.getattr(GobIcon.class);
	GameUI gui = gob.glob.sess.ui.gui;
	if(icon != null && gui != null) {
	    try {
		GobIcon.Setting s = gui.iconconf.get(icon.res.get());
		return s.show;
	    } catch (Loading ignored) {}
	}
	return true;
    }
    
    private static double distanceToPlayer(Gob gob) {
	Gob p = gob.glob.oc.getgob(gob.glob.sess.ui.gui.plid);
	return p.rc.dist(gob.rc);
    }
    
    public static Comparator<Gob> byDistance = (o1, o2) -> {
	try {
	    Gob p = o1.glob.oc.getgob(o1.glob.sess.ui.gui.plid);
	    return Double.compare(p.rc.dist(o1.rc), p.rc.dist(o2.rc));
	} catch (Exception ignored) {}
	return Long.compare(o1.id, o2.id);
    };
    
    private static BotAction selectFlower(String... options) {
	return target -> {
	    if(target.hasMenu()) {
		FlowerMenu.lastTarget(target);
		Reactor.FLOWER.first().subscribe(flowerMenu -> {
		    Reactor.FLOWER_CHOICE.first().subscribe(choice -> unpause());
		    flowerMenu.forceChoose(options);
		});
		pause(5000);
	    }
	};
    }
    
    private static Predicate<Gob> startsWith(String text) {
	return gob -> {
	    try {
		return gob.getres().name.startsWith(text);
	    } catch (Exception ignored) {}
	    return false;
	};
    }
    
    private static Predicate<Gob> has(GobTag tag) {
	return gob -> gob.is(tag);
    }
    
    private interface BotAction {
	void call(Target target) throws InterruptedException;
    }
    
    public static class Target {
	public final Gob gob;
	public final WItem item;
	
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
    
	public void interact() {
	    if(!disposed()) {
		if(gob != null) {gob.itemact();}
		if(item != null) {/*TODO: implement*/}
	    }
	}
    
	public void highlight() {
	    if(!disposed()) {
		if(gob != null) {gob.highlight();}
	    }
	}
    
	public boolean hasMenu() {
	    if(gob != null) {return gob.is(GobTag.MENU);}
	    return item != null;
	}
    
	public boolean disposed() {
	    return (item != null && item.disposed()) || (gob != null && gob.disposed());
	}
    }
}
