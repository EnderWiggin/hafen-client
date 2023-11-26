package me.ender.timer;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import haven.Config;
import haven.TimerPanel;
import haven.Widget;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class Timer {
    public static final float SERVER_RATIO = 3.29f;
    private static final String TIMERS_CFG = "timers.cfg";
    
    private static final Object lock = new Object();
    private static final List<Timer> timers = load();
    
    private static long server;
    private static long local;
    private static double delta;
    private static Widget parent = null;
    
    public interface UpdateCallback {
	void update(Timer timer);
    }
    
    private long start;
    private final long duration;
    public final String name;
    transient public long remaining;
    transient public UpdateCallback listener;
    
    private static List<Timer> load() {
	List<Timer> timers = null;
	try {
	    Gson gson = new GsonBuilder().create();
	    timers = gson.fromJson(Config.loadFile(TIMERS_CFG), new TypeToken<List<Timer>>() {
	    }.getType());
	} catch (Exception ignored) {
	}
	return timers == null ? new LinkedList<>() : timers;
    }
    
    public static void save() {
	Gson gson = new GsonBuilder().create();
	Config.saveFile(TIMERS_CFG, gson.toJson(timers));
    }
    
    public static void server(long time) {
	server = time;
	local = System.currentTimeMillis();
    }
    
    public static void tick(double dt) {
	if(parent == null) {return;}
	delta += dt;
	if(delta > 0.2) {
	    updateTimers();
	    delta = 0;
	}
    }
    
    private static void updateTimers() {
	for (Timer timer : timers()) {
	    if(timer.isWorking() && timer.update()) {
		timer.stop();
	    }
	}
    }
    
    public static void start(Widget parent) {
	Timer.parent = parent;
	updateTimers();
    }
    
    public static Timer add(String name, long duration) {
	Timer t = new Timer(name, duration);
	synchronized (lock) {
	    timers.add(t);
	    save();
	}
	return t;
    }
    
    public static List<Timer> timers() {
	synchronized (lock) {
	    return new ArrayList<>(timers);
	}
    }
    
    public static int count() {
	synchronized (lock) {
	    return timers.size();
	}
    }
    
    private static void remove(Timer t) {
	synchronized (lock) {
	    timers.remove(t);
	    save();
	}
    }
    
    private Timer(String name, long duration) {
	this.name = name;
	this.duration = duration;
    }
    
    public boolean isWorking() {
	return start != 0;
    }
    
    public void stop() {
	start = 0;
	if(listener != null) {
	    listener.update(this);
	}
	save();
    }
    
    public void start() {
	start = (long) (server + SERVER_RATIO * (System.currentTimeMillis() - local));
	save();
    }
    
    public synchronized boolean update() {
	long now = System.currentTimeMillis();
	remaining = (long) (duration - now + local - (server - start) / SERVER_RATIO);
	if(remaining <= 0) {
	    if(parent != null) {TimerPanel.complete(this, parent);}
	    return true;
	}
	if(listener != null) {
	    listener.update(this);
	}
	return false;
    }
    
    public synchronized long getStart() {
	return start;
    }
    
    public synchronized void setStart(long start) {
	this.start = start;
    }
    
    public synchronized long getFinishDate() {
	return (long) (duration + local - (server - start) / SERVER_RATIO);
    }
    
    @Override
    public String toString() {
	long t = Math.abs(isWorking() ? remaining : duration) / 1000;
	int h = (int) (t / 3600);
	int m = (int) ((t % 3600) / 60);
	int s = (int) (t % 60);
	if(h >= 24) {
	    int d = h / 24;
	    h = h % 24;
	    return String.format("%d:%02d:%02d:%02d", d, h, m, s);
	} else {
	    return String.format("%d:%02d:%02d", h, m, s);
	}
    }
    
    public void destroy() {
	Timer.remove(this);
	listener = null;
    }
    
}