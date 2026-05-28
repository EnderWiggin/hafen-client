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
public class FilterDisplay <E> extends Widget {
    public final Filtered<E> cw;
    private final Widget head;
    private Object last = null;
    private Collection<Widget> dsps = Collections.emptyList();

    public FilterDisplay(Filtered<E> cw) {
	super(Coord.of(0, Book.HEIGHT));
	this.cw = cw;
	head = adda(new Label("Filters: ", CharWnd.attrf), Coord.of(0, sz.y / 2), 0.0, 0.5);
    }

    public static interface Filtered<E> {
	public Collection<Filter<E>> filters();
	public void filters(Collection<Filter<E>> flt);
    }

    public class Display extends Widget {
	public final Filter<E> f;

	public Display(Filter<E> f) {
	    super(f.icon().sz());
	    this.f = f;
	}

	public void draw(GOut g) {
	    g.image(f.icon(), Coord.z);
	}

	public boolean mousedown(MouseDownEvent ev) {
	    if(ev.b == 1) {
		Collection<Filter<E>> flt = new ArrayList<>(cw.filters());
		flt.remove(f);
		cw.filters(flt);
		return(true);
	    }
	    return(super.mousedown(ev));
	}
    }

    public void tick(double dt) {
	super.tick(dt);
	Collection<Filter<E>> cur = cw.filters();
	if(cur != last) {
	    dsps.forEach(Widget::reqdestroy);
	    Collection<Widget> ndsp = new ArrayList<>();
	    if(cur.isEmpty()) {
		ndsp.add(add(new Label("None", CharWnd.attrf), head.pos("ur")));
	    } else {
		Widget prev = head;
		for(Filter<E> flt : cur)
		    ndsp.add(prev = adda(new Display(flt), prev.pos("ur").y(sz.y / 2), 0.0, 0.5));
	    }
	    resizew(contentsz().x);
	    dsps = ndsp;
	    last = cur;
	}
    }
}
