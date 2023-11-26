package haven;

import me.ender.Reflect;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import static haven.CharWnd.*;
import static haven.PUtils.*;

public class GildingWnd extends WindowX {
    private final WItem target;
    private final WItem gild;
    private BufferedImage igild;
    private BufferedImage islots;
    private BufferedImage matches;
    private UI.Grab mg;
    private double min, max, koeff;


    public GildingWnd(WItem target, WItem gild) {
	super(UI.scale(200, 100), "Gilding");
	justclose = true;
    
	this.target = target;
	this.gild = gild;
    }

    private void init() {
	gild.hide();

	Pair<Double, BufferedImage> result = findMatches();
	if(result != null) {
	    koeff = result.a;
	    matches = result.b;
	}

	List<ItemInfo> gild_infos = gild.gilding.get();
	List<ItemInfo> target_infos = target.slots.get();
    
	ItemInfo gild_info = gild_infos.get(0);
	ItemInfo target_info = target_infos.get(0);
    
	min = Reflect.getFieldValueDouble(gild_info, "pmin") * Reflect.getFieldValueDouble(target_info, "pmin");
	max = Reflect.getFieldValueDouble(gild_info, "pmax") * Reflect.getFieldValueDouble(target_info, "pmax");
    
	koeff = min + koeff * (max - min);
    
	igild = ItemInfo.longtip(gild_infos);
	islots = ItemInfo.longtip(target_infos);
    
	int itemH = Math.max(target.sz.y, gild.sz.y);
	int h = Math.max(igild.getHeight(), islots.getHeight()) + itemH;
	int w = igild.getWidth() + islots.getWidth();
    
	resize(new Coord(w + UI.scale(45), h + UI.scale(100) + (matches != null ? matches.getHeight() : 0)));
    
	add(new FWItem(target.item), UI.scale(10, 5));
	Coord asz = ca().sz();
	add(new FWItem(gild.item), asz.x - UI.scale(5) - gild.sz.x, UI.scale(5));
    
	boolean canSlot = true;
	try {
	    canSlot = target_infos.stream()
		.map(itemInfo -> (List<?>) Reflect.getFieldValue(itemInfo, "s"))
		.flatMap(Collection::stream)
		.map(o -> (String) Reflect.getFieldValue(o, "name"))
		.noneMatch(o -> {
		    String name = gild.name.get();
		    return Objects.equals(name, o);
		});
	} catch (Loading ignored) {}

	if(target.gildable.get() && canSlot) {
	    add(new Button(UI.scale(120), "Gild", false) {
		@Override
		public void click() {
		    gild();
		}
	    }, asz.x / 2 - UI.scale(60), asz.y - UI.scale(20));
	} else {
	    String msg = "Can't gild: " + (canSlot ? "there are no more slots left!" : "item of this type already slotted!");
	    Label label = new Label(msg);
	    label.setcolor(Color.RED);
	    add(label, (asz.x - label.sz.x) / 2, asz.y - UI.scale(15));
	}
    }

    private Pair<Double, BufferedImage> findMatches() {
	try {
	    CharWnd charWnd = ui.gui.chrwdg;

	    List<Resource> slot_attrs = target.slots.get().stream()
		.map(itemInfo -> (Resource[]) Reflect.getFieldValue(itemInfo, "attrs"))
		.flatMap(Arrays::stream)
		.collect(Collectors.toList());

	    List<Resource> matches = gild.gilding.get().stream()
		.map(itemInfo -> (Resource[]) Reflect.getFieldValue(itemInfo, "attrs"))
		.flatMap(Arrays::stream)
		.filter(slot_attrs::contains)
		.sorted(charWnd::BY_PRIORITY)
		.collect(Collectors.toList());

	    if(!matches.isEmpty()) {
		double k = 1 - Math.pow(2, -Math.sqrt(matches.stream()
		    .map(charWnd::findattr)
		    .map(cAttr -> cAttr.comp * cAttr.comp)
		    .reduce(0, Integer::sum)) / 100f);

		return new Pair<>(k, ItemInfo.catimgsh(8, matches.stream()
		    .map(res -> {
			BufferedImage val = charWnd.findattr(res).compline().img;
			Coord tsz = new Coord(val.getHeight(), val.getHeight());
			return ItemInfo.catimgsh(1, convolve(res.layer(Resource.imgc).img, tsz, iconfilter), val);
		    })
		    .toArray(BufferedImage[]::new)
		));
	    }
	} catch (Exception ignored) {
	    ignored.printStackTrace();
	}
	return null;
    }

    @Override
    protected void added() {
	super.added();
	mg = ui.grabmouse(this);
	init();
    }

    private void gild() {
	target.item.wdgmsg("itemact", ui.modflags());
    }

    @Override
    public void cdraw(GOut g) {
	int itemH = Math.max(target.sz.y, gild.sz.y);
	Coord asz = ca().sz();
	g.image(islots, UI.scale(5, 10).addy(itemH));
	g.image(igild, new Coord(asz.x - UI.scale(5) - igild.getWidth(), UI.scale(10) + itemH));
	if(matches != null) {
	    Coord c1 = new Coord(asz.x / 2, asz.y - matches.getHeight() - UI.scale(70));
	    g.atext("Matching skills:", c1, 0.5, 0.5);
	    g.image(matches, c1.sub(matches.getWidth() / 2, matches.getHeight() / 2).add(UI.scale(0, 18)));
	}
	
	Coord ul = new Coord(0, asz.y - UI.scale(34));
	Coord sz = new Coord(asz.x, UI.scale(14));
	g.chcolor(new Color(122, 61, 61, 255));
	g.frect(ul, sz);
	g.chcolor(new Color(35, 111, 33, 255));
	g.frect(ul, new Coord((int) (asz.x * koeff), UI.scale(14)));
	g.chcolor();
	g.atext("Chance for a new slot:", ul.add(sz.div(2)).sub(UI.scale(0, 16)), 0.5, 0.5);
	g.atext(String.format("%.2f%%", 100 * koeff), ul.add(sz.div(2)), 0.5, 0.5);
	g.atext("Min:", ul.add(2, sz.y / 2 - 16), 0, 0.5);
	g.atext(String.format("%.2f%%", 100 * min), ul.add(2, sz.y / 2), 0, 0.5);
	g.atext("Max:", ul.add(sz.x - UI.scale(2), sz.y / 2 - UI.scale(16)), 1, 0.5);
	g.atext(String.format("%.2f%%", 100 * max), ul.add(sz.x - 2, sz.y / 2), 1, 0.5);
    }

    @Override
    public boolean mousedown(Coord c, int button) {
	if(c.isect(Coord.z, sz)) {
	    super.mousedown(c, button);
	} else {
	    close();
	}
	return true;
    }

    @Override
    public void close() {
	gild.show();
	if(mg != null) {
	    mg.remove();
	}
	mg = null;
	super.close();
    }

    public static boolean processGilding(UI ui, WItem target, WItem gild) {
	boolean result = false;
	List<ItemInfo> gilding = gild.gilding.get();
	List<ItemInfo> slots = target.slots.get();
	if(gilding != null && !gilding.isEmpty() && slots != null && !slots.isEmpty()) {
	    boolean isRing = target.name.get().contains("Ring");
	    boolean isGem = gild.item.resname().contains("gems/gemstone");
	    result = isGem == isRing;
	    if(!result) {
		if(isRing) {
		    ui.message("Only gems can be gilded onto rings!", GameUI.MsgType.ERROR);
		} else {
		    ui.message("Gems can only be gilded onto rings!", GameUI.MsgType.ERROR);
		}
	    }
	}
	if(result) {
	    ui.gui.add(new GildingWnd(target, gild));
	}
	return result;
    }

    private static class FWItem extends WItem {
	public FWItem(GItem item) {
	    super(item);
	}

	@Override
	public boolean iteminteract(WItem target, Coord cc, Coord ul) {
	    return false;
	}

	@Override
	public boolean mousedown(Coord c, int btn) {
	    return false;
	}
    }

}
