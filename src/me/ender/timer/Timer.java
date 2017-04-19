package me.ender.timer;

public class Timer {
    public void setDuration(long duration) {
	this.duration = duration;
    }

    public interface UpdateCallback {
	void update(Timer timer);

	void complete(Timer timer);
    }

    public static final float SERVER_RATIO = 3.29f;
    
    public static long server;
    public static long local;
    
    private long start;
    private long duration;
    private String name;
    transient public long remaining;
    transient public UpdateCallback listener;
    transient public TimerController controller;


    public Timer() {
    }

    public boolean isWorking() {
	return start != 0;
    }
    
    public void stop() {
	start = 0;
	if(listener != null) {
	    listener.update(this);
	}
	controller.save();
    }
    
    public void start() {
	start = (long) (server + SERVER_RATIO * (System.currentTimeMillis() - local));
	controller.save();
    }
    
    public synchronized boolean update() {
	long now = System.currentTimeMillis();
	remaining = (long) (duration - now + local - (server - start) / SERVER_RATIO);
	if(remaining <= 0) {
	    if(listener != null) {
		listener.complete(this);
	    }
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

    public synchronized String getName() {
	return name;
    }

    public synchronized void setName(String name) {
	this.name = name;
    }

    public synchronized long getFinishDate() {
	return (long) (duration + local - (server - start) / SERVER_RATIO);
    }

    public synchronized long getDuration() {
	return duration;
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
	controller.remove(this);
	controller.save();
	listener = null;
    }
    
}