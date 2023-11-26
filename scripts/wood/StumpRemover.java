package haven.purus.automation.wood;

import java.util.List;
import java.util.Optional;

import haven.purus.automation.exception.EarlyFinishException;
import haven.purus.pbot.api.PBotGob;
import haven.purus.pbot.api.PBotSession;
import haven.purus.pbot.api.PBotUtils.AreaReturn;
import haven.purus.pbot.api.model.CharacterAct;
import haven.purus.pbot.external.HnhBot;

public class StumpRemover extends HnhBot {
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
                .filter(gob -> gob.getResname() != null && gob.getResname().endsWith("stump"))
                .forEach(this::removeStump);
    }

    void removeStump(PBotGob gob) {
        gob.pfClick(1, 0);
        botSession.PBotUtils().pfWait(15000);

        while (isNotInterruptedCheck() && checkPrerequisites(gob)) {
            botSession.PBotCharacterAPI().doAct(CharacterAct.DESTROY);
            gob.pfClick(1, 0);
            botSession.PBotUtils().pfWait(15000);
            botSession.PBotUtils().waitForHourglass();
            if (botSession.PBotGobAPI().findGobById(gob.getId()) == null) {
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

}
