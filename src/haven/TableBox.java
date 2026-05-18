/*
 *  This file is part of the Haven & Hearth game client.
 *  Copyright (C) 2009 Fredrik Tolf <fredrik@dolda2000.com>, and
 *                     Björn Johannessen <johannessen.bjorn@gmail.com>
 *
 *  Redistribution and/or modification of this file is subject to the
 *  terms of the GNU Lesser General Public License, version 3, as
 *  published by the Free Software Foundation.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  Other parts of this source tree adhere to other copying
 *  rights. Please see the file `COPYING' in the root directory of the
 *  source tree for details.
 *
 *  A copy the GNU Lesser General Public License is distributed along
 *  with the source tree of which this file is a part in the file
 *  `doc/LPGL-3'. If it is missing for any reason, please see the Free
 *  Software Foundation's website at <http://www.fsf.org/>, or write
 *  to the Free Software Foundation, Inc., 59 Temple Place, Suite 330,
 *  Boston, MA 02111-1307 USA
 */

package haven;

import java.util.*;
import java.util.function.*;

public abstract class TableBox<I> extends Widget {
    public final List<Column<I>> cols;
    public final MainList main;

    public TableBox(Coord sz) {
	super(sz);
	List<ColSpec<? super I>> spec = spec();
	this.cols = new ArrayList<>(spec.size());
	for(int i = 0; i < spec.size(); i++)
	    cols.add(new Column<I>(this, spec.get(i)));
	widths();
	int h = headh();
	if(h > 0) {
	    for(Column<I> col : cols) {
		Widget head = col.spec.heading(col, Coord.of(col.w, h));
		if(head != null)
		    col.head = adda(head, Coord.of(col.x + (int)Math.round(col.w * col.spec.halign()), h / 2), col.spec.halign(), 0.5);
	    }
	}
	main = add(makelist(sz.sub(0, h)), Coord.of(0, h));
    }

    protected abstract List<? extends I> items();
    protected abstract List<ColSpec<? super I>> spec();
    protected abstract int itemh();
    protected int headh() {return(0);}
    protected int colmarg() {return(1);}
    protected int rowmarg() {return(1);}
    protected MainList makelist(Coord sz) {return(new MainList(sz));}
    protected Row makeitem(I item, int idx, Coord sz) {return(new Row(item, idx, sz));}

    public static interface HeadFactory<I> {
	public Widget heading(Column<? extends I> col, Coord sz);

	public static <T> HeadFactory<T> of(Tex heading) {
	    return((col, sz) -> new Img(heading));
	}
    }

    public static abstract class ColSpec<I> {
	public int fixw() {return(0);}
	public double flexw() {return(0);}
	public double align() {return(0);}
	public double halign() {return(0.5);}
	public abstract Widget heading(Column<? extends I> col, Coord sz);
	public abstract Widget makecell(I item, int idx, Coord sz);

	public static <T> ColSpec<T> of(int fixw, double flexw, double align, double halign,
					HeadFactory<? super T> heading,
					SListWidget.ItemFactory<T, ? extends Widget> makecell) {
	    return(new ColSpec<T>() {
		public int fixw() {return(fixw);}
		public double flexw() {return(flexw);}
		public double align() {return(align);}
		public double halign() {return(halign);}
		public Widget heading(Column<? extends T> col, Coord sz) {return(heading.heading(col, sz));}
		public Widget makecell(T item, int idx, Coord sz) {return(makecell.makeitem(item, idx, sz));}
	    });
	}

	public static <T> ColSpec<T> of(int fixw, double flexw, double align, double halign, Tex heading,
					SListWidget.ItemFactory<T, ? extends Widget> makecell) {
	    return(of(fixw, flexw, align, halign, HeadFactory.of(heading), makecell));
	}
    }

    public static class Column<I> {
	public final TableBox<I> tbl;
	public final ColSpec<? super I> spec;
	public Widget head;
	public int x, w;
	public Collection<BiConsumer<GOut, Column<? super I>>> drawcb = new ArrayList<>();

	protected Column(TableBox<I> tbl, ColSpec<? super I> spec) {
	    this.tbl = tbl;
	    this.spec = spec;
	}
    }

    public class Row extends Widget {
	protected Row(I item, int idx, Coord sz) {
	    super(sz);
	    for(Column<I> col : cols) {
		Widget colw = col.spec.makecell(item, idx, Coord.of(col.w, sz.y));
		if(colw != null)
		    adda(colw, Coord.of(col.x + (int)Math.round(col.w * col.spec.align()), sz.y), col.spec.align(), 1.0);
	    }
	}
    }

    public class MainList extends SListBox<I, Row> {
	protected MainList(Coord sz) {
	    super(sz, TableBox.this.itemh(), TableBox.this.rowmarg());
	}

	public List<? extends I> items() {
	    return(TableBox.this.items());
	}

	public Row makeitem(I item, int idx, Coord sz) {
	    return(TableBox.this.makeitem(item, idx, sz));
	}

	protected boolean unselect(int button) {
	    return(TableBox.this.unselect(button));
	}
    }

    protected boolean unselect(int button) {
	return(false);
    }

    private void widths() {
	List<ColSpec<? super I>> spec = spec();
	int n = spec.size(), m = colmarg();;
	int fw = 0;
	double ws = 0;
	for(int i = 0; i < n; i++) {
	    fw += spec.get(i).fixw();
	    ws += spec.get(i).flexw();
	}
	fw += (n - 1) * m;
	int rw = sz.x - fw;
	if(ws == 0) ws = 1;
	double cw = 0;
	for(int i = 0, x = 0; i < n; i++) {
	    Column c = cols.get(i);
	    ColSpec s = spec.get(i);
	    c.x = x;
	    c.w = s.fixw() + (int)(Math.round((cw + s.flexw()) * rw / ws) - Math.round(cw * rw / ws));
	    cw += s.flexw();
	    x += c.w + m;
	}
    }

    protected void drawgrid(GOut g) {
	g.chcolor(255, 255, 0, 64);
	for(Column<I> col : cols) {
	    int x = col.x + col.w;
	    g.line(new Coord(x, 0), new Coord(x, sz.y), 1);
	}
	g.chcolor();
	for(Column<I> col : cols) {
	    for(BiConsumer<GOut, Column<? super I>> cb : col.drawcb)
		cb.accept(g, col);
	}
    }

    public void draw(GOut g) {
	drawgrid(g);
	super.draw(g);
    }

    public abstract static class IHeading extends Widget {
	private boolean hovering;

	public IHeading(Column<?> col, Coord sz) {
	    super(sz);
	    col.drawcb.add(this::col);
	}

	protected abstract boolean click(MouseDownEvent ev);

	private void col(GOut g, Column<?> col) {
	    if(hovering) {
		g.chcolor(255, 255, 0, 16);
		g.frect2(new Coord(col.x, 0), new Coord(col.x + col.w, col.tbl.sz.y));
		g.chcolor();
	    }
	}

	public boolean mousedown(MouseDownEvent ev) {
	    if(ev.propagate(this) || super.mousedown(ev))
		return(true);
	    return(click(ev));
	}

	public boolean mousehover(MouseHoverEvent ev, boolean hovering) {
	    if(ev.propagate(this) || super.mousehover(ev, hovering)) {
		this.hovering = false;
		return(true);
	    }
	    this.hovering = hovering;
	    return(true);
	}

	public static <T> HeadFactory<T> wrap(HeadFactory<T> bk, Function<Column<? extends T>, Predicate<? super MouseDownEvent>> clickf) {
	    return((col, sz) -> {
		Predicate<? super MouseDownEvent> click = clickf.apply(col);
		IHeading ret = new IHeading(col, sz) {
		    protected boolean click(MouseDownEvent ev) {
			return(click.test(ev));
		    }
		};
		ret.adda(bk.heading(col, sz), Coord.of((int)Math.round(sz.x * col.spec.halign()), sz.y / 2), col.spec.halign(), 0.5);
		return(ret);
	    });
	}
    }
}
