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

import java.awt.Color;

public abstract class Dropbox<T> extends ListWidget<T> {
    public static final Tex drop = Resource.loadtex("gfx/hud/drop");
    public final int listh;
    protected final Coord dropc;
    private Droplist dl;
    public Color bgcolor = new Color(20, 20, 20, 214);

    public Dropbox(int w, int listh, int itemh) {
	super(new Coord(w, itemh), itemh);
	this.listh = listh;
	dropc = new Coord(sz.x - drop.sz().x, 0);
    }
    
    @Override
    public boolean mousewheel(Coord c, int amount) {
	if(!super.mousewheel(c, amount)) {
	    int count = listitems();
	    if(count > 0) {
		int n = find(sel) + ((int) Math.signum(amount));
		while (n < 0) {n += count;}
		change(listitem(n % count));
		return true;
	    } else {
		return false;
	    }
	}
	return true;
    }

    private class Droplist extends Listbox<T> {
	private UI.Grab grab = null;
	private boolean risen = false;

	private Droplist() {
	    super(Dropbox.this.sz.x, Math.min(listh, Dropbox.this.listitems()), Dropbox.this.itemh);
	    sel = Dropbox.this.sel;
	    Dropbox.this.ui.root.add(this, Dropbox.this.rootpos().add(0, Dropbox.this.sz.y));
	    grab = ui.grabmouse(this);
	    display();
	}

	@Override
	public void tick(double dt) {
	    if(!risen){
		risen = true;
		raise();
	    }
	}

	protected T listitem(int i) {return(Dropbox.this.listitem(i));}
	protected int listitems() {return(Dropbox.this.listitems());}
	protected void drawitem(GOut g, T item, int idx) {Dropbox.this.drawitem(g, item, idx);}
    
	@Override
	protected Object itemtip(T item) {
	    return Dropbox.this.itemtip(item);
	}
    
	public boolean mousedown(Coord c, int btn) {
	    if(!c.isect(Coord.z, sz)) {
		reqdestroy();
		return(true);
	    }
	    return(super.mousedown(c, btn));
	}

	public void destroy() {
	    grab.remove();
	    super.destroy();
	    dl = null;
	}

	public void change(T item) {
	    Dropbox.this.change(item);
	    reqdestroy();
	}
    }

    public Object itemtip(T item) {
        return null;
    }
    
    public void draw(GOut g) {
	if(bgcolor != null){
	    g.chcolor(bgcolor);
	    g.frect(Coord.z, sz);
	    g.chcolor();
	}
	if(sel != null)
	    drawitem(g.reclip(Coord.z, new Coord(sz.x - drop.sz().x, itemh)), sel, 0);
	g.image(drop, dropc);
	super.draw(g);
    }

    public boolean mousedown(Coord c, int btn) {
	if(super.mousedown(c, btn))
	    return(true);
	if((dl == null) && (btn == 1)) {
	    dl = new Droplist();
	    dl.bgcolor = bgcolor;
	    return(true);
	}
	return(true);
    }
}
