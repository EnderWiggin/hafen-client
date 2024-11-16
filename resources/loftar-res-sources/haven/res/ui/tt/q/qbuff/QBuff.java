/* Preprocessed source code */
package haven.res.ui.tt.q.qbuff;

import haven.*;
import java.util.*;
import java.awt.image.BufferedImage;

@haven.FromResource(name = "ui/tt/q/qbuff", version = 7)
public class QBuff extends ItemInfo.Tip {
    public BufferedImage icon;
    public String name;
    public double q;

    public QBuff(Owner owner, BufferedImage icon, String name, double q) {
	super(owner);
	this.icon = icon;
	this.name = name;
	this.q = q;
    }

    public static interface Modifier {
	public void prepare(QList ql);
    }

    public abstract static class QList extends Tip {
	public final List<QBuff> ql = new ArrayList<>();
	public final List<Modifier> mods = new ArrayList<>();

	QList() {super(null);}

	void sort() {
	    Collections.sort(ql, new Comparator<QBuff>() {
		    public int compare(QBuff a, QBuff b) {
			return(a.name.compareTo(b.name));
		    }
		});
	    for(Modifier mod : mods)
		mod.prepare(this);
	}
    }

    public static class Table extends QList {
	public int order() {return(10);}

	public void layout(Layout l) {
	    sort();
	    CompImage tab = new CompImage();
	    CompImage.Image[] ic = new CompImage.Image[ql.size()];
	    CompImage.Image[] nm = new CompImage.Image[ql.size()];
	    CompImage.Image[] qv = new CompImage.Image[ql.size()];
	    int i = 0;
	    for(QBuff q : ql) {
		ic[i] = CompImage.mk(q.icon);
		nm[i] = CompImage.mk(Text.render(q.name + ":").img);
		qv[i] = CompImage.mk(Text.render((((int)q.q) == q.q)?String.format("%d", (int)q.q):String.format("%.1f", q.q)).img);
		i++;
	    }
	    tab.table(Coord.z, new CompImage.Image[][] {ic, nm, qv}, new int[] {5, 15}, 0, new int[] {0, 0, 1});
	    l.cmp.add(tab, new Coord(0, l.cmp.sz.y));
	}
    }

    public static final Layout.ID<Table> lid = new Layout.ID<Table>() {
	public Table make() {return(new Table());}
    };

    public static class Summary extends QList {
	public int order() {return(10);}

	public void layout(Layout l) {
	    sort();
	    CompImage buf = new CompImage();
	    for(int i = 0; i < ql.size(); i++) {
		QBuff q = ql.get(i);
		Text t = Text.render(String.format((i < ql.size() - 1)?"%,d, ":"%,d", Math.round(q.q)));
		buf.add(q.icon, new Coord(buf.sz.x, Math.max(0, (t.sz().y - q.icon.getHeight()) / 2)));
		buf.add(t.img, new Coord(buf.sz.x, 0));
	    }
	    l.cmp.add(buf, new Coord(l.cmp.sz.x + 10, 0));
	}
    }

    public static final Layout.ID<Summary> sid = new Layout.ID<Summary>() {
	public Summary make() {return(new Summary());}
    };

    public void prepare(Layout l) {
	l.intern(lid).ql.add(this);
    }

    public static class Short extends Tip {
	public final QBuff q;

	public Short(Owner owner, QBuff q) {
	    super(owner);
	    this.q = q;
	}

	public void prepare(Layout l) {
	    l.intern(sid).ql.add(q);
	}
    }

    public Tip shortvar() {
	return(new Short(owner, this));
    }
}
