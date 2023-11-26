package haven.purus.pbot.api;

import java.awt.*;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicReference;

import haven.*;
import haven.purus.Config;
import haven.purus.pathfinder.Pathfinder;
import haven.purus.pbot.api.callback.AreaSelectCallback;
import haven.purus.pbot.api.callback.WItemCallback;
import haven.purus.pbot.api.subroutine.DrinkWater;

import static java.lang.Thread.currentThread;

public class PBotUtils {
	private PBotSession pBotSession;

	PBotUtils(PBotSession pBotSession) {
		this.pBotSession = pBotSession;
	}

	/**
	 * Send message to be displayed in the client
	 * @param msg Message content
	 */
	public void sysMsg(String msg) {
		pBotSession.gui.msg(msg);
	}

	/**
	 * Send message to be displayed in the client
	 * @param msg Message content
	 * @param r Red color 0-255
	 * @param g Green color 0-255
	 * @param b Blue color 0-255
	 */
	public void sysMsg(String msg, int r, int g, int b) {
		sysMsg(msg, new Color(r, g, b));
	}

	public void sysMsg(String msg, Color color) {
		pBotSession.gui.msg(msg, color);
	}

	/**
	 * Waits until the flowermenu appears with timeout
	 * @param timeout Timeout to wait before returning null if menu is not opened
	 * @return Flowermenu that was opened
	 */
	public PBotFlowerMenu getFlowermenu(long timeout) {
		long start = System.currentTimeMillis();
		try {
			while(System.currentTimeMillis() - start < timeout) {
				FlowerMenu menu = pBotSession.gui.ui.root.getchild(FlowerMenu.class);
				if(menu != null)
					return new PBotFlowerMenu(menu);
				Thread.sleep(30);
			}
		} catch(InterruptedException ie) {}
		return null;
	}

	public boolean isFlowerMenuOpen() {
		return pBotSession.gui.ui.root.getchild(FlowerMenu.class) != null;
	}

	/**
	 * Itemact with item in hand, for example, to plant a crop or tree
	 * @param x x to click to
	 * @param y y to click to
	 * @param mod modifier for example 1 = shift etc
	 */
	public void itemact(double x, double y, int mod) {
		pBotSession.gui.map.wdgmsg("itemact", Coord.z, new Coord2d(x, y).floor(OCache.posres), mod);
	}

	/**
	 * Use to place something, for example, a stockpile
	 * @param x x place stockpile to
	 * @param y y place stockpile to
	 */
	public void placeThing(double x, double y) {
		pBotSession.gui.map.wdgmsg("place", new Coord2d(x, y).floor(OCache.posres), 0, 1, 0);
	}

	/**
	 * Click some place on map
	 * @param x x-coordinate
	 * @param y y-coordinate
	 * @param btn 1 = left click, 3 = right click
	 * @param mod 1 = shift, 2 = ctrl, 4 = alt
	 */
	public void mapClick(double x, double y, int btn, int mod) {
		pBotSession.gui.map.wdgmsg("click", getCenterScreenCoord(), new Coord2d(x, y).floor(OCache.posres), btn, mod);
	}

	/**
	 * Use to cancel stockpile placing for example
	 */
	public void cancelPlace() {
		pBotSession.gui.map.wdgmsg("place", pBotSession.gui.map.player().rc.floor(OCache.posres), 0, 3, 0);
	}

	/**
	 * Coordinates of the center of the screen
	 * @return Coordinates of the center of the screen
	 */
	public Coord getCenterScreenCoord() {
		return pBotSession.gui.map.sz.div(2);
	}

	/**
	 * Wait for the given time
	 * @param time time in milliseconds
	 */
	public static void sleep(int time) {
		try {
			Thread.sleep(time);
		} catch(InterruptedException e) {
			Thread.currentThread().interrupt();
			e.printStackTrace();
		}
	}

	/**
	 * Returns the item currently in the hand
	 * @return Item at hand
	 */
	public PBotItem getItemAtHand() {
		if(pBotSession.gui.vhand == null)
			return null;
		else
			return new PBotItem(pBotSession.gui.vhand.item, pBotSession);
	}

	/**
	 * Left click to somewhere with pathfinder
	 * @param x X-Coordinate
	 * @param y Y-Coordinate
	 */
	public void pfLeftClick(double x, double y) {
		Pathfinder.run(new Coord2d(x, y),null, 1, 0, -1,"", pBotSession.gui);
	}

	/**
	 * Waits for the hourglass timer when crafting or drinking for example
	 * Also waits until the hourglass has been seen to change at least once
	 * If hourglass does not appear within timeout, returns false, else true
	 * @param timeout Timeout in milliseconds
	 */
	public boolean waitForHourglass(int timeout) {
		GameUI.Progress progress = pBotSession.gui.prog;
		int retries = 0;
		while (!Thread.currentThread().isInterrupted() && progress == null) {
			if (retries > timeout / 5) {
				return false;
			}
			retries++;
			sleep(5);
			progress = pBotSession.gui.prog;
		}
		while (!Thread.currentThread().isInterrupted() && progress.prog >= 0 && pBotSession.gui.prog != null) {
			sleep(25);
		}
		if (Thread.currentThread().isInterrupted()) {
			throw new RuntimeException("Interrupted");
		}
		return true;
	}

	public boolean drink(boolean wait) {
		boolean isDrinking = new DrinkWater(pBotSession).drink();
		if (isDrinking && wait) {
			waitForHourglass(5_000);
		}
		return isDrinking;
	}

	/**
	 * Starts crafting item with the given name
	 * @param name Name of the item ie. "clogs"
	 * @param makeAll 0 To craft once, 1 to craft all
	 */
	public void craftItem(String name, int makeAll) {
		openCraftingWnd(name);
		pBotSession.gui.makewnd.makeWidget.wdgmsg("make", makeAll);
	}

	/**
	 * Opens the crafting window for given item
	 * @param name Name of craft for wdgmsg
	 */
	public void openCraftingWnd(String name) {
		// Close current window and wait for it to close
		if(pBotSession.gui.makewnd.makeWidget != null)
			pBotSession.gui.makewnd.makeWidget.reqdestroy();
		pBotSession.gui.wdgmsg("act", "craft", name);
		pBotSession.PBotWindowAPI().waitForWindow("Crafting", 60*1000);
		while(pBotSession.gui.makewnd.makeWidget == null) {
			PBotUtils.sleep(10);
		}
	}

	/**
	 * Waits for the hourglass timer when crafting or drinking for example
	 * Also waits until the hourglass has been seen to change at least once
	 */
	public void waitForHourglass() {
		GameUI.Progress progress = pBotSession.gui.prog;
		while (progress == null) {
			sleep(5);
			if (Thread.currentThread().isInterrupted()) {
				return;
			}
			progress = pBotSession.gui.prog;
		}
		while (progress.prog >= 0 && pBotSession.gui.prog != null) {
			sleep(20);
			if (Thread.currentThread().isInterrupted()) {
				return;
			}
		}
	}

	/**
	 * Returns value of hourglass, -1 = no hourglass, else the value between 0.0 and 1.0
	 * @return value of hourglass
	 */
	public double getHourglass() {
		return pBotSession.gui.prog.prog;
	}

	/**
	 * Returns the players inventory
	 * @return Inventory of the player
	 */
	public PBotInventory playerInventory() {
		return new PBotInventory(pBotSession.gui.maininv, pBotSession);
	}

	/**
	 * Drops an item from the hand and waits until it has been dropped
	 * @param mod 1 = shift, 2 = ctrl, 4 = alt
	 */
	public void dropItemFromHand(int mod, boolean wait) {
		pBotSession.gui.map.wdgmsg("drop", Coord.z, pBotSession.gui.map.player().rc.floor(OCache.posres), mod);
		if(wait) {
			while(getItemAtHand() != null)
				sleep(25);
		}
	}

	public void dropItemFromHand(boolean wait) {
		int modifier = Config.reverseDropProtection.val ? 2 : 0;
		dropItemFromHand(modifier, wait);
	}

	/**
	 * Resource name of the tile in the given location
	 * @param x X-Coord of the location (rc coord)
	 * @param y Y-Coord of the location (rc coord)
	 * @return
	 */
	public String tileResnameAt(int x, int y) {
		while(true) {
			try {
				Coord loc = new Coord(x, y);
				int t = pBotSession.gui.ui.sess.glob.map.gettile(loc.div(MCache.tilesz2));
				Resource res = pBotSession.gui.ui.sess.glob.map.tilesetr(t);
				if(res != null)
					return res.name;
				else
					return null;
			} catch(Loading l) {
				Thread.onSpinWait();
			}
		}
	}

	@Deprecated
	public void selectItem(Callback cb) {
		selectItem((WItemCallback) cb::callback);
	}

	/**
	 * Next click to item in inventory calls the callback with PBotItem object of the clicked item
	 */
	public void selectItem(WItemCallback cb) {
		pBotSession.PBotUtils().sysMsg("Left click item in inventory to select it!");
		synchronized(pBotSession.gui.itemCallbacks) {
			pBotSession.gui.itemCallbacks.add(new Pair<>(cb, pBotSession));
		}
	}

	public static class AreaReturn {
		private Coord a;
		private Coord b;
		public AreaReturn(Coord a, Coord b) {
			this.a = a.min(b.x, b.y);
			this.b = a.max(b.x, b.y);
		}

		public Coord getA() {
			return a;
		}

		public Coord getB() {
			return b;
		}
	}

	public Optional<AreaReturn> selectAreaSynchronously() {
		AtomicReference<AreaReturn> selectedArea = new AtomicReference<>(null);
		AreaSelectCallback areaCallback = selectedArea::set;

		sysMsg("Select area");
		selectArea(areaCallback);
		while (selectedArea.get() == null) {
			PBotUtils.sleep(500);
			if (currentThread().isInterrupted()) {
				return Optional.empty();
			}
		}
		return Optional.ofNullable(selectedArea.get());
	}

	@Deprecated
	public void selectArea(Callback cb) {
		selectArea((AreaSelectCallback) cb::callback);
	}

	/**
	 * Select area returns AreaReturn object
	 * @param cb
	 */
	public void selectArea(AreaSelectCallback cb) {
		sysMsg("Select area by left clicking and dragging ground!");
		synchronized(pBotSession.gui.map.areaSelectCbQueue) {
			pBotSession.gui.map.areaSelectCbQueue.add(new Pair<>(cb, pBotSession));
		}
	}

	// RC coords for this session, may return null if grid id not found
	public Coord2d getCoords(long gridId, double ofsX, double ofsY, boolean wait) {
		while(true) {
			MCache.Grid g = pBotSession.gui.ui.sess.glob.map.getgrid(gridId);
			if(g == null)
				if(!wait)
					return null;
				else {
					PBotUtils.sleep(50);
				}
			else
				return g.gc.mul(MCache.cmaps).mul(MCache.tilesz).add(ofsX, ofsY);
		}
	}

	public void give() {
		if(pBotSession.gui.fv.current != null) {
			pBotSession.gui.fv.wdgmsg("give", (int) pBotSession.gui.fv.current.gobid, 1);
		}
	}

	public int combatState() {
		if(pBotSession.gui.fv.current != null) {
			return pBotSession.gui.fv.curdisp.give.state;
		}
		return -1;
	}

	// Wait for the pathfinder to stop, returns true if route finding was successful false otherwise
	// Timeout in milliseconds
	public boolean pfWait(int timeout) {
		try {
			if(pBotSession.gui.map.pf_route_found == null)
				return false;
			return pBotSession.gui.map.pf_route_found.get(timeout, TimeUnit.MILLISECONDS);
		} catch(TimeoutException e) {
		} catch(InterruptedException | ExecutionException ie) {
			ie.printStackTrace();
		}
		return false;
	}

	public boolean hasCombat() {
		return pBotSession.gui.fv.current != null;
	}

	public double combatCooldown() {
		return Math.max(0.0, pBotSession.gui.fv.atkct - Utils.rtime());
	}
}
