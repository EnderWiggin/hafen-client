/* Preprocessed source code */
package haven.res.ui.alchbook;

import java.util.*;
import java.util.function.*;
import haven.*;
import haven.MenuGrid.*;
import java.awt.Color;
import java.awt.image.BufferedImage;
import static haven.PType.*;

@FromResource(name = "ui/alchbook", version = 4)
public class TabButton extends IButton {
    public TabButton(int up, int down) {
	super(Resource.classres(TabButton.class).flayer(Resource.imgc, up).scaled(),
	      Resource.classres(TabButton.class).flayer(Resource.imgc, down).scaled());
    }

    protected void depress() {
	ui.sfx(Button.clbtdown.stream());
    }

    protected void unpress() {
	ui.sfx(Button.clbtup.stream());
    }
}

/* >wdg: BookFac */
