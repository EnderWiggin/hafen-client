package haven.purus.automation.gather;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import haven.purus.automation.exception.EarlyFinishException;
import haven.purus.pbot.api.PBotFlowerMenu;
import haven.purus.pbot.api.PBotGob;
import haven.purus.pbot.api.PBotSession;
import haven.purus.pbot.external.HnhBot;

public class GatherReeds extends HnhBot {

    @Override
    public void execute(PBotSession botSession) {
        Optional<PBotGob> closestReed = findClosestReed(botSession);
        int freeSlotsInv = botSession.PBotUtils().playerInventory().freeSlotsInv();

        while (closestReed.isPresent() && botSession.PBotUtils().getItemAtHand() == null && freeSlotsInv > 1 &&
               isNotInterruptedCheck()) {
            if (botSession.PBotCharacterAPI().getStamina() < 90) {
                if (!botSession.PBotUtils().drink(true)) {
                    throw new EarlyFinishException("Could not drink water");
                }
            }
            PBotGob reed = closestReed.get();
            reed.pfClick(3, 0);
            botSession.PBotUtils().pfWait(5000);
            PBotFlowerMenu flowermenu = botSession.PBotUtils().getFlowermenu(5_000);
            if (flowermenu.choosePetal("Clear")) {
                botSession.PBotUtils().waitForHourglass();
            }

            closestReed = findClosestReed(botSession);
            freeSlotsInv = botSession.PBotUtils().playerInventory().freeSlotsInv();
        }
    }

    private Optional<PBotGob> findClosestReed(PBotSession botSession) {
        PBotGob playerGob = botSession.PBotGobAPI().getPlayerGob();
        List<PBotGob> gobs = botSession.PBotGobAPI().findGobsInPlayerProximity(50);
        return gobs.stream()
                   .filter(gob -> gob.getResname() != null && gob.getResname().endsWith("reeds"))
                   .min(Comparator.comparingDouble(gob -> gob.dist(playerGob)));
    }

}
