/* Preprocessed source code */
package haven.res.ui.tt.slot;

import haven.*;
import static haven.PUtils.*;
import java.awt.image.*;
import java.awt.Graphics;
import java.util.*;

/* >tt: Slotted */
@haven.FromResource(name = "ui/tt/slot", version = 18)
public class Slotted extends ItemInfo.Tip {
    public static final Text.Line ch = Text.render("As gilding:");
    public final double pmin, pmax;
    public final Resource[] attrs;
    public final List<ItemInfo> sub;

    public Slotted(Owner owner, double pmin, double pmax, Resource[] attrs, List<ItemInfo> sub) {
	super(owner);
	this.pmin = pmin;
	this.pmax = pmax;
	this.attrs = attrs;
	this.sub = sub;
    }

    public static ItemInfo mkinfo(Owner owner, Object... args) {
	Resource.Resolver rr = owner.context(Resource.Resolver.class);
	int a = 1;
	double pmin = ((Number)args[a++]).doubleValue();
	double pmax = ((Number)args[a++]).doubleValue();
	List<Resource> attrs = new LinkedList<Resource>();
	while(args[a] instanceof Integer)
	    attrs.add(rr.getres((Integer)args[a++]).get());
	Object[] raw = (Object[])args[a++];
	return(new Slotted(owner, pmin, pmax, attrs.toArray(new Resource[0]), buildinfo(owner, raw)));
    }

    public static final String chc = "192,192,255";
    public void layout(Layout l) {
	l.cmp.add(ch.img, new Coord(0, l.cmp.sz.y));
	if(attrs.length > 0) {
	    BufferedImage head = RichText.render(String.format("Chance: $col[%s]{%d%%} to $col[%s]{%d%%}", chc, Math.round(100 * pmin), chc, Math.round(100 * pmax)), 0).img;
	    int h = head.getHeight();
	    int x = 10, y = l.cmp.sz.y;
	    l.cmp.add(head, new Coord(x, y));
	    x += head.getWidth() + 10;
	    for(int i = 0; i < attrs.length; i++) {
		BufferedImage icon = convolvedown(attrs[i].layer(Resource.imgc).img, new Coord(h, h), CharWnd.iconfilter);
		l.cmp.add(icon, new Coord(x, y));
		x += icon.getWidth() + 2;
	    }
	} else {
	    BufferedImage head = RichText.render(String.format("Chance: $col[%s]{%d%%}", chc, (int)Math.round(100 * pmin)), 0).img;
	    l.cmp.add(head, new Coord(10, l.cmp.sz.y));
	}

	BufferedImage stip = longtip(sub);
	if(stip != null)
	    l.cmp.add(stip, new Coord(10, l.cmp.sz.y));
    }

    public int order() {
	return(200);
    }
}
