## Make stockpiles of some item
import sys, os, threading, time
sys.path.insert(0, os.path.abspath('..'))

from __pbot.PBotSession import PBotSession
from __pbot.PBotItem import PBotItem

class Script:
    area = None
    item: PBotItem = None
    cb_ok = threading.Event()
    def area_cb(self, a, b):
        self.area = (a,b)
        self.cb_ok.set()

    def item_cb(self, item):
        self.item = item
        self.cb_ok.set()

    def run(self, sess: PBotSession):
        sess.PBotUtils.select_area(self.area_cb)
        self.cb_ok.wait()
        self.cb_ok.clear()
        sess.PBotUtils.select_item(self.item_cb)
        self.cb_ok.wait()
        itms = sess.PBotUtils.player_inventory().get_inventory_items_by_names(self.item.get_name())
        itm = itms.pop()
        itm.take_item(True)
        sess.PBotUtils.make_pile()
        x = min(self.area[0][0], self.area[1][0]) + 5.5
        y = min(self.area[0][1], self.area[1][1]) + 5.5
        x_lim = max(self.area[0][0], self.area[1][0])
        y_lim = max(self.area[0][1], self.area[1][1])
        sess.PBotUtils.placeThing(x, y)
        while sess.PBotGobAPI.get_gob_by_coords(x, y) is None:
            time.sleep(0.025)
        while sess.PBotGobAPI.get_gob_by_coords(x, y).get_resname() is None:
            time.sleep(0.025)
        bb = sess.PBotGobAPI.get_gob_by_coords(x, y).get_boundingbox_rect()
        for itm in itms:
            x += abs(bb[0][0]) + abs(bb[1][0])
            if x >= x_lim:
                y += abs(bb[0][1]) + abs(bb[1][1])
                x = min(self.area[0][0], self.area[1][0]) + 5.5
            if y >= y_lim:
                break
            loc_found = False
            for loc in [[bb[0][0] + 5.5, 0], [bb[1][0] - 5.5, 0], [0, bb[0][1] + 5.5], [0, bb[1][1] - 5.5]]:
                sess.PBotUtils.pf_left_click(x + loc[0], y + loc[1])
                if sess.PBotUtils.pf_wait():
                    loc_found = True
                    break
            if not loc_found:
                return
            itm.take_item(True)
            sess.PBotUtils.make_pile()
            sess.PBotUtils.placeThing(x, y)
            while sess.PBotGobAPI.get_gob_by_coords(x, y) is None:
                time.sleep(0.025)

