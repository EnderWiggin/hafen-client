package haven;

import me.ender.timer.Timer;

public class TimerPanel extends WindowX {
    private static final Coord PAD = UI.scale(5, 5);
    
    private final Button btnnew;
    
    public TimerPanel() {
	super(Coord.z, "Timers");
	justclose = true;
	btnnew = add(new Button(100, "Add timer"));
	
	for (Timer timer : Timer.timers()) {
	    add(new TimerWdg(timer));
	}
	pack();
    }
    
    public static void complete(Timer timer, Widget parent) {
	String name = timer.name;
	Window wnd = parent.add(new WindowX(Coord.z, "Timer complete"), UI.scale(250, 100));
	String str;
	if(timer.remaining < -1500) {
	    str = String.format("%s elapsed since timer \"%s\"  finished", timer.toString(), name);
	} else {
	    str = String.format("Timer \"%s\" just finished", name);
	}
	wnd.add(new Label(str));
	wnd.justclose = true;
	wnd.pack();
    }
    
    @Override
    public void pack() {
	int n = Timer.count(), i = 0, h = 0;
	n = (int) Math.ceil(Math.sqrt((double) n / 3));
	for (Widget wdg = child; wdg != null; wdg = wdg.next) {
	    if(!(wdg instanceof TimerWdg))
		continue;
	    wdg.c = new Coord((i % n) * (wdg.sz.x + PAD.x), (i / n) * (wdg.sz.y + PAD.y));
	    h = wdg.c.y + wdg.sz.y + PAD.y;
	    i++;
	}
	
	btnnew.c = new Coord(0, h);
	super.pack();
    }

    @Override
    public void wdgmsg(Widget sender, String msg, Object... args) {
	if(sender == btnnew) {
	    ui.gui.add(new TimerAddWdg(this), c);
	} else {
	    super.wdgmsg(sender, msg, args);
	}
    }
    
    @Override
    public void close() {
	ui.gui.timers = null;
	super.close();
    }
    
    static class TimerAddWdg extends WindowX {
	
	private final TextEntry name, hours, minutes, seconds;
	private final Button btnadd;
	private TimerPanel panel;
	
	public TimerAddWdg(TimerPanel panel) {
	    super(Coord.z, "Add timer");
	    justclose = true;
	    this.panel = panel;
	    name = add(new TextEntry(UI.scale(150), "timer"));
	    add(new Label("hours"), UI.scale(0, 25));
	    add(new Label("min"), UI.scale(50, 25));
	    add(new Label("sec"), UI.scale(100, 25));
	    int textw = UI.scale(45);
	    hours = add(new TextEntry(textw, "00"), UI.scale(0, 40));
	    minutes = add(new TextEntry(textw, "00"), UI.scale(50, 40));
	    seconds = add(new TextEntry(textw, "00"), UI.scale(100, 40));
	    btnadd = add(new Button(UI.scale(100), "Add"), UI.scale(0, 60));
	    
	    hours.onFocused(TimerAddWdg::clear);
	    minutes.onFocused(TimerAddWdg::clear);
	    seconds.onFocused(TimerAddWdg::clear);
	    
	    pack();
	}
	
	private static void clear(Widget widget, boolean focused) {
	    TextEntry textEntry = (TextEntry) widget;
	    if(focused) {
		textEntry.settext("");
	    } else {
		try {
		    textEntry.settext(String.format("%02d", Integer.parseInt(textEntry.text())));
		} catch (NumberFormatException ignored) {
		    textEntry.settext("00");
		}
	    }
	}
	
	@Override
	public void wdgmsg(Widget sender, String msg, Object... args) {
	    if(sender == btnadd) {
		
		long time = 0;
		try { time += Integer.parseInt(seconds.text());} catch (NumberFormatException ignored) {}
		try {time += Integer.parseInt(minutes.text()) * 60;} catch (NumberFormatException ignored) {}
		try {time += Integer.parseInt(hours.text()) * 3600;} catch (NumberFormatException ignored) {}
		panel.add(new TimerWdg(Timer.add(name.text(), 1000 * time)));
		panel.pack();
		ui.destroy(this);
	    } else {
		super.wdgmsg(sender, msg, args);
	    }
	}

	@Override
	public void destroy() {
	    panel = null;
	    super.destroy();
	}
	
    }
}
