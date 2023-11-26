# Picks dreams from the closest dreamcatcher
from __pbot.PBotSession import PBotSession
from __pbot.PBotGob import PBotGob
import time

class Script:
    def pick_dream(self, gob: PBotGob, sess: PBotSession):
        gob.do_click(3)
        menu = sess.PBotUtils.get_flowermenu(1000)
        if menu is not None:
            menu.choose_petal("Harvest")

    def run(self, sess: PBotSession):
        dreca = sess.PBotGobAPI.get_closest_gob_by_resname("gfx/terobjs/dreca")
        if dreca is None or dreca.dist(sess.PBotGobAPI.get_player()) > 3*11:
            return
        pinv = sess.PBotUtils.player_inventory()
        dcnt = len(pinv.get_inventory_items_by_resnames("gfx/invobjs/dream"))
        self.pick_dream(dreca, sess)
        start = time.time()
        while len(pinv.get_inventory_items_by_resnames("gfx/invobjs/dream")) == dcnt:
            time.sleep(0.025)
            if time.time()-start > 1.0:
                return
        self.pick_dream(dreca, sess)
