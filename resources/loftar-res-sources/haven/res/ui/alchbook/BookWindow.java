/* Preprocessed source code */
package haven.res.ui.alchbook;

import java.util.*;
import java.util.function.*;
import haven.*;
import haven.MenuGrid.*;
import java.awt.Color;
import java.awt.image.BufferedImage;
import static haven.PType.*;

@haven.FromResource(name = "ui/alchbook", version = 3)
public class BookWindow extends Window {
    public BookWindow() {
	super(Coord.z, "Alchemy Book", true);
    }

    public void reqclose() {
	hide();
    }
}

/* >pagina: BookButton */
