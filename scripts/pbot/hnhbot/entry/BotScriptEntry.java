package haven.purus.pbot.hnhbot.entry;

import haven.*;
import haven.purus.pbot.external.HnhBot;

public abstract class BotScriptEntry extends Widget {

    private static final Tex RUNNING_ICON = Resource.loadtex("hud/script/botrunning");

    private final String name;

    public BotScriptEntry(String name) {
        this.name = name;
    }

    public abstract HnhBot getBotInstance();

    public abstract boolean isInitialized();

    public void clearBotInstance() {

    }

    public String getName() {
        return name;
    }

    @Override
    public void draw(GOut g) {
        if (isInitialized() && getBotInstance().isRunning()) {
            g.image(RUNNING_ICON, UI.scale(180, 0), UI.scale(20, 20));
        }
        g.atext(name, UI.scale(25, 0), 0, 0);
        super.draw(g);
    }

    @Override
    public Object tooltip(Coord c, Widget prev) {
        return name;
    }

}
