package haven.purus.pbot.gui;

import java.util.function.Consumer;

import haven.Coord;
import haven.Widget;
import haven.purus.BetterWindow;
import haven.purus.pbot.api.PBotSession;

public class CustomWindow extends BetterWindow {

    private final PBotSession session;

    public CustomWindow(Coord sz, String cap, PBotSession session) {
        super(sz, cap);
        this.session = session;
    }

    /**
     * Add a button to the window
     * Invokes function with the given name when clicked
     *
     * @param callback Function to call when button is activated
     * @param label    Label of the button
     * @param width    Width of the button
     * @param x        X-Coordinate of the button
     * @param y        Y-Coordinate of the button
     * @return PBotButton object
     */
    public CustomButton addButton(Runnable callback, String label, int width, int x, int y) {
        CustomButton button = new CustomButton(width, label) {
            @Override
            public void click() {
                Thread t = new Thread(() -> {
                    try {
                        callback.run();
                    } catch (Exception e) {
                        session.getpBotError().handleException(e);
                    }
                });
                t.start();
            }
        };
        add(button, new Coord(x, y));
        return button;
    }

    /**
     * Add a checkbox to the window
     * Invokes function with the given name with boolean as argument
     *
     * @param callback     Function to call when checkbox is ticked, value of the checkbox is used as an argument
     * @param label        Label of the checkbox
     * @param initialState Initial state of the checkbox
     * @param x            X-Coordinate of the checkbox
     * @param y            Y-Coordinate of the checkbox
     * @return PBotCheckbox object
     */
    public CustomCheckbox addCheckbox(Consumer<Boolean> callback, String label, boolean initialState, int x, int y) {
        CustomCheckbox checkbox = new CustomCheckbox(label, initialState) {
            @Override
            public void set(boolean val) {
                a = val;
                Thread t = new Thread(() -> {
                    try {
                        callback.accept(val);
                    } catch (Exception e) {
                        session.getpBotError().handleException(e);
                    }
                });
                t.start();
            }
        };
        add(checkbox, new Coord(x, y));
        return checkbox;
    }

    /**
     * Add a label to the window
     *
     * @param text Text in the label
     * @param x    X-Coordinate of the checkbox
     * @param y    Y-Coordinate of the checkbox
     * @return PBotLabel object
     */
    public CustomLabel addLabel(String text, int x, int y) {
        CustomLabel label = new CustomLabel(text);
        add(label, new Coord(x, y));
        return label;
    }

    /**
     * Add a TextEntry box to the window
     *
     * @param width       Width of the box
     * @param initialText Initial text of the box
     * @param x           X-Coordinate of the box
     * @param y           Y-Coordinate of the box
     * @return Returns TextEntry object
     */
    public CustomTextEntry addTextEntry(int width, String initialText, int x, int y) {
        CustomTextEntry te = new CustomTextEntry(width, initialText);
        add(te, new Coord(x, y));
        return te;
    }

    /**
     * Closes the window
     */
    public void closeWindow() {
        reqdestroy();
    }

    @Override
    public void wdgmsg(Widget sender, String msg, Object... args) {
        if (sender == this) {
            reqdestroy();
        } else {
            super.wdgmsg(sender, msg, args);
        }
    }

}
