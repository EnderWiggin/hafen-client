package haven.purus.pbot.api;

import haven.FlowerMenu;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class PBotFlowerMenu {

	private FlowerMenu menu;

	PBotFlowerMenu(FlowerMenu menu) {
		this.menu = menu;
	}

	/**
	 * Close this flowermenu
	 */
	public void closeMenu() {
		this.menu.choose(null);
		this.menu.destroy();
	}

	/**
	 * Choose option
	 * @param name Exact name of the option
	 * @return whether the option was chosen or not
	 */
	public boolean choosePetal(String name) {
		for(FlowerMenu.Petal p : this.menu.opts) {
			if(p.name.equals(name)) {
				try {
					menu.choose(p);
					menu.destroy();
					return true;
				} catch(IndexOutOfBoundsException iee) {}
			}
		}
		return false;
	}

	/**
	 * Choose option by number
	 * @param num Option number 0-indexed
	 */
	public void choosePetal(int num) {
		menu.choose(menu.opts[num]);
		menu.destroy();
	}

	/**
	 * Get options of flowermenu
	 * @return Names of the petal options
	 */
	public List<String> getPetalNames() {
		return Arrays.stream(this.menu.opts)
				.map(opt -> opt.name)
				.collect(Collectors.toList());
	}
}
