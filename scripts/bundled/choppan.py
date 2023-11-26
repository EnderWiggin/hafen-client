# Chop trees in the selected area, bind water containers to belts for server side autodrink
import time

sess = None
class Listener(object):
    def callback(self, area):
        # Chop trees
        trees = 0
        stumps = 0
        for gob in sess.PBotGobAPI().gobsInArea(area.getA(), area.getB()):
            if sess.PBotCharacterAPI().getEnergy() < 30:
                sess.PBotCharacterAPI().msgToChat("Area Chat", "Energy is too low, stopping!")
                return
            resname = gob.getResname()
            if (not resname.startswith("gfx/terobjs/trees/") and not resname.startswith("gfx/terobjs/bushes/")) or resname.endswith("log") or resname.endswith("stump") or resname.endswith("trunk"):
                continue
            else:
                if sess.PBotGobAPI().findGobById(gob.getId()) == None:
                    continue
                gob.pfClick(3,0)
                menu = sess.PBotUtils().getFlowermenu(1000*30)
                if menu == None:
                    sess.PBotCharacterAPI().msgToChat("Area Chat", "Flowermenu timeout!")
                    continue
                menu.choosePetal("Chop")
                timeout = time.time()
                while sess.PBotGobAPI().findGobById(gob.getId()) != None:
                    time.sleep(0.1)

                    if timeout-time.time() > 30:
                        sess.PBotCharacterAPI().msgToChat("Area Chat", "Timeout exceeded! Stopping, probably ran out of water");
                        return
                trees += 1
        # Remove stumps
        for gob in sess.PBotGobAPI().gobsInArea(area.getA(), area.getB()):
            if sess.PBotCharacterAPI().getEnergy() < 30:
                sess.PBotCharacterAPI().msgToChat("Area Chat", "Energy is too low, stopping!")
                return
            resname = gob.getResname()
            if not resname.startswith("gfx/terobjs/trees/") or not resname.endswith("stump"):
                continue
            else:
                if sess.PBotGobAPI().findGobById(gob.getId()) == None:
                    continue
                gob.pfClick(1,0,list(["destroy"]))
                timeout = time.time()
                while sess.PBotGobAPI().findGobById(gob.getId()) != None:
                    time.sleep(0.1)

                    if timeout-time.time() > 60:
                        sess.PBotCharacterAPI().msgToChat("Area Chat", "Timeout exceeded! Stopping, probably ran out of water");
                        return
                stumps += 1
        sess.PBotCharacterAPI().msgToChat("Area Chat", "Finished. Got rid of {} trees and {} stumps".format(trees, stumps))

    class Java:
        implements = ["haven.purus.pbot.api.Callback"]

class Script:
    def run(self, session):
        global sess
        sess = session
        PBotGobAPI = sess.PBotGobAPI()
        sess.PBotUtils().selectArea(Listener())
