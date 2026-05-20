/* Preprocessed source code */
package haven.res.ui.alchbook;

import java.util.*;
import haven.*;
import haven.MenuGrid.*;
import java.awt.Color;
import java.awt.image.BufferedImage;
import static haven.PType.*;

@FromResource(name = "ui/alchbook", version = 1)
public class BookFac implements Widget.Factory {
    public Widget create(UI ui, Object... args) {
	try {
	    return((Widget)(Class.forName("haven.res.ui.alchbook.Book").newInstance()));
	} catch(Throwable e) {
	    ui.error("Please update your client!");
	    new Warning(e, "cannot create alchemy book").issue();
	    return(new Label("Please update your client!"));
	}
    }
}
