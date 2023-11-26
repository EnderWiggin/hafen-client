package haven;

import rx.functions.Action1;

import java.awt.image.BufferedImage;

public class ToggleButton extends IButton {
    private final BufferedImage upf, downf, hoverf, upt, downt, hovert;
    private boolean state = false;
    private Action1<Boolean> action;
    
    public ToggleButton(String basef, String upf, String downf, String hoverf,
			String baset, String upt, String downt, String hovert) {
	super(basef, upf, downf, hoverf);
	this.upf = up;
	this.downf = down;
	this.hoverf = hover;
	
	this.upt = Resource.loadsimg(baset + upt);
	this.downt = Resource.loadsimg(baset + downt);
	this.hovert = Resource.loadsimg(baset + (hovert == null ? upt : hovert));
    }
    
    public void action(Action1<Boolean> action) {
	this.action = action;
    }
    
    @Override
    public void click() {
	state(!state);
	if(action != null) {
	    action.call(state);
	} else {
	    super.click();
	}
    }
    
    public void state(boolean state) {
	if(this.state != state) {
	    this.state = state;
	    up = state ? upt : upf;
	    down = state ? downt : downf;
	    hover = state ? hovert : hoverf;
	    redraw();
	}
    }
    
    public boolean state() {return state;}
}
