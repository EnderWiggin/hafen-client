#Color gobs in the selected area
#used to demonstrate PBot API callbacks
import sys, os
sys.path.insert(0, os.path.abspath('..'))
import random

from __pbot.PBotSession import PBotSession

class Script:
    def cb(self, a, b):
        for gob in self.session.PBotGobAPI.gobs_in_area(*a, *b):
            gob.set_marked(*[random.randint(0, 255) for _ in range(4)])

    def run(self, sess: PBotSession):
        self.session = sess
        sess.PBotUtils.select_area(self.cb)
