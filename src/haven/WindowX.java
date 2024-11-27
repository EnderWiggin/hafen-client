package haven;

public class WindowX extends Window {
    public Gob gob;
    
    public WindowX(Coord sz, String cap, boolean lg, Deco deco, boolean defdeco) {
	super(sz, cap, lg, deco, defdeco);
    }
    
    public WindowX(Coord sz, String cap, boolean lg, Deco deco) {
	super(sz, cap, lg, deco, false);
    }
    
    @Override
    protected void initCfg() {
	if(CFG.UI_DISABLE_CONTAINER_POS.get() && getchild(ExtInventory.class) != null) {
	    skipInitPos = true;
	}
	super.initCfg();
    }
    
    public WindowX(Coord sz, String cap, boolean lg) {
	super(sz, cap, lg, null, true);
    }
    
    public WindowX(Coord sz, String cap) {
	super(sz, cap, false);
    }
    
    protected Deco makedeco() {
	return(new DecoX(this.large));
    }
}
