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

import java.awt.*;

public class HelpWnd extends WindowX {
    public static final RichText.Foundry fnd;
    public Indir<Resource> res;
    private Indir<Resource> showing = null;
    private final RichTextBox text;
    
    static {
	/* XXX: This should use the shown resource's respool. */
	fnd = new RichText.Foundry(RichText.ImageSource.res(Resource.remote()));
	fnd.aa = true;
    }
    
    public HelpWnd(Indir<Resource> res) {
	super(Coord.z, "Help!", true);
	this.res = res;
	this.text = add(new RichTextBox(UI.scale(400, 500), "", fnd), Coord.z);
	add(new Button(UI.scale(100), "Dismiss", false) {
		public void click() {
		    if(justclose)
			HelpWnd.this.close();
		    else
		    HelpWnd.this.wdgmsg("close");
		}
	    }, UI.scale(150, 500));
	text.bg = new Color(16, 24, 16);
	pack();
    }
    
    public void tick(double dt) {
	super.tick(dt);
	if(res != showing) {
	    try {
		text.settext(res.get().flayer(Resource.pagina).text);
		showing = res;
	    } catch(Loading e) {}
	}
    }
    
    public static void show(UI ui, String res) {
	show(ui, Resource.local().load(res));
    }
    
    public static void show(UI ui, Indir<Resource> res) {
	HelpWnd wnd = new HelpWnd(res);
	wnd.justclose = true;
	ui.gui.add(wnd);
    }
}
