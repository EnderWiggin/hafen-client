package haven.purus.pbot.api;

import haven.Coord;
import haven.Inventory;
import haven.UI;
import haven.WItem;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class PBotInventory {

	private final Inventory inventory;
	private final PBotSession pBotSession;

	PBotInventory(Inventory inventory, PBotSession pBotSession) {
		this.inventory = inventory;
		this.pBotSession = pBotSession;
	}

	/**
	 * Return all items that the inventory contains
	 * @return List of items in the inventory
	 */
	public List<PBotItem> getInventoryContents() {
		return inventory.children(WItem.class).stream()
				.map(witem -> new PBotItem(witem.item, pBotSession))
				.collect(Collectors.toList());
	}

	/**
	 * @return false if timeout passed without change
	 */
	public boolean waitUntilInventoryChanges(int timeout) {
		int startFreeInventorySlots = freeSlotsInv();
		int timePassed = 0;

		while (startFreeInventorySlots == freeSlotsInv() && timePassed < timeout && !Thread.currentThread().isInterrupted()) {
			PBotUtils.sleep(20);
			timePassed += 20;
		}

		return timePassed < timeout;
	}

	public void dropEverything() {
		List<PBotItem> inventoryContents = getInventoryContents();
		inventoryContents.forEach(PBotItem::dropItemFromInventory);
	}

	/**
	 * Returns a list of items with specific regex pattern from the inventory
	 * @param pattern Regex pattern matching item names
	 * @return List of items with name matching given pattern
	 */
	public List<PBotItem> getInventoryItemsByNames(String pattern) {
		Pattern pat = Pattern.compile(pattern);
		return inventory.children(WItem.class).stream()
				.map(witem -> new PBotItem(witem.item, pBotSession))
				.filter(item -> {
					String name = item.getName();
					return (name != null && pat.matcher(name).matches());
				})
				.collect(Collectors.toList());
	}

	/**
	 * @param pattern Regex pattern matching item resnames
	 * @return List of items with resname matching pattern
	 */
	public List<PBotItem> getInventoryItemsByResnames(String pattern) {
		List<PBotItem> items = new ArrayList<>();
		Pattern pat = Pattern.compile(pattern);
		return inventory.children(WItem.class).stream()
				.map(witem -> new PBotItem(witem.item, pBotSession))
				.filter(item -> {
					String resname = item.getResname();
					return (resname != null && pat.matcher(resname).matches());
				})
				.collect(Collectors.toList());
	}

	/**
	 * Finds an item with certain location from the inventory
	 * @param x x-coordinate of the item location in inventory
	 * @param y y-coordinate of the item location in inventory
	 * @return Null if not found
	 */
	public PBotItem getItemFromInventoryAtLocation(int x, int y) {
		for(WItem witem : inventory.children(WItem.class)) {
			if(witem.c.div(UI.scale(33)).x == x && witem.c.div(UI.scale(33)).y == y) {
				return new PBotItem(witem.item, pBotSession);
			}
		}
		return null;
	}

	/**
	 * Drop item from the hand to given slot in inventory
	 * @param x x coordinate in inventory to drop the item into
	 * @param y y coordinate in inventory to drop the item into
	 */
	public void dropItemToInventory(int x, int y) {
		inventory.wdgmsg("drop", new Coord(x, y));
	}

	/**
	 * Amount of free slots in the inventory
	 * @return Amount of free inventory slots
	 */
	public int freeSlotsInv() {
		return inventory.isz.x * inventory.isz.y -  inventory.children(WItem.class)
				.stream().map(witem -> witem.sz.div(UI.scale(33)))
				.mapToInt((c) -> c.x * c.y)
				.sum();
	}

	/**
	 * Transfer 1 item by scrolling
	 */
	public void xferTo() {
		pBotSession.PBotUtils().playerInventory().inventory.wdgmsg("invxf", inventory.wdgid(), 1);
	}

	/**
	 * Size of the inventory
	 * @return Size
	 */
	public Coord size() {
		return this.inventory.isz;
	}

	// Returns coordinates for placement if the given inventory matrix has space for item, 1 = grid reserved, 0 = grid free O(n*m) where n and m dimensions of matrix, null if no space
	public Coord freeSpaceForItem(PBotItem item) {
		return (freeSpaceForItem(item.getSize()));
	}

	public Coord freeSpaceForItem(Coord size) {
		short[][] inventoryMatrix = containerMatrix();
		int[][] d = new int[inventoryMatrix.length][];
		{
			for (int i = 0; i < d.length; i++) {
				d[i] = new int[inventoryMatrix[i].length];
			}
		}

		int sizeX = size.x;
		int sizeY = size.y;
		for (int i = 0; i < inventoryMatrix.length; i++) {
			for (int j = 0; j < inventoryMatrix[i].length; j++) {
				if (inventoryMatrix[i][j] != 0)
					d[i][j] = 0;
				else
					d[i][j] = (j == 0 ? 1 : d[i][j - 1] + 1);
			}
		}

		for (int i = 0; i < inventoryMatrix[0].length; i++) {
			int curLen = 0;
			for (int j = 0; j < inventoryMatrix.length; j++) {
				if (d[j][i] >= sizeY)
					curLen++;
				else
					curLen = 0;
				if (curLen >= sizeX)
					return (new Coord(j, i));
			}
		}

		return (null);
	}

	// Returns a matrix representing the container and items inside, 1 = item in this grid, 0 = free grid, -1 = invalid
	public short[][] containerMatrix() {
		short[][] ret = new short[inventory.isz.x][inventory.isz.y];
		for (PBotItem item : getInventoryContents()) {
			int xSize = item.getSize().x;
			int ySize = item.getSize().y;
			int xLoc = item.getInvLoc().x;
			int yLoc = item.getInvLoc().y;

			for (int i = 0; i < xSize; i++) {
				for (int j = 0; j < ySize; j++) {
					ret[i + xLoc][j + yLoc] = 1;
				}
			}
		}
		int mo = 0;
		for (int i = 0; i < inventory.isz.y; i++) {
			for (int j = 0; j < inventory.isz.x; j++) {
				if ((inventory.sqmask != null) && inventory.sqmask[mo++]) ret[j][i] = -1;
			}
		}
		return (ret);
	}
}
