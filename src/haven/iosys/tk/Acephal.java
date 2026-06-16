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
import haven.render.*;
import java.util.*;
import java.lang.annotation.*;

public interface Acephal {
    public static final Config.Variable<String> deftype = Config.Variable.prop("haven.acephal", null);
    public Environment env();
    public void dispose();

    public static interface Factory {
	public Acephal open(String... args);
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

    public static Map<String, Factory> types() {
	return(Found.found());
    }

    public static Acephal instance() {
	return(Found.instance());
    }

    static class Found {
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
				public Acephal open(String... args) {throw(new Unavailable(e));}
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

	private static Map<String, Factory> found = null;
	public static Map<String, Factory> found() {
	    if(found == null) {
		synchronized(Found.class) {
		    if(found == null) {
			found = find();
		    }
		}
	    }
	    return(found);
	}

	private static Acephal instance = null;
	public static Acephal instance() {
	    if(instance == null) {
		synchronized(Found.class) {
		    if(instance == null) {
			if(Utils.eq(deftype.get(), "help")) {
			    List<Map.Entry<String, Factory>> types = new ArrayList<>(found().entrySet());
			    Collections.sort(types, Comparator.comparing(Map.Entry<String, Factory>::getValue, Comparator.comparing(Factory::priority)).reversed());
			    for(Map.Entry<String, Factory> ent : types)
				System.out.printf("name: %-19s priority: %5d\n", ent.getKey(), ent.getValue().priority());
			    System.exit(0);
			} else if(deftype.get() != null) {
			    Factory f = types().get(deftype.get());
			    if(f == null)
				throw(new Unavailable("no such headless name: " + deftype.get()));
			    instance = f.open();
			} else {
			    List<Factory> types = new ArrayList<>(found().values());
			    Collections.sort(types, Comparator.comparing(Factory::priority).reversed());
			    Collection<Throwable> errors = new ArrayList<>();
			    Acephal first = null;
			    for(Factory type : types) {
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
				Unavailable exc = new Unavailable("could find no working headless renderer");
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
