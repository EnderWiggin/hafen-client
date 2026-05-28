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
public class BookFac implements Widget.Factory {
    public Widget create(UI ui, Object... args) {
	try {
	    return((Widget)(Utils.construct(Class.forName("haven.res.ui.alchbook.Book").getConstructor())));
	} catch(Throwable e) {
	    ui.error("Please update your client!");
	    new Warning(e, "cannot create alchemy book").issue();
	    return(new Label("Please update your client!"));
	}
    }
}
