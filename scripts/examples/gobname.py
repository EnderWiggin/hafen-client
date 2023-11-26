#Add text to selected gob
#used to demonstrate PBot API callbacks
import sys, os
sys.path.insert(0, os.path.abspath('..'))

from __pbot.PBotSession import PBotSession
from __pbot import PBotGob

import time
class Script:
    def selectGobCb(self, gob: PBotGob.PBotGob):
        id = gob.add_gob_text("Selected")
        time.sleep(5)
        gob.remove_gob_text(id)

    def run(self, sess: PBotSession):
        sess.PBotGobAPI.select_gob(self.selectGobCb)
