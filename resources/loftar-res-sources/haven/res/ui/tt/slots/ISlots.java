/* Preprocessed source code */
package haven.res.ui.tt.slots;

import haven.*;
import static haven.PUtils.*;
import java.awt.image.*;
import java.awt.Graphics;
import java.awt.Font;
import java.awt.Color;
import java.util.*;

/* >tt: Fac */
@haven.FromResource(name = "ui/tt/slots", version = 31)
public class ISlots extends ItemInfo.Tip implements GItem.NumberInfo {
    public static final Text ch = Text.render("Gilding:");
    public static final Text.Foundry progf = new Text.Foundry(Text.dfont.deriveFont(Font.ITALIC), 10, new Color(0, 169, 224));
    public final Collection<SItem> s = new ArrayList<SItem>();
    public final int left;
    public final double pmin, pmax;
    public final Resource[] attrs;
    public final boolean ignol;

    public ISlots(Owner owner, int left, double pmin, double pmax, Resource[] attrs) {
	super(owner);
	this.left = left;
	this.pmin = pmin;
	this.pmax = pmax;
	this.attrs = attrs;
	// XXX? Should the format be changed instead?
	ignol = owner.fcontext(MenuGrid.class, false) != null;
    }

    public static final String chc = "192,192,255";
    public void layout(Layout l) {
	l.cmp.add(ch.img, new Coord(0, l.cmp.sz.y));
	if(attrs.length > 0) {
	    BufferedImage head = RichText.render(String.format("Chance: $col[%s]{%d%%} to $col[%s]{%d%%}", chc, Math.round(100 * pmin), chc, Math.round(100 * pmax)), 0).img;
	    int h = head.getHeight();
	    int x = 10, y = l.cmp.sz.y;
	    l.cmp.add(head, new Coord(x, y));
	    x += head.getWidth() + 10;
	    for(int i = 0; i < attrs.length; i++) {
		BufferedImage icon = convolvedown(attrs[i].layer(Resource.imgc).img, new Coord(h, h), CharWnd.iconfilter);
		l.cmp.add(icon, new Coord(x, y));
		x += icon.getWidth() + 2;
	    }
	} else {
	    BufferedImage head = RichText.render(String.format("Chance: $col[%s]{%d%%}", chc, (int)Math.round(100 * pmin)), 0).img;
	    l.cmp.add(head, new Coord(10, l.cmp.sz.y));
	}
	for(SItem si : s)
	    si.layout(l);
	if(left > 0)
	    l.cmp.add(progf.render((left > 1)?String.format("Gildable \u00d7%d", left):"Gildable").img, new Coord(10, l.cmp.sz.y));
    }

    public static final Object[] defn = {Loading.waitfor(Resource.classres(ISlots.class).pool.load("ui/tt/defn", 7))};
    public class SItem {
	public final Resource res;
	public final GSprite spr;
	public final List<ItemInfo> info;
	public final String name;

	public SItem(ResData sdt, Object[] raw) {
	    this.res = sdt.res.get();
	    ItemSpec spec1 = new ItemSpec(owner, sdt, Utils.extend(new Object[] {defn}, raw));
	    this.spr = spec1.spr();
	    this.name = spec1.name();
	    ItemSpec spec2 = new ItemSpec(owner, sdt, raw);
	    this.info = spec2.info();
	}

	private BufferedImage img() {
	    if(spr instanceof GSprite.ImageSprite)
		return(((GSprite.ImageSprite)spr).image());
	    return(res.layer(Resource.imgc).img);
	}

	public void layout(Layout l) {
	    BufferedImage icon = PUtils.convolvedown(img(), new Coord(16, 16), CharWnd.iconfilter);
	    BufferedImage lbl = Text.render(name).img;
	    BufferedImage sub = longtip(info);
	    int x = 10, y = l.cmp.sz.y;
	    l.cmp.add(icon, new Coord(x, y));
	    l.cmp.add(lbl, new Coord(x + 16 + 3, y + ((16 - lbl.getHeight()) / 2)));
	    if(sub != null)
		l.cmp.add(sub, new Coord(x + 16, y + 16));
	}
    }

    public int order() {
	return(200);
    }

    public int itemnum() {
	return(s.size());
    }

    public static final Color avail = new Color(128, 192, 255);
    public Color numcolor() {
	return((left > 0) ? avail : Color.WHITE);
    }

    public void drawoverlay(GOut g, Tex tex) {
	if(!ignol)
	    GItem.NumberInfo.super.drawoverlay(g, tex);
    }
}
