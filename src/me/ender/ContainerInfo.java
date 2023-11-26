package me.ender;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import haven.Config;

import java.util.*;

public class ContainerInfo {
    private static final Map<String, Container> data = new HashMap<>();
    
    static {init();}
    
    private static void init() {
	final Gson gson = new Gson();
	
	final Map<String, Container> cfg = gson.fromJson(Config.loadJarFile("containers.json5"), new TypeToken<Map<String, Container>>() {
	}.getType());
	data.putAll(cfg);
    }
    
    public static Optional<Container> get(String id) {
	return Optional.ofNullable(data.get(id));
    }
    
    public static class Container {
	Set<Integer> full, empty;
	
	public boolean isFull(int sdt) {return full.contains(sdt);}
	
	public boolean isEmpty(int sdt) {return empty.contains(sdt);}
    }
    
}
