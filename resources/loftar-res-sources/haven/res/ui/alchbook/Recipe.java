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
public class Recipe {
    public static final Comparator<ItemSpec> iconorder = Comparator.comparing(ItemSpec::name);
    public static final Comparator<Input> inputorder = new Comparator<Input>() {
	public int compare(Input a, Input b) {
	    int c = iconorder.compare(a.type, b.type);
	    if(c != 0)
		return(c);
	    return(inputsorder.compare(a.sub, b.sub));
	}
    };
    public static final Comparator<List<Input>> inputsorder = new Comparator<List<Input>>() {
	public int compare(List<Input> a, List<Input> b) {
	    for(int i = 0; i < Math.max(a.size(), b.size()); i++) {
		if(i >= a.size())
		    return(-1);
		if(i >= b.size())
		    return(1);
		int c = inputorder.compare(a.get(i), b.get(i));
		if(c != 0)
		    return(c);
	    }
	    return(0);
	}
    };
    public static final Comparator<Recipe> rcpcraftorder = new Comparator<Recipe>() {
	public int compare(Recipe a, Recipe b) {
	    int c = iconorder.compare(a.rcp, b.rcp);
	    if(c != 0)
		return(c);
	    return(inputsorder.compare(a.inputs, b.inputs));
	}
    };
    public static final Comparator<Recipe> rcpinputsorder = new Comparator<Recipe>() {
	public int compare(Recipe a, Recipe b) {
	    int c = inputsorder.compare(a.inputs, b.inputs);
	    if(c != 0)
		return(c);
	    return(iconorder.compare(a.rcp, b.rcp));
	}
    };
    public static final Comparator<List<EffectInfo>> effectorder = new Comparator<List<EffectInfo>>() {
	public int compare(List<EffectInfo> a, List<EffectInfo> b) {
	    for(int i = 0; i < Math.max(a.size(), b.size()); i++) {
		if(i >= a.size())
		    return(-1);
		if(i >= b.size())
		    return(1);
		int c = a.get(i).sortkey().compareTo(b.get(i).sortkey());
		if(c != 0)
		    return(c);
	    }
	    return(0);
	}
    };
    public static final Comparator<Recipe> rcpeffectorder = Comparator.comparing((Recipe rcp) -> rcp.effects, effectorder).thenComparing(rcpcraftorder);
    public static final Comparator<Recipe> rcpmmeffectorder = Comparator.comparing((Recipe rcp) -> rcp.mmeffects, effectorder).thenComparing(rcpeffectorder);
    public final Book book;
    public final ItemSpec rcp;
    public final List<Input> inputs;
    public final List<EffectInfo> effects, mmeffects;
    private List<EffectSpec> raweffects, rawmmeffects;

    public Recipe(Book book, ItemSpec rcp, List<Input> inputs, List<EffectSpec> raweffects, List<EffectSpec> rawmmeffects) {
	this.book = book;
	this.rcp = rcp;
	this.inputs = inputs;
	this.effects = new ArrayList<>();
	this.mmeffects = new ArrayList<>();
	this.raweffects = raweffects;
	this.rawmmeffects = rawmmeffects;
    }

    public void fin(OwnerContext owner) {
	for(Iterator<EffectSpec> i = raweffects.iterator(); i.hasNext();) {
	    EffectSpec raw = i.next();
	    raw.resolve(effects);
	    i.remove();
	}
	for(Iterator<EffectSpec> i = rawmmeffects.iterator(); i.hasNext();) {
	    EffectSpec raw = i.next();
	    raw.resolve(mmeffects);
	    i.remove();
	}
    }

    private void canonicalize(Input inp) {
	for(Input sub : inp.sub)
	    canonicalize(sub);
	Collections.sort(inp.sub, inputorder);
    }

    public void canonicalize() {
	for(Input inp : inputs)
	    canonicalize(inp);
	Collections.sort(inputs, inputorder);
    }

    public static void parseinputs(OwnerContext owner, List<Input> buf, Object[] args) {
	Resource.Resolver rr = owner.context(Resource.Resolver.class);
	int a = 0;
	while(a < args.length) {
	    Indir<Resource> res = rr.getresv(args[a++]);
	    Message sdt = Message.nil;
	    if((a < args.length) && BYTES.is(args[a]))
		sdt = new MessageBuf(BYTES.of(args[a++]));
	    Input inp = new Input(new ItemSpec(owner, new ResData(res, sdt), null));
	    if((a < args.length) && (OBJS.is(args[a])))
		parseinputs(owner, inp.sub, OBJS.of(args[a++]));
	    buf.add(inp);
	}
    }

    public static Recipe parse(Book book, Object... args) {
	OwnerContext owner = OwnerContext.uictx.curry(book.ui);
	ItemSpec rcp = new ItemSpec(owner, new ResData(owner.context(Resource.Resolver.class).getresv(args[0]), Message.nil), null);
	List<Input> inputs = new ArrayList<>();
	parseinputs(owner, inputs, OBJS.of(args[1]));
	List<EffectSpec> effects = new LinkedList<>();
	for(Object eff : OBJS.of(args[2]))
	    effects.add(new EffectSpec(owner, new ItemInfo.Raw(new Object[] {eff})));
	List<EffectSpec> mmeffects = new LinkedList<>();
	for(Object eff : OBJS.of(args[3]))
	    mmeffects.add(new EffectSpec(owner, new ItemInfo.Raw(new Object[] {eff})));
	return(new Recipe(book, rcp, inputs, effects, mmeffects));
    }

    public String toString() {
	return(rcp.res.toString() + ": " + inputs.stream().map(Object::toString).collect(java.util.stream.Collectors.joining(" + ")));
    }

    public static class CraftFilter implements Filter<Recipe> {
	public final ItemSpec rcp;

	public CraftFilter(ItemSpec rcp) {
	    this.rcp = rcp;
	}

	public boolean test(Recipe rcp) {
	    return(Utils.eq(this.rcp.res, rcp.rcp.res));
	}

	private Tex icon = null;
	public Tex icon() {
	    if(icon == null)
		icon = new TexI(Book.rowscalea(rcp.image()));
	    return(icon);
	}

	public int hashCode() {return(rcp.res.hashCode());}
	public boolean equals(Object x) {return((x instanceof CraftFilter) && Utils.eq(rcp.res, ((CraftFilter)x).rcp.res));}
    }

    public static class InputFilter implements Filter<Recipe> {
	public final Input inp;

	public InputFilter(Input inp) {
	    this.inp = inp;
	}

	public boolean test(Input inp) {
	    return(Utils.eq(inp, this.inp));
	}

	public boolean test(List<Input> inps) {
	    for(Input inp : inps) {
		if(test(inp))
		    return(true);
	    }
	    for(Input inp : inps) {
		if(test(inp.sub))
		    return(true);
	    }
	    return(false);
	}

	public boolean test(Recipe rcp) {
	    return(test(rcp.inputs));
	}

	private Tex icon = null;
	public Tex icon() {
	    if(icon == null)
		icon = new TexI(Book.rowscalea(inp.type.image()));
	    return(icon);
	}

	public int hashCode() {return(inp.hashCode());}
	public boolean equals(Object x) {return((x instanceof InputFilter) && Utils.eq(inp, ((InputFilter)x).inp));}
    }

    public static class EffectFilter extends EffectInfo.EffectFilter<Recipe> {
	public EffectFilter(EffectInfo eff) {super(eff);}

	public boolean test(Recipe rcp) {
	    for(EffectInfo eff : rcp.effects) {
		if(test(eff))
		    return(true);
	    }
	    return(false);
	}
    }
}
