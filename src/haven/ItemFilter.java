package haven;

import haven.QualityList.SingleType;
import haven.resutil.Curiosity;
import haven.resutil.FoodInfo;
import me.ender.Reflect;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ItemFilter {
    private static final Pattern q = Pattern.compile("(?:(\\w+))?(?:^|:)([\\w\\p{L}*]+)?(?:([<>=+~])(\\d+(?:\\.\\d+)?)?([<>=+~])?)?");
    private static final Pattern float_p = Pattern.compile("(\\d+(?:\\.\\d+)?)");
    
    public static final String HELP_SIMPLE = "$size[20]{$b{Simple search}}\n" +
	"Just enter text and items with matching names will get highlighted\n";
    
    public static final String HELP_FULL_TEXT = "$size[20]{$b{Full text search}}\n" +
	"$font[monospaced,16]{txt:[text]}\n" +
	"Will search for $font[monospaced,13]{[text]} in any text field in item tooltip (currently only Name and Coinage)\n";
    
    public static final String HELP_CONTENT = "$size[20]{$b{Contents search}}\n" +
	"$font[monospaced,16]{has:[txt][sign][value]}\n" +
	"Will highlight all items that have $font[monospaced,13]{[txt]} in their contents in quantity specified by $font[monospaced,13]{[sign]} and $font[monospaced,13]{[value]}.\n" +
	"$size[16]{\nExamples:}\n" +
	"$font[monospaced,13]{  has:water    }will find items that have water in their Contents.\n" +
	"$font[monospaced,13]{  has:water>2  }will find items that contain more than 2L of water.\n" +
	"$font[monospaced,13]{  has:water+3  }will find items that contain at least 3L of water.\n" +
	"$font[monospaced,13]{  has:water<10 }will find items that contain less than 10L of water.\n" +
	"$font[monospaced,13]{  has:water=2  }will find items that contain exactly 2L of water.\n";
    
    public static final String HELP_QUALITY = "$size[20]{$b{Quality search}}\n" +
	"$font[monospaced,16]{q[:][sign][value]}\n" +
	"Will highlight items with quality value specified by $font[monospaced,13]{[sign]} and $font[monospaced,13]{[value]}.\n" +
	"$font[monospaced,13]{[sign]} can be $font[monospaced,13]{>} (more), $font[monospaced,13]{+} (at least), $font[monospaced,13]{<} (less), $font[monospaced,13]{=} (exactly).\n" +
	"$size[16]{\nExamples:}\n" +
	"$font[monospaced,13]{  q:5   }will find items with quality of 5\n" +
	"$font[monospaced,13]{  q<12  }will find items with quality less than 12\n" +
	"$font[monospaced,13]{  q+21  }will find items with quality of at least 21\n";
    
    public static final String HELP_CURIO = "$size[20]{$b{Curiosity search}}\n" +
	"Supports $font[monospaced,13]{lp} (learning point gained), $font[monospaced,13]{lph} (learning point gained per hour), $font[monospaced,13]{xp} (experience required) and $font[monospaced,13]{mw} (mental weight required) tags. They all interchangeable in the examples below.\n" +
	"$size[16]{\nExamples:}\n" +
	"$font[monospaced,13]{  lp:    }will find items that grant LP.\n" +
	"$font[monospaced,13]{  lp>100 }will find items that grant more than 100 LP.\n" +
	"$font[monospaced,13]{  lp+200 }will find items that grant at least 200 LP.\n" +
	"$font[monospaced,13]{  lp<300 }will find items that grant no more than 300 LP.\n" +
	"$font[monospaced,13]{  lp=400 }will find items that grant exactly 400 LP.\n";
    
    public static final String HELP_FEP = "$size[20]{$b{FEP search}}\n" +
	"$font[monospaced,16]{fep:[type][sign][value]}\n" +
	"Will highlight items that grant FEPs of type $font[monospaced,13]{[type]} in quantity described by $font[monospaced,13]{[sign]} and $font[monospaced,13]{[value]}.\n" +
	"$size[16]{\nExamples:}\n" +
	"$font[monospaced,13]{  fep:str>1 }will find food giving more than 1 Strength FEPs.\n" +
	"$font[monospaced,13]{  fep:agi+2 }will find food giving at least than 2 Agility FEPs.\n" +
	"$font[monospaced,13]{  fep:cha<3 }will find food giving less than 3 Charisma FEPs.\n" +
	"$font[monospaced,13]{  fep:dex=4 }will find food giving exactly 4 Dexterity FEPs.\n"+
	"\n" +
	"$size[20]{$b{Food values search}}\n" +
	"Supports $font[monospaced,13]{hunger} or $font[monospaced,13]{hng}, (Hunger satiated) and $font[monospaced,13]{energy} or $font[monospaced,13]{nrg} (Energy restored) tags. They all interchangeable in the examples below.\n" +
	"$size[16]{\nExamples:}\n" +
	"$font[monospaced,13]{  nrg:>50  }will find food which restores more than 50 energy\n" +
	"$font[monospaced,13]{  nrg<120  }will find food which restores no more than 120 energy\n" +
	"$font[monospaced,13]{  nrg:+200 }will find food which restores at least 200 energy\n";
    
    public static final String HELP_ARMOR = "$size[20]{$b{Armor search}}\n" +
	"$font[monospaced,16]{armor:[type][sign][value]}\n" +
	"Will highlight items that grant armor of type $font[monospaced,13]{[type]} in quantity described by $font[monospaced,13]{[sign]} and $font[monospaced,13]{[value]}.\n" +
	"Use $font[monospaced,13]{hard} or $font[monospaced,13]{deflect} or $font[monospaced,13]{h} type to denote hard armor.\n" +
	"Use $font[monospaced,13]{soft} or $font[monospaced,13]{soak} or $font[monospaced,13]{s} type to denote soft armor.\n" +
	"Use $font[monospaced,13]{all}, $font[monospaced,13]{any}, $font[monospaced,13]{total}, $font[monospaced,13]{*} or leave empty to denote sum of hard and soft armor.\n" +
	"$font[monospaced,13]{[type]} can be entered partially.\n" +
	"$size[16]{\nExamples:}\n" +
	"$font[monospaced,13]{  armor:hard>1 }will find items providing more than 1 hard armor.\n" +
	"$font[monospaced,13]{  armor:soft<2 }will find items providing less than 2 soft armor.\n" +
	"$font[monospaced,13]{  armor:all=3  }will find items providing exactly 3 total armor.\n" +
	"$font[monospaced,13]{  armor:>4     }will find items providing exactly 4 total armor.\n" +
	"$font[monospaced,13]{  armor:h>5    }will find items providing more than 5 hard armor.\n";
    
    public static final String HELP_SYMBEL = "$size[20]{$b{Symbel search}}\n" +
	    "$font[monospaced,16]{symb:[type][sign][value]}\n" +
	    "Will highlight items that have symbel values defined by $font[monospaced,13]{[type]} in quantity described by $font[monospaced,13]{[sign]} and $font[monospaced,13]{[value]}.\n" +
	    "Use $font[monospaced,13]{fep} type to denote fep bonus.\n" +
	    "Use $font[monospaced,13]{hunger} type to denote hunger modifier.\n" +
	    "$font[monospaced,13]{[type]} can be entered partially.\n" +
	    "$size[16]{\nExamples:}\n" +
	    "$font[monospaced,13]{  symb:          }will find all symbel items.\n" +
	    "$font[monospaced,13]{  symb:fep>2     }will find items with more than 2% fep bonus.\n" +
	    "$font[monospaced,13]{  symb:hunger<3  }will find items with less than 3% hunger reduction.\n" +
	    "$font[monospaced,13]{  symb:h=5       }will find items with exactly 5% hunger reduction.\n";
    
    public static final String HELP_ATTR = "$size[20]{$b{Attribute search}}\n" +
	"$font[monospaced,16]{attr:[type][sign][value]}\n" +
	"Will highlight items (equipment or gilding items) that grant attribute or skill bonuses defined by $font[monospaced,13]{[type]} in amount described by $font[monospaced,13]{[sign]} and $font[monospaced,13]{[value]}.\n" +
	"$font[monospaced,13]{[type]} can be any attribute/skill name from character sheet and can be entered partially.\n" +
	"$size[16]{\nExamples:}\n" +
	"$font[monospaced,13]{  attr:survival }will find all items that grant survival.\n" +
	"$font[monospaced,13]{  attr:str>2    }will find items granting more than 2 str bonus.\n" +
	"$font[monospaced,13]{  attr:agi<0    }will find items giving agility penalty.\n";
    
    public static final String HELP_INPUTS = "$size[20]{$b{Crafting inputs search}}\n" +
	"$font[monospaced,16]{use:[what][sign][value]}\n" +
	"Will highlight items that have $font[monospaced,13]{[what]} items in its crafting inputs in amount described by $font[monospaced,13]{[sign]} and $font[monospaced,13]{[value]}.\n" +
	"$font[monospaced,13]{[what]} can be entered partially, but note that all meat has name Meat, regardless of type - limitation of data from server.\n" +
	"$size[16]{\nExamples:}\n" +
	"$font[monospaced,13]{  use:snow   }will find all items that require snow to craft.\n" +
	"$font[monospaced,13]{  use:iron>2 }will find items that require more that 2 iron bars/ingots to craft.\n";
    
    public static final String[] FILTER_HELP = {HELP_SIMPLE, HELP_FULL_TEXT, HELP_CONTENT, HELP_QUALITY, HELP_CURIO, HELP_FEP, HELP_ARMOR, HELP_SYMBEL, HELP_ATTR, HELP_INPUTS};
    
    public boolean matches(List<ItemInfo> info) {
	for (ItemInfo item : info) {
	    if(match(item)) {return true;}
	}
	return match(QualityList.make(info));

    }
    
    final public boolean matches(ItemData data, Session sess) {
	return data != null && matches(data.iteminfo(sess));
    }

    final public boolean matches(MenuGrid.Pagina pagina, Session sess) {
	List<ItemInfo> infos = pagina.info();
	if(infos == null || infos.isEmpty()){
	    return matches(ItemData.get(pagina), sess);
	}
	return matches(infos);
    }

    protected boolean match(ItemInfo item) { return false; }

    public static ItemFilter create(String query) {
	Compound result = new Compound();
	Matcher m = q.matcher(query);
	while (m.find()) {
	    String tag = m.group(1);
	    String text = m.group(2);
	    String sign = m.group(3);
	    String value = m.group(4);
	    String opt = m.group(5);

	    if(text == null) {
		text = "";
	    } else {
		text = text.toLowerCase();
	    }

	    ItemFilter filter = null;
	    if(sign != null && tag == null) {
		switch (text) {
		    case "energy":
		    case "nrg":
		        tag = text = "energy";
		        break;
		    case "hunger":
		    case "hng":
			tag = text = "hunger";
			break;
		    case "xp":
		    case "lp":
		    case "mw":
		    case "lph":
			tag = text;
			break;
		    case "q":
			tag = "q";
			text = "single";
			break;
		    case "armor":
		        tag = text;
		        text = "all";
		        break;
		}
	    }
	    if(tag == null) {
		filter = new Text(text, true);
	    } else {
		tag = tag.toLowerCase();
		switch (tag) {
		    case "txt":
			filter = new Text(text, true);
			break;
		    case "xp":
		    case "lp":
		    case "mw":
		    case "lph":
			filter = new XP(tag, sign, value, opt);
			break;
		    case "energy":
		    case "fill":
			filter = new Food(tag, sign, value, opt);
			break;
		    case "has":
			filter = new Has(text, sign, value, opt);
			break;
		    case "q":
			filter = new Q(text, sign, value, opt);
			break;
		    case "fep":
			filter = new FEP(text, sign, value, opt);
			break;
		    case "armor":
			filter = new Armor(text, sign, value, opt);
			break;
		    case "gast":
		    case "symb":
			filter = new Gastronomy(text, sign, value, opt);
			break;
		    case "attr":
			filter = new Attribute(text, sign, value, opt);
			break;
		    case "use":
		    case "uses":
			filter = new Inputs(text, sign, value, opt);
			break;
		}
	    }
	    if(filter != null) {
		result.add(filter);
	    }
	}
	return result;
    }
    
    public static void showHelp(UI ui, String ...blocks) {
	Window log = ui.root.add(new WindowX(new Coord(50, 50), "Filter Help"), new Coord(100, 50));
	log.justclose = true;
	Textlog txt = log.add(new Textlog(UI.scale(450, 500)));
	txt.quote = false;
	txt.maxLines = -1;
	log.pack();
	txt.append(String.join("\n", blocks));
	txt.append(" ");
	txt.setprog(0);
    }
    
    public static class Compound extends ItemFilter {
	List<ItemFilter> filters = new LinkedList<>();

	@Override
	public boolean matches(List<ItemInfo> info) {
	    if(filters.isEmpty()) {return false;}
	    for (ItemFilter filter : filters) {
		if(!filter.matches(info)) {return false;}
	    }
	    return true;
	}

	public void add(ItemFilter filter) {
	    filters.add(filter);
	}
    }

    private static class Complex extends ItemFilter {
	protected final String text;
	protected final Sign sign;
	protected final Sign opts;
	protected float value;
	protected final boolean all;
	protected final boolean any;

	public Complex(String text, String sign, String value, String opts) {
	    this.text = text.toLowerCase();
	    this.sign = getSign(sign);
	    this.opts = getSign(opts);
	    float tmp = 0;
	    try {
		tmp = Float.parseFloat(value);
	    } catch (Exception ignored) {}
	    this.value = tmp;

	    all = text.equals("*") || text.equals("all");
	    any = text.equals("any");
	}
    
	protected boolean test(double actual) {
	    return test(actual, value);
	}
	
	protected boolean test(double actual, double target) {
	    switch (sign) {
		case GREATER:
		    return actual > target;
		case LESS:
		    return actual <= target;
		case EQUAL:
		    return actual == target;
		case GREQUAL:
		    return actual >= target;
		default:
		    return actual != 0;
	    }
	}

	protected Sign getSign(String sign) {
	    if(sign == null) {
		return getDefaultSign();
	    }
	    switch (sign) {
		case ">":
		    return Sign.GREATER;
		case "<":
		    return Sign.LESS;
		case "=":
		    return Sign.EQUAL;
		case "+":
		    return Sign.GREQUAL;
		case "~":
		    return Sign.WAVE;
		default:
		    return getDefaultSign();
	    }
	}

	protected Sign getDefaultSign() {
	    return Sign.DEFAULT;
	}

	public enum Sign {GREATER, LESS, EQUAL, GREQUAL, WAVE, DEFAULT}
    }

    private static class Has extends Complex {
	public Has(String text, String sign, String value, String opts) {
	    super(text, sign, value, opts);
	}

	@Override
	protected boolean match(ItemInfo item) {
	    if(item instanceof ItemInfo.Contents) {
		String name = this.name(((ItemInfo.Contents) item).sub);
		if(name != null) {
		    name = name.toLowerCase();
		    return name.contains(text) && test(count(name));
		}
	    }
	    return false;
	}

	@Override
	protected Sign getDefaultSign() {
	    return Sign.GREQUAL;
	}

	private float count(String txt) {
	    float n = 0;
	    if(txt != null) {
		try {
		    Matcher matcher = float_p.matcher(txt);
		    if(matcher.find()) {
			n = Float.parseFloat(matcher.group(1));
		    }
		} catch (Exception ignored) {}
	    }
	    return n;
	}

	private String name(List<ItemInfo> sub) {
	    ItemInfo.Name name = ItemInfo.find(ItemInfo.Name.class, sub);
	    return name != null ? name.str.text : null;
	}
    }

    public static class Text extends ItemFilter {
	private String text;
	private final boolean full;

	public Text(String text, boolean full) {
	    this.full = full;
	    this.text = text.toLowerCase();
	}

	public void update(String text) {
	    this.text = text.toLowerCase();
	}

	@Override
	protected boolean match(ItemInfo item) {
	    if(text != null && !text.isEmpty()) {
		if(item instanceof ItemInfo.Name) {
		    return ((ItemInfo.Name) item).str.text.toLowerCase().contains(text);
		} else if(full) {
		    if(item instanceof ItemInfo.AdHoc) {
			return ((ItemInfo.AdHoc) item).str.text.toLowerCase().contains(text);
		    } else if(Reflect.is(item, "Coinage")) {
			String coinage = Reflect.getFieldValueString(item, "nm");
			return coinage != null && coinage.toLowerCase().contains(text);
		    }
		}
	    }

	    return false;
	}
    }

    private static class XP extends Complex {
	public XP(String text, String sign, String value, String opt) {super(text, sign, value, opt);}

	@Override
	protected boolean match(ItemInfo item) {
	    if(item instanceof Curiosity) {
		Curiosity curio = (Curiosity) item;
		if("lp".equals(text)) {
		    return test(curio.exp);
		} else if ("lph".equals(text)) {
		    return test(Curiosity.lph(curio.lph));
		} else if("xp".equals(text)) {
		    return test(curio.enc);
		} else if("mw".equals(text)) {
		    return test(curio.mw);
		}
	    }
	    return false;
	}


	@Override
	protected Sign getDefaultSign() {
	    return Sign.GREQUAL;
	}
    }

    private static class Q extends Complex {
	public Q(String text, String sign, String value, String opts) { super(text, sign, value, opts); }

	@Override
	protected boolean match(ItemInfo item) {
	    if(!(item instanceof QualityList)) {return false;}
	    QualityList quality = (QualityList) item;
	    if(quality.isEmpty()) {return false;}

	    SingleType type = null;
	    if(text != null && !text.isEmpty()) {
		type = getTextType(text);
	    }

	    if(type == null) {
		type = getGenericType();
	    }

	    if(type == null) {
		return test(quality.single().value);
	    } else {
		return test(quality.single(type).value);
	    }
	}
    
	@Override
	public boolean matches(List<ItemInfo> info) {
	    QualityList q = ItemInfo.getContent(info).q;
	    if(!q.isEmpty()) {
		return match(q);
	    }
	    return super.matches(info);
	}
    
	@Override
	protected Sign getDefaultSign() {
	    return Sign.EQUAL;
	}
    
	private SingleType getTextType(String text) {
	    SingleType[] types = SingleType.values();
	    for (SingleType type : types) {
		if(type.name().toLowerCase().startsWith(text)) {
		    return type;
		}
	    }
	    return null;
	}

	private SingleType getGenericType() {
	    switch (opts) {
		case GREATER:
		    return SingleType.Max;
		case LESS:
		    return SingleType.Min;
		case EQUAL:
		case WAVE:
		    return SingleType.Average;
		default:
		    return null;
	    }
	}
    }

    private static class FEP extends Complex {
	public FEP(String text, String sign, String value, String opts) {
	    super(text, sign, value, opts);
	}

	@Override
	protected boolean match(ItemInfo item) {
	    if(item instanceof FoodInfo) {
		FoodInfo fep = (FoodInfo) item;
		if(text != null && text.length() >= 3) {
		    for (FoodInfo.Event event : fep.evs) {
			if(event.ev.nm.toLowerCase().startsWith(text) && test(event.a)) {
			    return true;
			}
		    }
		} else {
		    return true;
		}
	    }

	    return false;
	}
    }

    private static class Food extends Complex {
	public Food(String text, String sign, String value, String opts) {
	    super(text, sign, value, opts);
	}

	@Override
	protected boolean match(ItemInfo item) {
	    if(item instanceof FoodInfo) {
		FoodInfo food = (FoodInfo) item;
		if("energy".equals(text)) {
		    return test(Utils.round(100 * food.end, 2));
		} else if("hunger".equals(text)) {
		    return test(Utils.round(100 * food.glut, 2));
		}
	    }
	    return false;
	}
    }
    
    private static class Armor extends Complex {
	private static final String[] hard = {"hard", "deflect", "h"};
	private static final String[] soft = {"soft", "soak", "s"};
	private static final String[] all = {"all", "any", "total", "*"};
	
	private Armor(String text, String sign, String value, String opts) {
	    super(text, sign, value, opts);
	}
	
	@Override
	public boolean matches(List<ItemInfo> info) {
	    Pair<Integer, Integer> armor = ItemInfo.getArmor(info);
	    if(armor != null) {
		switch (getType(text)) {
		    case 0://all
			return test(armor.a + armor.b);
		    case 1://hard
			return test(armor.a);
		    case 2://soft
			return test(armor.b);
		    default:
			return false;
		}
	    }
	    return false;
	}
	
	private int getType(String text) {
	    if(text == null || text.isEmpty()) {
		return 0;//all
	    } else {
		for (String tmp : all) {
		    if(tmp.startsWith(text)) {
			return 0;//all
		    }
		}
		for (String tmp : hard) {
		    if(tmp.startsWith(text)) {
			return 1;//hard
		    }
		}
		for (String tmp : soft) {
		    if(tmp.startsWith(text)) {
			return 2;//soft
		    }
		}
	    }
	    return -1;
	}
    }
    
    private static class Gastronomy extends Complex {
	
	public Gastronomy(String text, String sign, String value, String opts) {
	    super(text, sign, value, opts);
	}
	
	@Override
	protected boolean match(ItemInfo item) {
	    if(Reflect.is(item, "Gast")) {
		if(text.isEmpty()) {
		    return true;
		}
		if("fep".startsWith(text)) {
		    return test(Utils.round(100D * Reflect.getFieldValueDouble(item, "fev"), 1));
		}
		if("hunger".startsWith(text)) {
		    return test(Utils.round(100D * Reflect.getFieldValueDouble(item, "glut"), 1));
		}
	    }
	    return false;
	}
    }
    
    private static class Attribute extends Complex {
	
	public Attribute(String text, String sign, String value, String opts) {
	    super(text, sign, value, opts);
	}
	
	@Override
	public boolean matches(List<ItemInfo> info) {
	    Map<Resource, Integer> bonuses = ItemInfo.getBonuses(info, null);
	    if(text != null && text.length() >= 3) {
		for (Resource res : bonuses.keySet()) {
		    Resource.Tooltip tip = res.layer(Resource.tooltip);
		    if(tip.t.toLowerCase().startsWith(text) || tip.o.toLowerCase().startsWith(text)) {
			return test(bonuses.get(res));
		    }
		}
	    }
	    return false;
	}
    }
    
    private static class Inputs extends Complex {
	
	public Inputs(String text, String sign, String value, String opts) {
	    super(text, sign, value, opts);
	}
	
	@Override
	public boolean matches(List<ItemInfo> info) {
	    List<Pair<Resource, Integer>> inputs = ItemInfo.getInputs(info);
	    if(text != null && text.length() >= 3) {
		for (Pair<Resource, Integer> input : inputs) {
		    Resource res = input.a;
		    if(res.layer(Resource.tooltip).t.toLowerCase().contains(text)) {
			return test(input.b);
		    }
		}
	    }
	    return false;
	}
    }
}
