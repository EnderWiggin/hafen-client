package haven.purus.pbot.hnhbot.entry;

import java.util.Optional;

import haven.purus.pbot.external.HnhBot;
import haven.purus.pbot.hnhbot.BotLoader;
import haven.purus.pbot.hnhbot.dto.HnhBotConfiguration;

public class GroovyBotScriptEntry extends BotScriptEntry {

    private final HnhBotConfiguration configuration;
    private HnhBot botInstance;
    private boolean isInitialized;

    public GroovyBotScriptEntry(String name, HnhBotConfiguration configuration) {
        super(name);
        this.configuration = configuration;
    }

    @Override
    public synchronized HnhBot getBotInstance() {
        if (botInstance != null) {
            return botInstance;
        }

        Optional<HnhBot> loadedBot = BotLoader.INSTANCE.loadBot(configuration);
        botInstance = loadedBot.orElseThrow();
        isInitialized = true;
        return botInstance;
    }

    @Override
    public boolean isInitialized() {
        return isInitialized;
    }

    @Override
    public void clearBotInstance() {
        isInitialized = false;
        botInstance = null;
    }

}
