/*
 *  This file is part of the Haven & Hearth game client.
 *  Copyright (C) 2009 Fredrik Tolf <fredrik@dolda2000.com>, and
 *                     Björn Johannessen <johannessen.bjorn@gmail.com>
 *
 *  Redistribution and/or modification of this file is subject to the
 *  terms of the GNU Lesser General Public License, version 3, as
 *  published by the Free Software Foundation.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  Other parts of this source tree adhere to other copying
 *  rights. Please see the file `COPYING' in the root directory of the
 *  source tree for details.
 *
 *  A copy the GNU Lesser General Public License is distributed along
 *  with the source tree of which this file is a part in the file
 *  `doc/LPGL-3'. If it is missing for any reason, please see the Free
 *  Software Foundation's website at <http://www.fsf.org/>, or write
 *  to the Free Software Foundation, Inc., 59 Temple Place, Suite 330,
 *  Boston, MA 02111-1307 USA
 */

package haven;

import me.ender.Reflect;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.List;
import java.util.*;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public abstract class ItemInfo {
    public static final Resource armor_hard = Resource.local().loadwait("gfx/hud/chr/custom/ahard");
    public static final Resource armor_soft = Resource.local().loadwait("gfx/hud/chr/custom/asoft");
    public static final Resource detection = Resource.local().loadwait("gfx/hud/chr/custom/detect");
    public static final Resource sneak = Resource.local().loadwait("gfx/hud/chr/custom/sneak");
    public static final Resource mining = Resource.local().loadwait("gfx/hud/chr/custom/mine");
    static final Pattern count_pattern = Pattern.compile("(?:^|[\\s])([0-9]*\\.?[0-9]+\\s*%?)");
    public final Owner owner;
    
    public static ItemInfo make(Session sess, String resname, Object... args) {
	Resource res = Resource.remote().load(resname).get();
	InfoFactory f = res.layer(Resource.CodeEntry.class).get(InfoFactory.class);
	return f.build(new SessOwner(sess), args);
    }
    
    public interface Owner extends OwnerContext {
	@Deprecated
	public default Glob glob() {return(context(Glob.class));}
	public List<ItemInfo> info();
    }

    private static class SessOwner implements ItemInfo.Owner {
	private final OwnerContext.ClassResolver<SessOwner> ctxr;

	public SessOwner(Session sess) {
	    ctxr = new OwnerContext.ClassResolver<SessOwner>()
		.add(Glob.class, x -> sess.glob)
		.add(Session.class, x -> sess);
	}

	@Override
	public List<ItemInfo> info() {
	    return null;
	}

	@Override
	public <T> T context(Class<T> cl) {
	    return (ctxr.context(cl, this));
	}
    }
    
    public interface ResOwner extends Owner {
	Resource resource();
    }

    public interface SpriteOwner extends ResOwner {
	GSprite sprite();
    }

    public static class Raw {
	public final Object[] data;
	public final double time;

	public Raw(Object[] data, double time) {
	    this.data = data;
	    this.time = time;
	}

	public Raw(Object[] data) {
	    this(data, Utils.rtime());
	}
    }

    @Resource.PublishedCode(name = "tt", instancer = FactMaker.class)
    public static interface InfoFactory {
	public default ItemInfo build(Owner owner, Raw raw, Object... args) {
	    return(build(owner, args));
	}
	@Deprecated
	public default ItemInfo build(Owner owner, Object... args) {
	    throw(new AbstractMethodError("info factory missing either build bmethod"));
	}
    }

    public static class FactMaker extends Resource.PublishedCode.Instancer.Chain<InfoFactory> {
	public FactMaker() {super(InfoFactory.class);}
	{
	    add(new Direct<>(InfoFactory.class));
	    add(new StaticCall<>(InfoFactory.class, "mkinfo", ItemInfo.class, new Class<?>[] {Owner.class, Object[].class},
				 (make) -> new InfoFactory() {
					 public ItemInfo build(Owner owner, Raw raw, Object... args) {
					     return(make.apply(new Object[]{owner, args}));
					 }
				     }));
	    add(new StaticCall<>(InfoFactory.class, "mkinfo", ItemInfo.class, new Class<?>[] {Owner.class, Raw.class, Object[].class},
				 (make) -> new InfoFactory() {
					 public ItemInfo build(Owner owner, Raw raw, Object... args) {
					     return(make.apply(new Object[]{owner, raw, args}));
					 }
				     }));
	    add(new Construct<>(InfoFactory.class, ItemInfo.class, new Class<?>[] {Owner.class, Object[].class},
				(cons) -> new InfoFactory() {
					public ItemInfo build(Owner owner, Raw raw, Object... args) {
					    return(cons.apply(new Object[] {owner, args}));
					}
				    }));
	    add(new Construct<>(InfoFactory.class, ItemInfo.class, new Class<?>[] {Owner.class, Raw.class, Object[].class},
				(cons) -> new InfoFactory() {
					public ItemInfo build(Owner owner, Raw raw, Object... args) {
					    return(cons.apply(new Object[] {owner, raw, args}));
					}
				    }));
	}
    }

    public ItemInfo(Owner owner) {
	this.owner = owner;
    }

    public static class Layout {
	private final List<Tip> tips = new ArrayList<Tip>();
	private final Map<ID, Tip> itab = new HashMap<ID, Tip>();
	public final CompImage cmp = new CompImage();
	public int width = 0;

	public interface ID<T extends Tip> {
	    T make();
	}

	@SuppressWarnings("unchecked")
	public <T extends Tip> T intern(ID<T> id) {
	    T ret = (T)itab.get(id);
	    if(ret == null) {
		itab.put(id, ret = id.make());
		add(ret);
	    }
	    return(ret);
	}

	public void add(Tip tip) {
	    tips.add(tip);
	    tip.prepare(this);
	}

	public BufferedImage render() {
	    Collections.sort(tips, (a, b) -> (a.order() - b.order()));
	    for(Tip tip : tips)
		tip.layout(this);
	    return(cmp.compose());
	}
    }

    public static abstract class Tip extends ItemInfo {
	public Tip(Owner owner) {
	    super(owner);
	}

	public BufferedImage tipimg() {return(null);}
	public BufferedImage tipimg(int w) {return(tipimg());}
	public Tip shortvar() {return(null);}
	public void prepare(Layout l) {}
	public void layout(Layout l) {
	    BufferedImage t = tipimg(l.width);
	    if(t != null)
		l.cmp.add(t, new Coord(0, l.cmp.sz.y));
	}
	public int order() {return(100);}
    }

    public static class AdHoc extends Tip {
	public final Text str;

	public AdHoc(Owner owner, String str) {
	    super(owner);
	    this.str = Text.render(str);
	}

	public BufferedImage tipimg() {
	    return(str.img);
	}
    }

    public static class Name extends Tip {
	public final Text str;
	public final String original;
	
	public Name(Owner owner, Text str, String orig) {
	    super(owner);
	    original = orig;
	    this.str = str;
	}
    
	public Name(Owner owner, Text str) {
	    this(owner, str, str.text);
	}
	
	public Name(Owner owner, String str) {
	    this(owner, Text.render(str), str);
	}
    
	public Name(Owner owner, String str, String orig) {
	    this(owner, Text.render(str), orig);
	}

	public BufferedImage tipimg() {
	    return(str.img);
	}

	public int order() {return(0);}

	public Tip shortvar() {
	    return(new Tip(owner) {
		    public BufferedImage tipimg() {return(str.img);}
		    public int order() {return(0);}
		});
	}

	public static interface Dynamic {
	    public String name();
	}

	public static class Default implements InfoFactory {
	    public ItemInfo build(Owner owner, Object... args) {
		if(owner instanceof SpriteOwner) {
		    GSprite spr = ((SpriteOwner)owner).sprite();
		    if(spr instanceof Dynamic)
			return(new Name(owner, ((Dynamic)spr).name()));
		}
		if(!(owner instanceof ResOwner))
		    return(null);
		Resource res = ((ResOwner)owner).resource();
		Resource.Tooltip tt = res.layer(Resource.tooltip);
		if(tt == null)
		    throw(new RuntimeException("Item resource " + res + " is missing default tooltip"));
		return(new Name(owner, tt.t));
	    }
	}
    }

    public static class Pagina extends Tip {
	public final String str;

	public Pagina(Owner owner, String str) {
	    super(owner);
	    this.str = str;
	}

	public BufferedImage tipimg(int w) {
	    return(RichText.render(str, w).img);
	}

	public void layout(Layout l) {
	    BufferedImage t = tipimg((l.width == 0) ? UI.scale(200) : l.width);
	    if(t != null)
		l.cmp.add(t, new Coord(0, l.cmp.sz.y + UI.scale(10)));
	}

	public int order() {return(10000);}
    }

    public static class Contents extends Tip {
        private static final Pattern PARSE = Pattern.compile("([\\d.]*) ([\\w]+) of (.*)");
	public final List<ItemInfo> sub;
	private static final Text.Line ch = Text.render("Contents:");
	
	public Contents(Owner owner, List<ItemInfo> sub) {
	    super(owner);
	    this.sub = sub;
	}
	
	public BufferedImage tipimg() {
	    BufferedImage stip = longtip(sub);
	    BufferedImage img = TexI.mkbuf(Coord.of(stip.getWidth(), stip.getHeight()).add(UI.scale(10, 15)));
	    Graphics g = img.getGraphics();
	    g.drawImage(ch.img, 0, 0, null);
	    g.drawImage(stip, UI.scale(10), UI.scale(15), null);
	    g.dispose();
	    return(img);
	}

	public Tip shortvar() {
	    return(new Tip(owner) {
		    public BufferedImage tipimg() {return(shorttip(sub));}
		    public int order() {return(100);}
		});
	}
    
	public Content content() {
	    QualityList q = QualityList.make(sub);
	    for (ItemInfo i : sub) {
		if(i instanceof Name) {
		    return content(((Name) i).original, q);
		}
	    }
	    return Content.EMPTY;
	}
	
	public static Content content(String name){
	    return content(name, QualityList.make(Collections.emptyList()));
	}
	
	public static Content content(String name, QualityList q){
	    Matcher m = PARSE.matcher(name);
	    if(m.find()) {
		float count = 0;
		try {
		    count = Float.parseFloat(m.group(1));
		} catch (Exception ignored) {}
		return new Content(m.group(3), m.group(2), count, q);
	    }
	    return Content.EMPTY;
	} 
    
	public static class Content {
	    public final String name;
	    public final String unit;
	    public final float count;
	    public final QualityList q;
	    
	    public Content(String name, String unit, float count) {
		this(name, unit, count, QualityList.make(Collections.emptyList()));
	    }
	    
	    public Content(String name, String unit, float count, QualityList q) {
		this.name = name;
		this.unit = unit;
		this.count = count;
		this.q = q;
	    }
	    
	    public String name() {
		if("seeds".equals(unit)) {
		    return String.format("Seeds of %s", name);
		}
		return name;
	    }
	
	    public boolean is(String what) {
		if(name == null || what == null) {
		    return false;
		}
		return name.contains(what);
	    }
	    
	    public boolean empty() {return count == 0 || name == null;}
	
	    public static final Content EMPTY = new Content(null, null, 0);
	}
    }

    public static BufferedImage catimgs(int margin, BufferedImage... imgs) {
	return catimgs(margin, false, imgs);
    }

    public static BufferedImage catimgs(int margin, boolean right, BufferedImage... imgs) {
	int w = 0, h = -margin;
	for(BufferedImage img : imgs) {
	    if(img == null)
		continue;
	    if(img.getWidth() > w)
		w = img.getWidth();
	    h += img.getHeight() + margin;
	}
	BufferedImage ret = TexI.mkbuf(new Coord(w, h));
	Graphics g = ret.getGraphics();
	int y = 0;
	for(BufferedImage img : imgs) {
	    if(img == null)
		continue;
	    g.drawImage(img, right ? w - img.getWidth() : 0, y, null);
	    y += img.getHeight() + margin;
	}
	g.dispose();
	return(ret);
    }

    public static BufferedImage catimgsh(int margin, BufferedImage... imgs) {
	return catimgsh(margin, 0, null, imgs);
    }
    
    public static BufferedImage catimgsh(int margin, int pad, Color bg, BufferedImage... imgs) {
	int w = 2 * pad - margin, h = 0;
	for(BufferedImage img : imgs) {
	    if(img == null)
		continue;
	    if(img.getHeight() > h)
		h = img.getHeight();
	    w += img.getWidth() + margin;
	}
	BufferedImage ret = TexI.mkbuf(new Coord(w, h));
	Graphics g = ret.getGraphics();
	if(bg != null) {
	    g.setColor(bg);
	    g.fillRect(0, 0, w, h);
	}
	int x = pad;
	for(BufferedImage img : imgs) {
	    if(img == null)
		continue;
	    g.drawImage(img, x, (h - img.getHeight()) / 2, null);
	    x += img.getWidth() + margin;
	}
	g.dispose();
	return(ret);
    }

    public static BufferedImage longtip(List<ItemInfo> info) {
	Layout l = new Layout();
	for(ItemInfo ii : info) {
	    if(ii instanceof Tip) {
		Tip tip = (Tip)ii;
		l.add(tip);
	    }
	}
	if(l.tips.size() < 1)
	    return(null);
	return(l.render());
    }

    public static BufferedImage shorttip(List<ItemInfo> info) {
	Layout l = new Layout();
	for(ItemInfo ii : info) {
	    if(ii instanceof Tip) {
		Tip tip = ((Tip)ii).shortvar();
		if(tip != null)
		    l.add(tip);
	    }
	}
	if(l.tips.size() < 1)
	    return(null);
	return(l.render());
    }

    public static <T> T find(Class<T> cl, List<ItemInfo> il) {
	for(ItemInfo inf : il) {
	    if(cl.isInstance(inf))
		return(cl.cast(inf));
	}
	return(null);
    }

    public static <T> List<T> findall(Class<T> cl, List<ItemInfo> il) {
	List<T> ret = new LinkedList<>();
	for(ItemInfo inf : il) {
	    if(cl.isInstance(inf))
		ret.add(cl.cast(inf));
	}
	return ret;
    }

    public static List<ItemInfo> findall(String cl, List<ItemInfo> infos){
	return infos.stream()
	    .filter(inf -> Reflect.is(inf, cl))
	    .collect(Collectors.toCollection(LinkedList::new));
    }

    public static List<ItemInfo> buildinfo(Owner owner, Raw raw) {
	List<ItemInfo> ret = new ArrayList<ItemInfo>();
	for(Object o : raw.data) {
	    if(o instanceof Object[]) {
		Object[] a = (Object[])o;
		Resource ttres;
		if(a[0] instanceof Integer) {
		    ttres = owner.glob().sess.getres((Integer)a[0]).get();
		} else if(a[0] instanceof Resource) {
		    ttres = (Resource)a[0];
		} else if(a[0] instanceof Indir) {
		    ttres = (Resource)((Indir)a[0]).get();
		} else {
		    throw(new ClassCastException("Unexpected info specification " + a[0].getClass()));
		}
		InfoFactory f = ttres.getcode(InfoFactory.class, true);
		ItemInfo inf = f.build(owner, raw, a);
		if(inf != null)
		    ret.add(inf);
	    } else if(o instanceof String) {
		ret.add(new AdHoc(owner, (String)o));
	    } else {
		throw(new ClassCastException("Unexpected object type " + o.getClass() + " in item info array."));
	    }
	}
	return(ret);
    }

    public static List<ItemInfo> buildinfo(Owner owner, Object[] rawinfo) {
	return(buildinfo(owner, new Raw(rawinfo)));
    }
    

    public static String getCount(List<ItemInfo> infos) {
	String res = null;
	for (ItemInfo info : infos) {
	    if(info instanceof Contents) {
		Contents cnt = (Contents) info;
		res = getCount(cnt.sub);
	    } else if(info instanceof AdHoc) {
		AdHoc ah = (AdHoc) info;
		try {
		    Matcher m = count_pattern.matcher(ah.str.text);
		    if(m.find()) {
			res = m.group(1);
		    }
		} catch (Exception ignored) {
		}
	    } else if(info instanceof Name) {
		Name name = (Name) info;
		try {
		    Matcher m = count_pattern.matcher(name.original);
		    if(m.find()) {
			res = m.group(1);
		    }
		} catch (Exception ignored) {
		}
	    }
	    if(res != null) {
		return res.trim();
	    }
	}
	return null;
    }
    
    public static Contents.Content getContent(List<ItemInfo> infos) {
	for (ItemInfo info : infos) {
	    if(info instanceof Contents) {
		return ((Contents) info).content();
	    }
	}
	return Contents.Content.EMPTY;
    }
    
    public static Pair<Integer, Integer> getWear(List<ItemInfo> infos) {
	infos = findall("haven.res.ui.tt.wear.Wear", infos);
	for (ItemInfo info : infos) {
	    if(Reflect.hasField(info, "m") && Reflect.hasField(info, "d")){
		return new Pair<>(Reflect.getFieldValueInt(info, "d"), Reflect.getFieldValueInt(info, "m"));
	    }
	}
	return null;
    }

    public static Pair<Integer, Integer> getArmor(List<ItemInfo> infos) {
	infos = findall("Armor", infos);
	for (ItemInfo info : infos) {
	    if(Reflect.hasField(info, "hard") && Reflect.hasField(info, "soft")){
		return new Pair<>(Reflect.getFieldValueInt(info, "hard"), Reflect.getFieldValueInt(info, "soft"));
	    }
	}
	return null;
    }

    private final static String[] mining_tools = {"Pickaxe", "Stone Axe", "Metal Axe", "Woodsman's Axe"};
    
    @SuppressWarnings("unchecked")
    public static Map<Resource, Integer> getBonuses(List<ItemInfo> infos, Map<String, Glob.CAttr> attrs) {
	List<ItemInfo> slotInfos = ItemInfo.findall("ISlots", infos);
	List<ItemInfo> gilding = ItemInfo.findall("Slotted", infos);
	Map<Resource, Integer> bonuses = new HashMap<>();
	try {
	    for (ItemInfo islots : slotInfos) {
		List<Object> slots = (List<Object>) Reflect.getFieldValue(islots, "s");
		for (Object slot : slots) {
		    parseAttrMods(bonuses, (List) Reflect.getFieldValue(slot, "info"));
		}
	    }
	    for (ItemInfo info : gilding) {
		List<Object> slots = (List<Object>) Reflect.getFieldValue(info, "sub");
		parseAttrMods(bonuses, slots);
	    }
	    parseAttrMods(bonuses, ItemInfo.findall("haven.res.ui.tt.attrmod.AttrMod", infos));
	} catch (Exception ignored) {}
	Pair<Integer, Integer> wear = ItemInfo.getArmor(infos);
	if (wear != null) {
	    bonuses.put(armor_hard, wear.a);
	    bonuses.put(armor_soft, wear.b);
	}
	if(attrs != null) {
	    Glob.CAttr str = attrs.get("str");
	    Name name = ItemInfo.find(Name.class, infos);
	    QualityList q = QualityList.make(infos);
	    if(str != null && name != null && !q.isEmpty() && GobTag.ofType(name.original, mining_tools)) {
		double miningStrength = str.comp * q.single().value;
		if(name.original.equals("Pickaxe")) {
		    miningStrength = 2 * miningStrength;
		}
		bonuses.put(mining, (int) Math.sqrt(miningStrength));
	    }
	}
	return bonuses;
    }
    
    public static List<Pair<Resource, Integer>> getInputs(List<ItemInfo> infos) {
	List<ItemInfo> inputInfos = ItemInfo.findall("Inputs", infos);
	List<Pair<Resource, Integer>> result = new LinkedList<>();
	try {
	    for (ItemInfo info : inputInfos) {
		Object[] inputs = (Object[]) Reflect.getFieldValue(info, "inputs");
		for (Object input : inputs) {
		    int num = Reflect.getFieldValueInt(input, "num");
		    Object spec = Reflect.getFieldValue(input, "spec");
		    ResData resd = (ResData) Reflect.getFieldValue(spec, "res");
		    Resource r = resd.res.get();
		    //Resource.Tooltip tt = r.layer(Resource.tooltip);
		    //System.out.printf("%s x %d%n", (tt != null) ? tt.t : r.name, num);
		    result.add(new Pair<>(r, num));
		}
	    }
	} catch (Exception ignored) {}
	return result;
    }


    @SuppressWarnings("unchecked")
    public static void parseAttrMods(Map<Resource, Integer> bonuses, List infos) {
	for (Object inf : infos) {
	    List<Object> mods = (List<Object>) Reflect.getFieldValue(inf, "mods");
	    for (Object mod : mods) {
		Resource attr = (Resource) Reflect.getFieldValue(mod, "attr");
		int value = Reflect.getFieldValueInt(mod, "mod");
		if (bonuses.containsKey(attr)) {
		    bonuses.put(attr, bonuses.get(attr) + value);
		} else {
		    bonuses.put(attr, value);
		}
	    }
	}
    }

    @SuppressWarnings("unchecked")
    private static Map<Resource, Integer> parseAttrMods2(List infos) {
	Map<Resource, Integer> bonuses = new HashMap<>();
	for (Object inf : infos) {
	    List<Object> mods = (List<Object>) Reflect.getFieldValue(inf, "mods");
	    for (Object mod : mods) {
		Resource attr = (Resource) Reflect.getFieldValue(mod, "attr");
		int value = Reflect.getFieldValueInt(mod, "mod");
		if (bonuses.containsKey(attr)) {
		    bonuses.put(attr, bonuses.get(attr) + value);
		} else {
		    bonuses.put(attr, value);
		}
	    }
	}
	return bonuses;
    }

    private static String dump(Object arg) {
	if(arg instanceof Object[]) {
	    StringBuilder buf = new StringBuilder();
	    buf.append("[");
	    boolean f = true;
	    for(Object a : (Object[])arg) {
		if(!f)
		    buf.append(", ");
		buf.append(dump(a));
		f = false;
	    }
	    buf.append("]");
	    return(buf.toString());
	} else {
	    return(arg.toString());
	}
    }

    public static class AttrCache<R> implements Indir<R> {
	private final Supplier<List<ItemInfo>> from;
	private final Function<List<ItemInfo>, Supplier<R>> data;
	private final R def;
	private List<ItemInfo> forinfo = null;
	private Supplier<R> save;

	public AttrCache(Supplier<List<ItemInfo>> from, Function<List<ItemInfo>, Supplier<R>> data, R def) {
	    this.from = from;
	    this.data = data;
	    this.def = def;
	}
    
	public AttrCache(Supplier<List<ItemInfo>> from, Function<List<ItemInfo>, Supplier<R>> data) {
	    this(from, data, null);
	}

	public R get() {
	    return get(def);
	}
    
	public R get(R def) {
	    try {
		List<ItemInfo> info = from.get();
		if(info != forinfo) {
		    save = data.apply(info);
		    forinfo = info;
		}
		return(save.get());
	    } catch(Loading l) {
		return(def);
	    }
	}

	public static <I, R> Function<List<ItemInfo>, Supplier<R>> map1(Class<I> icl, Function<I, Supplier<R>> data) {
	    return(info -> {
		    I inf = find(icl, info);
		    if(inf == null)
			return(() -> null);
		    return(data.apply(inf));
		});
	}

	public static <I, R> Function<List<ItemInfo>, Supplier<R>> map1s(Class<I> icl, Function<I, R> data) {
	    return(info -> {
		    I inf = find(icl, info);
		    if(inf == null)
			return(() -> null);
		    R ret = data.apply(inf);
		    return(() -> ret);
		});
	}
 
	public static <R> Function<List<ItemInfo>, Supplier<R>> cache(Function<List<ItemInfo>, R> data) {
	    return (info -> {
		R result = data.apply(info);
		return (() -> result);
	    });
	}
    }

    public static interface InfoTip {
	public List<ItemInfo> info();
    }
}
