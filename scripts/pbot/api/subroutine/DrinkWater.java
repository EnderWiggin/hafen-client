package haven.purus.pbot.api.subroutine;

import java.util.ArrayList;
import java.util.List;

import haven.purus.pbot.api.PBotFlowerMenu;
import haven.purus.pbot.api.PBotItem;
import haven.purus.pbot.api.PBotSession;

public class DrinkWater {
	private final PBotSession botSession;

	public DrinkWater(PBotSession botSession) {
		this.botSession = botSession;
	}

	public boolean drink() {
		if (botSession.PBotUtils().isFlowerMenuOpen()) {
			return false;
		}

		List<PBotItem> items = new ArrayList<>();
		items.addAll(botSession.PBotCharacterAPI().getEquipment());
		items.addAll(botSession.PBotCharacterAPI().getBeltEquipment());
		items.addAll(botSession.PBotUtils().playerInventory().getInventoryContents());
		for (var item : items) {
			if (item != null && canDrinkFromEquipment(item)) {
				item.activateItem();
				PBotFlowerMenu flowermenu = botSession.PBotUtils().getFlowermenu(5000);
				return flowermenu.choosePetal("Drink");
			}
		}

		return false;
	}

	private boolean canDrinkFromEquipment(PBotItem item) {
		var contents = item.getContentsName();
		return contents != null && contents.contains("Water");
	}

}
