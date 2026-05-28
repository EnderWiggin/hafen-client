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
public class EffectList extends TableBox<KnownEffects> implements FilterDisplay.Filtered<KnownEffects> {
    public static final int HEIGHT = Book.HEIGHT;
    public static final List<ColSpec<? super KnownEffects>> cols =
	Arrays.asList(ColSpec.of(UI.scale(50), 0.00, 0.5, 0.5, sorthead(HeadFactory.of(CharWnd.attrf.render("Type").tex()),    KnownEffects.ikinputorder),
				 (ik, idx, sz) -> new Icon(HEIGHT, ik.input.type)),
		      ColSpec.of(0,            1.00, 0.0, 0.5, sorthead(HeadFactory.of(CharWnd.attrf.render("Effects").tex()), KnownEffects.kieffectorder),
				 (ik, idx, sz) -> new Effects(ik.effs, eff -> ik.book.el.filter(new KnownEffects.EffectFilter(eff)))));
    public final Map<Input, KnownEffects> knowledge = new HashMap<>();
    public final Collection<KnownEffects> loading = new LinkedList<>();
    public Collection<Filter<KnownEffects>> filters = new ArrayList<>();
    public List<KnownEffects> selection = Collections.emptyList();
    public Comparator<? super KnownEffects> order = KnownEffects.ikinputorder;
    private boolean dirty;

    public EffectList() {
	super(UI.scale(900, 400));
    }

    private static <T> HeadFactory<T> sorthead(HeadFactory<T> bk, Comparator<? super KnownEffects> order) {
	return(IHeading.wrap(bk, col -> ev -> {
	    if(ev.b != 1)
		return(false);
	    EffectList el = (EffectList)col.tbl;
	    el.order = (el.order == order) ? order.reversed() : order;
	    el.dirty = true;
	    return(true);
	}));
    }

    public void filter(Filter<KnownEffects> flt) {
	if(!this.filters.contains(flt)) {
	    Collection<Filter<KnownEffects>> filters = new ArrayList<>(this.filters);
	    filters.add(flt);
	    filters(filters);
	}
    }

    public Collection<Filter<KnownEffects>> filters() {
	return(filters);
    }
    public void filters(Collection<Filter<KnownEffects>> flt) {
	filters = flt;
	dirty = true;
    }

    public List<KnownEffects> items() {return(selection);}
    public List<ColSpec<? super KnownEffects>> spec() {return(cols);}
    public int itemh() {return(HEIGHT);}
    public int headh() {return(UI.scale(32));}

    public void tick(double dt) {
	for(Iterator<KnownEffects> i = loading.iterator(); i.hasNext();) {
	    KnownEffects ik = i.next();
	    try {
		ik.fin(OwnerContext.uictx.curry(ui));
	    } catch(Loading l) {
		continue;
	    }
	    i.remove();
	    knowledge.put(ik.input, ik);
	    dirty = true;
	}
	if(dirty) {
	    selection = new ArrayList<>();
	    select: for(KnownEffects ik : knowledge.values()) {
		for(Filter<KnownEffects> flt : filters) {
		    if(!flt.test(ik))
			continue select;
		}
		selection.add(ik);
	    }
	    Collections.sort(selection, order);
	    dirty = false;
	}
	super.tick(dt);
    }

    public void add(KnownEffects ik) {
	loading.add(ik);
    }
}
