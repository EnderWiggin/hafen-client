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

package haven.iosys.tk;

import haven.*;
import haven.iosys.*;
import java.lang.annotation.*;
import java.util.*;
import java.io.*;
import java.awt.image.BufferedImage;

public interface Toolkit {
    public static final Config.Variable<String> toolkit = Config.Variable.prop("haven.toolkit", null);

    public default Collection<Monitor> monitors() {return(Collections.emptyList());}
    public Cursor.Caps cursorcaps();
    public Cursor makecursor(BufferedImage img, Coord hotspot);
    public Windeye window();

    public void dispose();
    public String description();

    public default FilePicker.Factory picker() {
	return(FilePicker.nil);
    }
    public default void browse(java.net.URI location) throws IOException {
	throw(new IOException("No web browser available."));
    }

    public static interface Factory {
	public Toolkit open(String... args);
	public default int priority() {return(0);}
	public default boolean experimental() {return(false);}
	public default boolean autouse() {return(true);}
    }

    @dolda.jglob.Discoverable
    @Target(ElementType.TYPE)
    @Retention(RetentionPolicy.RUNTIME)
    public static @interface Available {
	String name();
    }

    public static interface Event {
    }

    public static interface CloseRequest extends Event {
    }

    public static interface KeyEvent extends Event {
	public String string();
	public Key key();
	public Set<Key.Mod> mods();
    }
    public static interface KeyDownEvent extends KeyEvent {}
    public static interface KeyUpEvent extends KeyEvent {}

    public static interface MouseEvent extends Event {
	public Coord wndc();
	public Set<MouseBtn> held();
	public Set<Key.Mod> mods();
    }
    public static interface MouseButtonEvent extends MouseEvent {
	public MouseBtn button();
    }
    public static interface MouseDownEvent extends MouseButtonEvent {}
    public static interface MouseUpEvent extends MouseButtonEvent {}
    public static interface MouseMoveEvent extends MouseEvent {}
    public static interface MouseWheelEvent extends MouseEvent {
	public static enum Axis {
	    VERT, HORIZ;
	}

	public Axis axis();
	public int amount();
	public double subamount();
    }

    public static interface EventListener {
	public void event(Event ev);
    }

    public static Map<String, Toolkit.Factory> toolkits() {
	return(Found.toolkits());
    }

    public static Toolkit instance() {
	return(Found.instance());
    }

    class Found {
	private static Map<String, Factory> find() {
	    Map<String, Factory> ret = new HashMap<>();
	    ClassLoader loader = Available.class.getClassLoader();
	    for(String clnm : dolda.jglob.Loader.get(Available.class).names()) {
		try {
		    Class<?> cl;
		    try {
			cl = loader.loadClass(clnm);
		    } catch(ClassNotFoundException e) {
			continue;
		    }
		    String nm = cl.getAnnotation(Available.class).name();
		    Factory fac;
		    try {
			fac = (Factory)Utils.invoke(cl.getDeclaredMethod("get"), null);
		    } catch(NoSuchMethodException e) {
			throw(new AssertionError(e));
		    } catch(Unavailable | LinkageError e) {
			fac = new Factory() {
				public Toolkit open(String... args) {throw(new Unavailable(e));}
				public int priority() {return(-1000);}
			    };
		    }
		    ret.put(nm, fac);
		} catch(UnsupportedClassVersionError e) {
		    continue;
		}
	    }
	    return(ret);
	}

	private static Map<String, Factory> toolkits;
	static Map<String, Factory> toolkits() {
	    if(toolkits == null) {
		synchronized(Found.class) {
		    if(toolkits == null) {
			toolkits = find();
		    }
		}
	    }
	    return(toolkits);
	}

	private static Toolkit instance = null;
	public static Toolkit instance() {
	    if(instance == null) {
		synchronized(Found.class) {
		    if(instance == null) {
			if(Utils.eq(toolkit.get(), "help")) {
			    List<Map.Entry<String, Factory>> toolkits = new ArrayList<>(toolkits().entrySet());
			    Collections.sort(toolkits, Comparator.comparing(Map.Entry<String, Factory>::getValue, Comparator.comparing(Factory::priority)).reversed());
			    for(Map.Entry<String, Factory> ent : toolkits)
				System.out.printf("name: %-19s priority: %5d%s\n", ent.getKey(), ent.getValue().priority(), ent.getValue().autouse() ? "" : " (manual only)");
			    System.exit(0);
			} else if(toolkit.get() != null) {
			    Factory f = toolkits().get(toolkit.get());
			    if(f == null)
				throw(new Unavailable("no such toolkit: " + toolkit.get()));
			    instance = f.open();
			} else {
			    List<Factory> toolkits = new ArrayList<>(toolkits().values());
			    Collections.sort(toolkits, Comparator.comparing(Factory::priority).reversed());
			    Collection<Throwable> errors = new ArrayList<>();
			    Toolkit first = null;
			    for(Factory type : toolkits) {
				if(!type.autouse() || (type.experimental() && !Config.exp.get()))
				    continue;
				try {
				    first = type.open();
				    break;
				} catch(Unavailable e) {
				    errors.add(e);
				}
			    }
			    if(first == null) {
				Unavailable exc = new Unavailable("could find no working toolkit");
				errors.forEach(exc::addSuppressed);
				throw(exc);
			    }
			    instance = first;
			}
		    }
		}
	    }
	    return(instance);
	}
    }
}
