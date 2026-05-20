/* Preprocessed source code */
package haven.res.ui.alchbook;

import java.util.*;
import haven.*;
import haven.MenuGrid.*;
import java.awt.Color;
import java.awt.image.BufferedImage;
import static haven.PType.*;

@FromResource(name = "ui/alchbook", version = 1)
public class BookButton extends PagButton {
    public final GameUI gui;
    public Window wnd;

    public BookButton(Pagina pag) {
	super(pag);
	this.gui = pag.scm.getparent(GameUI.class);
    }

    public void register(Widget book) {
	wnd = new BookWindow();
	wnd.add(book, Coord.z);
	wnd.pack();
	gui.addchild(wnd, "misc", new Coord2d(0.3, 0.3), new Object[] {"id", "alchbook"});
    }

    public void use(Interaction iact) {
	if(wnd == null) {
	    super.use(iact);
	} else {
	    if(wnd.show(!wnd.visible)) {
		wnd.raise();
		gui.setfocus(wnd);
	    }
	}
    }
}
