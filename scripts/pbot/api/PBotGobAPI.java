package haven.purus.pbot.api;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Pattern;

import haven.*;
import haven.purus.pbot.api.PBotUtils.AreaReturn;
import haven.purus.pbot.api.callback.GobCallback;

public class PBotGobAPI {
	PBotSession pBotSession;

	PBotGobAPI(PBotSession pBotSession) {
		this.pBotSession = pBotSession;
	}

	/**
	 * Get gobs matching the specific regex pattern
	 * @param resname regex pattern
	 * @return List of PBotGobs with resname matching the given regex pattern
	 */
	public List<PBotGob> getGobsByResname(String resname) {
		List<PBotGob> ret = new ArrayList<>();
		Pattern pat = Pattern.compile(resname);
		List<Gob> gobs = new ArrayList<>();
		synchronized(pBotSession.gui.ui.sess.glob.oc) {
			pBotSession.gui.ui.sess.glob.oc.forEach(gobs::add);
		}
		for(Gob gob : gobs) {
			Resource res;
			while(true) {
				try {
					res = gob.getres();
					break;
				} catch(Loading l) {
					PBotUtils.sleep(10);
				}
			}
			if(res != null && pat.matcher(res.name).matches())
				ret.add(new PBotGob(gob, pBotSession));
		}
		return ret;
	}

	/**
	 * Get the gob player if it exists
	 * @return Player gob or null if not found
	 */
	public PBotGob getPlayer() {
		Gob gob = pBotSession.gui.map.player();
		if(gob == null)
			return null;
		else
			return new PBotGob(gob, pBotSession);
	}

	/**
	 * Get closest gob matching the specific regex pattern
	 * @param resname regex pattern
	 * @return Closest PBotGob from player with resname matching the given regex pattern
	 */
	public PBotGob getClosestGobByResname(String resname) {
		double bestDistance = Double.MAX_VALUE;
		PBotGob bestGob = null;
		Pattern pat = Pattern.compile(resname);
		List<Gob> gobs = new ArrayList<>();
		synchronized(pBotSession.gui.ui.sess.glob.oc) {
			pBotSession.gui.ui.sess.glob.oc.forEach(gobs::add);
		}
		for(Gob gob : gobs) {
			if(gob.isPlayer())
				continue;
			Resource res;
			while(true) {
				try {
					res = gob.getres();
					break;
				} catch(Loading l) {
					PBotUtils.sleep(10);
				}
			}
			PBotGob pl = getPlayer();
			if(pl == null)
				continue;
			PBotGob pgob = new PBotGob(gob, pBotSession);
			if(res != null && res.name.startsWith("gfx/kritter/horse/") && pgob.dist(pl) == 0)
				continue;
			if(res != null && pat.matcher(res.name).matches() && pgob.dist(pl) < bestDistance) {
				bestDistance = pgob.dist(pl);
				bestGob = pgob;
			}
		}
		return bestGob;
	}

	/**
	 * Returns gob with exactly the given coords or null if not found
	 * @param x x-coordinate of the gob
	 * @param y y-coordinate of the gob
	 * @return Gob with coordinates or null
	 */
	public PBotGob getGobWithCoords(double x, double y) {
		while(true) {
			try {
				synchronized(pBotSession.gui.ui.sess.glob.oc) {
					for(Gob gob : pBotSession.gui.ui.sess.glob.oc) {
						if(Math.abs(gob.getc().x - x) < 0.01 && Math.abs(gob.getc().y - y) < 0.01)
							return new PBotGob(gob, pBotSession);
					}
				}
			} catch(Loading l) {
				try {
					l.waitfor();
				} catch(InterruptedException e) {
					e.printStackTrace();
				}
				continue;
			}
			return null;
		}
	}

	public boolean gobExists(PBotGob gob) {
		return findGobById(gob.getId()) != null;
	}

	/**
	 * Find object by ID
	 * @param id ID of the object to look for
	 * @return Object, or null if not found
	 */
	public PBotGob findGobById(long id) {
		Gob gob = pBotSession.gui.ui.sess.glob.oc.getgob(id);
		if(gob == null)
			return null;
		else
			return new PBotGob(pBotSession.gui.ui.sess.glob.oc.getgob(id), pBotSession);
	}

	@Deprecated
	public void selectGob(Callback cb) {
		selectGob((GobCallback) cb::callback);
	}

	/**
	 * Used to register gob callbacks see examples/gobname.py for usage
	 * @param cb callback
	 */
	public void selectGob(GobCallback cb) {
		pBotSession.PBotUtils().sysMsg("To select gob, alt + left click");
		synchronized(pBotSession.gui.map.gobCbQueue) {
			pBotSession.gui.map.gobCbQueue.add(new Pair<>(cb, pBotSession));
		}
	}

	// Sorts coordinate array to efficient zig-zag-like sequence for farming etc.
	private static class CoordSort implements Comparator<PBotGob> {
		public int compare(PBotGob a, PBotGob b) {
			if (a.getCoords().floor().x == b.getCoords().floor().x) {
				if (a.getCoords().floor().x % 2 == 0)
					return Integer.compare(b.getCoords().floor().y, a.getCoords().floor().y);
				else
					return Integer.compare(a.getCoords().floor().y, b.getCoords().floor().y);
			} else
				return Integer.compare(a.getCoords().floor().x, b.getCoords().floor().x);
		}
	}

	public ArrayList<PBotGob> gobsInArea(AreaReturn area) {
		Coord a = area.getA();
		Coord b = area.getB();
		return gobsInArea(a.x, a.y, b.x, b.y);
	}

	/**
	 * Returns a list of gobs in the rectangle between A and B points
	 * @param ax x-coord of A point
	 * @param ay y-coord of A point
	 * @param bx x-coord of B point
	 * @param by y-coord of B point
	 * @return List of gobs in the area, sorted to zig-zag pattern
	 */
	public ArrayList<PBotGob> gobsInArea(double ax, double ay, double bx, double by) {
		// Initializes list of crops to harvest between the selected coordinates
		ArrayList<PBotGob> gobs = new ArrayList<PBotGob>();
		double bigX = Math.max(ax, bx);
		double smallX = Math.min(ax, bx);
		double bigY = Math.max(ay, by);
		double smallY = Math.min(ay, by);
		synchronized(pBotSession.gui.ui.sess.glob.oc) {
			for(Gob gob : pBotSession.gui.ui.sess.glob.oc) {
				if(gob.rc.x <= bigX && gob.rc.x >= smallX && gob.getres() != null && gob.rc.y <= bigY
						&& gob.rc.y >= smallY) {
					gobs.add(new PBotGob(gob, pBotSession));
				}
			}
		}
		gobs.sort(new CoordSort());
		return gobs;
	}

	/**
	 * Returns a list of gobs in the circle with radius around player
	 * @param radius of gob search circle around player
	 * @return List of gobs in the circle
	 */
	public List<PBotGob> findGobsInPlayerProximity(double radius) {
		Gob playerGob = pBotSession.gui.map.player();
		if (playerGob == null) {
			return new ArrayList<>();
		}

		List<PBotGob> resultGobs = new ArrayList<>();

		synchronized (pBotSession.gui.ui.sess.glob.oc) {
			for (Gob gob : pBotSession.gui.ui.sess.glob.oc) {
				if (gob.rc.dist(playerGob.rc) < radius) {
					resultGobs.add(new PBotGob(gob, pBotSession));
				}
			}
		}

		return resultGobs;
	}

	public PBotGob getPlayerGob() {
		return new PBotGob(pBotSession.gui.map.player(), pBotSession);
	}

}
