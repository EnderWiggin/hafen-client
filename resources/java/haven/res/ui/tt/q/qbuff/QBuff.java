package haven.res.ui.tt.q.qbuff;

import haven.*;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class QBuff extends ItemInfo.Tip {
    public final BufferedImage icon;
    public final String name, original;
    public final double q;
    
    public QBuff(Owner owner, BufferedImage icon, String name, double q) {
	super(owner);
	this.icon = icon;
	this.name = L10N.tooltip(name);
	this.original = name;
	this.q = q;
    }
    
    public abstract static class QList extends Tip {
	final List<QBuff> ql = new ArrayList<>();
	
	QList() {super(null);}
	
	void sort() {
	    ql.sort(Comparator.comparing(a -> a.name));
	}
    }
    
    public static class Table extends QList {
	public int order() {return (10);}
	
	public void layout(Layout l) {
	    sort();
	    CompImage tab = new CompImage();
	    CompImage.Image[] ic = new CompImage.Image[ql.size()];
	    CompImage.Image[] nm = new CompImage.Image[ql.size()];
	    CompImage.Image[] qv = new CompImage.Image[ql.size()];
	    int i = 0;
	    for (QBuff q : ql) {
		ic[i] = CompImage.mk(q.icon);
		nm[i] = CompImage.mk(Text.render(q.name + ":").img);
		qv[i] = CompImage.mk(Text.render((((int) q.q) == q.q) ? String.format("%d", (int) q.q) : String.format("%.1f", q.q)).img);
		i++;
	    }
	    tab.table(Coord.z, new CompImage.Image[][]{ic, nm, qv}, new int[]{5, 15}, 0, new int[]{0, 0, 1});
	    l.cmp.add(tab, new Coord(0, l.cmp.sz.y));
	}
    }
    
    public static final Layout.ID<Table> lid = Table::new;
    
    public static class Summary extends QList {
	public int order() {return (10);}
	
	public void layout(Layout l) {
	    sort();
	    CompImage buf = new CompImage();
	    for (int i = 0; i < ql.size(); i++) {
		QBuff q = ql.get(i);
		Text t = Text.render(String.format((i < ql.size() - 1) ? "%,d, " : "%,d", Math.round(q.q)));
		buf.add(q.icon, new Coord(buf.sz.x, Math.max(0, (t.sz().y - q.icon.getHeight()) / 2)));
		buf.add(t.img, new Coord(buf.sz.x, 0));
	    }
	    l.cmp.add(buf, new Coord(l.cmp.sz.x + 10, 0));
	}
    }
    
    public static final Layout.ID<Summary> sid = Summary::new;
    
    public void prepare(Layout l) {
	l.intern(lid).ql.add(this);
    }
    
    public Tip shortvar() {
	return (new Tip(owner) {
	    public void prepare(Layout l) {
		l.intern(sid).ql.add(QBuff.this);
	    }
	});
    }
}
