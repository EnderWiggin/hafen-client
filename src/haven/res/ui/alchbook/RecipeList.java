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
public class RecipeList extends TableBox<Recipe> implements FilterDisplay.Filtered<Recipe> {
    public static final int HEIGHT = Book.HEIGHT;
    public static final List<ColSpec<? super Recipe>> cols =
	Arrays.asList(ColSpec.of(UI.scale(50), 0.00, 0.5, 0.5, sorthead(HeadFactory.of(CharWnd.attrf.render("Type").tex()),      Recipe.rcpcraftorder),
				 (rcp, idx, sz) -> new Icon(HEIGHT, rcp.rcp, spec -> rcp.book.rl.filter(new Recipe.CraftFilter(spec)))),
		      ColSpec.of(0,            1.00, 0.0, 0.5, sorthead(HeadFactory.of(CharWnd.attrf.render("Formula").tex()),   Recipe.rcpinputsorder),
				 (rcp, idx, sz) -> new Formula(rcp.book, rcp.inputs, inp -> rcp.book.rl.filter(new Recipe.InputFilter(inp)))),
		      ColSpec.of(0,            0.25, 0.0, 0.5, sorthead(HeadFactory.of(CharWnd.attrf.render("Effects").tex()),   Recipe.rcpeffectorder),  
				 (rcp, idx, sz) -> new Effects(rcp.effects, eff -> rcp.book.rl.filter(new Recipe.EffectFilter(eff)))),
		      ColSpec.of(0,            0.25, 0.0, 0.5, sorthead(HeadFactory.of(CharWnd.attrf.render("Negatives").tex()), Recipe.rcpmmeffectorder),
				 (rcp, idx, sz) -> new Effects(rcp.mmeffects, null)));
    public final Map<RKey, Recipe> recipes = new HashMap<>();
    public final Collection<Recipe> loading = new LinkedList<>();
    public Collection<Filter<Recipe>> filters = new ArrayList<>();
    public List<Recipe> selection = Collections.emptyList();
    public Comparator<? super Recipe> order = Recipe.rcpcraftorder;
    private boolean dirty;

    public RecipeList() {
	super(UI.scale(900, 400));
    }

    private static <T> HeadFactory<T> sorthead(HeadFactory<T> bk, Comparator<? super Recipe> order) {
	return(IHeading.wrap(bk, col -> ev -> {
	    if(ev.b != 1)
		return(false);
	    RecipeList rl = (RecipeList)col.tbl;
	    rl.order = (rl.order == order) ? order.reversed() : order;
	    rl.dirty = true;
	    return(true);
	}));
    }

    public void filter(Filter<Recipe> flt) {
	if(!this.filters.contains(flt)) {
	    Collection<Filter<Recipe>> filters = new ArrayList<>(this.filters);
	    filters.add(flt);
	    filters(filters);
	}
    }

    public Collection<Filter<Recipe>> filters() {
	return(filters);
    }
    public void filters(Collection<Filter<Recipe>> flt) {
	filters = flt;
	dirty = true;
    }

    public List<Recipe> items() {return(selection);}
    public List<ColSpec<? super Recipe>> spec() {return(cols);}
    public int itemh() {return(HEIGHT);}
    public int headh() {return(UI.scale(32));}

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
	    try {
		List<Recipe> ns = new ArrayList<>();
		select: for(Recipe rcp : recipes.values()) {
		    for(Filter<Recipe> flt : filters) {
			if(!flt.test(rcp))
			    continue select;
		    }
		    ns.add(rcp);
		}
		Collections.sort(ns, order);
		selection = ns;
		dirty = false;
	    } catch(Loading l) {}
	}
	super.tick(dt);
    }

    public void add(Recipe rcp) {
	loading.add(rcp);
    }
}
