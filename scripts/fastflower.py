# Quickly rightclicks all items with the same name and chooses the first menu option
# For example, blocks into sticks
from __pbot.PBotItem import PBotItem
from __pbot.PBotSession import PBotSession
from __pbot.PBotInventory import get_inventory_for_item

class Script:
    def cb(self, itm: PBotItem):
        inv = get_inventory_for_item(itm)
        if inv is not None:
            items = inv.get_inventory_items_by_names(itm.get_name())
            for itm in items:
                itm.activate_item()
                menu = self.session.PBotUtils.get_flowermenu(1000*15)
                if menu is not None:
                    menu.choose_petal_num(0)
                else:
                    break
    def run(self, sess: PBotSession):
        self.session = sess
        sess.PBotUtils.select_item(self.cb)
