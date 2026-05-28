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
public interface EffectInfo {
    public BufferedImage image();
    public String desc();
    public default String sortkey() {return(desc());}

    public abstract static class EffectFilter<T> implements Filter<T> {
	public final EffectInfo eff;

	public EffectFilter(EffectInfo eff) {
	    this.eff = eff;
	}

	public boolean test(EffectInfo eff) {
	    return(Utils.eq(eff.desc(), this.eff.desc()));
	}

	private Tex icon = null;
	public Tex icon() {
	    if(icon == null)
		icon = new TexI(Book.rowscalea(eff.image()));
	    return(icon);
	}

	public int hashCode() {return(eff.desc().hashCode());}
	public boolean equals(Object x) {return((x instanceof EffectFilter) && Utils.eq(eff.desc(), ((EffectFilter)x).eff.desc()));}
    }
}
