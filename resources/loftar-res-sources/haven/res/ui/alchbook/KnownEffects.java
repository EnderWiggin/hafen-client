/* Preprocessed source code */
package haven.res.ui.alchbook;

import java.util.*;
import java.util.function.*;
import haven.*;
import haven.MenuGrid.*;
import java.awt.Color;
import java.awt.image.BufferedImage;
import static haven.PType.*;

@haven.FromResource(name = "ui/alchbook", version = 4)
public class KnownEffects {
    public static final Comparator<KnownEffects> ikinputorder = Comparator.comparing(ik -> ik.input.type, Recipe.iconorder);
    public static final Comparator<KnownEffects> kieffectorder = Comparator.comparing((KnownEffects ik) -> ik.effs, Recipe.effectorder).thenComparing(ikinputorder);
    public final Book book;
    public final Input input;
    public final List<EffectInfo> effs = new ArrayList<>();
    private List<EffectSpec> raweffs;

    public KnownEffects(Book book, Input input, List<EffectSpec> effs) {
	this.book = book;
	this.input = input;
	this.raweffs = effs;
    }

    public void fin(OwnerContext owner) {
	for(Iterator<EffectSpec> i = raweffs.iterator(); i.hasNext();) {
	    EffectSpec raw = i.next();
	    raw.resolve(effs);
	    i.remove();
	}
    }

    public static KnownEffects parse(Book book, Object[] args) {
	OwnerContext owner = OwnerContext.uictx.curry(book.ui);
	List<Input> buf = new ArrayList<>();
	Recipe.parseinputs(owner, buf, OBJS.of(args[0]));
	List<EffectSpec> effects = new LinkedList<>();
	for(Object eff : OBJS.of(args[1]))
	    effects.add(new EffectSpec(owner, new ItemInfo.Raw(new Object[] {eff})));
	return(new KnownEffects(book, buf.get(0), effects));
    }

    public static class EffectFilter extends EffectInfo.EffectFilter<KnownEffects> {
	public EffectFilter(EffectInfo eff) {super(eff);}

	public boolean test(KnownEffects ik) {
	    for(EffectInfo eff : ik.effs) {
		if(test(eff))
		    return(true);
	    }
	    return(false);
	}
    }
}
