# Sort 1x1 items by name, quality in Cupboards, Chests, Crates, Woodboxes and Baskets
import sys, os
sys.path.insert(0, os.path.abspath('..'))

from __pbot.PBotSession import PBotSession

class Script:
    def run(self, sess: PBotSession):
        wnds = sess.PBotWindowAPI.get_windows("Cupboard|Chest|Crate|Woodbox|Basket")
        if len(wnds) == 0:
            sess.PBotUtils.sys_msg("No inventories found")
        for wnd in wnds:
            for inv in wnd.get_inventories():
                inv_grid = [[False for _ in range(inv.size()[1])] for __ in range(inv.size()[0])]
                for itm in inv.get_inventory_items():
                    if itm.get_size() == (1,1,):
                        continue
                    loc = itm.get_inv_loc()
                    sz = itm.get_size()
                    for x in range(sz[0]):
                        for y in range(sz[1]):
                            inv_grid[loc[0] + x][loc[1] + y] = True


                itms = sorted(filter(lambda x: x.get_size() == (1,1,), inv.get_inventory_items()), key=lambda x: (x.get_name(), -x.get_quality(),))
                itms = list(map(lambda x: [x, x.get_inv_loc()], itms))
                cur_x = -1
                cur_y = 0
                for x in itms:
                    while True:
                        cur_x += 1
                        if cur_x == inv.size()[0]:
                            cur_x = 0
                            cur_y += 1
                        if inv_grid[cur_x][cur_y] == False:
                            x.append((cur_x, cur_y,))
                            break
                handu = None
                for x in itms:
                    if x[1] == x[2]:
                        continue
                    x[0].take_item()
                    handu = x
                    while handu is not None:
                        inv.drop_item_to_inventory(*handu[2])
                        small = list(filter(lambda x: x[1] == handu[2], itms))
                        handu[1] = handu[2]
                        if len(small) == 1:
                            handu = small[0]
                        else:
                            handu = None
