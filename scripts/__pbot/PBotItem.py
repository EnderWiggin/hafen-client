from typing import Optional, List, Tuple, Union


class PBotItem(object):
    def __init__(self, item):
        self.__item = item
        self._item = item

    def __eq__(self, other):
        if other is None:
            return False
        return self.__item.equals(other._item)

    ## Get name of the item
    # @return Name of the item or None if the name couldn't be determined
    def get_name(self) -> str:
        return self.__item.getName()

    ## Name of the item content
    # @return name of the content or None if not found
    def get_contents_name(self) -> Optional[str]:
        return self.__item.getContentsName()

    ## Get elixir effects
    # @return object describing effects, if they exist, heals are quality adjusted
    def get_elixir_info(self) -> Optional[any]:
        info = self.__item.getElixirInfo()
        if info is None:
            return None
        return {"duration": int(info.getTime()), "heals": list({"name": x.getName(), "resname": x.getResname(), "amount": x.getAmount()} for x in info.getHeals()), "hurts": list({"name": x.getName(), "resname": x.getResname(), "amount": x.getAmount()} for x in info.getHurts())}

    ## Check if item has been selected as input for craft
    # @return true if selected for craft
    def used_for_craft(self) -> bool:
        return self.__item.usedForCraft()

    ## Quality of the item content
    # @return quality of the item content or -1 if not found
    def get_contents_quality(self) -> float:
        return self.__item.getContentsQuality()

    ## Take the item to hand
    # @param wait wait for the item to appear on hand
    def take_item(self, wait: bool = False):
        self.__item.takeItem(wait)

    ## Transfer an item to the active inventory, does not wait for item to transfer
    def transfer(self):
        self.__item.transferItem()

    ## Right clicks the item in the inventory
    def activate_item(self):
        self.__item.activateItem()

    ## Iact (Right click item, like open drink flowermenu)
    # @param mod modifier for example 1 = shift etc
    def iact(self, mod):
        self.__item.iact(mod)

    ## Itemact (Act with item in hand, like equip a lure)
    # @param mod modifier for example 1 = shift etc
    def itemact(self, mod = 0):
        self.__item.itemact(mod)

    ## Get an amount of something such as seeds in a stack
    # @return amount of something in the item
    def get_amount(self) -> int:
        return self.__item.getAmount()

    ## Get location of the item in inventory
    # @return x y of the item
    def get_inv_loc(self) -> Tuple[int, int]:
        c = self.__item.getInvLoc()
        return c.x, c.y,

    ## Name of the item if it exists
    # @return name of the item or None
    def get_name(self) -> Optional[str]:
        return self.__item.getName()

    ## Resname of the item
    # @return resname of the item or None if not found
    def get_resname(self) -> Optional[str]:
        return self.__item.getResname()

    ## Quality of the item
    # @return Quality or -1 if not found
    def get_quality(self) -> float:
        return self.__item.getQuality()

    ## Quality of the item wait until calculated
    # @return Quality or -1 if not found
    def get_quality2(self) -> float:
        return self.__item.getQuality2()

    ## Drops the item from the inventory to ground, doesn't wait until drop
    def drop(self):
        self.__item.dropItemFromInventory()

    ## Size of this item
    # @return size x y
    def get_size(self) -> Tuple[int, int]:
        c = self.__item.getSize()
        return c.x, c.y,

