package haven;

import java.awt.event.KeyEvent;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

public abstract class FilteredListBox<T> extends Listbox<T> implements ReadLine.Owner {
    
    private boolean needfilter = false;
    protected final ReadLine filter = ReadLine.make(this, "");
    protected List<T> items = new LinkedList<>();
    protected List<T> filtered = new LinkedList<>();
    protected boolean showFilterText = true;
    
    public FilteredListBox(int w, int h, int itemh) {
	super(w, h, itemh);
	setcanfocus(true);
    }
    
    public void setItems(List<T> items) {
	this.items = items;
	filter();
    }
    
    @Override
    protected T listitem(int i) {
	return filtered.get(i);
    }
    
    @Override
    protected int listitems() {
	return filtered.size();
    }
    
    @Override
    public boolean keydown(KeyDownEvent ev) {
	if(ignoredKey(ev.awt)) {
	    return false;
	}
	if(ev.code == KeyEvent.VK_ESCAPE) {
	    if(!filter.line().isEmpty()) {
		filter.setline("");
		needfilter();
		return true;
	    }
	}
	
	String before = filter.line();
	if(filter.key(ev.awt) && !before.equals(filter.line())) {
	    needfilter();
	    return true;
	}
	return false;
    }
    
    private static boolean ignoredKey(KeyEvent ev) {
	int code = ev.getKeyCode();
	int mods = ev.getModifiersEx();
	//any modifier except SHIFT pressed alone is ignored, TAB is also ignored
	return (mods != 0 && mods != KeyEvent.SHIFT_DOWN_MASK)
	    || code == KeyEvent.VK_CONTROL
	    || code == KeyEvent.VK_ALT
	    || code == KeyEvent.VK_META
	    || code == KeyEvent.VK_TAB;
    }
    
    public void needfilter() {
	needfilter = true;
	sb.val = 0;
    }

    public void filter(String filter) {
	this.filter.setline(filter);
	needfilter();
    }
    
    @Override
    public void tick(double dt) {
	super.tick(dt);
	if(needfilter) {
	    filter();
	}
    }
    
    @Override
    public void draw(GOut g) {
	super.draw(g);
	if(showFilterText && !filter.line().isEmpty()) {
	    g.atext(filter.line(), g.sz(), 1, 1);
	}
    }
    
    protected void filter() {
	filtered = items.stream().filter(t -> match(t, filter.line())).collect(Collectors.toList());
	needfilter = false;
    }
    
    protected abstract boolean match(T item, String filter);
}
