package haven.purus.automation.wood;


import java.util.*;
import java.util.stream.Collectors;

import haven.purus.automation.exception.EarlyFinishException;
import haven.purus.pbot.api.PBotFlowerMenu;
import haven.purus.pbot.api.PBotGob;
import haven.purus.pbot.api.PBotItem;
import haven.purus.pbot.api.PBotSession;
import haven.purus.pbot.api.PBotUtils.AreaReturn;
import haven.purus.pbot.external.HnhBot;

public class BlockChopper extends HnhBot {
    private PBotSession botSession;

    @Override
    public void execute(PBotSession botSession) {
        this.botSession = botSession;
        Optional<AreaReturn> selectedArea = botSession.PBotUtils().selectAreaSynchronously();
        if (selectedArea.isEmpty()) {
            return;
        }

        List<PBotGob> gobsInArea = botSession.PBotGobAPI().gobsInArea(selectedArea.get());
        gobsInArea.stream()
                .filter(this::isLog)
                .forEach(this::handleLog);
    }

    private boolean isLog(PBotGob gob) {
        return gob.resourceNameStartsWith("gfx/terobjs/trees") && gob.getResname().endsWith("log");
    }

    private void handleLog(PBotGob gob) {
        if (botSession.PBotGobAPI().findGobById(gob.getId()) == null) {
            return;
        }

        chopLog(gob);
        botSession.PBotUtils().dropItemFromHand(true);
        botSession.PBotUtils().drink(true);
        List<PBotGob> blocksToPickup = findBlocksToPickup();
        while (!blocksToPickup.isEmpty() && isNotInterruptedCheck()) {
            pickupBlocks(blocksToPickup);
            putIntoStockpiles();
            blocksToPickup = blocksToPickup.stream().filter(block -> botSession.PBotGobAPI().gobExists(block)).toList();
        }
    }

    private void chopLog(PBotGob gob) {
        gob.pfClick(1, 0);
        botSession.PBotUtils().pfWait(15000);

        while (isNotInterruptedCheck() && checkPrerequisites(gob)) {
            gob.pfClick(3, 0);
            PBotFlowerMenu flowerMenu = botSession.PBotUtils().getFlowermenu(15_000);
            if (flowerMenu.choosePetal("Chop into blocks")) {
                botSession.PBotUtils().waitForHourglass();
            }
            if (!botSession.PBotGobAPI().gobExists(gob)) {
                return;
            }
        }
    }

    private boolean checkPrerequisites(PBotGob gob) {
        botSession.PBotUtils().dropItemFromHand(true);

        if (botSession.PBotCharacterAPI().getEnergy() < 25) {
            throw new EarlyFinishException("Energy is too low");
        }

        if (botSession.PBotCharacterAPI().getStamina() < 80) {
            if (!botSession.PBotUtils().drink(true)) {
                throw new EarlyFinishException("Could not drink water");
            }
        }

        return botSession.PBotGobAPI().findGobById(gob.getId()) != null;
    }

    private void putIntoStockpiles() {
        List<PBotItem> inventoryContents = botSession.PBotUtils().playerInventory().getInventoryContents();
        List<PBotItem> blockInInventory = inventoryContents.stream()
                                                           .filter(item -> item.getResname()
                                                                               .startsWith("gfx/invobjs/wblock"))
                                                           .collect(Collectors.toList());
        if (blockInInventory.isEmpty()) {
            return;
        }

        while (!blockInInventory.isEmpty()) {
            List<PBotGob> gobsInProximity = botSession.PBotGobAPI().findGobsInPlayerProximity(200);
            PBotGob playerGob = botSession.PBotGobAPI().getPlayerGob();
            Optional<PBotGob> stockpile = gobsInProximity.stream().filter(this::isBlockStockpile).min(
                    Comparator.comparingDouble(o -> o.dist(playerGob)));
            if (stockpile.isEmpty()) {
                throw new EarlyFinishException("No not full stockpile found");
            }

            PBotGob targetStockpile = stockpile.get();
            targetStockpile.pfClick(3, 0);
            botSession.PBotUtils().pfWait(15000);
            botSession.PBotWindowAPI().waitForWindow("Stockpile", 5000);
            while (isBlockStockpile(targetStockpile) && !blockInInventory.isEmpty()) {
                PBotItem blockItem = blockInInventory.remove(0);
                blockItem.transferItem();
                botSession.PBotUtils().playerInventory().waitUntilInventoryChanges(5000);
            }
        }
    }

    private boolean isBlockStockpile(PBotGob gob) {
        boolean isBlockStockpile = gob.resourceNameStartsWith("gfx/terobjs/stockpile-wblock");
        return isBlockStockpile && !isStockpileFull(gob);
    }

    private boolean isStockpileFull(PBotGob gob) {
        return gob.getSdt(0) == 31;
    }

    private List<PBotGob> findBlocksToPickup() {
        List<PBotGob> gobsInProximity = botSession.PBotGobAPI().findGobsInPlayerProximity(60);
        return gobsInProximity.stream().filter(this::isBlockToPickup).toList();
    }

    private boolean isBlockToPickup(PBotGob gob) {
        return gob.resourceNameStartsWith("gfx/terobjs/items/wblock");
    }

    private void pickupBlocks(List<PBotGob> blocksToPickup) {
        blocksToPickup = new ArrayList<>(blocksToPickup);
        Optional<PBotGob> blockToPickup = findExistingGob(blocksToPickup);
        PBotGob previousBlock = null;
        int freeSlotsInv = botSession.PBotUtils().playerInventory().freeSlotsInv();
        int retries = 0;

        while (blockToPickup.isPresent() && botSession.PBotUtils().getItemAtHand() == null && freeSlotsInv > 1 &&
               isNotInterruptedCheck()) {
            PBotGob block = blockToPickup.get();
            block.pfClick(3, 0);
            botSession.PBotUtils().pfWait(5000);
            botSession.PBotUtils().playerInventory().waitUntilInventoryChanges(1000);

            blockToPickup = findExistingGob(blocksToPickup);
            freeSlotsInv = botSession.PBotUtils().playerInventory().freeSlotsInv();
            if (retries > 5) {
                blocksToPickup.remove(block);
                retries = 0;
                continue;
            }
            if (Objects.equals(previousBlock, block)) {
                retries++;
                blocksToPickup.remove(block);
                blocksToPickup.add(block);
            } else {
                retries = 0;
            }
            previousBlock = block;
        }

        botSession.PBotUtils().dropItemFromHand(true);
    }

    private Optional<PBotGob> findExistingGob(List<PBotGob> blocksToPickup) {
        return blocksToPickup.stream().filter(gob -> botSession.PBotGobAPI().gobExists(gob)).findFirst();
    }

}
