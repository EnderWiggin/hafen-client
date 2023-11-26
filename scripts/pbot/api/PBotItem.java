package haven.purus.pbot.api;

import haven.*;
import haven.res.ui.tt.alch.elixir.Elixir;
import haven.res.ui.tt.alch.heal.HealWound;
import haven.res.ui.tt.alch.hurt.AddWound;
import haven.res.ui.tt.attrmod.AttrMod;
import haven.res.ui.tt.craftprep.CraftPrep;
import haven.res.ui.tt.q.qbuff.QBuff;

import java.util.ArrayList;
import java.util.List;

public class PBotItem {

	private PBotSession pBotSession;
	private GItem item;

	public PBotItem(GItem item, PBotSession pBotSession) {
		this.item = item;
		this.pBotSession = pBotSession;
	}

	public boolean equals(PBotItem itm) {
		return (item.wdgid() == itm.item.wdgid());
	}

	/**
	 * Returns name of the item content
	 * @return Name of the item content or null if not found
	 */
	public String getContentsName() {
		while(true) {
			try {
				for(ItemInfo info : item.info()) {
					if(info instanceof ItemInfo.Contents) {
						for(ItemInfo info2 : ((ItemInfo.Contents) info).sub) {
							if(info2 instanceof ItemInfo.Name) {
								return ((ItemInfo.Name) info2).str.text;
							}
						}
					}
				}
			} catch(Loading l) {
				PBotUtils.sleep(25);
			}
			return null;
		}
	}


	private class ElixirInfo {
		private final int time;
		private final List<ElixirEffect> heals = new ArrayList<>();
		private final List<ElixirEffect> hurts = new ArrayList<>();

		private class ElixirEffect {
			private final String resname;
			private final String name;
			private final int amount;

			ElixirEffect(String name, String resname, int amount) {
				this.name = name;
				this.resname = resname;
				this.amount = amount;
			}

			public String getResname() {
				return resname;
			}

			public String getName() {
				return name;
			}

			public int getAmount() {
				return amount;
			}
		}

		ElixirInfo(int time) {
			this.time = time;
		}

		public int getTime() {
			return time;
		}

		public List<ElixirEffect> getHeals() {
			return heals;
		}

		public List<ElixirEffect> getHurts() {
			return hurts;
		}

		public void addHeal(String name, String resname, int pts) {
			heals.add(new ElixirEffect(name, resname, pts));
		}

		public void addHurt(String name, String resname, int pts) {
			hurts.add(new ElixirEffect(name, resname, pts));
		}
	}

	/**
	 * Returns name of the item content
	 * @return Name of the item content or null if not found
	 */
	public ElixirInfo getElixirInfo() {
		while(true) {
			try {
				for(ItemInfo info : item.info()) {
					if(info instanceof ItemInfo.Contents) {
						for(ItemInfo cont : ((ItemInfo.Contents) info).sub) {
							if(cont instanceof Elixir) {
								ElixirInfo ret = new ElixirInfo(((Elixir) cont).time);
								for(ItemInfo info2 : ((Elixir) cont).effs) {
									if(info2 instanceof HealWound) {
										ret.addHeal(((HealWound) info2).res.get().layer(Resource.tooltip).t, ((HealWound) info2).res.get().name, (int)Math.round(((HealWound) info2).a/(Math.sqrt(getContentsQuality()/10))));
									} else if(info2 instanceof AddWound) {
										ret.addHurt(((AddWound) info2).res.get().layer(Resource.tooltip).t, ((AddWound) info2).res.get().name, ((AddWound) info2).a);
									} else if(info2 instanceof AttrMod) {
										for(AttrMod.Mod mod : ((AttrMod) info2).mods) {
											ret.addHeal(mod.attr.layer(Resource.tooltip).t, mod.attr.name, (int)Math.round(mod.mod/Math.sqrt(getContentsQuality()/10)));
										}
									} else {
										System.out.println("Elixir unknown type: " + info2.getClass().getName());
									}
								}
								return ret;
							}
						}
					}
				}
			} catch(Loading l) {
				PBotUtils.sleep(25);
				continue;
			}
			return null;
		}
	}

	// True if selected green for craft
	public boolean usedForCraft() {
		while(true) {
			try {
				for(ItemInfo info : item.info()) {
					if(info instanceof CraftPrep)
						return true;
				}
			} catch(Loading l) {
				PBotUtils.sleep(20);
				continue;
			}
			return false;
		}
	}

	/**
	 * Returns quality of the item content
	 * @return Quality of the item content or -1 if not found
	 */
	public double getContentsQuality() {
		for(ItemInfo info : item.info()) {
			if(info instanceof ItemInfo.Contents) {
				for(ItemInfo info2: ((ItemInfo.Contents)info).sub) {
					if(info2 instanceof QBuff) {
						return ((QBuff) info2).q;
					}
				}
			}
		}
		return -1;
	}

	/**
	 * Take the item to hand
	 * @param wait Wait for item to appear on hand
	 */
	public void takeItem(boolean wait) {
		item.wdgmsg("take", Coord.z);
		if(wait) {
			while(pBotSession.PBotUtils().getItemAtHand() == null) {
				PBotUtils.sleep(25);
			}
		}
	}

	/**
	 * Get the inventory that this item is located in
	 * @return Inventory or null if not found
	 */
	public PBotInventory getInventory() {
		if(item.witem().parent instanceof Inventory)
			return new PBotInventory((Inventory)item.witem().parent, pBotSession);
		else
			return null;
	}

	/**
	 * Transfer an item to the active inventory, does not wait for item to transfer
	 */
	public void transferItem() {
		item.wdgmsg("transfer", Coord.z);
	}

	/**
	 * Right clicks the item in the inventory
	 */
	public void activateItem() {
		item.wdgmsg("iact", Coord.z, 0);
	}

	/**
	 * Iact (Right click item, like open drink flowermenu)
	 * @param mod modifier for example 1 = shift etc.
	 */
	public void iact(int mod) {
		item.wdgmsg("iact", Coord.z, mod);
	}

	/**
	 * Itemact (Act with item in hand, like equip a lure)
	 * @param mod modifier for example 1 = shift etc.
	 */
	public void itemact(int mod) {
		item.wdgmsg("itemact", mod);
	}

	/**
	 * Get an amount of something such as seeds in a stack
	 * @return Amount of something in the item
	 */
	public int getAmount() {
		int ret = -1;
		synchronized(item.ui) {
			for(ItemInfo o : item.info()) {
				if(o instanceof GItem.Amount)
					ret = ((GItem.Amount) o).itemnum();
			}
		}
		return ret;
	}

	/**
	 * Get location of the item in inventory, ie. 5,4
	 * @return Coord-object, access x and y for coords
	 */
	public Coord getInvLoc() {
		return item.witem().c.div(33);
	}

	/**
	 * Returns name of the item if it exists
	 * @return Name of item or null
	 */
	public String getName() {
		while(true) {
			try {
				synchronized(item.ui) {
					for(Object o : item.info().toArray()) {
						if(o instanceof ItemInfo.Name)
							return ((ItemInfo.Name) o).str.text;
					}
					break;
				}
			} catch(Loading l) { }
			PBotUtils.sleep(20);
		}
		return null;
	}

	/**
	 * Returns resname of the item
	 * @return Resname of the item or null if not found
	 */
	public String getResname() {
		while(true) {
			try {
				Resource res = item.getres();
				if(res == null)
					return null;
				else
					return res.name;
			} catch(Loading l) {
			}
		}
	}

	/**
	 * Returns quality of the item, or -1 if quality was not found
	 * @return Quality of the item
	 */
	public double getQuality() {
		return item.witem().quality();
	}

	/**
	 * Returns quality of the item, wait until the quality is calculated for the item
	 * @return Quality of the item
	 */
	public double getQuality2() {
		while(getQuality() == -1)
			PBotUtils.sleep(5);
		return getQuality() ;
	}

	/*
	 * Checks if player can drink from the item
	 * Only checks water and tea
	 * @return True if player can drink, else false

	public boolean canDrinkFrom() {
		synchronized(gitem.ui) {
			Pattern liquidPattern = Pattern.compile(String.format("[0-9.]+ l of (%s)",
					String.join("|", new String[]{"Water", "Piping Hot Tea", "Tea"}), Pattern.CASE_INSENSITIVE));
			ItemInfo.Contents contents = getContents();
			if(contents != null && contents.sub != null) {
				for(ItemInfo info : contents.sub) {
					if(info instanceof ItemInfo.Name) {
						ItemInfo.Name name = (ItemInfo.Name) info;
						if(name.str != null && liquidPattern.matcher(name.str.text).matches())
							return true;
					}
				}
			}
		}
		return false;
	}*/

	/**
	 * Drops the item from the inventory to ground, does not wait for it to drop
	 */
	public void dropItemFromInventory() {
		item.wdgmsg("drop", Coord.z);
	}

	/**
	 * Get size of this item
	 * @return Size of item access x, y
	 */
	public Coord getSize() {
		return item.size();
	}

}
