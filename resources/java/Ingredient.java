import haven.*;
import java.util.*;
import java.awt.image.BufferedImage;
import haven.res.lib.tspec.Spec;

/* >tt: Ingredient$Fac */
public class Ingredient extends ItemInfo.Tip {
    public final String name;
    public final Double val;

    public Ingredient(Owner owner, String name, Double val) {
	super(owner);
	this.name = name;
	this.val = val;
    }

    public Ingredient(Owner owner, String name) {
	this(owner, name, null);
    }

    public static class Fac implements InfoFactory {
	public ItemInfo build(Owner owner, Raw raw, Object... args) {
	    int a = 1;
	    String name;
	    if(args[a] instanceof String) {
		name = L10N.ingredient((String)args[a++]);
	    } else if(args[1] instanceof Integer) {
		Indir<Resource> res = owner.context(Resource.Resolver.class).getres((Integer)args[a++]);
		Message sdt = Message.nil;
		if((args.length > a) && (args[a] instanceof byte[]))
		    sdt = new MessageBuf((byte[])args[a++]);
		Spec spec = new Spec(new ResData(res, sdt), owner, null);
		name = spec.name();
	    } else {
		throw(new IllegalArgumentException());
	    }
	    Double val = null;
	    if(args.length > a)
		val = (args[a] == null)?null:((Number)args[a]).doubleValue();
	    return(new Ingredient(owner, name, val));
	}
    }

    public static class Line extends Tip {
	final List<Ingredient> all = new ArrayList<>();

	Line() {super(null);}

	public BufferedImage tipimg() {
	    StringBuilder buf = new StringBuilder();
	    all.sort(Comparator.comparing(a -> a.name));
	    buf.append(L10N.tooltip("Made from "));
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
	    return L10N.ingredient(name);
	return(String.format("%s (%d%%)", name, (int)Math.floor(val * 100.0)));
    }
}
