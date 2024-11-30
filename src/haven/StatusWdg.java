package haven;

import me.ender.ui.DrinkMeter;

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
    private static long lastUpdateTime = System.currentTimeMillis();
    private static boolean updatePingPending = false;
    private final HttpStatus httpStatus = new HttpStatus(HttpStatus.mond.get());

    private final static Pattern pattern = Pattern.compile(Config.iswindows ? ".+?=(\\d+)[^ \\d\\s]" : ".+?time=(\\d+\\.?\\d*) ms");

    public void tick(double dt) {
	super.tick(dt);

	if(!CFG.SHOW_STATS.get() || System.currentTimeMillis() - lastUpdateTime < 1000) return;

	lastUpdateTime = System.currentTimeMillis();
	players = Text.renderstroked(String.format("Players: %s", httpStatus.users), Color.WHITE, Color.BLACK).tex();

	updatePing();
    }

    protected void added() {
	httpStatus.start();
    }

    public void dispose() {
	httpStatus.quit();
    }

    private static void updatePing() {
	if (updatePingPending) return;
	updatePingPending = true;
	Thread pingUpdaterThread = new Thread(tg, () -> {
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
		if(matcher.find()) {
		    ping = matcher.group(1);
		}
	    } catch (IOException ex) {
		// NOP
	    } finally {
		if(standardOutput != null)
		    try {
			standardOutput.close();
		    } catch (IOException e) { // ignored
		    }
	    }

	    if(ping.isEmpty())
		ping = "?";

	    pingtime = Text.renderstroked(String.format("Ping: %s ms", ping), Color.WHITE, Color.BLACK).tex();
	    updatePingPending = false;
	}, "PingUpdater");
	pingUpdaterThread.start();
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