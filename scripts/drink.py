import itertools
from typing import Optional
from __pbot.PBotSession import PBotSession
from __pbot.PBotWindow import PBotWindow
from __pbot.PBotInventory import PBotInventory

class Script:
    def getWInv(self, wnd: PBotWindow) -> Optional[PBotInventory]:
        if wnd != None:
            inv = wnd.get_inventories()
            if len(inv) > 0:
                return inv[0]
        return None

    def run(self, sess: PBotSession):
        invs = [sess.PBotUtils.player_inventory(), self.getWInv(sess.PBotWindowAPI.get_window("Belt"))]
        for inv in filter(None, invs):
            for itm in filter(lambda x: x != None and str(x.get_contents_name()).endswith("of Water"), itertools.chain(inv.get_inventory_items_by_resnames(".*"), sess.PBotCharacterAPI.get_equipment())):
                itm.activate_item()
                menu = sess.PBotUtils.get_flowermenu(5000)
                if menu != None:
                    menu.choose_petal("Drink")
                    return
