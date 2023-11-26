package haven.purus.pbot.api.model;

public enum CharacterAct {
    DIG("dig"),
    MINE("mine"),
    CARRY("carry"),
    DESTROY("destroy"),
    FISH("fish"),
    INSPECT("inspect"),
    REPAIR("repair"),
    CRIME("crime"),
    SWIM("swim"),
    TRACKING("tracking"),
    AGGRO("aggro"),
    SHOOT("shoot"),
    DRINK("drink");

    private final String actMessage;

    CharacterAct(String actMessage) {
        this.actMessage = actMessage;
    }

    public String getActMessage() {
        return actMessage;
    }

}
