## Transfer items from inventory to closest gob, like drying frame oven etc.
import sys, os
sys.path.insert(0, os.path.abspath('..'))

from __pbot.PBotSession import PBotSession


class Script:
    def run(self, sess: PBotSession):
        gob = sess.PBotGobAPI.get_closest_gob_by_resname(".*")
        if gob is None:
            return
        wnd_name = gob.window_name_for_gob()
        if wnd_name is None:
            return
        gob.do_click(3)
        wnd = sess.PBotWindowAPI.get_window(wnd_name, 1000*5)
        if wnd is None:
            return
        invs = wnd.get_inventories()
        if len(invs) < 1:
            return
        for x in range(invs[0].free_slots()):
            invs[0].xfer_to()
