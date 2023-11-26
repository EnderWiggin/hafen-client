package haven.purus.pbot.api;

import haven.*;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class PBotWindow {

	public final Window window;
	PBotSession pBotSession;

	public PBotWindow(Window window, PBotSession pBotSession) {
		this.window = window;
		this.pBotSession = pBotSession;
	}

	/**
	 * Get tooltips of widgets in the window Example: Trough
	 * @return List containing the tooltips that were found
	 */
	public List<String> getTooltips() {
		return window.children().stream()
				.filter(w -> w.tooltip instanceof Text)
				.map(w -> ((Text)w.tooltip).text)
				.collect(Collectors.toList());
	}

	/**
	 * Get amounts of meters of the window, from 0 to 100, some windows may have more than 1 meter, like chicken coops
	 * @return List containing amounts of the meters that were found
	 */
	public List<Integer> getAmounts() {
		ArrayList<Integer> amounts = new ArrayList<>();
		for(VMeter vm:window.children(VMeter.class)) {
			amounts.add(vm.amount);
		}
		return amounts;
	}

	/**
	 * Returns used capacity of the stockpile window which is currently open
	 * @return Used capacity, or -1 if stockpile window could not be found
	 */
	public int getStockpileUsedCapacity() {
		ISBox ib = window.getchild(ISBox.class);
		if(ib != null)
			return ib.getUsedCapacity();
		return -1;
	}

	/**
	 * Put an item from the hand to a stockpile window that is currently open
	 */
	public void putItemFromHandToStockpile() {
		ISBox ib = window.getchild(ISBox.class);
		if(ib != null)
			ib.wdgmsg("drop");
	}

	/**
	 * Attempts to get items from the stockpile that is currently open
	 * @param count How many items to take
	 */
	public void takeItemsFromStockpile(int count) {
		if(window.getchild(ISBox.class) != null)
			for(int i=0; i<count; i++)
				window.getchild(ISBox.class).wdgmsg("xfer");
	}

	/**
	 * Click stockpile to get item to hand
	 */
	public void takeItemsFromStockpileHand() {
		if(window.getchild(ISBox.class) != null)
			window.getchild(ISBox.class).wdgmsg("click");
	}

	/**
	 * Attempts to put item that fits form inventory to the stockpile, like when scrolling to stockpile
	 * @param count How many items to put into the stockpile
	 */
	public void putItemFromInventoryToStockpile(int count) {
		if(window.getchild(ISBox.class) != null)
			for(int i=0; i<count; i++)
				window.getchild(ISBox.class).wdgmsg("xfer2", 1, 1);
	}

	/**
	 * Returns total capacity of the stockpile, if the window is stockpile
	 * @return Total capacity, or -1 if stockpile window could not be found
	 */
	public int getStockpileTotalCapacity() {
		ISBox ib = window.getchild(ISBox.class);
		if(ib != null)
			return ib.getTotalCapacity();
		return -1;
	}

	/**
	 * Tries to find an inventories attached to the given window, such as cupboard
	 * @return List of inventories of the window, empty if not found
	 */
	public List<PBotInventory> getInventories() {
		ArrayList<PBotInventory> inventories = new ArrayList<>();
		for(Inventory i : window.children(Inventory.class)) {
			inventories.add(new PBotInventory(i, pBotSession));
		}
		return inventories;
	}

	/**
	 * Close this window
	 */
	public void closeWnd(boolean immediately) {
		window.wdgmsg("close");
		if(immediately)
			window.reqdestroy();
	}

	/**
	 * Hide this window
	 */
	public void hideWnd() {
		window.hide();
	}
}
