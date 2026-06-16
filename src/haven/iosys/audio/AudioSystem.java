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

package haven.iosys.audio;

import haven.*;
import haven.iosys.*;
import java.util.*;
import java.util.function.*;
import java.lang.annotation.*;
import haven.Audio.*;

public interface AudioSystem {
    public static final Config.Variable<String> audiosystem = Config.Variable.prop("haven.audio-system", null);
    public static final Object SPEC_RATE = "rate";
    public static final Object SPEC_CHANNELS = "ch";
    public static final Object SPEC_SAMPLESIZE = "ssz";
    public static final Object SPEC_BUFSIZE = "buf";
    public static final Object SPEC_SINKDEV = "sink";

    public interface Player {
	public void stop(boolean async);
    }

    public interface SinkLine {
	public Player open(CS stream, int bufsize);
	public Player open(CS stream);
    }

    public interface SinkDevice {
	public String id();
	public String desc();
    }

    public List<SinkDevice> sinkdevs();
    public SinkLine sinkline(Map<?, ?> spec);

    public static int intspec(Map<?, ?> spec, Object key, Integer defval) {
	Object val = spec.get(key);
	if(val == null) {
	    if(defval == null)
		throw(new IllegalArgumentException("required parameter " + key + " missing"));
	    return(defval);
	}
	if(val instanceof Number)
	    return(((Number)val).intValue());
	if(val instanceof String)
	    return(Integer.parseInt((String)val));
	throw(new IllegalArgumentException(key + ": " + val));
    }

    public static interface Factory {
	public AudioSystem open(String... args);
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

    public static AudioSystem instance() {
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
				public AudioSystem open(String... args) {throw(new Unavailable(e));}
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

	private static AudioSystem instance = null;
	public static AudioSystem instance() {
	    if(instance == null) {
		synchronized(Found.class) {
		    if(instance == null) {
			if(Utils.eq(audiosystem.get(), "help")) {
			    List<Map.Entry<String, Factory>> types = new ArrayList<>(found().entrySet());
			    Collections.sort(types, Comparator.comparing(Map.Entry<String, Factory>::getValue, Comparator.comparing(Factory::priority)).reversed());
			    for(Map.Entry<String, Factory> ent : types)
				System.out.printf("name: %-19s priority: %5d\n", ent.getKey(), ent.getValue().priority());
			    System.exit(0);
			} else if(audiosystem.get() != null) {
			    Factory f = types().get(audiosystem.get());
			    if(f == null)
				throw(new Unavailable("no such audio-system name: " + audiosystem.get()));
			    instance = f.open();
			} else {
			    List<Factory> types = new ArrayList<>(found().values());
			    Collections.sort(types, Comparator.comparing(Factory::priority).reversed());
			    Collection<Throwable> errors = new ArrayList<>();
			    AudioSystem first = null;
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
				Unavailable exc = new Unavailable("could find no working audio system");
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
