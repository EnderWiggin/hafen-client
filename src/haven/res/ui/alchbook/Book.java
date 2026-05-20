/* Preprocessed source code */
package haven.res.ui.alchbook;

import java.util.*;
import haven.*;
import haven.MenuGrid.*;
import java.awt.Color;
import java.awt.image.BufferedImage;
import static haven.PType.*;

@FromResource(name = "ui/alchbook", version = 1)
public class Book extends TableBox<Recipe> {
    public static final int HEIGHT = UI.scale(24);
    public static final List<ColSpec<? super Recipe>> cols =
	Arrays.asList(ColSpec.of(UI.scale(50), 0.0, 0.5, 0.5, sorthead(HeadFactory.of(CharWnd.attrf.render("Type").tex()),    Recipe.rcpcraftorder),  (rcp, idx, sz) -> new Icon(HEIGHT, rcp.rcp)),
		      ColSpec.of(0,            1.0, 0.0, 0.5, sorthead(HeadFactory.of(CharWnd.attrf.render("Formula").tex()), Recipe.rcpinputsorder), (rcp, idx, sz) -> new Formula(rcp.inputs)),
		      ColSpec.of(0,            0.5, 0.0, 0.5, sorthead(HeadFactory.of(CharWnd.attrf.render("Effects").tex()), Recipe.rcpeffectorder), (rcp, idx, sz) -> new Effects(rcp.effects)));
    public final Map<RKey, Recipe> recipes = new HashMap<RKey, Recipe>();
    public List<Recipe> selection = Collections.emptyList();
    public Collection<Recipe> loading = new LinkedList<>();
    public Comparator<? super Recipe> order = Recipe.rcpcraftorder;
    private boolean dirty;

    public Book() {
	super(UI.scale(900, 400));
    }

    private static <T> HeadFactory<T> sorthead(HeadFactory<T> bk, Comparator<? super Recipe> order) {
	return(IHeading.wrap(bk, col -> ev -> {
	    if(ev.b != 1)
		return(false);
	    Book book = (Book)col.tbl;
	    book.order = (book.order == order) ? order.reversed() : order;
	    book.dirty = true;
	    return(true);
	}));
    }

    public List<Recipe> items() {return(selection);}
    public List<ColSpec<? super Recipe>> spec() {return(cols);}
    public int itemh() {return(HEIGHT);}
    public int headh() {return(UI.scale(32));}

    /*
    public static Book mkwidget(UI ui, Object... args) {
	return(new Book());
    }
    */

    public void tick(double dt) {
	for(Iterator<Recipe> i = loading.iterator(); i.hasNext();) {
	    Recipe rcp = i.next();
	    try {
		rcp.fin(OwnerContext.uictx.curry(ui));
		rcp.canonicalize();
	    } catch(Loading l) {
		continue;
	    }
	    i.remove();
	    recipes.put(new RKey(rcp), rcp);
	    dirty = true;
	}
	if(dirty) {
	    selection = new ArrayList<>(recipes.values());
	    Collections.sort(selection, order);
	    dirty = false;
	}
	super.tick(dt);
    }

    public void uimsg(String name, Object... args) {
	if(name == "add") {
	    Recipe rcp = Recipe.parse(OwnerContext.uictx.curry(ui), args);
	    loading.add(rcp);
	} else if(name == "addto") {
	    MenuGrid menu = (MenuGrid)ui.getwidget(Utils.iv(args[0]));
	    Pagina pag = menu.paginafor(args[1], null);
	    BookButton btn = (BookButton)pag.button();
	    btn.register(this);
	} else {
	    super.uimsg(name, args);
	}
    }
}
