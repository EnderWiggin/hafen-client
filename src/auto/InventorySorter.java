package auto;

import haven.*;
import rx.functions.Action1;

import java.util.*;
import java.util.stream.Collectors;

public class InventorySorter implements Defer.Callable<Void> {
    private static final Set<String> EXCLUDES;
    static {
	EXCLUDES = Collections.unmodifiableSet(new HashSet<>(Arrays.asList(
	    "Character Sheet",
	    "Study",

	    "Chicken Coop",
	    "Belt",
	    "Pouch",
	    "Purse",

	    "Cauldron",
	    "Finery Forge",
	    "Fireplace",
	    "Frame",
	    "Herbalist Table",
	    "Kiln",
	    "Ore Smelter",
	    "Smith's Smelter",
	    "Oven",
	    "Pane mold",
	    "Rack",
	    "Smoke shed",
	    "Stack Furnace",
	    "Steelbox",
	    "Tub"
	)));
    }

    private static final Object lock = new Object();
    private static InventorySorter current;
    private Defer.Future<Void> task;
    private boolean cancelled = false;

    private List<Object[]> sorteditems = new ArrayList<>();
    private final List<Inventory> inventories;

    public InventorySorter(List<Inventory> inv) {
	this.inventories = inv;
    }

    public static void sortInventory(List<Inventory>  inv, GameUI gui) {
	start(new InventorySorter(inv), gui);
    }

    @Override
    public Void call() throws InterruptedException {
	for(Inventory inv : inventories) {
	    if (inv != null)
		sortInv(inv);
	}
	synchronized (lock) {
	    if(current == this) {current = null;}
	}
	return null;
    }

    private void sortInv(Inventory inv) {
	boolean[][] invgrid = new boolean[inv.isz.x][inv.isz.y];
	List<WItem> items = new ArrayList<>();
	for (Widget wdg = inv.lchild; wdg != null; wdg = wdg.prev) {
	    if (wdg.visible && wdg instanceof WItem) {
		WItem wItem = (WItem) wdg;
		Coord sz = wItem.lsz;
		Coord loc = wItem.c.sub(1, 1).div(Inventory.sqsz);
		if (sz.x * sz.y == 1)
		    items.add(wItem);
		else
		    for (int x = 0; x < sz.x; x++)
			for (int y = 0; y < sz.y; y++)
			    invgrid[loc.x + x][loc.y + y] = true;
	    }
	}
	sorteditems = items.stream()
	    .filter(witem -> witem.lsz.x * witem.lsz.y == 1)
	    .sorted(Comparator.comparing(InventorySorter::nameforsorting)
		.thenComparing(WItem::quality, Comparator.reverseOrder()))
	    .map(witem -> new Object[] {witem, witem.c.sub(1, 1).div(Inventory.sqsz), new Coord(0, 0)})
	    .collect(Collectors.toList());
	int cur_x = -1, cur_y = 0;
	for (Object[] a : sorteditems) {
	    while (true) {
		cur_x += 1;
		if (cur_x == inv.isz.x) {
		    cur_x = 0;
		    cur_y += 1;
		    if (cur_y == inv.isz.y)
			break;
		}
		if (!invgrid[cur_x][cur_y]) {
		    a[2] = new Coord(cur_x, cur_y);
		    break;
		}
	    }
	    if (cur_y == inv.isz.y)
		break;
	}

	Object[] handu = null;
	for (Object[] a : sorteditems) {
	    if (a[1].equals(a[2])) // item in right place
		continue;
	    ((WItem) a[0]).take(); // item in wrong place, take it
	    handu = a;
	    while (handu != null) {
		inv.wdgmsg("drop", handu[2]); // place item in right place
		// find item in new pos
		Object[] b = null;
		for (Object[] x : sorteditems) {
		    if(x[1].equals(handu[2])) {
			b = x;
			break;
		    }
		}
		handu[1] = handu[2]; // update item position
		handu = b;
	    }
	}
    }

    private void run(Action1<String> callback) {
	task = Defer.later(this);
	task.callback(() -> callback.call(task.cancelled() ? "cancelled" : "complete"));
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

    private static void start(InventorySorter inventorySorter, GameUI gui) {
	cancel();
	synchronized (lock) { current = inventorySorter; }
	inventorySorter.run((result) -> {
	    if (result.equals("cancelled"))
		gui.ui.message(String.format("Sort is %s.", result), GameUI.MsgType.INFO);
	});
    }

    public static String nameforsorting(WItem item) {
	try {
	    return item.item.resname();
	} catch (Loading e) {
	    String name = item.getname();
	    int a = name.indexOf("seed");
	    return a > 0 ? name.substring(a + (name.contains("seeds") ? 5 : 4)) : name;
	}
    }

    public static void SortAll(GameUI gui) {
	List<Inventory> invs = new ArrayList<>();
	for (Widget w : gui.ui.getwidgets(ExtInventory.class)) {
	    if (w == null) continue;
	    WindowX window = w.getparent(WindowX.class);
	    if (window != null && !EXCLUDES.contains(window.caption())) {
		if (((ExtInventory) w).inv != null)
		    invs.add(((ExtInventory) w).inv);
	    }
	}
	if (invs.size() > 0)
	    start(new InventorySorter(invs), gui);
    }
}