package haven;

import java.awt.*;
import java.awt.event.KeyEvent;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import haven.MenuGrid.Pagina;

public class ActWindow extends GameUI.Hidewnd {
    private static final int WIDTH = 200;

    private final ActList filtered;
    private final TextEntry filter;
    private final List<Pagina> all = new LinkedList<>();
    private final Pattern category;
    private int pagseq = 0;
    private boolean needfilter = false;

    public ActWindow(String cap, String category) {
	super(Coord.z, cap);
	this.category = Pattern.compile(category);
	setfocusctl(true);
	filter = add(new TextEntry(WIDTH, "") {
	    @Override
	    public void activate(String text) {
		act(filtered.sel.pagina);
	    }

	    @Override
	    protected void changed() {
		super.changed();
		needfilter();
	    }

	    @Override
	    public boolean keydown(KeyDownEvent e) {
		if(e.code == KeyEvent.VK_UP) {
		    filtered.change(Math.max(filtered.selindex - 1, 0));
		    filtered.showsel();
		    return true;
		} else if(e.code == KeyEvent.VK_DOWN) {
		    filtered.change((Math.min(filtered.selindex + 1, filtered.listitems() - 1)));
		    filtered.showsel();
		    return true;
		} else {
		    return super.keydown(e);
		}
	    }
	});
	setfocus(filter);
	filtered = add(new ActList(WIDTH, 10) {
	    @Override
	    protected void itemactivate(ActItem item) {
		act(item.pagina);
	    }
	}, 0, filter.sz.y + 5);
	filtered.bgcolor = new Color(0, 0, 0, 128);
	pack();
    }

    private void act(Pagina pagina) {
	ui.gui.menu.use(pagina, false);
	ActWindow.this.hide();
    }

    @Override
    public void show() {
	super.show();
	pagseq = 0;
	filter.settext("");
	filtered.change(0);
	filtered.showsel();
	raise();
	parent.setfocus(this);
    }

    @Override
    public void lostfocus() {
	super.lostfocus();
	hide();
    }
    
    @Override
    public boolean keydown(KeyDownEvent ev) {
	return !ignoredKey(ev.awt) && super.keydown(ev);
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
    
    private void needfilter() {
	needfilter = true;
    }

    private void filter() {
	needfilter = false;
	String filter = this.filter.text().toLowerCase();
	synchronized (all) {
	    filtered.clear();
	    ItemFilter itemFilter = ItemFilter.create(filter);
	    for (Pagina p : all) {
		try {
		    Resource res = p.res.get();
		    String name = res.layer(Resource.action).name.toLowerCase();
		    if(name.contains(filter) || itemFilter.matches(p)) {
			filtered.add(p);
		    }
		} catch (Loading e) {
		    needfilter = true;
		}
	    }
	}
	filtered.sort(new ItemComparator(filter));
	if(filtered.listitems() > 0) {
	    filtered.change(filtered.sel);
	    if(filtered.selindex == -1) {
		filtered.change(0);
	    }
	    filtered.sb.val = 0;
	    filtered.showsel();
	}
    }

    @Override
    public void tick(double dt) {
	super.tick(dt);
    
	MenuGrid menu = ui.gui.menu;
	synchronized (menu.paginae) {
	    if(pagseq != menu.pagseq) {
		synchronized (all) {
		    all.clear();
		    all.addAll(
			menu.paginae.stream()
			    .filter(p -> category.matcher(Pagina.resname(p)).matches())
			    .collect(Collectors.toList())
		    );

		    pagseq = menu.pagseq;
		    needfilter();
		}
	    }
	}
	if(needfilter) {
	    filter();
	}
    }

    public static class ItemComparator implements Comparator<ActList.ActItem> {
	private final String filter;
    
	public ItemComparator(String filter){
	    this.filter = filter;
	}
	
	@Override
	public int compare(ActList.ActItem a, ActList.ActItem b) {
	    if(!filter.isEmpty()) {
		boolean ai = a.name.text.toLowerCase().startsWith(filter);
		boolean bi = b.name.text.toLowerCase().startsWith(filter);
		if(ai && !bi) {return -1;}
		if(!ai && bi) {return 1;}
	    }
	    return a.name.text.compareTo(b.name.text);
	}
    }
}
