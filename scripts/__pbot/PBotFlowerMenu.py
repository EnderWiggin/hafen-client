from typing import List

class PBotFlowerMenu(object):
    def __init__(self, menu):
        self.__menu = menu

    ## Close this flowermenu
    def close(self):
        self.__menu.closeMenu()

    ## Choose option
    # @param name Exact name of the option
    # @return true if the petal was chosen, false if not found
    def choose_petal(self, name: str) -> bool:
        return self.__menu.choosePetal(name)

    ## Choose option by number
    # @param num option number 0-indexed
    def choose_petal_num(self, num: int):
        self.__menu.choosePetal(num)

    ## Get options of this flowermenu
    # @return list of options
    def get_petal_names(self) -> List[str]:
        return list(self.__menu.getPetalNames())