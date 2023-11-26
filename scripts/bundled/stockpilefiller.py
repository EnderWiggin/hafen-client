3## Collect leaves from mulberry trees into stockpiles
import sys, os, threading, time
sys.path.insert(0, os.path.abspath('..'))

from __pbot.PBotSession import PBotSession
from __pbot.PBotGob import PBotGob

class Script:
    area = None
    cb_gob: PBotGob = None
    cb_ok = threading.Event()
    def area_cb(self, a, b):
        self.area = (a,b)
        self.cb_ok.set()

    def gob_cb(self, gob):
        self.cb_gob = gob
        self.cb_ok.set()

    def run(self, sess: PBotSession):
        sess.PBotGobAPI.select_gob(self.gob_cb)
        sess.PBotUtils.sys_msg("Seelect item from ground by alt + left click")
        self.cb_ok.wait()
        self.cb_ok.clear()
        itm_name = self.cb_gob.get_resname()

        sess.PBotGobAPI.select_gob(self.gob_cb)
        sess.PBotUtils.sys_msg("Select target stockpile by alt + left click")
        self.cb_ok.wait()
        self.cb_ok.clear()
        stockpile_name = self.cb_gob.get_resname()

        sess.PBotUtils.select_area(self.area_cb)
        sess.PBotUtils.sys_msg("Select stockpile area by drag")
        self.cb_ok.wait()
        self.cb_ok.clear()
        ground_items = sess.PBotGobAPI.get_gobs_by_resname(itm_name)
        stockpiles = list(filter(lambda x: x.get_resname() == stockpile_name, sess.PBotGobAPI.gobs_in_area(*self.area[0], *self.area[1])))

        while len(ground_items) > 0 and len(stockpiles) > 0:
            picked_itms = 0
            ground_items[-1].do_click(1)
            sess.PBotUtils.pf_wait()
            while len(ground_items) > 0 and sess.PBotUtils.get_item_at_hand() is None:
                itm = ground_items.pop()
                slots_before = sess.PBotUtils.player_inventory().free_slots()
                itm.do_click(3)
                while sess.PBotUtils.player_inventory().free_slots() == slots_before and sess.PBotUtils.get_item_at_hand() is None:
                    time.sleep(0.025)
                picked_itms += 1

            while picked_itms > 0 and len(stockpiles) > 0:
                pile = stockpiles[-1]
                if pile.stockpile_is_full():
                    stockpiles.pop()
                    continue
                pile.pf_click(1)
                sess.PBotUtils.pf_wait()
                if sess.PBotUtils.get_item_at_hand() is not None:
                    pile.item_click(0)
                    picked_itms -= 1
                    while sess.PBotUtils.get_item_at_hand() is not None:
                        time.sleep(0.050)
                pile.do_click(3)
                wnd = sess.PBotWindowAPI.get_window("Stockpile", 1000*3600)
                rem = wnd.get_stockpile_total_capacity()-wnd.get_stockpile_used_capacity()
                wnd.put_item_from_inventory_to_stockpile(min(rem, picked_itms))
                wnd.close()
                if rem >= picked_itms:
                    break
                picked_itms -= min(rem, picked_itms)
            stockpiles.pop()
        if len(ground_items) > 0:
            sess.PBotUtils.sys_msg("Ran out of stockpiles!")
        else:
            sess.PBotUtils.sys_msg("Done!")

