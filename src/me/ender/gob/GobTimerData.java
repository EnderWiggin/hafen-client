package me.ender.gob;

import haven.Window;
import haven.*;
import haven.rx.Reactor;
import me.ender.*;
import me.ender.GobInfoOpts.InfoPart;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class GobTimerData {
    private static Gob interacted = null;
    private static String awaitWnd = null;
    private static final Map<Long, GobTimerData> MAP = new ConcurrentHashMap<>();
    private final Gob gob;

    private Window wnd;
    
    
    static {
	Reactor.GOB_INTERACT.subscribe(target -> {
	    String name = target.resid();
	    if(ResName.ORE_SMELTER.equals(name)) {
		interacted = target;
		awaitWnd = WindowDetector.WND_SMELTER;
	    } else if(ResName.FINERY_FORGE.equals(name)) {
		interacted = target;
		awaitWnd = WindowDetector.WND_FINERY_FORGE;
	    } else if(ResName.STACK_FURNACE.equals(name)) {
		interacted = target;
		awaitWnd = WindowDetector.WND_STACK_FURNACE;
	    } else {
		interacted = null;
	    }
	});
	
	Reactor.WINDOW.subscribe(pair -> {
	    Gob g = interacted;
	    if(Window.ON_PACK.equals(pair.b) && g != null && WindowDetector.isWindowType(pair.a, awaitWnd)) {
		g.info.timer.wnd = pair.a;
		if(pair.a instanceof WindowX) {
		    ((WindowX) pair.a).gob = g;
		}
		interacted = null;
	    }
	});
    }
    
    //Timer related info
    private int remainingSeconds = 0, currentTimerValue = 0;
    private long lastUpdateTs = 0;
    private final RichUText<Integer> text = new RichUText<Integer>(RichText.stdf) {
	public String text(Integer v) {
	    return v == null ? null : String.format("$img[gfx/hud/gob/timer,c]%s", ClientUtils.formatTimeShort(v));
	}
	
	@Override
	protected BufferedImage process(BufferedImage img) {
	    return Utils.outline2(img, Color.BLACK, true);
	}
	
	public Integer value() {
	    if(remainingSeconds <= 0 || lastUpdateTs <= 0) {return null;}
	    float multiplier = gob.is(GobTag.COLD) ? 2f : 1f;
	    return (int) (multiplier * remainingSeconds - ((System.currentTimeMillis() - lastUpdateTs) / 1000f));
	}
    };
    
    private GobTimerData(Gob gob) {
	this.gob = gob;
    }
    
    public static GobTimerData from(Gob gob) {
	GobTimerData data = MAP.getOrDefault(gob.id, null);
	if(data == null) {
	    data = new GobTimerData(gob);
	}
	return data;
    }
    
    public boolean update() {
	if(wnd != null) {
	    if(wnd.disposed() || wnd.closed()) {
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
	    MAP.remove(gob.id);
	} else if(currentTimerValue > 0) {
	    MAP.put(gob.id, this);
	}
	
	return prev != currentTimerValue;
    }
    
    public BufferedImage img() {
	if(GobInfoOpts.disabled(InfoPart.TIMER) || !gob.is(GobTag.LIT)) {return null;}
	return Optional.ofNullable(text.get()).map(t -> t.back).orElse(null);
    }
    
}
