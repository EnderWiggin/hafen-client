package haven.purus.automation.fish;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

import haven.*;
import haven.ChatUI.Channel.Message;
import haven.purus.pbot.api.*;
import haven.purus.pbot.api.model.CharacterAct;
import haven.purus.pbot.external.HnhBot;
import haven.purus.pbot.gui.CustomLabel;
import haven.purus.pbot.gui.CustomTextEntry;
import haven.purus.pbot.gui.CustomWindow;

import static haven.purus.pbot.api.PBotUtils.sleep;

public class Fisher extends HnhBot {

    private final static DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss");
    private static final int LOG_SIZE = 14;
    private PBotSession session;
    private List<CustomLabel> logEntries = new ArrayList<>();
    private CustomWindow window;
    private boolean stop;

    private Message lastMessage = null;

    private String stringRes;
    private String hookRes;
    private String lureRes;
    private Coord2d fishingLocation;

    @Override
    public void execute(PBotSession botSession) {
        session = botSession;
        window = createWindow();
        logEntries = createViewWindow(window);
        startMainLoop();
    }

    private void startMainLoop() {
        var fishNameInput = window.addTextEntry(310, "Eel, Perch, Bream, Plaice, Zander, Salmon", 0, 5);
        stringRes = selectItem("Click fishing string in inventory!", "String used: ");
        if (shouldStop()) {
            return;
        }

        hookRes = selectItem("Click hook in inventory!", "Hook used: ");
        if (shouldStop()) {
            return;
        }

        lureRes = selectItem("Click lure in inventory!", "Lure used: ");
        if (shouldStop()) {
            return;
        }

        logStatusMessage("Type in desired fish names and start fishing to begin!");
        var bobber = waitForFishingBobber(180000);
        if (bobber.isEmpty()) {
            logStatusMessage("Unable to detect fishing bobber!");
            stop = true;
            return;
        }

        fishingLocation = bobber.get().getCoords();
        logStatusMessage("Bot started. Target fish: " + parseFishNames(fishNameInput.getText()));
        mainLoop(fishNameInput);
    }

    private void mainLoop(CustomTextEntry fishNameInput) {
        PBotSession botSession = session;
        PBotUtils utils = botSession.PBotUtils();
        while (!shouldStop()) {
            //Drop hand items
            PBotItem itemAtHand = utils.getItemAtHand();
            if (itemAtHand != null) {
                if (utils.playerInventory().freeSpaceForItem(itemAtHand) == null) {
                    logStatusMessage("Inventory is full! Make space to continue!", true);
                    sleep(7500);
                    continue;
                }
            }

            //Look for bobber pos to know where to fish if stopped
            var bobber = getFishingBobber();
            bobber.ifPresent(pBotGob -> fishingLocation = pBotGob.getCoords());

            //Parse sysmessages to see if something breaks and repair
            var equipmentStatus = checkEquipmentStatus();
            if (equipmentStatus.isMissingSomething()) {
                if (!repairRod(equipmentStatus)) {
                    logStatusMessage("Failed to repair rod! Stopping!");
                    stop = true;
                    continue;
                }

                if (!resumeFishing()) {
                    logStatusMessage("Failed to resume fishing! Stopping!");
                    stop = true;
                    continue;
                }
            }

            //Press aim for button in popup window
            if (botSession.PBotWindowAPI().getWindow("This is bait") != null) {
                if (pressFishAimForButton(parseFishNames(fishNameInput.getText()))) {
                    PBotUtils.sleep(250);
                } else {
                    logStatusMessage("Target fish no longer available in this spot! Change location!", true);
                    PBotUtils.sleep(5000);
                    continue;
                }
            }

            sleep(50);
        }
    }

    private String selectItem(String commandText, String infoText) {
        logStatusMessage(commandText);

        AtomicReference<String> resName = new AtomicReference<>();
        session.PBotUtils().selectItem((item) -> {
            resName.set(item.getResname());
        });
        waitUntilFalse(() -> resName.get() == null);

        logStatusMessage(infoText + resName.get());
        return resName.get();
    }

    private List<CustomLabel> createViewWindow(CustomWindow window) {
        List<CustomLabel> logEntries = new ArrayList<>();
        for (var i = 0; i <= LOG_SIZE; i++) {
            var label = window.addLabel("", 2, 20 + i * 12);
            label.setColor(255, 255, 255);
            logEntries.add(label);
        }
        return logEntries;
    }

    private CustomWindow createWindow() {
        Coord windowSize = new Coord(320, 340);
        var window = session.PBotWindowAPI().createWindow(windowSize, "Fisher", "Fisher");
        session.getInternalGui().add(window);
        window.show();
        return window;
    }

    private void logMessage(String message) {
        var dateString = LocalDateTime.now().format(timeFormatter);
        String logEntry = dateString + " | " + message;
        for (int i = 1; i < logEntries.size(); i++) {
            logEntries.get(i - 1).settext(logEntries.get(i).getText());
        }
        logEntries.get(logEntries.size() - 1).setText(logEntry);
    }

    private void logStatusMessage(String message) {
        logStatusMessage(message, false);
    }

    private void logStatusMessage(String message, boolean sysMessage) {
        if (sysMessage) {
            session.PBotUtils().sysMsg(message);
        }
        logMessage(message);
    }

    private boolean shouldStop() {
        if (Thread.interrupted()) {
            logStatusMessage("Script interrupted", true);
            return true;
        }
        if (stop) {
            logStatusMessage("Stopping due to stop flag set", true);
            return true;
        }
        if (window.parent == null) {
            logStatusMessage("Stopping due to bot window closing.", true);
            return true;
        }
        double energy = session.PBotCharacterAPI().getEnergy();
        if (energy < 30) {
            logStatusMessage("Stopping due to low energy (" + energy + ")", true);
            return true;
        }
        return false;
    }

    ;

    public void waitUntilFalse(Supplier<Boolean> supplier) {
        while (supplier.get()) {
            sleep(50);
            if (Thread.currentThread().isInterrupted() || shouldStop()) {
                throw new RuntimeException("Interrupt ed");
            }
        }
    }

    private Optional<PBotGob> waitForFishingBobber(long timeout) {
        var tickDuration = 50;
        var timeWaited = 0;
        Optional<PBotGob> bobber;
        while (!shouldStop()) {
            if (timeWaited >= timeout) {
                return Optional.empty();
            }

            bobber = getFishingBobber();
            if (bobber.isPresent()) {
                return bobber;
            }
            sleep(tickDuration);
        }

        return Optional.empty();
    }

    private Optional<PBotGob> getFishingBobber() {
        return session.PBotGobAPI().getGobsByResname("gfx/fx/float").stream().findFirst();
    }

    private List<String> parseFishNames(String namesString) {
        namesString = namesString.replace(",", " ");
        namesString = namesString.replaceAll("\\s+", " ");
        namesString = namesString.trim();
        namesString = namesString.toLowerCase();
        return Arrays.stream(namesString.split(" ")).toList();
    }

    private boolean pressFishAimForButton(List<String> fishNames) {
        PBotSession botSession = session;
        var fishwindow = botSession.PBotWindowAPI().getWindow("This is bait");
        if (fishwindow == null) {
            return false;
        }

        List<Widget> windowWidgets = new ArrayList<>();
        for (var wdg = fishwindow.window.lchild; wdg != null; wdg = wdg.prev) {
            windowWidgets.add(wdg);
        }
        Collections.reverse(windowWidgets);

        boolean found = false;
        Button lastButton = null;
        for (var wdg : windowWidgets) {
            if (found) {
                break;
            }
            if (wdg instanceof Label label) {
                var text = label.text.text.toLowerCase();
                for (var fishName : fishNames) {
                    if (text.contains(fishName)) {
                        logStatusMessage("Selected " + text);
                        found = true;
                        break;
                    }
                }
            }
            if (wdg instanceof Button button) {
                lastButton = button;
            }
        }

        if (lastButton != null && found) {
            lastButton.wdgmsg("activate");
            return true;
        }

        return false;
    }

    private EquipmentStatus checkEquipmentStatus() {
        try {
            var messages = session.getInternalGui().syslog.msgs;
            var msg = messages.get(messages.size() - 1);
            if (lastMessage != null && Math.abs(msg.time - lastMessage.time) > 1) {
                var msgtxt = msg.getText();
                lastMessage = msg;
                if (msgtxt.contains("your lure")) {
                    logStatusMessage("Lost lure!");
                    return new EquipmentStatus(false, true, true);
                }
                if (msgtxt.contains("hook broke")) {
                    logStatusMessage("Lost hook!");
                    return new EquipmentStatus(false, true, false);
                }
                if (msgtxt.contains("line broke")) {
                    logStatusMessage("Lost string!");
                    return new EquipmentStatus(false, false, false);
                }
            }
            lastMessage = msg;
        } catch (Exception e) {
            session.PBotUtils().sysMsg("Error printed to log! %s".formatted(e.getMessage()));
            session.getpBotError().handleException(e);
        }
        return new EquipmentStatus(true, true, true);
    }

    private boolean repairRod(EquipmentStatus equipmentStatus) {
        PBotInventory inventory = session.PBotUtils().playerInventory();
        if (!equipmentStatus.haveString()) {
            logStatusMessage("Repairing rod string");
            var items = inventory.getInventoryItemsByResnames(stringRes);
            if (items.isEmpty()) {
                logStatusMessage("Failed to find string item", true);
                return false;
            }
            var item = items.get(0);
            if (!applyItemToRod(item)) {
                logStatusMessage("Failed to apply string to rod", true);
                return false;
            }
        }

        if (!equipmentStatus.haveHook()) {
            logStatusMessage("Repairing rod hook");
            var items = inventory.getInventoryItemsByResnames(hookRes);
            if (items.isEmpty()) {
                logStatusMessage("Failed to find hook item", true);
                return false;
            }
            var item = items.get(0);
            if (!applyItemToRod(item)) {
                logStatusMessage("Failed to apply hook to rod", true);
                return false;
            }
        }

        if (!equipmentStatus.haveLure()) {
            logStatusMessage("Repairing rod lure");
            var items = inventory.getInventoryItemsByResnames(lureRes);
            if (items.isEmpty()) {
                logStatusMessage("Failed to find lure item", true);
                return false;
            }
            var item = items.get(0);
            if (!applyItemToRod(item)) {
                logStatusMessage("Failed to apply lure to rod", true);
                return false;
            }
        }

        return true;
    }

    private boolean applyItemToRod(PBotItem applyItem) {
        PBotUtils utils = session.PBotUtils();
        if (utils.getItemAtHand() != null) {
            var playerInv = utils.playerInventory();
            var coord = playerInv.freeSpaceForItem(utils.getItemAtHand());
            if (coord == null) {
                logStatusMessage("Failed to drop existing item from hand", true);
                return false;
            }
            playerInv.dropItemToInventory(coord.x, coord.y);

            if (!waitForHandEmpty(false, 2000)) {
                logStatusMessage("Failed to drop existing item from hand", true);
                return false;
            }
        }

        List<PBotItem> equipment = session.PBotCharacterAPI().getEquipment();
        PBotItem rodItem = null;
        boolean foundRod = false;
        for (PBotItem item : equipment) {
            if (item != null && item.getName() != null) {
                if (item.getName().toLowerCase().contains("rod")) {
                    rodItem = item;
                    foundRod = true;
                    break;
                }
            }
        }

        if (!foundRod) {
            logStatusMessage("Failed to find rod item in hands!", true);
            return false;
        }

        applyItem.takeItem(false);
        if (!waitForHandEmpty(true, 5000)) {
            logStatusMessage("Failed to grab item from inventory", true);
            return false;
        }

        rodItem.itemact(0);
        if (!waitForHandEmpty(false, 5000)) {
            logStatusMessage("Failed to apply item on rod", true);
            return false;
        }

        return true;
    }

    private boolean waitForHandEmpty(boolean state, int timeout) {
        var tickDuration = 50;
        var timeWaited = 0;
        while ((session.PBotUtils().getItemAtHand() == null) == state) {
            if (timeWaited >= timeout) {
                return false;
            }
            PBotUtils.sleep(tickDuration);
            timeWaited += tickDuration;
        }
        return true;
    }

    private boolean resumeFishing() {
        PBotSession botSession = session;
        PBotCharacterAPI characterAPI = botSession.PBotCharacterAPI();
        characterAPI.cancelAct();
        PBotUtils.sleep(250);
        characterAPI.doAct(CharacterAct.FISH);
        PBotUtils.sleep(250);
        botSession.PBotUtils().mapClick(fishingLocation.floor().x, fishingLocation.floor().y, 1, 0);
        logStatusMessage("Clicked on water to fish, waiting for bobber to appear");
        var bobber = waitForFishingBobber(5000);
        if (bobber.isEmpty()) {
            logStatusMessage("Unable to start fishing! Timed out while waiting for bobber");
            return false;
        }

        logStatusMessage("Successfully started fishing!");
        characterAPI.cancelAct();
        return true;
    }

    ;

    private record EquipmentStatus(boolean haveLure, boolean haveString, boolean haveHook) {

        public boolean isMissingSomething() {
            return !haveLure || !haveHook || !haveString;
        }

    }

}
