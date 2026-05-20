package me.ender;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import haven.Config;
import haven.Resource;

import java.lang.reflect.Type;
import java.util.Collections;
import java.util.Map;

public class WoundTreatment {
    private static Map<String, String[]> TREAT_CFG;
    
    private static void initTreatments() {
	if(TREAT_CFG != null) {return;}
	
	Gson gson = (new GsonBuilder()).setPrettyPrinting().create();
	try {
	    Type type = new TypeToken<Map<String, String[]>>() {
	    }.getType();
	    TREAT_CFG = gson.fromJson(Config.loadFile("treatments.json5"), type);
	} catch (Exception ignored) {
	    TREAT_CFG = Collections.emptyMap();
	}
    }
    
    public static String treatment(Resource wound) {
	initTreatments();
	String[] treatment = TREAT_CFG.getOrDefault(wound.name, null);
	StringBuilder buf = new StringBuilder();
	if(treatment != null && treatment.length > 0) {
	    buf.append("\n\n$b{$font[serif,16]{Treatment}}\n\n");
	    for (String t : treatment) {
		try {
		    String name = Resource.remote().load(t).get().layer(Resource.tooltip).t;
		    buf.append("$img[").append(t).append(",c,h=16]").append(name).append("\n");
		} catch (Exception ignore) {
		    buf.append(t).append("\n");
		}
	    }
	}
	return buf.toString();
    }
}