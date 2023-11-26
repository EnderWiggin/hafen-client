package haven;

import java.awt.event.KeyEvent;

public class FilterWnd extends GameUI.Hidewnd {
    private final TextEntry input;
    
    FilterWnd() {
	super(new Coord(120, 200), "Filter");
	//cap = null;
	
	input = add(new TextEntry(200, "") {
	    @Override
	    protected void changed() {
		checkInput();
	    }
	});
    
	addtwdg(new IButton("gfx/hud/btn-help", "","-d","-h", () -> ItemFilter.showHelp(ui, ItemFilter.FILTER_HELP)));
	
	pack();
	hide();
    }
    
    @Override
    public boolean keydown(KeyEvent ev) {
	if(ev.getKeyCode() == KeyEvent.VK_ESCAPE) {
	    if(input.text().length() > 0) {
		input.settext("");
		return true;
	    }
	}
	return !ignoredKey(ev) && super.keydown(ev);
    }
    
    private static boolean ignoredKey(KeyEvent ev){
	int code = ev.getKeyCode();
	int mods = ev.getModifiersEx();
	//any modifier except SHIFT pressed alone is ignored, TAB is also ignored
	return (mods != 0 && mods != KeyEvent.SHIFT_DOWN_MASK)
		|| code == KeyEvent.VK_CONTROL
		|| code == KeyEvent.VK_ALT
		|| code == KeyEvent.VK_META
		|| code == KeyEvent.VK_TAB;
    }
    
    private void setFilter(String text) {
	ItemFilter filter = null;
	if (text != null) {
	    filter = ItemFilter.create(text);
	}
	GItem.setFilter(filter);
    }
    
    private void checkInput() {
	String text = input.text();
	if (text.length() >= 2) {
	    setFilter(text);
	} else {
	    setFilter(null);
	}
    }
    
    @Override
    public void hide() {
	super.hide();
	setFilter(null);
    }
    
    @Override
    public void show() {
	super.show();
	setfocus(input);
	checkInput();
	raise();
    }
}
