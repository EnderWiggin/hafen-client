package haven.purus.pbot.hnhbot;

import java.util.HashMap;
import java.util.Map;

import haven.GOut;
import haven.Listbox;
import haven.purus.pbot.api.PBotSession;
import haven.purus.pbot.external.HnhBot;
import haven.purus.pbot.hnhbot.entry.BotScriptEntry;

public class HnhBotListbox extends Listbox<BotScriptEntry> {

    private final HnhBotWindow hnhBotWindow;
    private final Map<String, Thread> threadCache = new HashMap<>();

    public HnhBotListbox(int w, int h, int itemh, HnhBotWindow hnhBotWindow) {
        super(w, h, itemh);
        this.hnhBotWindow = hnhBotWindow;
    }

    @Override
    protected BotScriptEntry listitem(int i) {
        return hnhBotWindow.getEntries().get(i);
    }

    @Override
    protected int listitems() {
        return hnhBotWindow.getEntries().size();
    }

    @Override
    protected void drawitem(GOut g, BotScriptEntry item, int i) {
        item.draw(g);
    }

    @Override
    protected void itemclick(BotScriptEntry item, int button) {
        if (button == 1) {
            try {
                useEntry(item);
            } catch (Exception e) {
                System.out.println("Starting script failed");
                e.printStackTrace();
            }
        }
        super.itemclick(item, button);
    }

    public void useEntry(BotScriptEntry item) {
        String name = item.getName();
        if (threadCache.containsKey(name)) {
            if (item.isInitialized() && item.getBotInstance() != null) {
                item.getBotInstance().stop();
            }

            Thread thread = threadCache.get(name);
            thread.interrupt();
            threadCache.remove(name);
            item.clearBotInstance();
            return;
        }

        var thread = createScriptThread(item);
        threadCache.put(name, thread);
        thread.start();
    }

    private Thread createScriptThread(BotScriptEntry item) {
        return new Thread(() -> {
            try {
                HnhBot bot = item.getBotInstance();
                PBotSession botSession = new PBotSession(gameui());
                bot.run(botSession, Thread.currentThread());
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                threadCache.remove(item.getName());
                item.clearBotInstance();
            }
        }, "Script_" + item.getName());
    }

}
