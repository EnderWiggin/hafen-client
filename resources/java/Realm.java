/* $use: ui/polity */

import haven.*;
import java.util.*;
import haven.res.ui.polity.*;
import static haven.BuddyWnd.width;

/* >wdg: Realm */
public class Realm extends Polity {
    public static final Map<String, Resource.Image> authimg = Utils.<String, Resource.Image>map().
	put("c", Loading.waitfor(Resource.classres(Realm.class).pool.load("gfx/terobjs/mm/bordercairn", 1)).layer(Resource.imgc)).
	put("s", Loading.waitfor(Resource.classres(Realm.class).pool.load("gfx/terobjs/mm/seamark", 1)).layer(Resource.imgc)).
	put("f", Loading.waitfor(Resource.classres(Realm.class).pool.load("gfx/terobjs/mm/fealtystone", 1)).layer(Resource.imgc)).
	map();
    final BuddyWnd.GroupSelector gsel;
    public final Map<String, Integer> authn = new HashMap<>();
    public Window actwnd;
    private final int my;

    public Realm(String name) {
	super("Realm", name);
	Composer lay = new Composer(this).vmrgn(UI.scale(5));
	lay.add(new Img(CharWnd.catf.i10n_label("Realm").tex()));
	lay.add(new Label.Untranslated(name, nmf));
	lay.add(new AuthMeter(new Coord(width, UI.scale(20))));
	lay.addar(width, new Authobj("c"), new Authobj("s"), new Authobj("f"));
	lay.add(new Button(width - UI.scale(20), "Realm Blessings") {
		public void click() {
		    if((actwnd != null) && actwnd.show(!actwnd.visible)) {
			actwnd.raise();
		    }
		}
	    }, 10);
	lay.vmrgn(UI.scale(2)).add(new Label("Groups:"));
	gsel = lay.vmrgn(UI.scale(5)).add(new BuddyWnd.GroupSelector(-1) {
		public void tick(double dt) {
		    if(mw instanceof GroupWidget)
			update(((GroupWidget)mw).id);
		    else
			update(-1);
		}

		public void select(int group) {
		    Realm.this.wdgmsg("gsel", group);
		}
	    });
	lay.vmrgn(UI.scale(2)).add(new Label("Members:"));
	lay.vmrgn(UI.scale(5)).add(Frame.with(new MemberList(width, 7), true));
	pack();
	this.my = lay.y();
    }

    public class Authobj extends Widget {
	public final String t;
	public final Resource.Image img;
	private Text rend;
	private int cn;

	public Authobj(String t) {
	    super(authimg.get(t).sz.add(UI.scale(25, 0)));
	    this.t = t;
	    this.img = authimg.get(t);
	}

	private int aseq = -1;
	private Tex rauth = null;
	public void draw(GOut g) {
	    int n;
	    synchronized(authn) {
		Integer apa = Realm.this.authn.get(t);
		n = (apa == null) ? 0 : apa;
	    }
	    g.image(img, Coord.z);
	    if((rend == null) || (n != cn))
		rend = Text.render(Integer.toString(n));
	    g.aimage(rend.tex(), new Coord(img.sz.x + UI.scale(5), img.sz.y / 2), 0, 0.5);
	}

	public Object tooltip(Coord c, Widget prev) {
	    return(this.img.getres().layer(Resource.tooltip).t);
	}
    }

    public static Widget mkwidget(UI ui, Object[] args) {
	String name = (String)args[0];
	return(new Realm(name));
    }

    public void addchild(Widget child, Object... args) {
	if(args[0] instanceof String) {
	    String p = (String)args[0];
	    if(p.equals("m")) {
		mw = child;
		add(child, 0, my);
		pack();
		return;
	    } else if(p.equals("act")) {
		actwnd = new Hidewnd(Coord.z, "Realm Blessings");
		actwnd.add(child);
		actwnd.pack();
		actwnd.hide();
		getparent(GameUI.class).add(actwnd);
		return;
	    }
	}
	super.addchild(child, args);
    }

    public void uimsg(String msg, Object... args) {
	if(msg == "authn") {
	    String tp = (String)args[0];
	    int n = (Integer)args[1];
	    synchronized(authn) {
		authn.put(tp, n);
	    }
	} else {
	    super.uimsg(msg, args);
	}
    }
}

