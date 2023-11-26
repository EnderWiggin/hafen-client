package haven.purus.pbot.api;

import java.util.*;
import java.util.stream.Collectors;

import haven.*;
import haven.GItem.ContentsWindow;
import haven.purus.pbot.api.model.CharacterAct;

public class
PBotCharacterAPI {

	private PBotSession pBotSession;

	PBotCharacterAPI(PBotSession pBotSession) {
		this.pBotSession = pBotSession;
	}

	/**
	 * Get the player stamina
	 * @return Returns 0-100
	 */
	public double getStamina() {
		return (100 * pBotSession.gui.getmeter("stam", 0).a);
	}

	/**
	 * Get the player energy
	 * @return Returns 0-100
	 */
	public double getEnergy() {
		return (100 * pBotSession.gui.getmeter("nrj", 0).a);
	}

	/**
	 * Get the player hp
	 * @return Returns 0-100
	 */
	public double getHp() {
		return (100 * pBotSession.gui.getmeter("hp", 1).a);
	}

	/**
	 * Send act message to server
	 * Act can be used for example to choose a cursor
	 * Some acts:
	 * dig, mine, carry, destroy, fish, inspect, repair, crime, swim, tracking, aggro, shoot
	 * @param act Act(s) to choose
	 */
	public void doAct(List<String> act) {
		pBotSession.gui.menu.wdgmsg("act", act.toArray());
	}

	public void doAct(CharacterAct act) {
		pBotSession.gui.menu.wdgmsg("act", act.getActMessage());
	}

	/**
	 * Get name of the selected cursor
	 * @return cursor
	 */
	public String getCursName() {
		try {
			return pBotSession.gui.map.ui.getcurs(pBotSession.gui.map.mouseLoc).name;
		} catch(Exception e) {
			return null;
		}
	}

	/**
	 * Cancels the current act by clicking
	 */
	public void cancelAct() {
		pBotSession.gui.map.wdgmsg("click", Coord.z, Coord.z, 3, 0);
	}

	/**
	 * Log out to character selection
	 */
	public void logoutChar() {
		pBotSession.gui.act("lo", "cs");
	}


	/**
	 * Log in with a character from character login menu
	 * @param charname Name of the character
	 */
	public void loginChar(String charname) {
		// TODO
	}

	/**
	 * Set player speed setting
	 * @param speed 1 = crawl, 2 = walk, 3 = run, 4 = sprint
	 */
	public void setSpeed(int speed) {
		Speedget sg = (Speedget) pBotSession.gui.ulpanel.getchild(Speedget.class);
		if(sg != null)
			sg.SpeedToSet = speed;
	}

	/**
	 * Get current speed setting of player
	 * @return 1 = crawl, 2 = walk, 3 = run, 4 = sprint
	 */
	public int getSpeed() {
		Speedget sg = (Speedget) pBotSession.gui.ulpanel.getchild(Speedget.class);
		if(sg != null)
			return sg.cur;
		return -1;
	}

	/**
	 * Get maximum speed setting that player can be set to
	 * @return 1 = crawl, 2 = walk, 3 = run, 4 = sprint
	 */
	public int getMaxSpeed() {
		Speedget sg = (Speedget) pBotSession.gui.ulpanel.getchild(Speedget.class);
		if(sg != null)
			return sg.max;
		return -1;
	}

	/**
	 * Send message to given chat
	 * @param chatName Name of the chat, for example "Area Chat"
	 * @param msg Message to send into the chat
	 */
	public void msgToChat(String chatName, String msg) {
		for(Widget w = pBotSession.gui.chat.lchild; w != null; w = w.prev) {
			if(w instanceof ChatUI.EntryChannel) {
				ChatUI.EntryChannel cht = (ChatUI.EntryChannel) w;
				if(cht.name().equals(chatName))
					cht.send(msg);
			}
		}
	}

	/**
	 * Returns content of each slot in players equipment menu
	 * @return List with each slot containing null or pbotitem
	 */
	public List<PBotItem> getEquipment() {
		Window equwnd = pBotSession.gui.equwnd;
		if(equwnd == null)
			return null;
		Equipory eq = equwnd.getchild(Equipory.class);
		if(eq == null)
			return null;
		ArrayList<PBotItem> ret = new ArrayList<>();
		for(GItem itm : eq.slots) {
			if(itm == null)
				ret.add(null);
			else
				ret.add(new PBotItem(itm, pBotSession));
		}
		return ret;
	}

	public List<PBotItem> getBeltEquipment() {
		GameUI gui = pBotSession.gui;
		Set<ContentsWindow> contentsWindows = gui.children(ContentsWindow.class);
		return contentsWindows.stream()
							  .filter(window -> "toolbelt".equals(window.getId()))
							  .map(window -> window.inv)
							  .filter(Inventory.class::isInstance)
							  .map(Inventory.class::cast)
							  .map(Inventory::getItems)
							  .flatMap(Collection::stream)
							  .map(item -> new PBotItem(item, pBotSession))
							  .collect(Collectors.toList());
	}

	/**
	 * Equip item in hand to the given slot
	 * @param slot equipment slot to equip item to
	 */
	public void equipEquipment(int slot) {
		Window equwnd = pBotSession.gui.equwnd;
		if(equwnd != null) {
			Equipory eq = equwnd.getchild(Equipory.class);
			if(eq != null)
				eq.wdgmsg("drop", slot);
		}
	}

	/**
	 * Get current character name
	 * @return character name as string, may return null if not in character context
	 */
	public String getCharacterName() {
		if (pBotSession != null && pBotSession.gui != null) {
			return pBotSession.gui.chrid;
		}

		return null;
	}

}
