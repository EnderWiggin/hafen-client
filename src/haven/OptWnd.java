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


import java.util.HashSet;
import java.util.LinkedList;
import haven.render.*;
import java.awt.event.KeyEvent;
import java.util.Set;

import static haven.Text.*;

public class OptWnd extends WindowX {
    public static final Coord PANEL_POS = new Coord(220, 30);
    public static final Coord Q_TYPE_PADDING = new Coord(3, 0);
    private final Panel display, general, camera, shortcuts, mapping, uipanel, combat;
    public final Panel main;
    private static final Text.Foundry LBL_FNT = new Text.Foundry(sans, 14);
    public Panel current;
    private WidgetList<KeyBinder.ShortcutWidget> shortcutList;
    
    public void chpanel(Panel p) {
	if(current != null)
	    current.hide();
	(current = p).show();
	cresize(p);
    }

    public void cresize(Widget ch) {
	if(ch == current) {
	    Coord cc = this.c.add(this.sz.div(2));
	    pack();
	    move(cc.sub(this.sz.div(2)));
	}
    }

    public class PButton extends Button {
	public final Panel tgt;
	public final int key;

	public PButton(int w, String title, int key, Panel tgt) {
	    super(w, title, false);
	    this.tgt = tgt;
	    this.key = key;
	}

	public void click() {
	    chpanel(tgt);
	}

	public boolean keydown(java.awt.event.KeyEvent ev) {
	    if((this.key != -1) && (ev.getKeyChar() == this.key)) {
		click();
		return (true);
	    }
	    return (false);
	}
    }
    
    private static class AButton extends Button {
	public final Action act;
	public final int key;
	
	public AButton(int w, String title, int key, Action act) {
	    super(w, title, false);
	    this.act = act;
	    this.key = key;
	}
	
	public void click() {
	    if(ui.gui != null) {act.run(ui.gui);}
	}
	
	public boolean keydown(java.awt.event.KeyEvent ev) {
	    if((this.key != -1) && (ev.getKeyChar() == this.key)) {
		click();
		return (true);
	    }
	    return (false);
	}
    }

    public class Panel extends Widget {
	public Panel() {
	    visible = false;
	    c = Coord.z;
	}
    }

    private void error(String msg) {
	GameUI gui = getparent(GameUI.class);
	if(gui != null)
	    gui.error(msg);
    }

    public class VideoPanel extends Panel {
	private final Widget back;
	private CPanel curcf;

	public VideoPanel(Panel prev) {
	    super();
	    back = add(new PButton(UI.scale(200), "Back", 27, prev));
	}

	public class CPanel extends Widget {
	    public GSettings prefs;

	    public CPanel(GSettings gprefs) {
		this.prefs = gprefs;
		Widget prev;
		int marg = UI.scale(5);
		prev = add(new CheckBox("Render shadows") {
			{a = prefs.lshadow.val;}

			public void set(boolean val) {
			    try {
				GSettings np = prefs.update(null, prefs.lshadow, val);
				ui.setgprefs(prefs = np);
			    } catch(GSettings.SettingException e) {
				error(e.getMessage());
				return;
			    }
			    a = val;
			}
		    }, Coord.z);
		prev = add(new Label("Render scale"), prev.pos("bl").adds(0, 5));
		{
		    Label dpy = new Label.Untranslated("");
		    final int steps = 4;
		    addhlp(prev.pos("bl").adds(0, 2), UI.scale(5),
			   prev = new HSlider(UI.scale(160), -2 * steps, 2 * steps, (int)Math.round(steps * Math.log(prefs.rscale.val) / Math.log(2.0f))) {
			       protected void added() {
				   dpy();
			       }
			       void dpy() {
				   dpy.settext(String.format("%.2f\u00d7", Math.pow(2, this.val / (double)steps)));
			       }
			       public void changed() {
				   try {
				       float val = (float)Math.pow(2, this.val / (double)steps);
				       ui.setgprefs(prefs = prefs.update(null, prefs.rscale, val));
				   } catch(GSettings.SettingException e) {
				       error(e.getMessage());
				       return;
				   }
				   dpy();
			       }
			   },
			   dpy);
		}
		prev = add(new CheckBox("Vertical sync") {
			{a = prefs.vsync.val;}

			public void set(boolean val) {
			    try {
				GSettings np = prefs.update(null, prefs.vsync, val);
				ui.setgprefs(prefs = np);
			    } catch(GSettings.SettingException e) {
				error(e.getMessage());
				return;
			    }
			    a = val;
			}
		    }, prev.pos("bl").adds(0, 5));
		prev = add(new Label("Framerate limit (active window)"), prev.pos("bl").adds(0, 5));
		{
		    Label dpy = new Label.Untranslated("");
		    final int max = 250;
		    addhlp(prev.pos("bl").adds(0, 2), UI.scale(5),
			   prev = new HSlider(UI.scale(160), 1, max, (prefs.hz.val == Float.POSITIVE_INFINITY) ? max : prefs.hz.val.intValue()) {
			       protected void added() {
				   dpy();
			       }
			       void dpy() {
				   if(this.val == max)
				       dpy.settext("None");
				   else
				       dpy.settext(Integer.toString(this.val));
			       }
			       public void changed() {
				   try {
				       if(this.val > 10)
					   this.val = (this.val / 2) * 2;
				       float val = (this.val == max) ? Float.POSITIVE_INFINITY : this.val;
				       ui.setgprefs(prefs = prefs.update(null, prefs.hz, val));
				   } catch(GSettings.SettingException e) {
				       error(e.getMessage());
				       return;
				   }
				   dpy();
			       }
			   },
			   dpy);
		}
		prev = add(new Label("Framerate limit (background window)"), prev.pos("bl").adds(0, 5));
		{
		    Label dpy = new Label.Untranslated("");
		    final int max = 250;
		    addhlp(prev.pos("bl").adds(0, 2), UI.scale(5),
			   prev = new HSlider(UI.scale(160), 1, max, (prefs.bghz.val == Float.POSITIVE_INFINITY) ? max : prefs.bghz.val.intValue()) {
			       protected void added() {
				   dpy();
			       }
			       void dpy() {
				   if(this.val == max)
				       dpy.settext("None");
				   else
				       dpy.settext(Integer.toString(this.val));
			       }
			       public void changed() {
				   try {
				       if(this.val > 10)
					   this.val = (this.val / 2) * 2;
				       float val = (this.val == max) ? Float.POSITIVE_INFINITY : this.val;
				       ui.setgprefs(prefs = prefs.update(null, prefs.bghz, val));
				   } catch(GSettings.SettingException e) {
				       error(e.getMessage());
				       return;
				   }
				   dpy();
			       }
			   },
			   dpy);
		}
		prev = add(new Label("Lighting mode"), prev.pos("bl").adds(0, 5));
		{
		    boolean[] done = {false};
		    RadioGroup grp = new RadioGroup(this) {
			    public void changed(int btn, String lbl) {
				if(!done[0])
				    return;
				try {
				    ui.setgprefs(prefs = prefs
						 .update(null, prefs.lightmode, GSettings.LightMode.values()[btn])
						 .update(null, prefs.maxlights, 0));
				} catch(GSettings.SettingException e) {
				    error(e.getMessage());
				    return;
				}
				resetcf();
			    }
			};
		    prev = grp.add("Global", prev.pos("bl").adds(5, 2));
		    prev.settip("Global lighting supports fewer light sources, and scales worse in " +
				"performance per additional light source, than zoned lighting, but " +
				"has lower baseline performance requirements.", true);
		    prev = grp.add("Zoned", prev.pos("bl").adds(0, 2));
		    prev.settip("Zoned lighting supports far more light sources than global " +
				"lighting with better performance, but may have higher performance " +
				"requirements in cases with few light sources, and may also have " +
				"issues on old graphics hardware.", true);
		    grp.check(prefs.lightmode.val.ordinal());
		    done[0] = true;
		}
		prev = add(new Label("Light-source limit"), prev.pos("bl").adds(0, 5).x(0));
		{
		    Label dpy = new Label("");
		    int val = prefs.maxlights.val;
		    if(val == 0) {    /* XXX: This is just ugly. */
			if(prefs.lightmode.val == GSettings.LightMode.ZONED)
			    val = Lighting.LightGrid.defmax;
			else
			    val = Lighting.SimpleLights.defmax;
		    }
		    addhlp(prev.pos("bl").adds(0, 2), UI.scale(5),
			   prev = new HSlider(UI.scale(160), 1, 32, val / 4) {
			       protected void added() {
				   dpy();
			       }
			       void dpy() {
				   dpy.settext(Integer.toString(this.val * 4));
			       }
			       public void changed() {dpy();}
			       public void fchanged() {
				   try {
				       ui.setgprefs(prefs = prefs.update(null, prefs.maxlights, this.val * 4));
				   } catch(GSettings.SettingException e) {
				       error(e.getMessage());
				       return;
				   }
				   dpy();
			       }
			       {
				   settip("The light-source limit means different things depending on the " +
					  "selected lighting mode. For Global lighting, it limits the total "+
					  "number of light-sources globally. For Zoned lighting, it limits the " +
					  "total number of overlapping light-sources at any point in space.",
					  true);
			       }
			   },
			   dpy);
		}
		prev = add(new Label("Frame sync mode"), prev.pos("bl").adds(0, 5).x(0));
		{
		    boolean[] done = {false};
		    RadioGroup grp = new RadioGroup(this) {
			    public void changed(int btn, String lbl) {
				if(!done[0])
				    return;
				try {
				    ui.setgprefs(prefs = prefs.update(null, prefs.syncmode, JOGLPanel.SyncMode.values()[btn]));
				} catch(GSettings.SettingException e) {
				    error(e.getMessage());
				    return;
				}
			    }
			};
		    prev = add(new Label("\u2191 Better performance, worse latency"), prev.pos("bl").adds(5, 2));
		    prev = grp.add("One-frame overlap", prev.pos("bl").adds(0, 2));
		    prev = grp.add("Tick overlap", prev.pos("bl").adds(0, 2));
		    prev = grp.add("CPU-sequential", prev.pos("bl").adds(0, 2));
		    prev = grp.add("GPU-sequential", prev.pos("bl").adds(0, 2));
		    prev = add(new Label("\u2193 Worse performance, better latency"), prev.pos("bl").adds(0, 2));
		    grp.check(prefs.syncmode.val.ordinal());
		    done[0] = true;
		}
		/* XXXRENDER
		composer.add(new CheckBox("Antialiasing") {
			{a = cf.fsaa.val;}

			public void set(boolean val) {
			    try {
				cf.fsaa.set(val);
			    } catch(GLSettings.SettingException e) {
				error(e.getMessage());
				return;
			    }
			    a = val;
			    cf.dirty = true;
			}
		    });
		composer.add(new Label("Anisotropic filtering"));
		if(cf.anisotex.max() <= 1) {
		    composer.add(new Label("(Not supported)"));
		} else {
		    final Label dpy = new Label("");
		    composer.addRow(
			    new HSlider(UI.scale(160), (int)(cf.anisotex.min() * 2), (int)(cf.anisotex.max() * 2), (int)(cf.anisotex.val * 2)) {
			    protected void added() {
				dpy();
			    }
			    void dpy() {
				if(val < 2)
				    dpy.settext("Off");
				else
				    dpy.settext(String.format("%.1f\u00d7", (val / 2.0)));
			    }
			    public void changed() {
				try {
				    cf.anisotex.set(val / 2.0f);
				} catch(GLSettings.SettingException e) {
				    error(e.getMessage());
				    return;
				}
				dpy();
				cf.dirty = true;
			    }
			},
			dpy
		    );
		}
		*/
		add(new Button(UI.scale(200), "Reset to defaults", false).action(() -> {
			    ui.setgprefs(GSettings.defaults());
			    curcf.destroy();
			    curcf = null;
		}), prev.pos("bl").adds(0, 5));
		pack();
	    }
	}

	public void draw(GOut g) {
	    if((curcf == null) || (ui.gprefs != curcf.prefs))
		resetcf();
	    super.draw(g);
	}

	private void resetcf() {
	    if(curcf != null)
		curcf.destroy();
	    curcf = add(new CPanel(ui.gprefs), 0, 0);
	    back.move(curcf.pos("bl").adds(0, 15));
	    pack();
	}
    }

    public class AudioPanel extends Panel {
	public AudioPanel(Panel back) {
	    prev = add(new Label("Master audio volume"), 0, 0);
	    prev = add(new HSlider(UI.scale(200), 0, 1000, (int)(Audio.volume * 1000)) {
		    public void changed() {
			Audio.setvolume(val / 1000.0);
		    }
		}, prev.pos("bl").adds(0, 2));
	    prev = add(new Label("Interface sound volume"), prev.pos("bl").adds(0, 15));
	    prev = add(new HSlider(UI.scale(200), 0, 1000, 0) {
		    protected void attach(UI ui) {
			super.attach(ui);
			val = (int)(ui.audio.aui.volume * 1000);
		    }
		    public void changed() {
			ui.audio.aui.setvolume(val / 1000.0);
		    }
		}, prev.pos("bl").adds(0, 2));
	    prev = add(new Label("In-game event volume"), prev.pos("bl").adds(0, 5));
	    prev = add(new HSlider(UI.scale(200), 0, 1000, 0) {
		    protected void attach(UI ui) {
			super.attach(ui);
			val = (int)(ui.audio.pos.volume * 1000);
		    }
		    public void changed() {
			ui.audio.pos.setvolume(val / 1000.0);
		    }
		}, prev.pos("bl").adds(0, 2));
	    prev = add(new Label("Ambient volume"), prev.pos("bl").adds(0, 5));
	    prev = add(new HSlider(UI.scale(200), 0, 1000, 0) {
		    protected void attach(UI ui) {
			super.attach(ui);
			val = (int)(ui.audio.amb.volume * 1000);
		    }
		    public void changed() {
			ui.audio.amb.setvolume(val / 1000.0);
		    }
		}, prev.pos("bl").adds(0, 2));
	    add(new PButton(UI.scale(200), "Back", 27, back), prev.pos("bl").adds(0, 30));
	    pack();
	}
    }

    public class InterfacePanel extends Panel {
	public InterfacePanel(Panel back) {
	    Widget prev = add(new Label("Interface scale (requires restart)"), 0, 0);
	    {
		Label dpy = new Label("");
		final double smin = 1, smax = Math.floor(UI.maxscale() / 0.25) * 0.25;
		final int steps = (int)Math.round((smax - smin) / 0.25);
		addhlp(prev.pos("bl").adds(0, 2), UI.scale(5),
		       prev = new HSlider(UI.scale(160), 0, steps, (int)Math.round(steps * (Utils.getprefd("uiscale", 1.0) - smin) / (smax - smin))) {
			       protected void added() {
				   dpy();
			       }
			       void dpy() {
				   dpy.settext(String.format("%.2f\u00d7", smin + (((double)this.val / steps) * (smax - smin))));
			       }
			       public void changed() {
				   double val = smin + (((double)this.val / steps) * (smax - smin));
				   Utils.setprefd("uiscale", val);
				   dpy();
			       }
			   },
		       dpy);
	    }
	    prev = add(new Label("Object fine-placement granularity"), prev.pos("bl").adds(0, 5));
	    {
		Label pos = add(new Label("Position"), prev.pos("bl").adds(5, 2));
		Label ang = add(new Label("Angle"), pos.pos("bl").adds(0, 2));
		int x = Math.max(pos.pos("ur").x, ang.pos("ur").x);
		{
		    Label dpy = new Label("");
		    final double smin = 1, smax = Math.floor(UI.maxscale() / 0.25) * 0.25;
		    final int steps = (int)Math.round((smax - smin) / 0.25);
		    int ival = (int)Math.round(MapView.plobpgran);
		    addhlp(Coord.of(x + UI.scale(5), pos.c.y), UI.scale(5),
			   prev = new HSlider(UI.scale(155 - x), 2, 17, (ival == 0) ? 17 : ival) {
				   protected void added() {
				       dpy();
				   }
				   void dpy() {
				       dpy.settext((this.val == 17) ? "\u221e" : Integer.toString(this.val));
				   }
				   public void changed() {
				       Utils.setprefd("plobpgran", MapView.plobpgran = ((this.val == 17) ? 0 : this.val));
				       dpy();
				   }
			       },
			   dpy);
		}
		{
		    Label dpy = new Label("");
		    final double smin = 1, smax = Math.floor(UI.maxscale() / 0.25) * 0.25;
		    final int steps = (int)Math.round((smax - smin) / 0.25);
		    int[] vals = {4, 5, 6, 8, 9, 10, 12, 15, 18, 20, 24, 30, 36, 40, 45, 60, 72, 90, 120, 180, 360};
		    int ival = 0;
		    for(int i = 0; i < vals.length; i++) {
			if(Math.abs((MapView.plobagran * 2) - vals[i]) < Math.abs((MapView.plobagran * 2) - vals[ival]))
			    ival = i;
		    }
		    addhlp(Coord.of(x + UI.scale(5), ang.c.y), UI.scale(5),
			   prev = new HSlider(UI.scale(155 - x), 0, vals.length - 1, ival) {
				   protected void added() {
				       dpy();
				   }
				   void dpy() {
				       dpy.settext(String.format("%d\u00b0", 360 / vals[this.val]));
				   }
				   public void changed() {
				       Utils.setprefd("plobagran", MapView.plobagran = (vals[this.val] / 2.0));
				       dpy();
				   }
			       },
			   dpy);
		}
	    }
	    add(new PButton(UI.scale(200), "Back", 27, back), prev.pos("bl").adds(0, 30).x(0));
	    pack();
	}
    }

    private static final Text kbtt = RichText.render("$col[255,255,0]{Escape}: Cancel input\n" +
						     "$col[255,255,0]{Backspace}: Revert to default\n" +
						     "$col[255,255,0]{Delete}: Disable keybinding", 0);
    public class BindingPanel extends Panel {
	private int addbtn(Widget cont, String nm, KeyBinding cmd, int y) {
	    return(cont.addhl(new Coord(0, y), cont.sz.x,
			      new Label(nm), new SetButton(UI.scale(175), cmd))
		   + UI.scale(2));
	}

	public BindingPanel(Panel back) {
	    super();
	    Scrollport scroll = add(new Scrollport(UI.scale(new Coord(300, 300))), 0, 0);
	    Widget cont = scroll.cont;
	    Widget prev;
	    int y = 0;
	    y = cont.adda(new Label("Main menu"), cont.sz.x / 2, y, 0.5, 0.0).pos("bl").adds(0, 5).y;
	    y = addbtn(cont, "Inventory", GameUI.kb_inv, y);
	    y = addbtn(cont, "Equipment", GameUI.kb_equ, y);
	    y = addbtn(cont, "Character sheet", GameUI.kb_chr, y);
	    y = addbtn(cont, "Map window", GameUI.kb_map, y);
	    y = addbtn(cont, "Kith & Kin", GameUI.kb_bud, y);
	    y = addbtn(cont, "Options", GameUI.kb_opt, y);
	    y = addbtn(cont, "Search actions", GameUI.kb_srch, y);
	    y = addbtn(cont, "Toggle chat", GameUI.kb_chat, y);
	    y = addbtn(cont, "Quick chat", ChatUI.kb_quick, y);
	    y = addbtn(cont, "Take screenshot", GameUI.kb_shoot, y);
	    y = addbtn(cont, "Minimap icons", GameUI.kb_ico, y);
	    y = addbtn(cont, "Toggle UI", GameUI.kb_hide, y);
	    y = addbtn(cont, "Log out", GameUI.kb_logout, y);
	    y = addbtn(cont, "Switch character", GameUI.kb_switchchr, y);
	    y = cont.adda(new Label("Map options"), cont.sz.x / 2, y + UI.scale(10), 0.5, 0.0).pos("bl").adds(0, 5).y;
	    y = addbtn(cont, "Display claims", GameUI.kb_claim, y);
	    y = addbtn(cont, "Display villages", GameUI.kb_vil, y);
	    y = addbtn(cont, "Display realms", GameUI.kb_rlm, y);
	    y = addbtn(cont, "Display grid-lines", MapView.kb_grid, y);
	    /*
	    y = cont.adda(new Label("Camera control"), cont.sz.x / 2, y + UI.scale(10), 0.5, 0.0).pos("bl").adds(0, 5).y;
	    y = addbtn(cont, "Rotate left", MapView.kb_camleft, y);
	    y = addbtn(cont, "Rotate right", MapView.kb_camright, y);
	    y = addbtn(cont, "Zoom in", MapView.kb_camin, y);
	    y = addbtn(cont, "Zoom out", MapView.kb_camout, y);
	    y = addbtn(cont, "Reset", MapView.kb_camreset, y);
	    */
	    y = cont.adda(new Label("Map window"), cont.sz.x / 2, y + UI.scale(10), 0.5, 0.0).pos("bl").adds(0, 5).y;
	    y = addbtn(cont, "Reset view", MapWnd.kb_home, y);
	    y = addbtn(cont, "Place marker", MapWnd.kb_mark, y);
	    y = addbtn(cont, "Toggle markers", MapWnd.kb_hmark, y);
	    y = addbtn(cont, "Compact mode", MapWnd.kb_compact, y);
	    y = cont.adda(new Label("Walking speed"), cont.sz.x / 2, y + UI.scale(10), 0.5, 0.0).pos("bl").adds(0, 5).y;
	    y = addbtn(cont, "Increase speed", Speedget.kb_speedup, y);
	    y = addbtn(cont, "Decrease speed", Speedget.kb_speeddn, y);
	    for(int i = 0; i < 4; i++)
		y = addbtn(cont, String.format("Set speed %d", i + 1), Speedget.kb_speeds[i], y);
	    y = cont.adda(new Label("Combat actions"), cont.sz.x / 2, y + UI.scale(10), 0.5, 0.0).pos("bl").adds(0, 5).y;
	    /*
	    for(int i = 0; i < Fightsess.kb_acts.length; i++)
		y = addbtn(cont, String.format("Combat action %d", i + 1), Fightsess.kb_acts[i], y);
	    */
	    y = addbtn(cont, "Switch targets", Fightsess.kb_relcycle, y);
	    prev = adda(new PointBind(UI.scale(200)), scroll.pos("bl").adds(0, 10).x(scroll.sz.x / 2), 0.5, 0.0);
	    prev = adda(new PButton(UI.scale(200), "Back", 27, back), prev.pos("bl").adds(0, 10).x(scroll.sz.x / 2), 0.5, 0.0);
	    pack();
	}

	public class SetButton extends KeyMatch.Capture {
	    public final KeyBinding cmd;

	    public SetButton(int w, KeyBinding cmd) {
		super(w, cmd.key());
		this.cmd = cmd;
	    }

	    public void set(KeyMatch key) {
		super.set(key);
		cmd.set(key);
	    }

	    public void draw(GOut g) {
		if(cmd.key() != key)
		    super.set(cmd.key());
		super.draw(g);
	    }

	    protected KeyMatch mkmatch(KeyEvent ev) {
		return(KeyMatch.forevent(ev, ~cmd.modign));
	    }

	    protected boolean handle(KeyEvent ev) {
		if(ev.getKeyCode() == KeyEvent.VK_BACK_SPACE) {
		    cmd.set(null);
		    super.set(cmd.key());
		    return(true);
		}
		return(super.handle(ev));
	    }
	    
	    @Override
	    protected boolean i10n() { return false; }
	    
	    public Object tooltip(Coord c, Widget prev) {
		return(kbtt.tex());
	    }
	}
    }


    public static class PointBind extends Button {
	public static final String msg = "Bind other elements...";
	public static final Resource curs = Resource.local().loadwait("gfx/hud/curs/wrench");
	private UI.Grab mg, kg;
	private KeyBinding cmd;

	public PointBind(int w) {
	    super(w, msg, false);
	    tooltip = RichText.render("Bind a key to an element not listed above, such as an action-menu " +
				      "button. Click the element to bind, and then press the key to bind to it. " +
				      "Right-click to stop rebinding.",
				      300);
	}

	public void click() {
	    if(mg == null) {
		change("Click element...");
		mg = ui.grabmouse(this);
	    } else if(kg != null) {
		kg.remove();
		kg = null;
		change(msg);
	    }
	}

	private boolean handle(KeyEvent ev) {
	    switch(ev.getKeyCode()) {
	    case KeyEvent.VK_SHIFT: case KeyEvent.VK_CONTROL: case KeyEvent.VK_ALT:
	    case KeyEvent.VK_META: case KeyEvent.VK_WINDOWS:
		return(false);
	    }
	    int code = ev.getKeyCode();
	    if(code == KeyEvent.VK_ESCAPE) {
		return(true);
	    }
	    if(code == KeyEvent.VK_BACK_SPACE) {
		cmd.set(null);
		return(true);
	    }
	    if(code == KeyEvent.VK_DELETE) {
		cmd.set(KeyMatch.nil);
		return(true);
	    }
	    KeyMatch key = KeyMatch.forevent(ev, ~cmd.modign);
	    if(key != null)
		cmd.set(key);
	    return(true);
	}

	public boolean mousedown(Coord c, int btn) {
	    if(mg == null)
		return(super.mousedown(c, btn));
	    Coord gc = ui.mc;
	    if(btn == 1) {
		this.cmd = KeyBinding.Bindable.getbinding(ui.root, gc);
		return(true);
	    }
	    if(btn == 3) {
		mg.remove();
		mg = null;
		change(msg);
		return(true);
	    }
	    return(false);
	}

	public boolean mouseup(Coord c, int btn) {
	    if(mg == null)
		return(super.mouseup(c, btn));
	    Coord gc = ui.mc;
	    if(btn == 1) {
		if((this.cmd != null) && (KeyBinding.Bindable.getbinding(ui.root, gc) == this.cmd)) {
		    mg.remove();
		    mg = null;
		    kg = ui.grabkeys(this);
		    change("Press key...");
		} else {
		    this.cmd = null;
		}
		return(true);
	    }
	    if(btn == 3)
		return(true);
	    return(false);
	}

	public Resource getcurs(Coord c) {
	    if(mg == null)
		return(null);
	    return(curs);
	}

	public boolean keydown(KeyEvent ev) {
	    if(kg == null)
		return(super.keydown(ev));
	    if(handle(ev)) {
		kg.remove();
		kg = null;
		cmd = null;
		change("Click another element...");
		mg = ui.grabmouse(this);
	    }
	    return(true);
	}
    }

    public OptWnd(boolean gopts) {
	super(Coord.z, "Options", true);
	main = add(new Panel());
	Panel video = add(new VideoPanel(main));
	Panel audio = add(new AudioPanel(main));
	Panel iface = add(new InterfacePanel(main));
	Panel keybind = add(new BindingPanel(main));
	display = add(new Panel());
	uipanel = add(new Panel());
	combat = add(new Panel());
	general = add(new Panel());
	camera = add(new Panel());
	shortcuts = add(new Panel());
	mapping = add(new Panel());

	int row = 0, colum = 0, mrow = 1;
    
	addPanelButton("Interface settings", 'i', iface, colum, row++);
	addPanelButton("Video settings", 'v', video, colum, row++);
	addPanelButton("Audio settings", 'a', audio, colum, row++);
	addPanelButton("Camera settings", 'c', camera, colum, row++);
	addPanelButton("Widget shortcuts", 'k', keybind, colum, row++);
	addPanelButton("Global shortcuts", 's', shortcuts, colum, row++);
    
	colum++;
	mrow = Math.max(mrow, row);
	row = 0;

	addPanelButton("General", 'g', general, colum, row++);
	addPanelButton("UI", 'u', uipanel, colum, row++);
	addPanelButton("Display", 'd', display, colum, row++);
	addPanelButton("Combat", 'b', combat, colum, row++);
	addPanelButton("Map upload", 'm', mapping, colum, row++);

	int y = 0;
	mrow = Math.max(mrow, row);
	Widget prev;
	//y = main.add(new PButton(UI.scale(200), "Interface settings", 'v', iface), 0, y).pos("bl").adds(0, 5).y;
	//y = main.add(new PButton(UI.scale(200), "Video settings", 'v', video), 0, y).pos("bl").adds(0, 5).y;
	//y = main.add(new PButton(UI.scale(200), "Audio settings", 'a', audio), 0, y).pos("bl").adds(0, 5).y;
	//y = main.add(new PButton(UI.scale(200), "Keybindings", 'k', keybind), 0, y).pos("bl").adds(0, 5).y;
	y += UI.scale((mrow + 1) * PANEL_POS.y);
	if(gopts) {
	    y = main.add(new Button(UI.scale(200), "Switch character", false).action(() -> {
			getparent(GameUI.class).act("lo", "cs");
	    }), 0, y).pos("bl").adds(0, 5).y;
	    y = main.add(new Button(UI.scale(200), "Log out", false).action(() -> {
			getparent(GameUI.class).act("lo");
	    }), 0, y).pos("bl").adds(0, 5).y;
	}
	y = main.add(new Button(UI.scale(200), "Close", false).action(() -> {
		    OptWnd.this.hide();
	}), 0, y).pos("bl").adds(0, 5).y;
	this.main.pack();

	chpanel(this.main);
	initDisplayPanel(display);
	initUIPanel(uipanel);
	initCombatPanel(combat);
	initGeneralPanel(general);
	initCameraPanel();
	initMappingPanel(mapping);
	main.pack();
	chpanel(main);
    }
    
    @Override
    protected void attach(UI ui) {
	super.attach(ui);
	initShortcutsPanel();
    }
    
    private void addPanelButton(String name, char key, Panel panel, int x, int y) {
	main.add(new PButton(UI.scale(200), name, key, panel), UI.scale(PANEL_POS.mul(x, y)));
    }
    
    private void addPanelButton(String name, char key, Action action, int x, int y) {
	main.add(new AButton(UI.scale(200), name, key, action), UI.scale(PANEL_POS.mul(x, y)));
    }

    private void initCameraPanel() {
	int x = 0, y = 0, my = 0;
	int STEP = UI.scale(25);
	int BIG_STEP = UI.scale(35);

	int tx = x + camera.add(new Label("Camera:"), x, y).sz.x + 5;
	camera.add(new Dropbox<String>(100, 5, 16) {
	    @Override
	    protected String listitem(int i) {
		return new LinkedList<>(MapView.camlist()).get(i);
	    }

	    @Override
	    protected int listitems() {
		return MapView.camlist().size();
	    }

	    @Override
	    protected void drawitem(GOut g, String item, int i) {
		g.text(item, Coord.z);
	    }

	    @Override
	    public void change(String item) {
		super.change(item);
		MapView.defcam(item);
		if(ui.gui != null && ui.gui.map != null) {
		    ui.gui.map.camera = ui.gui.map.restorecam();
		}
	    }
	}, tx, y).sel = MapView.defcam();
    
	y += BIG_STEP;
	camera.add(new Label("Brighten view"), x, y);
	y += UI.scale(15);
	camera.add(new HSlider(UI.scale(200), 0, 500, 0) {
	    public void changed() {
		CFG.CAMERA_BRIGHT.set(val / 1000.0f);
		if(ui.sess != null && ui.sess.glob != null) {
		    ui.sess.glob.brighten();
		}
	    }
	}, x, y).val = (int) (1000 * CFG.CAMERA_BRIGHT.get());
    
	y += BIG_STEP;
	camera.add(new CFGBox("Invert horizontal camera rotation", CFG.CAMERA_INVERT_X), x, y);
    
	y += STEP;
	camera.add(new CFGBox("Invert vertical camera rotation", CFG.CAMERA_INVERT_Y), x, y);
    
	y += BIG_STEP;
	my = Math.max(my, y);

	camera.add(new PButton(UI.scale(200), "Back", 27, main), 0, my);
	camera.pack();
    }


    private void initGeneralPanel(Panel panel) {
	int STEP = UI.scale(25);
	int START;
	int x, y;
	int my = 0, tx;
    
	Widget title = panel.add(new Label("General settings", LBL_FNT), 0, 0);
	START = title.sz.y + UI.scale(10);
    
	x = 0;
	y = START;
    
	tx = x + panel.add(new Label("Language (requires restart):"), x, y).sz.x + UI.scale(5);
	panel.add(new Dropbox<String>(UI.scale(80), 5, UI.scale(16)) {
	    @Override
	    protected String listitem(int i) {
		return L10N.LANGUAGES.get(i);
	    }
	
	    @Override
	    protected int listitems() {
		return L10N.LANGUAGES.size();
	    }
	
	    @Override
	    protected void drawitem(GOut g, String item, int i) {
		g.atext(item, UI.scale(3, 8), 0, 0.5);
	    }
	
	    @Override
	    public void change(String item) {
		super.change(item);
		if(!item.equals(L10N.LANGUAGE.get())) L10N.LANGUAGE.set(item);
	    }
	}, tx, y).change(L10N.LANGUAGE.get());
    
	y += STEP;
	panel.add(new CFGBox("Output missing translation lines", L10N.DBG), x, y);
    
	y += STEP;
	panel.add(new CFGBox("Force hardware cursor", CFG.FORCE_HW_CURSOR, null, true), x, y);
	
	y += STEP;
	panel.add(new CFGBox("Store minimap tiles", CFG.STORE_MAP), x, y);
    
	y += STEP;
	panel.add(new CFGBox("Store chat logs", CFG.STORE_CHAT_LOGS, "Logs are stored in 'chats' folder"), new Coord(x, y));
    
	y += STEP;
	panel.add(new CFGBox("Item drop protection", CFG.ITEM_DROP_PROTECTION, "Drop items on cursor only when CTRL is pressed"), new Coord(x, y));
    
	y += STEP;
	panel.add(new CFGBox("Enable path queueing", CFG.QUEUE_PATHS, "ALT+LClick will queue movement"), x, y);
    
	y += STEP;
	Coord tsz = panel.add(new Label("Default speed:"), x, y).sz;
	panel.adda(new Speedget.SpeedSelector(UI.scale(100)), new Coord(x + tsz.x + UI.scale(5), y + tsz.y / 2), 0, 0.5);
    
	y += 2 * STEP;
	Label label = panel.add(new Label(String.format("Auto pickup radius: %.2f", CFG.AUTO_PICK_RADIUS.get() / 11.0)), x, y);
	y += UI.scale(15);
	panel.add(new CFGHSlider(UI.scale(150), CFG.AUTO_PICK_RADIUS, 33, 352) {
	    @Override
	    public void changed() {
		label.settext(String.format("Auto pickup radius: %.02f", val / 11.0));
	    }
	}, x, y);
    
	y += STEP;
	panel.add(new CFGBox("Auto pickup only visible", CFG.AUTO_PICK_ONLY_RADAR, "If on will pickup only objects with enabled minimap icons"), x, y);
    
	y += 2 * STEP;
	panel.add(new Button(UI.scale(150), "Warning settings", false) {
	    @Override
	    public void click() {
		if(ui.gui != null) {
		    GobWarning.WarnCFGWnd.toggle(ui.gui);
		} else {
		    GobWarning.WarnCFGWnd.toggle(ui.root);
		}
	    }
	}, x, y);
 
	y += STEP;
	panel.add(new Button(UI.scale(150), "Toggle at login", false) {
	    @Override
	    public void click() {
		if(ui.gui != null) {
		    LoginTogglesCfgWnd.toggle(ui.gui);
		} else {
		    LoginTogglesCfgWnd.toggle(ui.root);
		}
	    }
	}, x, y);
    
	my = Math.max(my, y);
	x += UI.scale(250);
	y = START;
    
	panel.add(new Label("Choose menu items to select automatically:"), x, y);
	y += UI.scale(15);
	final FlowerList list = panel.add(new FlowerList(), x, y);
    
	y += list.sz.y + UI.scale(5);
	final TextEntry value = panel.add(new TextEntry(UI.scale(160), "") {
	    @Override
	    public void activate(String text) {
		list.add(text);
		settext("");
	    }
	}, x, y);
    
	panel.add(new Button(UI.scale(85), "Add") {
	    @Override
	    public void click() {
		list.add(value.text());
		value.settext("");
	    }
	}, x + UI.scale(165), y - UI.scale(2));
    
	y += STEP;
	tx = x + panel.add(new Label("Hold to ignore auto choose:"), x, y).sz.x + UI.scale(5);
	panel.add(new Dropbox<UI.KeyMod>(UI.scale(100), 5, UI.scale(16)) {
	    @Override
	    protected UI.KeyMod listitem(int i) {
		return UI.KeyMod.values()[i];
	    }
	
	    @Override
	    protected int listitems() {
		return UI.KeyMod.values().length;
	    }
	
	    @Override
	    protected void drawitem(GOut g, UI.KeyMod item, int i) {
		g.atext(item.name(), UI.scale(3, 8), 0, 0.5);
	    }
	
	    @Override
	    public void change(UI.KeyMod item) {
		super.change(item);
		if(!item.equals(CFG.MENU_SKIP_AUTO_CHOOSE.get())) CFG.MENU_SKIP_AUTO_CHOOSE.set(item, true);
	    }
	}, tx, y).change(CFG.MENU_SKIP_AUTO_CHOOSE.get());
    
	y += STEP;
	panel.add(new CFGBox("Single item CTRL choose", CFG.MENU_SINGLE_CTRL_CLICK, "If checked, will automatically select single item menus if CTRL is pressed when menu is opened."), x, y);
    
	y += STEP;
	panel.add(new CFGBox("Add \"Pick All\" option", CFG.MENU_ADD_PICK_ALL, "If checked, will add new option that will allow to pick all same objects."), x, y);
    
	my = Math.max(my, y);
    
	panel.add(new PButton(UI.scale(200), "Back", 27, main), 0, my + UI.scale(35));
	panel.pack();
	title.c.x = (panel.sz.x - title.sz.x) / 2;
    }

    private void initDisplayPanel(Panel panel) {
	int STEP = UI.scale(25);
	int START;
	int x, y;
	int my = 0, tx;
    
	Widget title = panel.add(new Label("Display settings", LBL_FNT), 0, 0);
	START = title.sz.y + UI.scale(10);
    
	x = 0;
	y = START;
	panel.add(new CFGBox("Show flavor objects", CFG.DISPLAY_FLAVOR, "Requires restart"), x, y);
	
	y += STEP;
	panel.add(new CFGBox("Simple crops", CFG.SIMPLE_CROPS, "Requires area reload"), x, y);
	
	y += STEP;
	panel.add(new CFGBox("Always show kin names", CFG.DISPLAY_KINNAMES), x, y);
	
	y += STEP;
	panel.add(new CFGBox("Play sound when kin changes status", CFG.DISPLAY_KINSFX), x, y);
    
	y += STEP;
	panel.add(new CFGBox("Show task status messages", CFG.SHOW_BOT_MESSAGES, "Will log task (like auto-pickup or auto-drink) status to system log"), x, y);

	y += STEP;
	panel.add(new CFGBox("Show object info", CFG.DISPLAY_GOB_INFO, "Enables damage and crop/tree growth stage displaying", true), x, y);
    
	y += STEP;
	panel.add(new CFGBox("Flat cupboards (needs restart)", CFG.FLAT_CUPBOARDS, "Makes cupboards look like floor hatches", true), x, y);

	y += STEP;
	panel.add(new CFGBox("Display container fullness", CFG.SHOW_CONTAINER_FULLNESS, "Makes containers tint different colors when they are empty or full", true), x, y);
    
	y += STEP;
	tx = panel.add(new CFGBox("Draw paths", CFG.DISPLAY_GOB_PATHS, "Draws lines where objects are moving", true), x, y).sz.x;
	panel.add(new IButton("gfx/hud/opt", "", "-d", "-h") {
	    @Override
	    public void click() {
		if(ui.gui != null) {
		    PathVisualizer.CategoryOpts.toggle(ui.gui);
		} else {
		    PathVisualizer.CategoryOpts.toggle(ui.root);
		}
	    }
	}, x + tx + UI.scale(10), y + UI.scale(1));
	
	y += 35;
	panel.add(new CFGBox("Show object radius", CFG.SHOW_GOB_RADIUS, "Shows radius of mine supports, beehives etc.", true), x, y);

	y += STEP;
	panel.add(new Button(UI.scale(150), "Show as buffs", false) {
	    @Override
	    public void click() {
		if(ui.gui != null) {
		    ShowBuffsCfgWnd.toggle(ui.gui);
		} else {
		    ShowBuffsCfgWnd.toggle(ui.root);
		}
	    }
	}, x, y);
 
	my = Math.max(my, y);

	panel.add(new PButton(UI.scale(200), "Back", 27, main), new Coord(0, my + UI.scale(35)));
	panel.pack();
	title.c.x = (panel.sz.x - title.sz.x) / 2;
    }
    
    private void initUIPanel(Panel panel) {
	int STEP = UI.scale(25);
	int START;
	int x, y;
	int my = 0, tx;
    
	Widget title = panel.add(new Label("UI settings", LBL_FNT), 0, 0);
	START = title.sz.y + UI.scale(10); 
	
	x = 0;
    	y = START;
	//first row
	tx = x + panel.add(new Label("UI Theme:"), x, y).sz.x + UI.scale(5);
	panel.add(new Dropbox<Theme>(UI.scale(100), 5, UI.scale(16)) {
	    @Override
	    protected Theme listitem(int i) {
		return Theme.values()[i];
	    }
	
	    @Override
	    protected int listitems() {
		return Theme.values().length;
	    }
	
	    @Override
	    protected void drawitem(GOut g, Theme item, int i) {
		g.atext(item.name(), UI.scale(3, 8), 0, 0.5);
	    }
	
	    @Override
	    public void change(Theme item) {
		super.change(item);
		if(!item.equals(CFG.THEME.get())) CFG.THEME.set(item, true);
	    }
	}, tx, y).change(CFG.THEME.get());
    
	y += STEP;
	panel.add(new CFGBox("Always show UI on start", CFG.DISABLE_UI_HIDING), x, y);
    
	y += STEP;
	panel.add(new CFGBox("Show F-key tool bar", CFG.SHOW_TOOLBELT_0), x, y);
    
	y += STEP;
	panel.add(new CFGBox("Show extra tool bar", CFG.SHOW_TOOLBELT_1), x, y);
	
	y += STEP;
	panel.add(new CFGBox("Show FEP meter", CFG.FEP_METER) {
	    @Override
	    public void set(boolean a) {
		super.set(a);
		if(ui.gui != null && ui.gui.chrwdg != null) {
		    if(a) {
			ui.gui.addcmeter(new FEPMeter(ui.gui.chrwdg.feps));
		    } else {
			ui.gui.delcmeter(FEPMeter.class);
		    }
		}
	    }
	}, x, y);
    
	y += STEP;
	panel.add(new CFGBox("Show hunger meter", CFG.HUNGER_METER) {
	    @Override
	    public void set(boolean a) {
		super.set(a);
		if(ui.gui != null && ui.gui.chrwdg != null) {
		    if(a) {
			ui.gui.addcmeter(new HungerMeter(ui.gui.chrwdg.glut));
		    } else {
			ui.gui.delcmeter(HungerMeter.class);
		    }
		}
	    }
	}, x, y);
	
	y += STEP;
	panel.add(new CFGBox("Show timestamps in chat messages", CFG.SHOW_CHAT_TIMESTAMP), new Coord(x, y));
    
	y += STEP;
	panel.add(new CFGBox("Show food categories", CFG.DISPLAY_FOD_CATEGORIES, "Shows list of food categories in the tooltip", true), x, y);
    
	y += STEP;
	panel.add(new CFGBox("Show biomes on minimap", CFG.MMAP_SHOW_BIOMES), x, y);
    
	y += STEP;
	panel.add(new CFGBox("Show queued path on minimap", CFG.MMAP_SHOW_PATH), x, y);
    
	y += 2*STEP;
	panel.add(new CFGBox("Require SHIFT to show stack inventory", CFG.UI_STACK_SUB_INV_ON_SHIFT, "Show stack hover-inventories only if SHIFT is pressed"), x, y);
    
	y += STEP;
	panel.add(new CFGBox("Unpack stacks in extra inventory", CFG.UI_STACK_EXT_INV_UNPACK, "Show stacked items 'unpacked' in extra inventory's list"), x, y);
    
	//second row
	my = Math.max(my, y);
	x += UI.scale(265);
	y = START;
	panel.add(new CFGBox("Real time curios", CFG.REAL_TIME_CURIO, "Show curiosity study time in real life hours, instead of server hours"), new Coord(x, y));
    
	y += STEP;
	panel.add(new CFGBox("Display curio remaining time in tooltip", CFG.SHOW_CURIO_REMAINING_TT), new Coord(x, y));
    
	y += STEP;
	panel.add(new CFGBox("Display curio remaining time instead of progress", CFG.SHOW_CURIO_REMAINING_METER), new Coord(x, y));
    
	y += STEP;
	panel.add(new CFGBox("Show LP/H for curios", CFG.SHOW_CURIO_LPH, "Show how much learning point curio gives per hour"), new Coord(x, y));
    
	y += 2*STEP;
	panel.add(new CFGBox("Show item quality", CFG.Q_SHOW_SINGLE), x, y);
    
	y += STEP;
	panel.add(new CFGBox("Swap item quality and number", CFG.SWAP_NUM_AND_Q), x, y);
    
	y += STEP;
	panel.add(new CFGBox("Show item progress as number", CFG.PROGRESS_NUMBER), x, y);
    
	y += STEP;
	panel.add(new CFGBox("Show item durability", CFG.SHOW_ITEM_DURABILITY), new Coord(x, y));
    
	y += STEP;
	panel.add(new CFGBox("Show item wear bar", CFG.SHOW_ITEM_WEAR_BAR), new Coord(x, y));
    
	y += STEP;
	panel.add(new CFGBox("Show item armor", CFG.SHOW_ITEM_ARMOR), new Coord(x, y));
	
	my = Math.max(my, y);
    
	panel.add(new PButton(UI.scale(200), "Back", 27, main), new Coord(0, my + UI.scale(35)));
	panel.pack();
	title.c.x = (panel.sz.x - title.sz.x) / 2;
    }
    
    private void initCombatPanel(Panel panel) {
	int STEP = UI.scale(25);
	int START;
	int x, y;
	int my = 0, tx;
    
	Widget title = panel.add(new Label("Combat settings", LBL_FNT), 0, 0);
	START = title.sz.y + UI.scale(10);
    
	x = 0;
	y = START;
	//first row
	panel.add(new CFGBox("Use new combat UI", CFG.ALT_COMBAT_UI), x, y);
    
	y += STEP;
	panel.add(new CFGBox("Always mark current target", CFG.ALWAYS_MARK_COMBAT_TARGET , "Usually current target only marked when there's more than one"), x, y);
    
	y += STEP;
	panel.add(new CFGBox("Auto peace on combat start", CFG.COMBAT_AUTO_PEACE , "Automatically enter peaceful mode on combat start id enemy is aggressive - useful for taming"), x, y);
    
	y += STEP;
	panel.add(new CFGBox("Show combat damage", CFG.SHOW_COMBAT_DMG), x, y);
    
	y += STEP;
	panel.add(new CFGBox("Clear player damage after combat", CFG.CLEAR_PLAYER_DMG_AFTER_COMBAT), x, y);
    
	y += STEP;
	panel.add(new CFGBox("Clear all damage after combat", CFG.CLEAR_ALL_DMG_AFTER_COMBAT), x, y);
    
	y += STEP;
	panel.add(new CFGBox("Simplified combat openings", CFG.SIMPLE_COMBAT_OPENINGS, "Show openings as solid colors with numbers"), x, y);
    
	y += STEP;
	panel.add(new CFGBox("Display combat keys", CFG.SHOW_COMBAT_KEYS), x, y);
	
	//second row
	my = Math.max(my, y);
	x += UI.scale(265);
	y = START;
	
	
	my = Math.max(my, y);
    
	panel.add(new PButton(UI.scale(200), "Back", 27, main), new Coord(0, my + UI.scale(35)));
	panel.pack();
	title.c.x = (panel.sz.x - title.sz.x) / 2;
    }

    private void populateShortcutsPanel(KeyBinder.KeyBindType type) {
        shortcutList.clear(true);
	KeyBinder.makeWidgets(type).forEach(shortcutList::additem);
	shortcutList.updateChildPositions();
    }
    
    private void initShortcutsPanel() {
	TabStrip<KeyBinder.KeyBindType> tabs = new TabStrip<>(this::populateShortcutsPanel);
	tabs.insert(0, null, "General", null).tag = KeyBinder.KeyBindType.GENERAL;
	tabs.insert(1, null, "Combat", null).tag = KeyBinder.KeyBindType.COMBAT;
	shortcuts.add(tabs);
	int y = tabs.sz.y;
	
	shortcutList = shortcuts.add(new WidgetList<KeyBinder.ShortcutWidget>(UI.scale(300, 24), 16) {
	    
	    @Override
	    public Object tooltip(Coord c0, Widget prev) {
		KeyBinder.ShortcutWidget item = itemat(c0);
		if(item != null) {
		    c0 = c0.add(0, sb.val * itemsz.y);
		    return item.tooltip(c0, prev);
		}
		return super.tooltip(c, prev);
	    }
	}, 0, y);
	shortcutList.canselect = false;
	tabs.select(KeyBinder.KeyBindType.GENERAL, false);
 
	shortcuts.pack();
	shortcuts.add(new PButton(UI.scale(200), "Back", 27, main), shortcuts.sz.x / 2 - 100, shortcuts.sz.y + 35);
	shortcuts.pack();
    }
    
    private void initMappingPanel(Panel panel) {
	int STEP = UI.scale(25);
	int START;
	int x, y;
	int my = 0, tx;
    
	Widget title = panel.add(new Label("Map upload settings", LBL_FNT), 0, 0);
	START = title.sz.y + UI.scale(10);
    
	x = 0;
	y = START;
	
	panel.add(new CFGBox("Upload enabled", CFG.AUTOMAP_UPLOAD), x, y);
	y += STEP;
	
	panel.add(new CFGBox("Tracking enabled", CFG.AUTOMAP_TRACK), x, y);
	y += STEP;
	
	panel.add(new Label("Mapping URL:"), x, y);
	y += STEP;
	
	panel.add(new TextEntry(UI.scale(250), CFG.AUTOMAP_ENDPOINT.get()) {
	    @Override
	    public boolean keydown(KeyEvent ev) {
		if(!parent.visible)
		    return false;
		CFG.AUTOMAP_ENDPOINT.set(text());
		return buf.key(ev);
	    }
	}, x, y);
 
	y += STEP;
	panel.add(new Label("Upload custom markers:"), x, y);
 
	y += STEP;
	panel.add(new BuddyWnd.GroupSelector(-1) {
	    {
		Set<BuddyWnd.Group> groups = CFG.AUTOMAP_MARKERS.get();
		for (BuddyWnd.Group g : groups) {
		    this.groups[g.ordinal()].select();
		}
	    }
	    
	    @Override
	    public void update(int idx) {
		if(idx >= 0 && idx < this.groups.length) {
		    BuddyWnd.GroupRect group = this.groups[idx];
		    if(group.selected()) {
			group.unselect();
		    } else {
			group.select();
		    }
		    
		    Set<BuddyWnd.Group> selected = new HashSet<>();
		    for (int i = 0; i < this.groups.length; i++) {
			if(this.groups[i].selected()) {
			    selected.add(BuddyWnd.Group.values()[i]);
			}
		    }
		    CFG.AUTOMAP_MARKERS.set(selected);
		}
	    }
	}, x, y);
 
	y += STEP;
	
	panel.add(new PButton(UI.scale(200), "Back", 27, main), x, y);
	panel.pack();
	title.c.x = (panel.sz.x - title.sz.x) / 2;
    }
    
    public OptWnd() {
	this(true);
    }

    public void wdgmsg(Widget sender, String msg, Object... args) {
	if((sender == this) && (msg == "close")) {
	    hide();
	} else {
	    super.wdgmsg(sender, msg, args);
	}
    }

    public void show() {
	chpanel(main);
	super.show();
    }

    public static class CFGBox extends CheckBox implements CFG.Observer<Boolean> {

	protected final CFG<Boolean> cfg;

	public CFGBox(String lbl, CFG<Boolean> cfg) {
	    this(lbl, cfg, null, false);
	}

	public CFGBox(String lbl, CFG<Boolean> cfg, String tip) {
	    this(lbl, cfg, tip, false);
	}

	public CFGBox(String lbl, CFG<Boolean> cfg, String tip, boolean observe) {
	    super(lbl);
	    set = null;
	    this.cfg = cfg;
	    defval();
	    if(tip != null) {
		tooltip = Text.render(tip).tex();
	    }
	    if(observe){ cfg.observe(this); }
	}

	protected void defval() {
	    a = cfg.get();
	}

	@Override
	public void set(boolean a) {
	    this.a = a;
	    cfg.set(a);
	    if(set != null) {set.accept(a);}
	}

	@Override
	public void destroy() {
	    cfg.unobserve(this);
	    super.destroy();
	}

	@Override
	public void updated(CFG<Boolean> cfg) {
	    a = cfg.get();
	}
    }
    
    public static class CFGHSlider extends HSlider {
	private final CFG<Integer> cfg;
	
	public CFGHSlider(int w, CFG<Integer> cfg, int min, int max) {
	    super(w, min, max, cfg.get());
	    this.cfg = cfg;
	}
	
	@Override
	public void released() {
	    cfg.set(val);
	}
    }

    public class QualityBox extends Dropbox<QualityList.SingleType> {
	protected final CFG<QualityList.SingleType> cfg;

	public QualityBox(int w, int listh, int itemh, CFG<QualityList.SingleType> cfg) {
	    super(w, listh, itemh);
	    this.cfg = cfg;
	    this.sel = cfg.get();
	}

	@Override
	protected QualityList.SingleType listitem(int i) {
	    return QualityList.SingleType.values()[i];
	}

	@Override
	protected int listitems() {
	    return QualityList.SingleType.values().length;
	}

	@Override
	protected void drawitem(GOut g, QualityList.SingleType item, int i) {
	    g.image(item.tex(), Q_TYPE_PADDING);
	}

	@Override
	public void change(QualityList.SingleType item) {
	    super.change(item);
	    if(item != null) {
		cfg.set(item);
	    }
	}
    };
}
