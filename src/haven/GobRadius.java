package haven;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import haven.res.gfx.fx.msrad.MSRad;

import java.awt.*;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;

public class GobRadius {
    private static final String GOB_RADIUS_JSON = "gob_radius.json";
    public static final Map<String, GobRadius> gobRadiusCfg;
    static final Color DEF_COL = new Color(255, 255, 255, 128);
    
    public String color, color2;
    public float radius;
    
    static {
	gobRadiusCfg = parseJson(Config.loadFile(GOB_RADIUS_JSON));
	showDefaultRadii(CFG.SHOW_GOB_RADIUS.get());
	CFG.SHOW_GOB_RADIUS.observe(GobRadius::showDefaultRadii);
    }
    
    private static Map<String, GobRadius> parseJson(String json) {
	Map<String, GobRadius> result = new HashMap<>();
	if(json != null) {
	    try {
		Gson gson = getGson();
		Type collectionType = new TypeToken<HashMap<String, GobRadius>>() {
		}.getType();
		result = gson.fromJson(json, collectionType);
	    } catch (Exception e) {
		result = new HashMap<>();
	    }
	}
	return result;
    }

    public static void save() {
	Config.saveFile(GOB_RADIUS_JSON, getGson().toJson(gobRadiusCfg));
    }

    public static GobRadius get(String resname) {
	return gobRadiusCfg.get(resname);
    }
    
    public static void showDefaultRadii(CFG<Boolean> show) {
	showDefaultRadii(show.get());
    }
    
    public static void showDefaultRadii(boolean show) {
	MSRad.show(show);
    }
    
    private static Gson getGson() {
	GsonBuilder builder = new GsonBuilder();
	builder.setPrettyPrinting();
	return builder.create();
    }

    public Color color() {
	Color c = Utils.hex2color(color, null);
	if(c == null) {
	    return DEF_COL;
	}
	return c;
    }
    
    public Color color2() {
	Color c = Utils.hex2color(color2, null);
	if(c == null) {
	    c = color();
	    return new Color(c.getRed(), c.getGreen(), c.getBlue(), 255);
	}
	return c;
    }

}
