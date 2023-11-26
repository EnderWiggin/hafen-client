package haven.purus.pbot;

import haven.Config;
import haven.MainFrame;
import haven.purus.pbot.api.PBotSession;

import java.util.regex.Matcher;

public class ClientStarter {

	public static void main(final String[] args) {
		MainFrame.main(args);
		new Thread(() -> {
			try {
				Thread.sleep(1000);
				((Py4j.PBotScriptLoader) Py4j.server.getPythonServerEntryPoint(new Class[]{Py4j.PBotScriptLoader.class})).start(Config.script.substring(8).replaceAll(Matcher.quoteReplacement("\\"), ".").replaceAll("/", ".").replaceAll(".py", ""), new PBotSession(null));
			} catch(Exception e) {
				e.printStackTrace();
			}
		},"PBot script").start();
	}
}
