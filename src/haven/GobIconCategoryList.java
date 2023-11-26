package haven;

import java.awt.*;
import java.util.Arrays;

public class GobIconCategoryList extends Listbox<GobIconCategoryList.GobCategory> {
    
    private static final Text.Foundry elf = CharWnd.attrf;
    private static final int elh = elf.height() + UI.scale(2);
    private static final Color every = new Color(255, 255, 255, 16), other = new Color(255, 255, 255, 32);
    
    private final Coord showc;
    
    public GobIconCategoryList(int w, int h, int itemh) {
	super(w, h, itemh);
	bgcolor = new Color(0, 0, 0, 84);
	showc = showc();
    }
    
    private Coord showc() {
	return (new Coord(sz.x - (sb.vis() ? sb.sz.x : 0) - ((elh - CheckBox.sbox.sz().y) / 2) - CheckBox.sbox.sz().x,
	    ((elh - CheckBox.sbox.sz().y) / 2)));
    }
    
    @Override
    protected GobCategory listitem(int i) {
	return GobCategory.values()[i];
    }
    
    @Override
    protected int listitems() {
	return GobCategory.values().length;
    }
    
    @Override
    protected void drawitem(GOut g, GobCategory cat, int idx) {
	g.chcolor(((idx % 2) == 0) ? every : other);
	g.frect(Coord.z, g.sz());
	g.chcolor();
	try {
	    GobIcon.SettingsWindow.Icon icon = cat.icon();
	    g.aimage(icon.img(), new Coord(0, elh / 2), 0.0, 0.5);
	    if(icon.tname != null) {
		g.aimage(icon.tname.tex(), new Coord(elh + UI.scale(5), elh / 2), 0.0, 0.5);
	    }
	    if(cat != GobCategory.ALL) {
		g.image(CheckBox.sbox, showc);
		if(cat.enabled()) {
		    g.image(CheckBox.smark, showc);
		}
	    }
	} catch (Loading ignored) {}
    }
    
    public boolean mousedown(Coord c, int button) {
	int idx = idxat(c);
	if((idx >= 0) && (idx < listitems())) {
	    GobCategory cat = listitem(idx);
	    if(cat != GobCategory.ALL) {
		Coord ic = c.sub(idxc(idx));
		if(ic.isect(showc, CheckBox.sbox.sz())) {
		    cat.toggle();
		    return true;
		}
	    }
	}
	return (super.mousedown(c, button));
    }
    
    enum GobCategory {
	ALL("all"),
	ANIMALS("kritters"),
	HERBS("herbs"),
	ORES("ores"),
	ROCKS("rocks"),
	TREE("trees"),
	BUSHES("bushes"),
	OTHER("other");
	
	private final String resname;
	private final CFG<Boolean> cfg;
	private GobIcon.SettingsWindow.Icon icon;
	
	private static final String[] ANIMAL_PATHS = {
	    "/kritter/",
	    "/invobjs/bunny",
	    "/invobjs/bogturtle",
	    "/invobjs/cavecentipede",
	    "/invobjs/cavemoth",
	    "/invobjs/dragonfly",
	    "/invobjs/forestlizard",
	    "/invobjs/forestsnail",
	    "/invobjs/frog",
	    "/invobjs/grasshopper",
	    "/invobjs/grub",
	    "/invobjs/crab",
	    "/invobjs/firefly",
	    "/invobjs/hen",
	    "/invobjs/jellyfish",
	    "/invobjs/ladybug",
	    "/invobjs/magpie",
	    "/invobjs/mallard",
	    "/invobjs/mole",
	    "/invobjs/monarchbutterfly",
	    "/invobjs/moonmoth",
	    "/invobjs/ptarmigan",
	    "/invobjs/quail",
	    "/invobjs/rabbit",
	    "/invobjs/rat",
	    "/invobjs/rockdove",
	    "/invobjs/rooster",
	    "/invobjs/sandflea",
	    "/invobjs/seagull",
	    "/invobjs/silkmoth",
	    "/invobjs/squirrel",
	    "/invobjs/stagbeetle",
	    "/invobjs/swan",
	    "/invobjs/toad",
	    "/invobjs/waterstrider",
	    "/invobjs/woodgrouse",
	};
	
	private static final String[] HERB_PATHS = {
	    "/invobjs/herbs/",
	    "/invobjs/small/bladderwrack",
	    "/invobjs/small/snapdragon",
	    "/invobjs/small/thornythistle",
	};
	
	private static final String[] ORE_PATHS = {
	    "gfx/invobjs/argentite",
	    "gfx/invobjs/blackcoal",
	    "gfx/invobjs/cassiterite",
	    "gfx/invobjs/chalcopyrite",
	    "gfx/invobjs/cinnabar",
	    "gfx/invobjs/coal",
	    "gfx/invobjs/cuprite",
	    "gfx/invobjs/galena",
	    "gfx/invobjs/hematite",
	    "gfx/invobjs/hornsilver",
	    "gfx/invobjs/ilmenite",
	    "gfx/invobjs/leadglance",
	    "gfx/invobjs/limonite",
	    "gfx/invobjs/magnetite",
	    "gfx/invobjs/malachite",
	    "gfx/invobjs/nagyagite",
	    "gfx/invobjs/peacockore",
	    "gfx/invobjs/petzite",
	    "gfx/invobjs/sylvanite",
	};
	private static final String[] ROCK_PATHS = {
	    "gfx/invobjs/alabaster",
	    "gfx/invobjs/apatite",
	    "gfx/invobjs/arkose",
	    "gfx/invobjs/basalt",
	    "gfx/invobjs/breccia",
	    "gfx/invobjs/chert",
	    "gfx/invobjs/corund",
	    "gfx/invobjs/diabase",
	    "gfx/invobjs/diorite",
	    "gfx/invobjs/dolomite",
	    "gfx/invobjs/eclogite",
	    "gfx/invobjs/feldspar",
	    "gfx/invobjs/flint",
	    "gfx/invobjs/fluorospar",
	    "gfx/invobjs/gabbro",
	    "gfx/invobjs/gneiss",
	    "gfx/invobjs/granite",
	    "gfx/invobjs/graywacke",
	    "gfx/invobjs/greenschist",
	    "gfx/invobjs/hornblende",
	    "gfx/invobjs/jasper",
	    "gfx/invobjs/korund",
	    "gfx/invobjs/jasper",
	    "gfx/invobjs/kyanite",
	    "gfx/invobjs/limestone",
	    "gfx/invobjs/marble",
	    "gfx/invobjs/mica",
	    "gfx/invobjs/microlite",
	    "gfx/invobjs/olivine",
	    "gfx/invobjs/orthoclase",
	    "gfx/invobjs/pegmatite",
	    "gfx/invobjs/porphyry",
	    "gfx/invobjs/pumice",
	    "gfx/invobjs/quartz",
	    "gfx/invobjs/rhyolite",
	    "gfx/invobjs/sandstone",
	    "gfx/invobjs/schist",
	    "gfx/invobjs/serpentine",
	    "gfx/invobjs/slate",
	    "gfx/invobjs/soapstone",
	    "gfx/invobjs/sodalite",
	    "gfx/invobjs/sunstone",
	    "gfx/invobjs/zincspar",
	};
	
	GobCategory(String category) {
	    resname = "gfx/hud/mmap/categories/" + category;
	    cfg = new CFG<>("mmap.categories." + category, true);
	}
	
	public GobIcon.SettingsWindow.Icon icon() {
	    if(icon == null) {
		Resource.Spec spec = new Resource.Spec(null, resname);
		Resource res = spec.loadsaved(Resource.local());
		
		icon = new GobIcon.SettingsWindow.Icon(new GobIcon.Setting(spec));
		Resource.Tooltip name = res.layer(Resource.tooltip);
		icon.tname = elf.render((name == null) ? "???" : name.t);
	    }
	    return icon;
	}
	
	public boolean matches(GobIcon.SettingsWindow.Icon icon) {
	    return this == ALL || this == categorize(icon);
	}
	
	public static GobCategory categorize(GobIcon.SettingsWindow.Icon icon) {
	    return categorize(icon.conf);
	}
	
	public static GobCategory categorize(GobIcon.Setting conf) {
	    String name = conf.res.name;
	    if(name.contains("mm/trees/")) {
		return GobCategory.TREE;
	    } else if(Arrays.stream(ANIMAL_PATHS).anyMatch(name::contains)) {
		return GobCategory.ANIMALS;
	    } else if(Arrays.stream(ROCK_PATHS).anyMatch(name::contains)) {
		return GobCategory.ROCKS;
	    } else if(Arrays.stream(ORE_PATHS).anyMatch(name::contains)) {
		return GobCategory.ORES;
	    } else if(Arrays.stream(HERB_PATHS).anyMatch(name::contains)) {
		return GobCategory.HERBS;
	    } else if(name.contains("mm/bushes/")) {
		return GobCategory.BUSHES;
	    }
	    return GobCategory.OTHER;
	}
	
	public boolean enabled() {
	    return cfg.get();
	}
	
	public void toggle() {
	    cfg.set(!cfg.get());
	}
    }
}
