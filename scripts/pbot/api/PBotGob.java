package haven.purus.pbot.api;

import java.awt.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.Semaphore;

import haven.Composite;
import haven.*;
import haven.Gob.Type;
import haven.purus.BoundingBox;
import haven.purus.GobColor;
import haven.purus.GobText;
import haven.purus.pathfinder.Pathfinder;
import haven.render.MixColor;

public class PBotGob {

	private final Gob gob;
	private PBotSession pBotSession;

	public static HashMap<String, String> gobWindowMap = new HashMap<String, String>() {{
		put("gfx/terobjs/crate", "Crate");
		put("gfx/terobjs/dframe", "Frame");
		put("gfx/terobjs/kiln", "Kiln");
		put("gfx/terobjs/fineryforge", "Finery Forge");
		put("gfx/terobjs/steelcrucible", "Steelbox");
		put("gfx/terobjs/smelter", "Ore Smelter");
		put("gfx/terobjs/pow", "Fireplace");
		put("gfx/terobjs/oven", "Oven");
		put("gfx/terobjs/cauldron", "Cauldron");
		put("gfx/terobjs/woodbox", "Woodbox");
		put("gfx/terobjs/create", "Crate");
		put("gfx/terobjs/furn/table-stone", "Table");
		put("gfx/terobjs/furn/cottagetable", "Table");
		put("gfx/terobjs/wbasket", "Basket");
		put("gfx/terobjs/chickencoop", "Chicken Coop");
		put("gfx/terobjs/htable", "Herbalist Table");
		put("gfx/terobjs/studydesk", "Study Desk");
		put("gfx/terobjs/cupboard", "Cupboard");
		put("gfx/terobjs/ttub", "Tub");
		put("gfx/terobjs/chest", "Chest");
	}};


	public PBotGob(Gob gob, PBotSession pBotSession) {
		this.pBotSession = pBotSession;
		this.gob = gob;
	}

	public Gob getInternalGob() {
		return gob;
	}

	public Type getGobType() {
		return gob.type;
	}

	/**
	 * Click the gob
	 * @param btn 1 = left, 2 = middle, 3 = right
	 * @param mod Key modifier mask 1 = shift 2 = ctrl 4 = alt
	 * @param meshid can be a door, roasting spit etc.
	 * @param olid gob overlay to click, for example roasting spit
	 */
	public void doClick(int btn, int mod, int meshid, int olid) {
		pBotSession.gui.map.wdgmsg("click", pBotSession.PBotUtils().getCenterScreenCoord(), gob.rc.floor(OCache.posres), btn, mod, 1, (int)gob.id, gob.rc.floor(OCache.posres), olid, meshid);
	}

	/**
	 * Euclidean distance between this and target gob
	 * @param gob target
	 * @return the distance
	 */
	public double dist(PBotGob gob) {
		return gob.getCoords().dist(getCoords());
	}

	/**
	 * Returns rc-coords of the gob
	 * @return Coord object with x and y attributes
	 */
	public Coord2d getCoords() {
		final Coord2d[] ret = {null};
		Semaphore s = new Semaphore(1);
		s.acquireUninterruptibly();
		new Runnable() {
			@Override
			public void run() {
				try {
					ret[0] = Coord2d.z;
					ret[0] = new Coord2d(gob.getc());
				} catch(MCache.LoadingMap lm) {
					if(ret[0] == null) {
						lm.waitfor(this, waiting -> {
						});
						return;
					}
				}
				s.release();
			}
		}.run();
		s.acquireUninterruptibly();
		return ret[0];
	}

	/**
	 * Get gob id of the gob (not unique through sessions)
	 * @return id
	 */
	public long getId() {
		return this.gob.id;
	}

	/**
	 * Check if stockpile is full
	 * @return True if stockpile is full, else false
	 */
	public boolean stockpileIsFull() {
		return gob.getattr(ResDrawable.class).sdt.peekrbuf(0) == 31;
	}

	/**
	 * Itemact with gob, to fill trough with item in hand for example
	 * @param mod 1 = shift, 2 = ctrl, 4 = alt
	 */
	public void itemClick(int mod) {
		pBotSession.gui.map.wdgmsg("itemact", Coord.z, gob.rc.floor(OCache.posres), mod, 0, (int) gob.id, gob.rc.floor(OCache.posres), 0, -1);
	}

	/**
	 * Add cool hovering text above gob
	 * @param text text to add
	 * @param height height that the hext hovers at
	 * @return id id of text used to remove it
	 */
	public int addGobText(String text, int r, int g, int b, int a, int height) {
		Gob.Overlay gt = new Gob.Overlay(gob, new GobText(gob, text, new Color(r, g, b, a), height));
		pBotSession.gui.ui.sess.glob.loader.defer(() -> {synchronized(gob) {
			gob.addol(gt);
		}}, null);
		return gt.id;
	}

	/**
	 * Remove an added hovering text from gob that was added with addGobText
	 * @param id Id of the gobtext
	 */
	public void removeGobText(int id) {
		pBotSession.gui.ui.sess.glob.loader.defer(() -> {synchronized(gob) {
			gob.findol(id).remove(false);
		}}, null);
	}
	/**
	 * Returns the name of the gobs resource file, or null if not found
	 * @return Name of the gob
	 */
	public String getResname() {
		while(true) {
			try {
				if(gob.getres() != null)
					return gob.getres().name;
				else
					return null;
			} catch(Loading l) {
				PBotUtils.sleep(10);
			}
		}
	}

	/**
	 * Get name of window for gob from gobWindowMap
	 * @return Inventory window name
	 */
	public String windowNameForGob() {
		while(true) {
			try {
				if(gob.getres() == null)
					return null;
				else
					return gobWindowMap.get(gob.getres().name);
			} catch(Loading l) {
				PBotUtils.sleep(10);
			}
		}
	}


	/**
	 * Set gob as colored, replaces previous if color already set
	 * @param r Red between 0-255
	 * @param g Green between 0-255
	 * @param b Blue between 0-255
	 * @param a Alpha between 0-255
	 */
	public void setMarked(int r, int g, int b, int a) {
		pBotSession.gui.ui.sess.glob.loader.defer(() -> {synchronized(gob) {
			gob.setattr(new GobColor(gob, new MixColor(r, g, b, a)));
		}}, null);
	}

	/**
	 * Remove color marking
	 */
	public void setUnmarked() {
		pBotSession.gui.ui.sess.glob.loader.defer(() -> {synchronized(gob) {
			gob.delattr(GobColor.class);
		}}, null);
	}

	/**
	 * Click a gob with pathfinder, with given button
	 * @param btn 1 = left click, 3 = right click
	 * @param mod 1 = shift, 2 = ctrl, 4 = alt
	 * @param meshId meshid to click
	 */
	public void pfClick(int btn, int mod, int meshId) {
		Pathfinder.run(gob.rc, gob, btn, mod, meshId,"", pBotSession.gui);
	}

	/**
	 * Click a gob with pathfinder, with given button
	 * @param btn 1 = left click, 3 = right click
	 * @param mod 1 = shift, 2 = ctrl, 4 = alt
	 * @param meshId meshid to click
	 * @param olid overlay id to click for example roasting spit
	 */
	public void pfClick(int btn, int mod, int meshId, int olid) {
		Pathfinder.run(gob.rc, gob, btn, mod, olid, meshId,"", pBotSession.gui);
	}

	/**
	 * Click a gob with pathfinder, with given button
	 * @param btn 1 = left click, 3 = right click
	 * @param mod 1 = shift, 2 = ctrl, 4 = alt
	 * @param action dig, destroy etc to select before the final click
	 */
	public void pfClick(int btn, int mod, List<String> action) {
		Pathfinder.run(gob.rc, gob, btn, mod, -1, String.join(" ", action), pBotSession.gui);
	}

	/**
	 * Click a gob with pathfinder, with given button
	 * @param btn 1 = left click, 3 = right click
	 * @param mod 1 = shift, 2 = ctrl, 4 = alt
	 */
	public void pfClick(int btn, int mod) {
		Pathfinder.run(gob.rc, gob, btn, mod, -1,"", pBotSession.gui);
	}

	/**
	 * Check if the object is moving
	 * @return Returns true if gob is moving, false otherwise
	 */
	public boolean isMoving() {
		if (gob.getv() == 0)
			return false;
		else
			return true;
	}

	/**
	 * Get speed of this gob if it is moving
	 * @return Speed of the gob, -1 if not moving object
	 */
	public double getSpeed() {
		LinMove lm = gob.getLinMove();
		if(lm == null)
			return -1;
		else
			return lm.getv();
	}

	/**
	 * Returns resnames of poses of this gob if it has any poses
	 * @return Resnames of poses
	 */
	public ArrayList<String> getPoses() {
		ArrayList<String> ret = new ArrayList<>();
		Drawable d = gob.getattr(Drawable.class);

		if(d instanceof Composite) {
			Composite comp = (Composite)d;
			for(ResData rd:comp.prevposes) {
				while(true) {
					try {
						ret.add(rd.res.get().name);
						break;
					} catch(Loading l) {
						PBotUtils.sleep(10);
					}
				}
			}
		}
		return ret;
	}

	/**
	 * Get overlays of the gob, get meshId with getOverlyId
	 * @return List containing resnames of the overlays
	 */
	public List<String> getOverlayNames() {
		List<String> ret = new ArrayList<>();
		for (Gob.Overlay ol : gob.ols) {
			if(ol.res != null)
				while(true) {
					try {
						ret.add(ol.res.get().name);
						break;
					} catch(Loading l) {
						PBotUtils.sleep(10);
					}
				}
		}
		return ret;
	}

	/**
	 * Return meshId of the overlay with given resname
	 * @param overlayName Exact match
	 * @return Meshid of the overlay -1 if not found
	 */
	public int getOverlayId(String overlayName) {
		for (Gob.Overlay ol : gob.ols) {
			if(ol.res != null)
			while(true) {
				try {
					if(ol.res.get().name.equals(overlayName)) {
						return ol.id;
					}
					break;
				} catch (Loading l) {
					PBotUtils.sleep(10);
				}
			}
		}
		return -1;
	}

	/**
	 * Sdt may tell information about things such as tanning tub state, crop stage etc.
	 * @return sdt of the gob, -1 if not found
	 */
	public int getSdt(int idx) {
		while(true) {
			try {
				Resource res = gob.getres();
				if(res != null) {
					GAttrib rd = gob.getattr(ResDrawable.class);
					return ((ResDrawable) rd).sdt.peekrbuf(idx);
				} else {
					return -1;
				}
			} catch(Loading l) {
				PBotUtils.sleep(10);
			}
		}
	}

	public long getGridId() {
		while(true) {
			try {
				return pBotSession.gui.ui.sess.glob.map.getgridt(getCoords().floor(MCache.tilesz)).id;
			} catch(MCache.LoadingMap l) {
				try {
					l.waitfor();
				} catch(InterruptedException e) {
					e.printStackTrace();
				}
			}
		}
	}

	// Get boundingbox of this gob, or null if not found
	public BoundingBox getBb() {
		return BoundingBox.getBoundingBox(gob);
	}

	public List<String> getCompEqu() {
		Composite comp = gob.getattr(Composite.class);
		List<String> ret = new ArrayList<>();
		if(comp != null) {
			if(comp.comp.cequ != null) {
				for(Composited.ED ed : comp.comp.cequ) {
					while(true) {
						try {
							ret.add(ed.res.res.get().name);
							break;
						} catch(Loading l) {
							try {
								l.waitfor();
							} catch(InterruptedException e) {
								e.printStackTrace();
							}
						}
					}
				}
			}
		}
		return ret;
	}

	public List<String> getCompMod() {
		Composite comp = gob.getattr(Composite.class);
		List<String> ret = new ArrayList<>();
		if(comp != null) {
			if(comp.comp.cmod != null) {
				for(Composited.MD md : comp.comp.cmod) {
					while(true) {
						try {
							ret.add(md.mod.get().name);
							break;
						} catch(Loading l) {
							try {
								l.waitfor();
							} catch(InterruptedException e) {
								e.printStackTrace();
							}
						}
					}
				}
			}
		}
		return ret;
	}

	public class KinInfo {
		public String name;
		public int group, type;
		KinInfo(String name, int group, int type) {
			this.name = name;
			this.group = group;
			this.type = type;
		}
	}
	public KinInfo getKinInfo() {
		haven.KinInfo kin = gob.getattr(haven.KinInfo.class);
		if(kin == null) {
			return null;
		} else {
			return new KinInfo(kin.name, kin.group, kin.type);
		}
	}

	/**
	 * Is the gob knocked out/dead
	 * @return True if animal is knocked out, false if not
	 */
	public boolean isKnocked() {
		return gob.knocked == Gob.Knocked.TRUE;
	}

	public boolean resourceNameStartsWith(String prefix) {
		String resname = getResname();
		return resname != null && resname.startsWith(prefix);
	}

}
