package haven;

import me.ender.timer.Timer;
import newsh.TimerWebhook;

import java.util.Date;

public class TimerWdg extends Widget {

    static Tex bg = Resource.loadtex("gfx/hud/bosq");
    private Timer timer;
    public final Label time;
    public Label name;
    private Button start, stop, delete;
    
    public TimerWdg(Timer timer) {
	super(bg.sz());

	this.timer = timer;
	timer.listener = new Timer.UpdateCallback() {
	    
	    @Override
	    public void update(Timer timer) {
		synchronized(time) {
		    time.settext(timer.toString());
		    updbtns();
		}
		
	    }

	    @Override
	    public void complete(Timer timer) {
		String name = timer.getName();
		Window wnd = ui.root.add(new Window(Coord.z, name),new Coord(250, 100));
		String str;
		if (timer.remaining < -1500) {
		    str = String.format("%s elapsed since timer named \"%s\"  finished it's work", timer.toString(), name);
		} else {
		    str = String.format("Timer named \"%s\" just finished it's work", name);
		}
		wnd.add(new Label(str));
		wnd.justclose = true;
		wnd.pack();
	    }
	};
	name = add(new Label(timer.getName()), 5, 5);
	time = add(new Label(timer.toString()), 5, 25);
	
	start = add(new Button(50, "start"), 90, 2);
	stop = add(new Button(50, "stop"), 90, 2);
	delete = add(new Button(50, "delete"), 90, 21);
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
	g.image(bg, Coord.z);
	super.draw(g);
    }

    @Override
    public void wdgmsg(Widget sender, String msg, Object... args) {
	if(sender == start) {
	    timer.start();
	    updbtns();
	    TimerWebhook.updateTimerBot(timer.getName(), timer.getDuration(),"add");
	} else if(sender == stop) {
	    timer.stop();
	    updbtns();
	    TimerWebhook.updateTimerBot(timer.getName(), timer.getDuration(),"stop");
	} else if(sender == delete) {
	    timer.destroy();
	    TimerWebhook.updateTimerBot(timer.getName(), timer.getDuration(),"delete");
	    ui.destroy(this);
	} else {
	    super.wdgmsg(sender, msg, args);
	}
    }


}
