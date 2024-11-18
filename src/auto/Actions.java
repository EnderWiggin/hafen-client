package auto;

import haven.*;
import me.ender.ClientUtils;

import java.util.*;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static auto.GobHelper.*;
import static auto.InvHelper.*;

public class Actions {
    public static void fuelGob(GameUI gui, String name, String fuel, int count) {
	List<ITarget> targets = getNearest(gui, name, 1, 33);
	
	if(!targets.isEmpty()) {
	    Bot.process(targets).actions(fuelWith(gui, fuel, count)).start(gui.ui);
	} else {
	    gui.error("Cannot find target to add fuel to");
	}
    }
    
    public static void pickup(GameUI gui, String filter) {
	pickup(gui, filter, Integer.MAX_VALUE);
    }
    
    static void pickup(GameUI gui, String filter, int limit) {
	pickup(gui, resIdStartsWith(filter), limit);
    }
    
    static void pickup(GameUI gui, Predicate<Gob> filter) {
	pickup(gui, filter, Integer.MAX_VALUE);
    }
    
    static void pickup(GameUI gui, Predicate<Gob> filter, int limit) {
	List<ITarget> targets = gui.ui.sess.glob.oc.stream()
	    .filter(filter)
	    .filter(gob -> PositionHelper.distanceToPlayer(gob) <= CFG.AUTO_PICK_RADIUS.get())
	    .filter(BotUtil::isOnRadar)
	    .sorted(PositionHelper.byDistanceToPlayer)
	    .limit(limit)
	    .map(GobTarget::new)
	    .collect(Collectors.toList());
	
	Bot.process(targets).actions(
	    ITarget::rclick_shift,
	    (target, bot) -> Targets.gob(target).waitRemoval()
	).start(gui.ui);
    }
    
    public static void pickup(GameUI gui) {
	pickup(gui, gobIs(GobTag.PICKUP));
    }
    
    public static void openGate(GameUI gui) {
	List<ITarget> targets = gui.ui.sess.glob.oc.stream()
	    .filter(gobIs(GobTag.GATE))
	    .filter(gob -> !gob.isVisitorGate())
	    .filter(gob -> PositionHelper.distanceToPlayer(gob) <= 35)
	    .sorted(PositionHelper.byDistanceToPlayer)
	    .limit(1)
	    .map(GobTarget::new)
	    .collect(Collectors.toList());
	
	Bot.process(targets).actions(ITarget::rclick).start(gui.ui, true);
    }
    
    public static void refillDrinks(GameUI gui) {
	if(gui.hand() != null || gui.cursor != null) {
	    gui.error("You must have empty cursor to refill drinks!");
	    return;
	}
	
	Coord2d waterTile = null;
	Gob barrel = null;
	boolean needWalk = false;
	Gob player = gui.map.player();
	Bot.BotAction interact;
	
	if(MapHelper.isPlayerOnFreshWaterTile(gui)) {
	    waterTile = player.rc;
	} else {
	    needWalk = true;
	    List<ITarget> objs = getNearest(gui, 1, 32, GobTag.HAS_WATER);
	    if(!objs.isEmpty()) {
		barrel = Targets.gob(objs.get(0));
	    }
	    if(barrel == null) {
		waterTile = MapHelper.nearbyWaterTile(gui);
	    }
	}
	
	final Coord2d tile = barrel != null ? barrel.rc : waterTile;
	
	if(waterTile != null) {
	    interact = (t, b) -> gui.map.wdgmsg("itemact", Coord.z, tile.floor(OCache.posres), 0);
	} else if(barrel != null) {
	    final Gob gob = barrel;
	    interact = (t, b) -> gob.itemact(UI.MOD_META);
	} else {
	    gui.error("You must be near tile or barrel with fresh water to refill drinks!");
	    return;
	}

	List<ITarget> targets = Stream.of(
		POUCHES_CONTAINED(gui).get().stream().filter(InvHelper::isDrinkContainer),
		INVENTORY_CONTAINED(gui).get().stream().filter(InvHelper::isDrinkContainer),
		BELT_CONTAINED(gui).get().stream().filter(InvHelper::isDrinkContainer),
		HANDS_CONTAINED(gui).get().stream().filter(InvHelper::isBucket)
	    ).flatMap(x -> x)
	    .filter(x -> InvHelper.canBeFilledWith(x, ItemData.WATER))
	    .map(ContainedTarget::new)
	    .collect(Collectors.toList());
	
	if(targets.isEmpty()) {
	    gui.error("No non-full drink containers to refill!");
	    return;
	}
	
	Bot refillBot = Bot.process(targets).actions(
	    ITarget::take,
	    BotUtil.WaitHeldChanged,
	    interact,
	    BotUtil.doWait(70),
	    ITarget::putBack,
	    BotUtil.WaitHeldChanged
	);
	if(needWalk) {
	    refillBot.setup(
		(t, b) -> gui.map.click(tile, 1, Coord.z, tile.floor(OCache.posres), 1, 0),
		waitGobPose(player, 1500, "/walking", "/running"),
		waitGobNoPose(player, 1500, "/walking", "/running")
	    );
	}
	refillBot.start(gui.ui, true);
    }
    
    public static void selectFlower(GameUI gui, long gobid, String option) {
	List<ITarget> targets = gui.ui.sess.glob.oc.stream()
	    .filter(gob -> gob.id == gobid)
	    .map(GobTarget::new)
	    .collect(Collectors.toList());
	
	selectFlower(gui, option, targets);
    }
    
    public static void selectFlowerOnItems(GameUI gui, String option, List<WItem> items) {
	List<ITarget> targets = items.stream()
	    .map(ItemTarget::new)
	    .collect(Collectors.toList());
	
	selectFlower(gui, option, targets);
    }
    
    public static void selectFlower(GameUI gui, String option, List<ITarget> targets) {
	Bot.process(targets)
	    .actions(ITarget::rclick, BotUtil.selectFlower(option))
	    .start(gui.ui);
    }
    
    public static void drink(GameUI gui) {
	Collection<Supplier<List<WItem>>> everywhere = Arrays.asList(HANDS(gui), POUCHES(gui), INVENTORY(gui), BELT(gui));
	ClientUtils.chainOptionals(
	    () -> findFirstMatching(HAS_TEA, everywhere),
	    () -> findFirstMatching(HAS_WATER, everywhere)
	).ifPresent(Actions::drink);
    }
    
    public static void drink(WItem item) {
	Bot.process(Targets.of(item))
	    .actions(ITarget::rclick, BotUtil.selectFlower("Drink"))
	    .start(item.ui, true);
    }

    public static void craftCount(Makewindow make, int count) {
	Bot.execute((target, bot) -> {
	    int remaining = count;
	    while (remaining > 0) {
		if(make.disposed()) {bot.cancel();}
		make.wdgmsg("make", 0);
		if(!BotUtil.waitProgress(bot, 1000, 60000)) {bot.cancel();}
		remaining--;
	    }
	}).start(make.ui, true);
    }

    public static void mountClosestHorse(GameUI gui) {
	List<ITarget> targets = getGobs(gui, 1, PositionHelper.byDistanceToPlayer,
	    gob -> gob.anyOf(GobTag.MARE, GobTag.STALLION)
		&& !gob.anyOf(GobTag.DEAD, GobTag.KO)
		&& gob.occupants.isEmpty());

	Bot.process(targets).actions(
	    (target, bot) -> {
		Gob gob = ((GobTarget) target).gob;

		if(PositionHelper.distanceToPlayer(gob) < 20) {return;}
		
		Coord mc = gob.rc.floor(OCache.posres);
		bot.ui.gui.menu.wdgmsg("act", "pose", "whistle", 0, mc, 0, gob.id, mc, 0, -1);

		//wait for horse to be close
		long timeout = 3000;
		while (timeout > 0 && !gob.disposed() && PositionHelper.distanceToPlayer(gob) > 15.0) {
		    BotUtil.pause(20);
		    timeout -= 20;
		}
	    },
	    ITarget::rclick,
	    BotUtil.selectFlower("Giddyup!")
	).start(gui.ui, true);
    }
    
    public static void aggroOnePVE(GameUI gui) {aggroOne(gui, false);}
    
    public static void aggroOnePVP(GameUI gui) {aggroOne(gui, true);}
    
    static void aggroOne(GameUI gui, boolean pvp) {
	final Predicate<Gob> filter = pvp
	    ? gobIs(GobTag.PLAYER)
	    : gobIsNot(GobTag.PLAYER);
	
	PositionHelper.mapPosOfMouse(gui)
	    .thenAccept(mc -> aggro(gui, getNearestToPoint(gui, 1, mc, 33,
		gobIsAny(GobTag.AGGRO_TARGET, GobTag.IN_COMBAT), filter, GobHelper::isNotFriendlySteed)));
    }
    
    public static void aggroAll(GameUI gui) {
	aggro(gui, getNearest(gui, Integer.MAX_VALUE, 165, gobIs(GobTag.PLAYER), gobIs(GobTag.AGGRO_TARGET), GobHelper::isNotFriendlySteed));
    }

    public static void reAggroKritter(GameUI gui, long gobId) {
	Gob target = gui.map.glob.oc.getgob(gobId);
	if(target == null || target.anyOf(GobTag.KO, GobTag.DEAD)) {return;}
	if(PositionHelper.distanceToPlayer(target) > 200) {return;}
	String resid = target.resid();
	if(resid == null || !resid.contains("gfx/kritter/")) {return;}
	aggro(gui, Targets.of(target));
    }
    
    static void aggro(GameUI gui, List<ITarget> targets) {
	if(targets.isEmpty()) {
	    gui.error("No targets to aggro");
	    return;
	}
	Bot.process(targets)
	    .setup((t, b) -> gui.menu.paginafor("paginae/act/atk").button().use())
	    .actions(
		(target, bot) -> target.click(1, 0),
		BotUtil.doWait(65)//TODO: wait for relations change?
	    )
	    .cleanup((t, b) -> BotUtil.rclick(gui), ((t, b) -> gui.pathQueue.clear()))
	    .highlight(false)
	    .start(gui.ui, true);
    }
    
    private static Bot.BotAction fuelWith(GameUI gui, String fuel, int count) {
	return (target, bot) -> {
	    Supplier<List<WItem>> inventory = unstacked(INVENTORY(gui));
	    float has = countItems(fuel, inventory);
	    if(has < count) {
		bot.cancel(String.format("Not enough '%s' in inventory: found %d, need: %d", fuel, (int) has, count));
		return;
	    }
	    for (int i = 0; i < count; i++) {
		Optional<WItem> w = findFirstItem(fuel, inventory);
		if(!w.isPresent()) {
		    bot.cancel("no fuel in inventory");
		    return;
		}
		w.get().take();
		if(!BotUtil.waitHeld(gui, fuel)) {
		    bot.cancel("no fuel on cursor");
		    return;
		}
		target.interact();
		if(!BotUtil.waitHeld(gui, null)) {
		    bot.cancel("cursor is not empty");
		    return;
		}
	    }
	};
    }
}
