/* Preprocessed source code */
package haven.res.ui.tt.smoked;

import haven.*;
import java.util.*;
import java.awt.image.BufferedImage;

/* >tt: Smoke */
@haven.FromResource(name = "ui/tt/smoked", version = 8)
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

    public static ItemInfo mkinfo(ItemInfo.Owner owner, Object... args) {
	int a = 1;
	String name;
	if(args[a] instanceof String) {
	    name = (String)args[a++];
	} else {
	    Indir<Resource> res = owner.context(Resource.Resolver.class).getresv(args[a++]);
	    Message sdt = Message.nil;
	    if((args.length > a) && (args[a] instanceof byte[]))
		sdt = new MessageBuf((byte[])args[a++]);
	    ItemSpec spec = new ItemSpec(owner, new ResData(res, sdt), null);
	    name = spec.name();
	}
	Double val = null;
	if(args.length > a)
	    val = (args[a] == null) ? null : Utils.dv(args[a]);
	return(new Smoke(owner, name, val));
    }

    public static class Line extends Tip {
	final List<Smoke> all = new ArrayList<Smoke>();

	Line(Owner owner) {super(owner);}

	public BufferedImage tipimg() {
	    StringBuilder buf = new StringBuilder();
	    Collections.sort(all, (a, b) -> a.name.compareTo(b.name));
	    buf.append("Smoked with ");
	    buf.append(all.get(0).descr());
	    if(all.size() > 2) {
		for(int i = 1; i < all.size() - 1; i++) {
		    buf.append(", ");
		    buf.append(all.get(i).descr());
		}
	    }
	    if(all.size() > 1) {
		buf.append(" and ");
		buf.append(all.get(all.size() - 1).descr());
	    }
	    return(RichText.render(buf.toString(), 250).img);
	}
    }
    public static final Layout.TipID<Line> id = Line::new;

    public void prepare(Layout l) {
	l.intern(id).all.add(this);
    }

    public String descr() {
	if(val == null)
	    return(name);
	return(String.format("%s (%d%%)", name, (int)Math.floor(val * 100.0)));
    }
}
