/* Preprocessed source code */
/* $use: ui/tt/alch/effect */
/* $use: ui/alchbook */
package haven.res.ui.tt.alch.ingr_heal;

import haven.*;
import haven.res.ui.tt.alch.effect.*;
import haven.res.ui.alchbook.*;
import java.awt.image.BufferedImage;

/* >tt: HealWound */
@haven.FromResource(name = "ui/tt/alch/ingr-heal", version = 5)
public class HealWound extends Effect implements EffectInfo {
    public final Resource res, repl;

    public HealWound(Owner owner, Indir<Resource> res, Indir<Resource> repl) {
	super(owner);
	this.res = res.get();
	this.repl = (repl == null) ? null : repl.get();
    }

    public static ItemInfo mkinfo(Owner owner, Raw raw, Object... args) {
	Indir<Resource> res = owner.context(Resource.Resolver.class).getresv(args[1]);
	Indir<Resource> repl = null;
	if(args.length > 2)
	    repl = owner.context(Resource.Resolver.class).getresv(args[2]);
	return(new HealWound(owner, res, repl));
    }

    public BufferedImage alchtip() {
	BufferedImage t1 = Text.render("Heal ").img;
	BufferedImage t2 = Text.render(res.flayer(Resource.tooltip).t).img;
	int h = t1.getHeight();
	BufferedImage icon = PUtils.convolvedown(res.flayer(Resource.imgc).img, new Coord(h, h), CharWnd.iconfilter);
	BufferedImage ret = catimgsh(0, t1, icon, t2);
	if(repl != null) {
	    ret = catimgsh(0, ret,
			   Text.render(" into ").img,
			   PUtils.convolvedown(repl.flayer(Resource.imgc).img, new Coord(h, h), CharWnd.iconfilter),
			   Text.render(repl.flayer(Resource.tooltip).t).img);
	}
	return(ret);
    }

    public BufferedImage image() {
	return(res.flayer(Resource.imgc).img);
    }

    public String desc() {
	return("Heal " + res.flayer(Resource.tooltip).t);
    }
}
