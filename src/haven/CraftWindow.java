package haven;

import haven.MenuGrid.Pagina;

import java.util.HashMap;
import java.util.Map;

import static haven.TabStrip.frame;

public class CraftWindow extends GameUI.Hidewnd {
    private final TabStrip<Pagina> tabStrip;
    private final Map<String, TabStrip.Button<Pagina>> tabs = new HashMap<>();
    private Widget makeWidget;

    public CraftWindow() {
	super(Coord.z, "Crafting");
	tabStrip = add(new TabStrip<Pagina>() {
	    @Override
	    public void selected(Button<Pagina> button) {
		if(button == null) {return;}
		Pagina lastCraft = ui.gui.menu.lastCraft;
		Pagina pagina = button.tag;
		if(pagina != lastCraft) {
		    pagina.button().use();
		}
		ui.gui.menu.lastCraft = null;
	    }
	});
	setfocusctl(true);
    }

    @Override
    public void wdgmsg(Widget sender, String msg, Object... args) {
	if((sender == this) && msg.equals("close")) {
	    if(makeWidget != null)
		makeWidget.wdgmsg("close");
	}
	super.wdgmsg(sender, msg, args);
    }

    @Override
    public <T extends Widget> T add(T child) {
	child = super.add(child);
	if(child instanceof Makewindow) {
	    Pagina lastCraft = ui.gui.menu.lastCraft;
	    if(lastCraft != null) {
		addTab(lastCraft);
	    }
	    makeWidget = child;
	    makeWidget.c = new Coord(5, tabStrip.sz.y + 5);
	    makeWidget.resize(Math.max(makeWidget.sz.x, tabStrip.sz.x), makeWidget.sz.y);
	}
	return child;
    }

    @Override
    public void cdestroy(Widget w) {
	if(makeWidget == w) {
	    makeWidget = null;
	    if(visible) {hide();}
	}
    }

    @Override
    public void cdraw(GOut g) {
	super.cdraw(g);
	frame.draw(g, new Coord(0, Math.max(0, tabStrip.sz.y - 1)), ca().sz().sub(0, tabStrip.sz.y));
    }

    @Override
    public void resize(Coord sz) {
	super.resize(sz.add(5, 5));
    }

    @Override
    public boolean globtype(char ch, java.awt.event.KeyEvent ev) {
	if(visible && ch == 9 && ev.isShiftDown()) {
	    int nextIndex = (tabStrip.getSelectedButtonIndex() + 1) % tabStrip.getButtonCount();
	    tabStrip.select(nextIndex);
	    return true;
	}
	return super.globtype(ch, ev);
    }

    @Override
    public void hide() {
	super.hide();
	if(makeWidget != null) {
	    makeWidget.wdgmsg("close");
	}
    }

    private void addTab(Pagina pagina) {
	String resName = pagina.res().name;
	if(tabs.containsKey(resName)) {
	    TabStrip.Button<Pagina> old = tabs.get(resName);
	    tabStrip.remove(old);
	}
	Tex icon = new TexI(PUtils.convolvedown(pagina.res.get().layer(Resource.imgc).img, new Coord(20, 20), CharWnd.iconfilter));
	String text = pagina.act().name;
	if(text.length() > 12) {
	    text = text.substring(0, 12 - 2) + "..";
	}
	TabStrip.Button<Pagina> added = tabStrip.insert(0, icon, text, pagina.act().name);
	added.tag = pagina;
	tabStrip.select(added);
	added.setActive(true);
	if(tabStrip.getButtonCount() > 4) {
	    removeTab(tabStrip.getButtonCount() - 1);
	}
	tabs.put(resName, added);
    }

    private void removeTab(int index) {
	TabStrip.Button<Pagina> removed = tabStrip.remove(index);
	tabs.values().remove(removed);
    }
}