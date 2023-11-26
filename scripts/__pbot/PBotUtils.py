from typing import Optional, Tuple, Callable

from .PBotFlowerMenu import PBotFlowerMenu
from .PBotInventory import PBotInventory
from .PBotItem import PBotItem


class PBotUtils(object):
    def __init__(self, session):
        self.__session = session

    ## Send a colored message in the client system menu
    # @param msg: str Message to send
    # @param r: red color between 0-255, default 255
    # @param g: green color between 0-255, default 255
    # @param b: blue color between 0-255, default 255
    def sys_msg(self, msg: str, r: int = 255, g: int = 255, b: int = 255):
        self.__session.PBotUtils().sysMsg(msg, r, g, b)

    ## Waits until the flowermenu appears with timeout
    # @param timeout timeout to wait before returning null if menu is not opened, in milliseconds
    # @return Flowermenu or None if not found within the timeout
    def get_flowermenu(self, timeout: int = 3600000) -> Optional[PBotFlowerMenu]:
        menu = self.__session.PBotUtils().getFlowermenu(timeout)
        return PBotFlowerMenu(menu) if menu is not None else None

    ## Itemact with item in hand, for example to make a stockpile
    def make_pile(self):
        self.__session.PBotUtils().itemact(0.0, 0.0, 0)

    ## Itemact with item in hand, for example, to plant a crop or a tree
    # @param x x to click to
    # @param y y to click to
    # @param mod modifier for example 1 = shift etc
    def itemact(self, x: float, y: float, mod: int = 0):
        self.__session.PBotUtils().itemact(x, y, mod)

    ## Use to place something, for example a stockpile
    # @param x x to place stockpile to
    # @param y y to place stockpile to
    def placeThing(self, x: float, y: float):
        self.__session.PBotUtils().placeThing(x, y)

    ## Click some place on map
    # @param x x to click
    # @param y y to click
    # @param btn 1 = left click, 3 = right click
    # @param mod 1 = shift, 2 = ctrl, 4 = alt
    def map_click(self, x: float, y: float, btn: int = 1, mod: int = 0):
        self.__session.PBotUtils().mapClick(x, y, btn, mod)

    ## Use to cancel stockpile placing for example
    def cancel_place(self):
        self.__session.PBotUtils().cancelPlace()

    ## Coordinates of the center of the screen
    # @return Coordinates of the sceen center
    def get_center_screen_coord(self) -> Tuple[int, int]:
        c = self.__session.PBotUtils().getScenterScreenCoord()
        return c.x, c.y,

    ## Returns the item currently at hand
    # @return item at hand or None if not found
    def get_item_at_hand(self) -> Optional[PBotItem]:
        item = self.__session.PBotUtils().getItemAtHand()
        return PBotItem(item) if item is not None else None

    ## Left click somewhere with pathfinder
    # @param x X-Coordinate
    # @param y Y-Coordinate
    def pf_left_click(self, x: float, y: float):
        self.__session.PBotUtils().pfLeftClick(x, y)

    ## Waits for the hourglass timer when crafting or drinking for example
    # Also waits until the hourglass has been seen to change at least once
    # @param timeout timeout in milliseconds
    # @return If hourglass does not appear within timeout, returns false, else true
    def wait_for_hourglass(self, timeout: int = 3600000) -> bool:
        return self.__session.PBotUtils().waitForHourglass(timeout)

    ## Starts crafting item with the given name
    # @param name name of the item ie. "clogs"
    # @param make_all 0 to craft once, 1 to craft all
    def craft_item(self, name: str, make_all: int = 0):
        self.__session.PBotUtils().craftItem(name, make_all)

    ## Returns value of the hourglass
    # @return -1 = no hourglass else the value is between 0.0 and 1.0
    def get_hourglass(self) -> float:
        return self.__session.PBotUtils().getHourglass()

    ## Returns the players inventory
    # @return Inventory of the player
    def player_inventory(self) -> PBotInventory:
        return PBotInventory(self.__session.PBotUtils().playerInventory())

    ## Drops an item from the hand and optionally waits until it has been dropped
    # @param mod 1 = shift, 2 = ctrl, 4 = alt
    # @param wait if true, wait until the item has been dropped
    def drop_item_from_hand(self, mod: int = 0, wait: bool = False):
        self.__session.PBotUtils().dropItemFromHand(mod, wait)

    ## Resource name of the tile in the given location
    # @param x X-coord of the location (rc-coord)
    # @param y Y-coord of the location (rc-coord)
    # @return tile resname or None if couldnt determine
    def tile_resname_at(self, x: int, y: int) -> Optional[str]:
        return self.__session.PBotUtils().tileResnameAt(x, y)

    ## Next click to item in inventory calls the callback with PBotItem object of the clicked item
    # @param cb callback called with the item
    def select_item(self, cb: Callable[[PBotItem], any]):
        self.__session.PBotUtils().selectItem(_SelectItemCb(cb))

    ## Select area by dragging
    # @param cb callback called with the selected area
    def select_area(self, cb: Callable[[Tuple[float, float], Tuple[float, float]], any]):
        self.__session.PBotUtils().selectArea(_SelectAreaCb(cb))

    ## Wait for pathfinder to finish what its doing
    # @param timeout timeout in milliseconds before returning false if not finished
    # @return true if route was found and executed, false if not
    def pf_wait(self, timeout: int = 999999):
        return self.__session.PBotUtils().pfWait(timeout)

    ## Get rc coords of some gridid offset pair
    # @param grid_id mapgrid id
    # @param ofs_x x offset in mapgrid
    # @param ofs_y y offset in mapgrid
    # @param wait until the mapgrid has been loaded
    # @return coords of None if grid couldn't be found
    def get_coords(self, grid_id: int, ofs_x: float, ofs_y: float, wait: bool = True) -> Optional[Tuple[float, float]]:
        c = self.__session.PBotUtils().getCoords(grid_id, float(ofs_x), float(ofs_y), wait)
        if c is None:
            return None
        else:
            return (c.x, c.y)


class _SelectItemCb(object):
    def __init__(self, cb: Callable[[PBotItem], any]):
        self.cb = cb

    def callback(self, itm):
        self.cb(PBotItem(itm))

    class Java:
        implements = ["haven.purus.pbot.api.Callback"]



class _SelectAreaCb(object):
    def __init__(self, cb: Callable[[Tuple[float, float], Tuple[float, float]], any]):
        self.cb = cb

    def callback(self, area):
        self.cb((float(area.getA().x), float(area.getA().y),), (float(area.getB().x), float(area.getB().y),))

    class Java:
        implements = ["haven.purus.pbot.api.Callback"]
