package me.ender.gob;

import haven.Window;
import haven.*;
import haven.rx.Reactor;
import me.ender.GobInfoOpts;
import me.ender.GobInfoOpts.InfoPart;
import me.ender.RichUText;
import me.ender.WindowDetector;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class GobTimerData {
    private static Gob interacted = null; //TODO: expand to allow multiple gob/window types, not just smelters
    private static final Map<Long, GobTimerData> MAP = new ConcurrentHashMap<>();
    private final long id;
    
    private Window wnd;
    
    
    static {
	Reactor.GOB_INTERACT.subscribe(target -> {
	    String name = target.resid();
	    if("gfx/terobjs/smelter".equals(name)) {
		interacted = target;
	    } else {
		interacted = null;
	    }
	});
	
	Reactor.WINDOW.subscribe(pair -> {
	    Gob g = interacted;
	    if(Window.ON_PACK.equals(pair.b) && g != null && WindowDetector.isWindowType(pair.a, WindowDetector.WND_SMELTER)) {
		g.info.timer.wnd = pair.a;
		interacted = null;
	    }
	});
    }
    
    //Timer related info
    private int remainingSeconds = 0, currentTimerValue = 0;
    private long lastUpdateTs = 0;
    private final RichUText<Integer> text = new RichUText<Integer>(RichText.stdf) {
	public String text(Integer v) {
	    return v == null ? null : String.format("$img[gfx/hud/gob/timer,c]%s", Utils.formatTimeShort(v));
	}
	
	@Override
	protected BufferedImage process(BufferedImage img) {
	    return Utils.outline2(img, Color.BLACK, true);
	}
	
	public Integer value() {
	    if(remainingSeconds <= 0 || lastUpdateTs <= 0) {return null;}
	    return remainingSeconds - (int) ((System.currentTimeMillis() - lastUpdateTs) / 1000L);
	}
    };
    
    private GobTimerData(long id) {
	this.id = id;
    }
    
    public static GobTimerData from(Gob gob) {
	GobTimerData data = MAP.getOrDefault(gob.id, null);
	if(data == null) {
	    data = new GobTimerData(gob.id);
	}
	return data;
    }
    
    public boolean update() {
	if(wnd != null) {
	    if(wnd.disposed()) {
		wnd = null;
	    } else {
		lastUpdateTs = System.currentTimeMillis();
		remainingSeconds = wnd.children(WItem.class).stream()
		    .map(WItem::remainingSeconds)
		    .filter(s -> s >= 0)
		    .min(Integer::compareTo)
		    .orElse(-1);
	    }
	}
	
	int prev = currentTimerValue;
	currentTimerValue = Optional.ofNullable(text.value()).orElse(0);
	if(prev > 0 && currentTimerValue <= 0) {
	    remainingSeconds = 0;
	    MAP.remove(id);
	} else if(currentTimerValue > 0) {
	    MAP.put(id, this);
	}
	
	return prev != currentTimerValue;
    }
    
    public BufferedImage img() {
	if(GobInfoOpts.disabled(InfoPart.TIMER)) {return null;}
	return Optional.ofNullable(text.get()).map(t -> t.back).orElse(null);
    }
    
}
