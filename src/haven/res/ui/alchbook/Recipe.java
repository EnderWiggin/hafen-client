/* Preprocessed source code */
package haven.res.ui.alchbook;

import java.util.*;
import haven.*;
import haven.MenuGrid.*;
import java.awt.Color;
import java.awt.image.BufferedImage;
import static haven.PType.*;

@FromResource(name = "ui/alchbook", version = 1)
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
    public static final Comparator<Recipe> rcpeffectorder = new Comparator<Recipe>() {
	public int compare(Recipe a, Recipe b) {
	    for(int i = 0; i < Math.max(a.effects.size(), b.effects.size()); i++) {
		if(i >= a.effects.size())
		    return(-1);
		if(i >= b.effects.size())
		    return(1);
		int c = a.effects.get(i).desc().compareTo(b.effects.get(i).desc());
		if(c != 0)
		    return(c);
	    }
	    return(rcpcraftorder.compare(a, b));
	}
    };
    public final ItemSpec rcp;
    public final List<Input> inputs;
    public final List<EffectInfo> effects;
    private List<EffectSpec> raweffects;

    public Recipe(ItemSpec rcp, List<Input> inputs, List<EffectSpec> raweffects) {
	this.rcp = rcp;
	this.inputs = inputs;
	this.effects = new ArrayList<>();
	this.raweffects = raweffects;
    }

    public void fin(OwnerContext owner) {
	for(Iterator<EffectSpec> i = raweffects.iterator(); i.hasNext();) {
	    EffectSpec raw = i.next();
	    raw.resolve(effects);
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

    public static Recipe parse(OwnerContext owner, Object... args) {
	ItemSpec rcp = new ItemSpec(owner, new ResData(owner.context(Resource.Resolver.class).getresv(args[0]), Message.nil), null);
	List<Input> inputs = new ArrayList<>();
	List<EffectSpec> effects = new LinkedList<>();
	parseinputs(owner, inputs, OBJS.of(args[1]));
	for(Object eff : OBJS.of(args[2]))
	    effects.add(new EffectSpec(owner, new ItemInfo.Raw(new Object[] {eff})));
	return(new Recipe(rcp, inputs, effects));
    }

    public String toString() {
	return(rcp.res.toString() + ": " + inputs.stream().map(Object::toString).collect(java.util.stream.Collectors.joining(" + ")));
    }
}
