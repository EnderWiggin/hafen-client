package haven.rx;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import haven.*;

import java.awt.event.KeyEvent;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CharterBook extends WindowX {
    private static final String PREFIX = "The name of this charterstone is";
    public static final String CONFIG_JSON = "charterbook.json";
    private static boolean initialized = false;
    private static Pattern filter = Pattern.compile(String.format("%s \"(.*)\".", PREFIX));
    private static Map<String, List<String>> config;
    private static List<String> names;
    private static Gson gson;
    
    public CharterBook(Coord sz, String cap, boolean lg) {
	super(sz, cap, lg);
	pack();
    }
    
    public static void loadPlayer(String player) {
	names = new ArrayList<>(config.getOrDefault(player, Collections.emptyList()));
	names.sort(String::compareTo);
	boolean newUser = !config.containsKey(player);
	config.put(player, names);
	if(newUser) {save();}
    }
    
    public static void init() {
	if(initialized) {return;}
	initialized = true;
	gson = (new GsonBuilder()).setPrettyPrinting().create();
	load();
	
	Reactor.PLAYER.subscribe(CharterBook::loadPlayer);
	Reactor.IMSG.filter(s -> s.startsWith(PREFIX)).subscribe(CharterBook::addCharter);
    }
    
    private static void load() {
	if(config == null) {
	    try {
		config = gson.fromJson(Config.loadFile(CONFIG_JSON), new TypeToken<Map<String, List<String>>>() {
		}.getType());
	    } catch (Exception ignore) {}
	    if(config == null) {
		config = new HashMap<>();
	    }
	}
    }
    
    private static void save() {
	Config.saveFile(CONFIG_JSON, gson.toJson(config));
    }
    
    private static void addCharter(String message) {
	Matcher m = filter.matcher(message);
	if(m.find()) {
	    String name = m.group(1);
	    if(!names.contains(name)) {
		names.add(name);
		save();
	    }
	}
    }
    
    @Override
    public <T extends Widget> T add(T child) {
	if(child instanceof TextEntry) {
	    final TextEntry text = (TextEntry) child;
	    add(new DropboxOfStrings(child.sz.x + 15, 5, child.sz.y), child.c)
		.setData(names)
		.setChangedCallback((index, charter) -> {
		    text.settext(charter);
		    text.buf.key('\0', KeyEvent.VK_END, 0); //move caret to the end
		    setfocus(text);
		});
	    add(new Button(50, "GO", false, text::activate), child.c.add(child.sz.x + 20, 0));
	}
	return super.add(child);
    }
}
