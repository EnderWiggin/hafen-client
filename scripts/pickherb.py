from __pbot.PBotSession import PBotSession

class Script:
    def run(self, sess: PBotSession):
        closestHerb = sess.PBotGobAPI.get_closest_gob_by_resname("gfx/kritter/.*|gfx/terobjs/items/grub")
        if closestHerb == None or closestHerb.dist(sess.PBotGobAPI.get_player()) > 8*11:
            closestHerb = sess.PBotGobAPI.get_closest_gob_by_resname("gfx/terobjs/herbs/.*|gfx/kritter/jellyfish/jellyfish")
        if closestHerb != None and closestHerb.dist(sess.PBotGobAPI.get_player()) < 8*11:
            closestHerb.do_click(3, 0)
            menu = sess.PBotUtils.get_flowermenu(5000)
            if menu != None:
                menu.choose_petal("Pick")
