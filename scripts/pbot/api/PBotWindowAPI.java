package haven.purus.pbot.api;

import haven.Coord;
import haven.Window;
import haven.purus.pbot.gui.CustomWindow;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

public class PBotWindowAPI {

	private PBotSession pBotSession;

	PBotWindowAPI(PBotSession pBotSession) {
		this.pBotSession = pBotSession;
	}

	/**
	 * Wait for a window with a specific name to appear
	 * @param name Name of the window
	 * @param timeout Timeout in milliseconds to wait for the window to appear
	 * @return Returns the window or null if not found before timeout expires
	 */
	public PBotWindow waitForWindow(String name, long timeout) {
		int retries = 0;
		do {
			PBotWindow window = getWindow(name);
			if(window != null)
				return window;
			retries++;
			PBotUtils.sleep(25);
		} while(retries * 25L < timeout);
		return null;
	}

	/**
	 * Wait for a window with a specific name to disappear
	 * @param timeout in milliseconds
	 * @param name Name of the window
	 * @return false if the window did not close before the timeout (it may or may not still close in the future)
	 */
	public boolean waitForWindowClose(String name, long timeout) {
		int retries = 0;
		while(getWindow(name) != null) {
			if(retries * 25L >= timeout) {
				return false;
			}
			retries++;
			PBotUtils.sleep(25);
		}
		return true;
	}

	/**
	 * Get a window with name
	 * @param name Name of the window
	 * @return The window or null if not found
	 */
	public PBotWindow getWindow(String name) {
		for(Window w : pBotSession.gui.children(Window.class)) {
			if(w.cap.equals(name))
				return new PBotWindow(w, pBotSession);
		}
		return null;
	}

	/**
	 * Get a windows that match the given regex pattern
	 * @param name Pattern to match window titles to
	 * @return The window or null if not found
	 */
	public List<PBotWindow> getWindows(String name) {
		List<PBotWindow> ret = new ArrayList<>();
		Pattern pat = Pattern.compile(name);
		for(Window w : pBotSession.gui.children(Window.class)) {
			if(pat.matcher(w.cap).matches())
				ret.add(new PBotWindow(w, pBotSession));
		}
		return ret;
	}

	public CustomWindow createWindow(Coord size, String windowCaption, String windowId) {
		return new CustomWindow(size, windowCaption, pBotSession);
	}

}
