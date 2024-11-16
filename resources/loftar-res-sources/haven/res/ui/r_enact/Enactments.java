/* Preprocessed source code */
package haven.res.ui.r_enact;

import haven.*;
import haven.CharWnd.LoadingTextBox;
import java.util.*;
import java.awt.Color;

@haven.FromResource(name = "ui/r-enact", version = 64)
public class Enactments extends Widget {
    public static final Tex mpi = Resource.classres(Enactments.class).layer(Resource.imgc, 1).tex();
    public static final Tex ipi = Resource.classres(Enactments.class).layer(Resource.imgc, 0).tex();
    public static final Tex rpi = Resource.classres(Enactments.class).layer(Resource.imgc, 2).tex();
    public static final Tex lpi = Resource.classres(Enactments.class).layer(Resource.imgc, 3).tex();
    public static final Text.Foundry namef = CharWnd.attrf;
    public static final Text ell = namef.render("...");
    public static final int rwidth = UI.scale(300);
    public final LoadingTextBox info;
    public final EList list;
    public Cost used = new Cost(), avail = new Cost();
    public Costbox cost;

    public Enactments() {
	add(Frame.with(info = new LoadingTextBox(new Coord(CharWnd.attrw, UI.scale(290)), "", CharWnd.ifnd), true), 0, 0);
	info.bg = new Color(0, 0, 0, 128);
	int right = info.parent.sz.x + UI.scale(10);
	adda(Frame.with(list = new EList(Coord.of(rwidth, (namef.height() + UI.scale(2)) * 7)), false), info.parent.pos("ur").adds(10, 0), 0.0, 0.0);
	adda(Frame.with(cost = new Costbox(null), false), info.parent.pos("br").adds(10, 0), 0.0, 1.0);
	pack();
    }

    public class EList extends SListBox<Enactment, Widget> {
	public List<Enactment> acts = new ArrayList<>();
	private boolean loading = false;

	public EList(Coord sz) {
	    super(sz, namef.height() + UI.scale(2));
	}

	public void tick(double dt) {
	    if(loading) {
		loading = false;
		for(Enactment act : acts) {
		    try {
			act.sortkey = act.res.get().layer(Resource.tooltip).t;
		    } catch(Loading l) {
			act.sortkey = "\uffff";
			loading = true;
		    }
		}
		Collections.sort(acts, Comparator.comparing(act -> act.sortkey));
	    }
	    super.tick(dt);
	}

	protected List<Enactment> items() {return(acts);}
	protected Widget makeitem(Enactment act, int idx, Coord sz) {return(new Item(sz, act));}

	public class Item extends ItemWidget<Enactment> {
	    public Item(Coord sz, Enactment act) {
		super(EList.this, sz, act);
		String ls = (act.mlvl <= 0) ? String.format("%d", act.lvl) : String.format("%d/%d", act.lvl, act.mlvl);
		Widget prev = adda(new Label(ls, namef), sz.x - UI.scale(5), sz.y / 2, 1.0, 0.5);
		adda(IconText.of(Coord.of(prev.c.x - UI.scale(5), sz.y), act.res), 0, sz.y / 2, 0.0, 0.5);
	    }
	}

	public void pop(Collection<Enactment> nacts) {
	    List<Enactment> acts = new ArrayList<>(nacts);
	    Enactment psel = sel;
	    sel = null;
	    this.acts = acts;
	    if(psel != null) {
		for(Enactment act : acts) {
		    if(act.res == psel.res) {
			sel = act;
			break;
		    }
		}
	    }
	    updcostbox();
	    loading = true;
	    reset();
	}

	public void change(Enactment act) {
	    Enactment p = sel;
	    super.change(act);
	    if(act != null)
		info.settext(() -> act.rendertext());
	    else if(p != null)
		info.settext("");
	    updcostbox();
	}
    }

    private void updcostbox() {
	Costbox nc = cost.parent.add(new Costbox(list.sel), cost.c);
	cost.reqdestroy();
	cost = nc;
    }

    private static String costfmt(double p) {
	if((int)p == p)
	    return(String.format("%d", (int)p));
	return(String.format("%.1f", p));
    }

    private static int ox(Widget w) {
	return(w.c.x + w.sz.x);
    }

    public class Costbox extends Widget {
	public final Enactment act;

	public Costbox(Enactment act) {
	    super(new Coord(rwidth, 0));
	    int y = UI.scale(5), m = UI.scale(5), mph = mpi.sz().y;
	    {
		int x = sz.x - m, cy = y + (mph / 2);
		adda(new Label("Available:"), UI.scale(5), cy, 0.0, 0.5);
		x = adda(new Img(lpi), x, cy, 1.0, 0.5).c.x - m;
		x = adda(new Label(costfmt(avail.lp - used.lp) + "/" + costfmt(avail.lp)), x, cy, 1.0, 0.5).c.x - m;
		x = adda(new Img(rpi), x, cy, 1.0, 0.5).c.x - m;
		x = adda(new Label(costfmt(avail.rp - used.rp) + "/" + costfmt(avail.rp)), x, cy, 1.0, 0.5).c.x - m;
		x = adda(new Img(ipi), x, cy, 1.0, 0.5).c.x - m;
		x = adda(new Label(costfmt(avail.ip - used.ip) + "/" + costfmt(avail.ip)), x, cy, 1.0, 0.5).c.x - m;
		x = adda(new Img(mpi), x, cy, 1.0, 0.5).c.x - m;
		x = adda(new Label(costfmt(avail.mp - used.mp) + "/" + costfmt(avail.mp)), x, cy, 1.0, 0.5).c.x - m;
	    }
	    y += mph + UI.scale(15);
	    this.act = act;
	    if((act != null) && (act.cost != null)) {
		int cy = y + (mph / 2);
		adda(new Label("Cost:"), 5, cy, 0.0, 0.5);
		int x = sz.x - m - UI.scale(75) - m;
		if(act.cost.lp != 0) {
		    x = adda(new Img(lpi), x, cy, 1.0, 0.5).c.x - m;
		    x = adda(new Label(costfmt(act.cost.lp)), x, cy, 1.0, 0.5).c.x - m;
		}
		if(act.cost.rp != 0) {
		    x = adda(new Img(rpi), x, cy, 1.0, 0.5).c.x - m;
		    x = adda(new Label(costfmt(act.cost.rp)), x, cy, 1.0, 0.5).c.x - m;
		}
		if(act.cost.ip != 0) {
		    x = adda(new Img(ipi), x, cy, 1.0, 0.5).c.x - m;
		    x = adda(new Label(costfmt(act.cost.ip)), x, cy, 1.0, 0.5).c.x - m;
		}
		if(act.cost.mp != 0) {
		    x = adda(new Img(mpi), x, cy, 1.0, 0.5).c.x - m;
		    x = adda(new Label(costfmt(act.cost.mp)), x, cy, 1.0, 0.5).c.x - m;
		}
	    }
	    y += mph + UI.scale(10);
	    if((act != null) && (act.icost != null)) {
		int cy = y + (mph / 2);
		adda(new Label((act.lvl > 0) ? "Increase:" : "Enact:"), m, cy, 0.0, 0.5);
		int x = sz.x - m;
		x = adda(new Button(UI.scale(75), "Enact", false) {
			public void click() {
			    Enactments.this.wdgmsg("en", act.id);
			}
		    }, x, cy, 1, 0.5).c.x - m;
		if(act.icost.lp != 0) {
		    x = adda(new Img(lpi), x, cy, 1.0, 0.5).c.x - m;
		    x = adda(new Label(costfmt(act.icost.lp)), x, cy, 1.0, 0.5).c.x - m;
		}
		if(act.icost.rp != 0) {
		    x = adda(new Img(rpi), x, cy, 1.0, 0.5).c.x - m;
		    x = adda(new Label(costfmt(act.icost.rp)), x, cy, 1.0, 0.5).c.x - m;
		}
		if(act.icost.ip != 0) {
		    x = adda(new Img(ipi), x, cy, 1.0, 0.5).c.x - m;
		    x = adda(new Label(costfmt(act.icost.ip)), x, cy, 1.0, 0.5).c.x - m;
		}
		if(act.icost.mp != 0) {
		    x = adda(new Img(mpi), x, cy, 1.0, 0.5).c.x - m;
		    x = adda(new Label(costfmt(act.icost.mp)), x, cy, 1.0, 0.5).c.x - m;
		}
	    }
	    y += mph + UI.scale(10);
	    if((act != null) && (act.dcost != null)) {
		int cy = y + (mph / 2);
		adda(new Label("Decrease:"), m, cy, 0.0, 0.5);
		int x = sz.x - m;
		x = adda(new Button(UI.scale(75), "Rescind", false) {
			public void click() {
			    Enactments.this.wdgmsg("dis", act.id);
			}
		    }, x, cy, 1, 0.5).c.x - m;
		if(act.dcost.lp != 0) {
		    x = adda(new Img(lpi), x, cy, 1.0, 0.5).c.x - m;
		    x = adda(new Label(costfmt(act.dcost.lp)), x, cy, 1.0, 0.5).c.x - m;
		}
		if(act.dcost.rp != 0) {
		    x = adda(new Img(rpi), x, cy, 1.0, 0.5).c.x - m;
		    x = adda(new Label(costfmt(act.dcost.rp)), x, cy, 1.0, 0.5).c.x - m;
		}
		if(act.dcost.ip != 0) {
		    x = adda(new Img(ipi), x, cy, 1.0, 0.5).c.x - m;
		    x = adda(new Label(costfmt(act.dcost.ip)), x, cy, 1.0, 0.5).c.x - m;
		}
		if(act.dcost.mp != 0) {
		    x = adda(new Img(mpi), x, cy, 1.0, 0.5).c.x - m;
		    x = adda(new Label(costfmt(act.dcost.mp)), x, cy, 1.0, 0.5).c.x - m;
		}
	    }
	    y += mph;
	    resize(new Coord(sz.x, y + m));
	}

	public void draw(GOut g) {
	    super.draw(g);
	}
    }

    public static Widget mkwidget(UI ui, Object[] args) {
	return(new Enactments());
    }

    private static Enactment get(Collection<Enactment> from, int id) {
	for(Enactment act : from) {
	    if(act.id == id)
		return(act);
	}
	return(null);
    }

    public void uimsg(String nm, Object... args) {
	if(nm == "pop") {
	    Collection<Enactment> buf = ((Integer)args[0] != 0) ? new ArrayList<Enactment>() : list.acts;
	    int a = 1;
	    while(a < args.length) {
		int resid = (Integer)args[a++];
		Enactment act = get(buf, resid);
		if(act == null) {
		    act = new Enactment(resid, ui.sess.getres(resid));
		    buf.add(act);
		}
		act.lvl = (Integer)args[a++];
		act.mlvl = (Integer)args[a++];
		if(((Integer)args[a++]) != 0)
		    act.cost = new Cost(((Number)args[a++]).doubleValue(), ((Number)args[a++]).doubleValue(), ((Number)args[a++]).doubleValue(), ((Number)args[a++]).doubleValue());
		else
		    act.cost = null;
		if(((Integer)args[a++]) != 0)
		    act.dcost = new Cost(((Number)args[a++]).doubleValue(), ((Number)args[a++]).doubleValue(), ((Number)args[a++]).doubleValue(), ((Number)args[a++]).doubleValue());
		else
		    act.dcost = null;
		if(((Integer)args[a++]) != 0)
		    act.icost = new Cost(((Number)args[a++]).doubleValue(), ((Number)args[a++]).doubleValue(), ((Number)args[a++]).doubleValue(), ((Number)args[a++]).doubleValue());
		else
		    act.icost = null;
	    }
	    list.pop(buf);
	} else if(nm == "cur") {
	    avail = new Cost(((Number)args[0]).doubleValue(), ((Number)args[2]).doubleValue(), ((Number)args[4]).doubleValue(), ((Number)args[6]).doubleValue());
	    used  = new Cost(((Number)args[1]).doubleValue(), ((Number)args[3]).doubleValue(), ((Number)args[5]).doubleValue(), ((Number)args[7]).doubleValue());
	    updcostbox();
	} else {
	    super.uimsg(nm, args);
	}
    }
}
