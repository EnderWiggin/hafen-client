package auto;

import haven.*;

import java.util.*;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class InvHelper {
    
    private static List<WItem> items(Widget inv) {
	return inv != null ? new ArrayList<>(inv.children(WItem.class)) : new LinkedList<>();
    }
    
    static Optional<WItem> findFirstThatContains(String what, Collection<Supplier<List<WItem>>> where) {
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
    
    static float countItems(String what, Supplier<List<WItem>> where) {
	return where.get().stream()
	    .filter(wItem -> wItem.is(what))
	    .map(wItem -> wItem.quantity.get())
	    .reduce(0f, Float::sum);
    }
    
    static boolean isDrinkContainer(ContainedItem item) {
	return isDrinkContainer(item.item);
    }
    
    static boolean isDrinkContainer(WItem item) {
	String resname = item.item.resname();
	return resname.endsWith("/waterskin") || resname.endsWith("/waterflask") || resname.endsWith("/glassjug") || resname.endsWith("/kuksa");
    }
    
    static boolean isNotFull(ContainedItem item) {
	return isNotFull(item.item);
    }
    
    static boolean isNotFull(WItem item) {
	Pair<Double, Double> fullness = item.fullness.get();
	return fullness == null || !Objects.equals(fullness.a, fullness.b);
    }
    
    static Collection<WItem> unstacked(WItem stack) {
	Widget contents = stack.item.contents;
	if(contents != null) {
	    return contents.children(WItem.class);
	}
	return Collections.singleton(stack);
    }
    
    static Supplier<List<WItem>> unstacked(Supplier<List<WItem>> where) {
	return () -> where.get().stream()
	    .map(InvHelper::unstacked)
	    .flatMap(Collection::stream)
	    .collect(Collectors.toList());
    }
    
    
    static Optional<WItem> findFirstItem(String what, Supplier<List<WItem>> where) {
	return where.get().stream()
	    .filter(wItem -> wItem.is(what))
	    .findFirst();
    }
    
    static Supplier<List<WItem>> INVENTORY(GameUI gui) {
	return () -> items(gui.maininv);
    }
    
    static Supplier<List<ContainedItem>> INVENTORY_CONTAINED(GameUI gui) {
	return () -> items(gui.maininv).stream().map(w -> new InventoryItem(w, gui.maininv)).collect(Collectors.toList());
    }
    
    static Supplier<List<WItem>> BELT(GameUI gui) {
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
    
    static Supplier<List<ContainedItem>> BELT_CONTAINED(GameUI gui) {
	return () -> {
	    Equipory e = gui.equipory;
	    if(e != null) {
		WItem w = e.slots[Equipory.SLOTS.BELT.idx];
		if(w != null) {
		    return items(w.item.contents).stream().map(i -> new BeltItem(i, w)).collect(Collectors.toList());
		}
	    }
	    return new LinkedList<>();
	};
    }
    
    static Supplier<List<WItem>> HANDS(GameUI gui) {
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
    
    public static abstract class ContainedItem {
	final WItem item;
	
	ContainedItem(WItem item) {this.item = item;}
	
	public boolean itemDisposed() {return item.disposed();}
	
	public abstract boolean containerDisposed();
	
	public abstract void take();
	
	public abstract void putBack();
    }
    
    private static class InventoryItem extends ContainedItem {
	
	private final Widget parent;
	private final Coord c;
	
	InventoryItem(WItem item, Widget parent) {
	    super(item);
	    this.parent = parent;
	    this.c = item.c.sub(1, 1).div(Inventory.sqsz);
	}
	
	@Override
	public boolean containerDisposed() {
	    return parent.disposed();
	}
	
	@Override
	public void take() {
	    item.item.wdgmsg("take", Coord.z);
	}
	
	@Override
	public void putBack() {
	    parent.wdgmsg("drop", c);
	}
    }
    
    private static class BeltItem extends ContainedItem {
	
	private final WItem belt;
	
	BeltItem(WItem item, WItem belt) {
	    super(item);
	    this.belt = belt;
	}
	
	@Override
	public boolean containerDisposed() {
	    return belt.disposed();
	}
	
	@Override
	public void take() {
	    item.item.wdgmsg("take", Coord.z);
	}
	
	@Override
	public void putBack() {
	    belt.item.wdgmsg("itemact", 0);
	}
    }
}
