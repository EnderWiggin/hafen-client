from typing import List, Optional, Tuple
from .PBotItem import PBotItem

class PBotInventory(object):
    def __init__(self, inv):
        self.__inv = inv

    ## Return all items that the inventory contains
    # @return list of items in the inventory
    def get_inventory_items(self) -> List[PBotItem]:
        return [PBotItem(x) for x in self.__inv.getInventoryContents()]

    ## Returns a list of items with name matching regex pattern from the inventory
    # @param pattern Regex pattern matching item names
    # @return List of items with name matching given pattern
    def get_inventory_items_by_names(self, pattern: str) -> List[PBotItem]:
        return [PBotItem(x) for x in self.__inv.getInventoryItemsByNames(pattern)]

    ## Returns a list of items with resname matching regex pattern from the inventory
    # @param pattern Regex pattern matching item resnames
    # @return List of items with resname matching pattern
    def get_inventory_items_by_resnames(self, pattern: str) -> List[PBotItem]:
        return [PBotItem(x) for x in self.__inv.getInventoryItemsByResnames(pattern)]

    ## Finds the item with certain location from the inventory
    # @param x x-coordinate of the item location in inventory
    # @param y y-coordinate of the item location in inventory
    # @return None if not found
    def get_item_by_location(self, x: int, y: int) -> Optional[PBotItem]:
        itm = self.__inv.getItemFromInventoryAtLocation(x, y)
        return PBotItem(itm) if itm is not None else None

    ## Drop item from the hand to given slot in the inventory
    # @param x x-coordinate in inventory to drop the item into
    # @param y y-coordinate in inventory to drop the item into
    def drop_item_to_inventory(self, x: int, y: int):
        self.__inv.dropItemToInventory(x, y)

    ## Amount of free slots in the inventory
    # @return Amount of free inventory slots
    def free_slots(self) -> int:
        return self.__inv.freeSlotsInv()

    ## Transfer 1 item by scrolling
    def xfer_to(self):
        self.__inv.xferTo()

    ## Size of the inventory slots
    # @return x y size
    def size(self) -> Tuple[int, int]:
        sz = self.__inv.size()
        return sz.x, sz.y,

## Inventory where the item is located in
# @return inventory or None if not found
def get_inventory_for_item(item: PBotItem) -> Optional["PBotInventory"]:
    inv = item._item.getInventory()
    return PBotInventory(inv) if inv is not None else None
