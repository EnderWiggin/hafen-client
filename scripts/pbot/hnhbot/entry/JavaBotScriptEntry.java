package haven.purus.pbot.hnhbot.entry;

import java.util.function.Supplier;

import haven.purus.pbot.external.HnhBot;

public class JavaBotScriptEntry extends BotScriptEntry {

    private final Supplier<HnhBot> botSupplier;
    private HnhBot bot;

    public JavaBotScriptEntry(String name, Supplier<HnhBot> botSupplier) {
        super(name);
        this.botSupplier = botSupplier;
    }

    @Override
    public HnhBot getBotInstance() {
        if (bot != null) {
            return bot;
        }

        bot = botSupplier.get();
        return bot;
    }

    @Override
    public boolean isInitialized() {
        return true;
    }

    @Override
    public void clearBotInstance() {
        bot = null;
    }

}
