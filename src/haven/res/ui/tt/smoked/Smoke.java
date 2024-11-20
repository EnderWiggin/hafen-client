/* Preprocessed source code */
package haven.res.ui.tt.smoked;

import haven.*;
import java.util.*;
import java.awt.image.BufferedImage;

/* >tt: Smoke */
@FromResource(name = "ui/tt/smoked", version = 6)
public class Smoke extends ItemInfo.Tip {
    public final String name;
    public final Double val;

    public Smoke(Owner owner, String name, Double val) {
	super(owner);
	this.name = name;
	this.val = val;
    }

    public Smoke(Owner owner, String name) {
	this(owner, name, null);
    }

    public static ItemInfo mkinfo(Owner owner, Object... args) {
	int a = 1;
	String name;
	if(args[a] instanceof String) {
	    name = L10N.ingredient((String)args[a++]);
	} else if(args[a] instanceof Integer) {
	    Indir<Resource> res = owner.context(Resource.Resolver.class).getres((Integer)args[a++]);
	    Message sdt = Message.nil;
	    if((args.length > a) && (args[a] instanceof byte[]))
		sdt = new MessageBuf((byte[])args[a++]);
	    ItemSpec spec = new ItemSpec(owner, new ResData(res, sdt), null);
	    name = spec.name();
	} else {
	    throw(new IllegalArgumentException());
	}
	Double val = null;
	if(args.length > a)
	    val = (args[a] == null)?null:((Number)args[a]).doubleValue();
	return(new Smoke(owner, name, val));
    }

    public static class Line extends Tip {
	final List<Smoke> all = new ArrayList<Smoke>();

	Line() {super(null);}

	public BufferedImage tipimg() {
	    StringBuilder buf = new StringBuilder();
	    Collections.sort(all, (a, b) -> a.name.compareTo(b.name));
	    buf.append(L10N.tooltip("Smoked with "));
	    buf.append(all.get(0).descr());
	    if(all.size() > 2) {
		for(int i = 1; i < all.size() - 1; i++) {
		    buf.append(", ");
		    buf.append(all.get(i).descr());
		}
	    }
	    if(all.size() > 1) {
		buf.append(L10N.tooltip(" and "));
		buf.append(all.get(all.size() - 1).descr());
	    }
	    return(RichText.render(buf.toString(), UI.scale(250)).img);
	}
    }
    public static final Layout.ID<Line> id = Line::new;

    public void prepare(Layout l) {
	l.intern(id).all.add(this);
    }

    public String descr() {
	if(val == null)
	    return(name);
	return(String.format("%s (%d%%)", name, (int)Math.floor(val * 100.0)));
    }
}
