package haven.purus.pbot;

import haven.*;
import haven.purus.BetterWindow;
import haven.purus.Config;
import haven.purus.pbot.api.PBotSession;

import javax.imageio.ImageIO;
import java.awt.event.KeyEvent;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;

public class PBotWindow extends BetterWindow {

	private static final Tex defaultDirIcon = Resource.loadtex("hud/script/pbotdir");
	private static final Tex defaultScriptIcon = Resource.loadtex("hud/script/pbot");
	private static final Tex setKeyIcon = Resource.loadtex("hud/script/keyset");
	public static final ConcurrentHashMap<String, KeyBinding> keyBindings = new ConcurrentHashMap<>();

	private String curPath = "scripts";

	private ArrayList<PBotScriptEntry> entries = new ArrayList<>();

	UI.Grab grab;
	PBotScriptEntry dragged;

	public PBotWindow() {
		super(UI.scale(200,300), "PBot Scripts");
		loadEntries();
		add(new Listbox<PBotScriptEntry>(UI.scale(200), 15, UI.scale(20)) {

			@Override
			protected PBotScriptEntry listitem(int i) {
				return entries.get(i);
			}

			@Override
			public Object tooltip(Coord c, Widget prev) {
				if(c.x > UI.scale(20) && c.x < UI.scale(180)) {
					if(itemat(c) != null && itemat(c).tooltip != null)
						return RichText.render(itemat(c).tooltip, 0);
				}
				return null;
			}

			@Override
			protected int listitems() {
				return entries.size();
			}

			@Override
			protected void drawitem(GOut g, PBotScriptEntry item, int i) {
				item.draw(g);
			}

			@Override
			protected void itemclick(PBotScriptEntry item, int button) {
				if(button == 1) {
					useEntry(item);
				}
				super.itemclick(item, button);
			}

			@Override
			public boolean mousedown(Coord c, int button) {
				if(button == 1) {
					PBotScriptEntry item = itemat(c);
					if(item != null) {
						if(c.x <= UI.scale(20)) {
							dragged = item;
							grab = ui.grabmouse(this);
							return true;
						} else if(c.x >= UI.scale(180)) {
							Widget w = gameui().getchild(PBotScriptKeybindWnd.class);
							if(w != null)
								w.reqdestroy();
							gameui().add(new PBotScriptKeybindWnd(item));
							return true;
						}
					}
				}
				return super.mousedown(c, button);
			}

			@Override
			public boolean mouseup(Coord c, int button) {
				if(button == 1 && grab != null) {
					ui.dropthing(ui.root, ui.mc, dragged);
					grab.remove();
					grab = null;
					dragged = null;
				}
				return super.mouseup(c, button);
			}
		});
	}

	public static KeyBinding getKeybinding(String path) {
		keyBindings.putIfAbsent(path, KeyBinding.get("kb_pbotscript_" + path, KeyMatch.nil));
		return keyBindings.get(path);
	}


	public class PBotScriptKeybindWnd extends BetterWindow {

		public PBotScriptKeybindWnd(PBotScriptEntry entry) {
			super(UI.scale(200, 30), "Set PBot script hotkey");
			if(!Config.scriptsKeybinded.val.contains(entry.scriptFile.getPath())) {
				Config.scriptsKeybinded.val.add(entry.scriptFile.getPath());
				Config.scriptsKeybinded.setVal(Config.scriptsKeybinded.val);
			}

			add(new SetButton(UI.scale(200), getKeybinding(entry.scriptFile.getPath())));
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
			public Object tooltip(Coord c, Widget prev) {
				return(OptWnd.kbtt.tex());
			}
		}

		@Override
		public void wdgmsg(Widget sender, String msg, Object... args) {
			if(sender == this)
				reqdestroy();
			super.wdgmsg(sender, msg, args);
		}
	}

	@Override
	public void draw(GOut g) {
		super.draw(g);
		if(dragged != null) {
			Tex dt = dragged.icon;
			ui.drawafter(new UI.AfterDraw() {
				public void draw(GOut g) {
					g.image(dt, ui.mc.add(-10, -10), UI.scale(20, 20));
				}
			});
		}
	}

	public void useEntry(PBotScriptEntry item) {
		if(item.directory) {
			curPath = item.scriptFile.getPath();
			loadEntries();
			this.show();
		} else {
			new Thread(() -> {
				try {
					((Py4j.PBotScriptLoader) Py4j.server.getPythonServerEntryPoint(new Class[]{Py4j.PBotScriptLoader.class})).start(item.scriptFile.getPath().substring(8).replaceAll(Matcher.quoteReplacement("\\"), ".").replaceAll("/", ".").replaceAll(".py", ""), new PBotSession(gameui()));
				} catch(Exception e) {
					e.printStackTrace();
				}
			},"PBot script").start();
		}
	}

	public void loadEntries() {
		entries.clear();
		File dir = new File(curPath);
		if(!dir.isDirectory()) {
			curPath = "scripts";
			loadEntries();
			return;
		}
		if(!curPath.equals("scripts"))
			entries.add(new PBotScriptEntry(new File(curPath).getParentFile(), "../"));
		for(File f : dir.listFiles()) {
			if(f.getName().startsWith("__") ||  f.equals(new File("scripts/loader.py")) || f.equals(new File("scripts/py4j")) || (!f.isDirectory() && !f.getName().endsWith(".py")))
				continue;
			entries.add(new PBotScriptEntry(f));
		}
		entries.sort((b, a) -> {
			int ret = Boolean.compare(a.directory, b.directory);
			if(ret == 0)
				ret = b.name.compareTo(a.name);
			return ret;
		});
	}

	public static class PBotScriptEntry extends Widget {

		public String name;
		public File scriptFile;
		public boolean directory = false;
		public Tex icon;
		public String tooltip = null;

		public PBotScriptEntry(File scriptFile) {
			this.scriptFile = scriptFile;
			if(scriptFile.isDirectory())
				directory = true;
			File iconF = new File(scriptFile.getPath().replaceFirst(".py", "") + ".png");
			if(!iconF.exists()) {
				defaultIcon();
			} else {
				try {
					icon = new TexI(ImageIO.read(iconF));
				} catch(IOException e) {
					defaultIcon();
					e.printStackTrace();
				}
			}
			if(!directory) {
				try {
					List<String> lines = Files.readAllLines(scriptFile.toPath());
					StringBuilder sb = new StringBuilder();
					for(String line : lines) {
						if(line.length() == 0 || line.charAt(0) != '#')
							break;
						if(sb.length() > 0)
							sb.append('\n');
						sb.append(line.substring(1));
					}
					if(sb.length() > 0)
						tooltip = sb.toString();
				} catch(IOException e) {
					e.printStackTrace();
				}
			}
			this.name = scriptFile.getName().replaceFirst(".py", "");
		}

		private void defaultIcon() {
			if(directory) {
				icon = defaultDirIcon;
			} else {
				icon = defaultScriptIcon;
			}
		}

		@Override
		public void draw(GOut g) {
			g.image(icon, Coord.z, UI.scale(20, 20));
			g.image(setKeyIcon, UI.scale(180, 0), UI.scale(20, 20));
			g.atext(name, UI.scale(25, 0), 0, -0.5);
			super.draw(g);
		}

		@Override
		public Object tooltip(Coord c, Widget prev) {
			return "testi";
		}

		public PBotScriptEntry(File scriptFile, String name) {
			this(scriptFile);
			this.name = name;
		}
	}

	@Override
	public void wdgmsg(Widget sender, String msg, Object... args) {
		if((sender == this) && msg.equals("close")) {
			this.hide();
		}
		super.wdgmsg(sender, msg, args);
	}
}
