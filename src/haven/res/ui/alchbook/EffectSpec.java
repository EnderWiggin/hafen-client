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
public class EffectSpec implements ItemInfo.Owner {
    public final OwnerContext ctx;
    public final ItemInfo.Raw spec;

    public EffectSpec(OwnerContext ctx, ItemInfo.Raw spec) {
	this.ctx = ctx;
	this.spec = spec;
    }

    private List<ItemInfo> info = null;
    public List<ItemInfo> info() {
	if(info == null)
	    info = ItemInfo.buildinfo(this, spec);
	return(info);
    }

    public <T> T context(Class<T> cls) {
	return(ctx.context(cls));
    }

    public void resolve(List<EffectInfo> buf) {
	for(ItemInfo part : info()) {
	    if(part instanceof EffectInfo)
		buf.add((EffectInfo)part);
	}
    }
}
