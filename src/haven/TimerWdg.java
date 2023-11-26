package haven;

import me.ender.timer.Timer;

import java.awt.*;
import java.util.Date;

public class TimerWdg extends Widget {
    private static final Coord PAD = UI.scale(3, 3);
    private static final Color BG = new Color(8, 8, 8, 128);
    private Timer timer;
    public final Label time;
    public Label name;
    private final Button start, stop, delete;
    
    public TimerWdg(Timer timer) {
	super(Coord.z);
	
	this.timer = timer;
	timer.listener = new Timer.UpdateCallback() {
	    
	    @Override
	    public void update(Timer timer) {
		synchronized (time) {
		    time.settext(timer.toString());
		    updbtns();
		}
		
	    }
	};
	name = add(new Label(timer.name), PAD);
	time = add(new Label(timer.toString()), PAD.x, UI.scale(25));
	
	start = add(new Button(UI.scale(50), "start", false), UI.scale(90), PAD.y);
	stop = add(new Button(UI.scale(50), "stop", false), UI.scale(90), PAD.y);
	delete = add(new Button(UI.scale(50), "delete", false), UI.scale(90, 30));
	
	pack();
	sz = sz.add(PAD);
	updbtns();
    }

    @Override
    public Object tooltip(Coord c, Widget prev) {
	if(timer.isWorking()) {
	    if(tooltip == null) {
		tooltip = Text.render(new Date(timer.getFinishDate()).toString()).tex();
	    }
	    return tooltip;
	}
	tooltip = null;
	return null;
    }

    private void updbtns() {
	start.visible = !timer.isWorking();
	stop.visible = timer.isWorking();
    }
    
    @Override
    public void destroy() {
	unlink();
	Window wnd = getparent(Window.class);
	if(wnd != null) {
	    wnd.pack();
	}
	timer.listener = null;
	timer = null;
	super.destroy();
    }

    @Override
    public void draw(GOut g) {
	g.chcolor(BG);
	g.frect2(Coord.z, sz);
	g.chcolor();
	super.draw(g);
    }

    @Override
    public void wdgmsg(Widget sender, String msg, Object... args) {
	if(sender == start) {
	    timer.start();
	    updbtns();
	} else if(sender == stop) {
	    timer.stop();
	    updbtns();
	} else if(sender == delete) {
	    timer.destroy();
	    ui.destroy(this);
	} else {
	    super.wdgmsg(sender, msg, args);
	}
    }


}
