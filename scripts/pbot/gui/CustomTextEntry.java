package haven.purus.pbot.gui;

import haven.TextEntry;

public class CustomTextEntry extends TextEntry {

    /**
     * Created via PBotWindow
     */
    public CustomTextEntry(int w, String deftext) {
        super(w, deftext);
    }

    /**
     * Change text of the text entry
     *
     * @param text Text to change to
     */
    public void setText(String text) {
        super.settext(text);
    }

    /**
     * Get content of the text antry
     *
     * @return Text of the text entry
     */
    public String getText() {
        return super.text();
    }

}
