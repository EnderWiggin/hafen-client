package haven.purus.pbot.hnhbot;

import java.util.ArrayList;
import java.util.List;

import haven.UI;
import haven.Widget;
import haven.purus.BetterWindow;
import haven.purus.automation.discovery.DiscoveryBot;
import haven.purus.automation.fish.Fisher;
import haven.purus.automation.gather.GatherReeds;
import haven.purus.automation.wood.BlockChopper;
import haven.purus.automation.wood.CutTreeInArea;
import haven.purus.automation.wood.StumpRemover;
import haven.purus.pbot.hnhbot.dto.HnhBotConfiguration;
import haven.purus.pbot.hnhbot.entry.BotScriptEntry;
import haven.purus.pbot.hnhbot.entry.GroovyBotScriptEntry;
import haven.purus.pbot.hnhbot.entry.JavaBotScriptEntry;

public class HnhBotWindow extends BetterWindow {

    private final List<BotScriptEntry> entries = new ArrayList<>();

    public HnhBotWindow() {
        super(UI.scale(200, 300), "HnH Bot Scripts");
        loadEntries();
        HnhBotListbox listbox = new HnhBotListbox(UI.scale(200), 15, UI.scale(20), this);
        add(listbox);
    }

    public void loadEntries() {
        List<HnhBotConfiguration> configurations = BotLoader.INSTANCE.loadConfigurations();
        configurations.forEach(config -> entries.add(new GroovyBotScriptEntry(config.name(), config)));
        entries.add(new JavaBotScriptEntry("Discovery", DiscoveryBot::new));
        entries.add(new JavaBotScriptEntry("Cut trees", CutTreeInArea::new));
        entries.add(new JavaBotScriptEntry("BlockChopper", BlockChopper::new));
        entries.add(new JavaBotScriptEntry("StumpRemover", StumpRemover::new));
        entries.add(new JavaBotScriptEntry("GatherReeds", GatherReeds::new));
        entries.add(new JavaBotScriptEntry("Fisher", Fisher::new));
    }

    public List<BotScriptEntry> getEntries() {
        return entries;
    }

    @Override
    public void wdgmsg(Widget sender, String msg, Object... args) {
        if ((sender == this) && msg.equals("close")) {
            this.hide();
        }
        super.wdgmsg(sender, msg, args);
    }

}
