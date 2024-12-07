package haven;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;

import java.io.File;
import java.net.URI;
import java.nio.file.*;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class L10N {
    private static final String DEFAULT_LANGUAGE = "en";
    private static final String SUBSTITUTE_TRANSLATED = "(?<!\\\\)\\$%d";
    private static final String SUBSTITUTE_DIRECT = "(?<!\\\\)@(%d)";
    
    public static final List<String> LANGUAGES;
    public static final CFG<String> LANGUAGE = new CFG<>("i10n.language", DEFAULT_LANGUAGE);
    public static final CFG<Boolean> DBG = new CFG<>("i10n.debug", false);
    public static final String language = LANGUAGE.get();
    
    enum Bundle {
	BUTTON("button"),
	PAGINA("pagina"),
	ACTION("action"),
	TOOLTIP("tooltip"),
	INGREDIENT("ingredient"),
	WINDOW("window", true),
	LABEL("label", true),
	FLOWER("flower", true),
	BIOME("biome"),
	MSG("msg");
    
	public final String name;
	public final boolean useMatch;
    
	Bundle(String name, boolean useMatch) {
	    this.name = name;
	    this.useMatch = useMatch;
	}
	
	Bundle(String name) {this(name, false);}
    }
    
    private final static Map<Bundle, Map<String, String>> simple = new HashMap<>();
    private final static Map<Bundle, Map<Pattern, String>> match = new HashMap<>();
    
    private final static Gson GSON_OUT = new GsonBuilder().setPrettyPrinting().create();
    private final static Map<Bundle, Map<String, String>> MISSING = new HashMap<>();
    
    static {
	Set<String> set = new LinkedHashSet<>();
	set.add(DEFAULT_LANGUAGE);
	List<String> tmp = getJARLanguages();
	tmp.addAll(getFSLanguages());
	tmp.sort(String::compareTo);
	set.addAll(tmp);
	LANGUAGES = new LinkedList<>(set);
    
	for (Bundle bundle : Bundle.values()) {
	    if(bundle.useMatch) {
		match.put(bundle, loadMatch(bundle));
	    } else {
		simple.put(bundle, loadSimple(bundle));
	    }
	    MISSING.put(bundle, new HashMap<>());
	}
    }
    
    public static boolean isDefaultLanguage() {
	return DEFAULT_LANGUAGE.equals(language);
    }
    
    private static List<String> getJARLanguages() {
	try {
	    URI uri = L10N.class.getResource("/i10n").toURI();
	    FileSystem fs = FileSystems.newFileSystem(uri, Collections.emptyMap());
	    return Files.list(fs.getPath("/i10n/"))
		.map(Path::getFileName)
		.map(Path::toString)
		.map(s -> (s.endsWith("/")) ? s.substring(0, s.length() - 1) : s)
		.collect(Collectors.toList());
	} catch (Exception ignored) {
	}
	return Collections.emptyList();
    }
    
    private static List<String> getFSLanguages() {
	try {
	    return Files.list(Paths.get("./i10n/"))
		.map(Path::toFile)
		.filter(File::isDirectory)
		.map(File::getName)
		.collect(Collectors.toList());
	} catch (Exception ignored) {
	}
	return Collections.emptyList();
    }
    
    public static String button(String text) {
	return process(Bundle.BUTTON, text);
    }
    
    public static String label(String text) {
	return process(Bundle.LABEL, text);
    }
    
    public static String flower(String text) {
	return process(Bundle.FLOWER, text);
    }
    
    public static String tooltip(String text) {
	return process(Bundle.TOOLTIP, text);
    }
    
    public static String biome(String text) {
	return process(Bundle.BIOME, text);
    }
    
    public static String msg(String text) {
	return process(Bundle.MSG, text);
    }
    
    public static String tooltip(String text, String def) {
	return process(Bundle.TOOLTIP, text, def);
    }
    
    public static String tooltip(Resource res, String def) {
	return process(Bundle.TOOLTIP, res.name, def);
    }
    
    public static String window(String text) {
	return process(Bundle.WINDOW, text);
    }
    
    public static String ingredient(String text) {
	return process(Bundle.INGREDIENT, text);
    }
    
    private static String ingredient(Matcher m, int g) {
	String value = m.group(g);
	if(value.matches("[\\w\\s]+") && !value.matches("[\\d]+")) {
	    return ingredient(value);
	}
	return value;
    }
    
    public static String pagina(Resource res, String def) {
	return process(Bundle.PAGINA, res.name, def);
    }
    
    public static String action(Resource res, String def) {
	return process(Bundle.ACTION, res.name, def);
    }
    
    private static String process(Bundle bundle, String key) {
	return process(bundle, key, key);
    }
    
    private static String process(Bundle bundle, String key, String def) {
	String result = null;
	if(key == null || key.isEmpty() || isDefaultLanguage()) {
	    return def;
	}
	if(bundle.useMatch) {
	    Map<Pattern, String> patterns = match.get(bundle);
	    Matcher m = patterns.keySet().stream().map(p -> p.matcher(key)).filter(Matcher::find).findFirst().orElse(null);
	    if(m != null) {
		String format = patterns.get(m.pattern());
		int k = m.groupCount();
		result = format;
		for (int i = 1; i <= k; i++) {
		    result = result.replaceAll(String.format(SUBSTITUTE_DIRECT, i), m.group(i));
		    String regex = String.format(SUBSTITUTE_TRANSLATED, i);
		    if(result.matches(".*" + regex + ".*")) {//crude way to say 'any match'
			result = result.replaceAll(regex, ingredient(m, i));
		    }
		}
	    }
	} else {
	    Map<String, String> map = simple.get(bundle);
	    if(map == null) {
		return def;
	    }
	    if(map.containsKey(key)) {
		result = map.get(key);
	    } else if(bundle == Bundle.TOOLTIP) {
		result = process(Bundle.ACTION, key, null);
	    }
	}
	if(DBG.get() && result == null && def != null) {
	    reportMissing(bundle, key, def);
	}
	return result != null ? result : def;
    }
    
    public static String revertFlower(String text) {
        return revertTranslation(text, Bundle.FLOWER);
    }

    private static String revertTranslation(String text, Bundle bundle) {
        if (text == null || text.isEmpty() || L10N.isDefaultLanguage()) {
            return text;
        }
        Map<?, String> map = null;
        if (bundle.useMatch) {
            map = match.get(bundle);
        } else {
            map = simple.get(bundle);
        }
        if(map == null || map.isEmpty()) {
            return text;
        }
        for(Map.Entry<?, String> entry : map.entrySet()) {
            if (entry.getValue().equals(text)){
                if (bundle.useMatch) {
                    String result = ((Pattern) entry.getKey()).toString();
                    return result.substring(1, result.length()-1);
                } else {
                    return (String) entry.getKey();
                }
            }
        }
        return text;
    }

    private static void reportMissing(Bundle bundle, String key, String def) {
	synchronized (MISSING) {
	    key = key.replaceAll("[()\\[\\]]", "\\\\$0");
	    Map<String, String> missingBundle = MISSING.get(bundle);
	    missingBundle.put(key, def);
	    String file = String.format("i10n/%s/missing/%s.json", language, bundle.name);
	    Config.saveFile(file, GSON_OUT.toJson(missingBundle));
	}
    }
    
    private static Map<String, String> loadSimple(Bundle bundle) {
	Map<String, String> map = new HashMap<>();
	if(!isDefaultLanguage()) {
	    String name = String.format("i10n/%s/%s.json", language, bundle.name);
	    map.putAll(parseJSON(Config.loadJarFile(name)));
	    map.putAll(parseJSON(Config.loadFSFile(name)));
	}
	return map;
    }
    
    private static Map<Pattern, String> loadMatch(Bundle bundle) {
	Map<String, String> tmp = loadSimple(bundle);
	HashMap<Pattern, String> map = new HashMap<>();
	for (Map.Entry<String, String> e : tmp.entrySet()) {
	    try {
		map.put(Pattern.compile(String.format("^%s$", e.getKey())), e.getValue());
	    } catch (Exception error) {
		error.printStackTrace();
	    }
	}
	return map;
    }
    
    private static Map<String, String> parseJSON(String json) {
	Map<String, String> map = null;
	if(json != null) {
	    try {
		Gson gson = new GsonBuilder().create();
		map = gson.fromJson(json, new TypeToken<Map<String, String>>() {
		}.getType());
		
	    } catch (JsonSyntaxException ignored) {
	    }
	}
	return map == null ? new HashMap<>() : map;
    }
}
