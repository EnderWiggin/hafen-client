/* Preprocessed source code */
package haven.res.ui.alchbook;

import java.util.*;
import java.util.function.*;
import haven.*;
import haven.MenuGrid.*;
import java.awt.Color;
import java.awt.image.BufferedImage;
import static haven.PType.*;

@FromResource(name = "ui/alchbook", version = 3)
public class InputIcon extends Icon implements ItemInfo.Owner {
    public final Book book;
    public final Input inp;
    public final Consumer<Input> action;

    public InputIcon(int sz, Book book, Input inp, Consumer<Input> action) {
	super(sz, inp.type);
	this.book = book;
	this.inp = inp;
	this.action = action;
    }
    public InputIcon(int sz, Book book, Input inp) {
	this(sz, book, inp, null);
    }

    private List<ItemInfo> info = null;
    public List<ItemInfo> info() {
	if(info == null) {
	    List<ItemInfo> info = new ArrayList<>();
	    info.add(new ItemInfo.Name(this, spec.name()));
	    KnownEffects ik = book.el.knowledge.get(inp);
	    if(ik != null) {
		for(EffectInfo ei : ik.effs)
		    info.add((ItemInfo)ei);
	    }
	    this.info = info;
	}
	return(this.info);
    }

    public <T> T context(Class<T> cl) {
	return(wdgctx.context(cl, this));
    }

    private Tex tip = null;
    public Object tooltip(Coord c, Widget prev) {
	if(tip == null)
	    tip = new TexI(ItemInfo.longtip(info()));
	return(tip);
    }

    public boolean mousedown(MouseDownEvent ev) {
	if((action != null) && (ev.b == 1)) {
	    action.accept(inp);
	    return(true);
	}
	return(super.mousedown(ev));
    }
}
