package haven.purus.pbot.gui;

import haven.CheckBox;

public class CustomCheckbox extends CheckBox {

    /**
     * Created via PBotWindow
     */
    public CustomCheckbox(String lbl, boolean val) {
        super(lbl, val);
    }

    /**
     * Set value of the checkbox
     * @param val
     */
    public void setValue(boolean val) {
        super.set(val);
    }

    /**
     * Get value of the checkbox
     * @return current value of the checkbox
     */
    public boolean getValue() {
        return super.a;
    }
}
