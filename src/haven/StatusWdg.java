package haven;

import haven.*;

import java.awt.*;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.SocketException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class StatusWdg extends Widget {

    private static Tex players = Text.renderstroked("Players: ?", Color.WHITE, Color.BLACK).tex();
    private static Tex pingtime = Text.renderstroked("ping: ?", Color.WHITE, Color.BLACK).tex();
    private static String ping = "?";
    private static ThreadGroup tg = new ThreadGroup("StatusUpdaterThreadGroup");
    private static long lastPingUpdate = 0;

    private final static Pattern pattern = Pattern.compile(Config.iswindows ? ".+?=(\\d+)[^ \\d\\s]" : ".+?time=(\\d+\\.?\\d*) ms");
    static {
	startUpdater();
    }

    public StatusWdg() {
	super();
    }

    private static void updatePing() {
	List<String> cmd = new ArrayList<>();
	cmd.add("ping");
	cmd.add(Config.iswindows ? "-n" : "-c");
	cmd.add("1");

	cmd.add("game.havenandhearth.com");

	BufferedReader standardOutput = null;
	try {
	    ProcessBuilder processBuilder = new ProcessBuilder(cmd);
	    Process process = processBuilder.start();

	    standardOutput = new BufferedReader(new InputStreamReader(process.getInputStream()));

	    StringBuilder output = new StringBuilder();
	    String line;
	    while ((line = standardOutput.readLine()) != null) {
		output.append(line);
	    }

	    Matcher matcher = pattern.matcher(output.toString());
	    if (matcher.find()) {
		ping = matcher.group(1);
	    }
	} catch (IOException  ex) {
	    // NOP
	} finally {
	    if (standardOutput != null)
		try {
		    standardOutput.close();
		} catch (IOException e) { // ignored
		}
	}

	if (ping.isEmpty())
	    ping = "?";

	pingtime = Text.renderstroked(String.format("Ping: %s ms", ping), Color.WHITE, Color.BLACK).tex();
    }

    private static void startUpdater() {
	Thread statusupdaterthread = new Thread(tg, () -> {
	    updatePing();
	    while (true) {
		URL url_;
		BufferedReader br = null;
		HttpURLConnection conn = null;

		try {
		    url_ = new URL("http://www.havenandhearth.com/mt/srv-mon");
		    conn = (HttpURLConnection)url_.openConnection();
		    InputStream is = conn.getInputStream();
		    br = new BufferedReader(new InputStreamReader(is));

		    String line;
		    while ((line = br.readLine()) != null) {
			if (line.startsWith("users ")) {
			    String p = line.substring("users ".length());
			    players = Text.renderstroked(String.format("Players: %s", p), Color.WHITE, Color.BLACK).tex();
			}

			// Update ping at least every 5 seconds.
			// This of course might take more than 5 seconds in case there were no new logins/logouts
			// but it's not critical.
			long now = System.currentTimeMillis();
			if (now - lastPingUpdate > 5000) {
			    lastPingUpdate = now;
			    updatePing();
			}

			if (Thread.interrupted())
			    return;
		    }
		} catch (SocketException se) {
		    // don't print socket exceptions when network is unreachable to prevent console spamming on bad connections
		    if (!se.getMessage().equals("Network is unreachable"))
			se.printStackTrace();
		} catch (MalformedURLException mue) {
		    mue.printStackTrace();
		} catch (IOException ioe) {
		    ioe.printStackTrace();
		} finally {
		    try {
			if (br != null)
			    br.close();
		    } catch (IOException ioe) {
		    }
		    if (conn != null)
			conn.disconnect();
		}

		if (Thread.interrupted())
		    return;

		try {
		    Thread.sleep(5000);
		} catch (InterruptedException ex) {
		    return;
		}
	    }
	}, "StatusUpdater");
	statusupdaterthread.start();
    }

    @Override
    public void draw(GOut g) {
	if (!CFG.SHOW_STATS.get()) return;
	g.image(players, Coord.z);
	g.image(pingtime, new Coord(0, players.sz().y));
	int w = players.sz().x;
	if (pingtime.sz().x > w)
	    w = pingtime.sz().x;
	this.sz = new Coord(w,  players.sz().y + pingtime.sz().y + FastText.h);
    }
}