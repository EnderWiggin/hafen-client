package haven.purus.automation.wood;


import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import haven.Gob.Type;
import haven.purus.automation.exception.BotInterruptedException;
import haven.purus.automation.exception.EarlyFinishException;
import haven.purus.pbot.api.PBotFlowerMenu;
import haven.purus.pbot.api.PBotGob;
import haven.purus.pbot.api.PBotSession;
import haven.purus.pbot.api.PBotUtils;
import haven.purus.pbot.api.PBotUtils.AreaReturn;
import haven.purus.pbot.external.HnhBot;

import static java.lang.Thread.currentThread;

public class CutTreeInArea extends HnhBot {

    private PBotSession botSession;

    @Override
    public void execute(PBotSession botSession) {
        this.botSession = botSession;

        Set<Type> typesToCut = new HashSet<>();
        typesToCut.add(Type.TREE);
        typesToCut.add(Type.BUSH);

        try {
            Optional<AreaReturn> selectedArea = botSession.PBotUtils().selectAreaSynchronously();
            if (selectedArea.isEmpty()) {
                return;
            }

            AreaReturn areaReturn = selectedArea.get();
            List<PBotGob> gobsInArea = botSession.PBotGobAPI().gobsInArea(areaReturn);
            gobsInArea.stream()
                      .filter(gob -> typesToCut.contains(gob.getGobType()))
                      .filter(gob -> gob.getResname() != null && !gob.getResname().endsWith("stump"))
                      .forEach(this::cutTree);
        } catch (Exception e) {
            botSession.PBotUtils().sysMsg("Finished with error " + e.getMessage());
            return;
        }

        botSession.PBotUtils().sysMsg("Cutting done!");
    }

    private void cutTree(PBotGob treeGob) {
        treeGob.pfClick(1, 0);

        while (!currentThread().isInterrupted() && checkPrerequisites(treeGob)) {
            treeGob.pfClick(3, 0);
            PBotFlowerMenu flowermenu = botSession.PBotUtils().getFlowermenu(15_000);
            if (flowermenu.choosePetal("Chop")) {
                botSession.PBotUtils().waitForHourglass();
            }
            if (botSession.PBotGobAPI().findGobById(treeGob.getId()) == null) {
                PBotUtils.sleep(3_000); // wait for tree to fall, so it does not mess up path finding
                return;
            }
        }

        if (currentThread().isInterrupted()) {
            throw new BotInterruptedException();
        }
    }

    private boolean checkPrerequisites(PBotGob treeGob) {
        botSession.PBotUtils().dropItemFromHand(true);

        if (botSession.PBotCharacterAPI().getEnergy() < 25) {
            throw new EarlyFinishException("Energy is too low");
        }

        if (botSession.PBotCharacterAPI().getStamina() < 80) {
            if (!botSession.PBotUtils().drink(true)) {
                throw new EarlyFinishException("Could not drink water");
            }
        }

        return botSession.PBotGobAPI().findGobById(treeGob.getId()) != null;
    }
}
