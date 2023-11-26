package haven.purus.pbot.gui;

import haven.Button;

import java.awt.*;

public class CustomButton extends Button {

    /**
     * Created via PBotWindow
     */
    public CustomButton(int w, String text) {
        super(w, text);
    }

    /**
     * Change text of the button
     * @param text
     */
    public void changeText(String text) {
        super.change(text);
    }

    /**
     * Delete the button
     */
    public void destroy() {
        super.destroy();
    }

    /**
     * Change color of the buttons text
     * @param r Amount of red color, between 0-255
     * @param g Amount of green color, between 0-255
     * @param b Amount of blue color, between 0-255
     */
    public void changeColor(int r, int g, int b) {
        super.change(super.text.text, new Color(r, g, b));
    }
}
