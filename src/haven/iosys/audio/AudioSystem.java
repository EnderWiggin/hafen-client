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

public interface AudioSystem extends Disposable {
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
    public default void dispose() {}

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

    @dolda.jglob.Discoverable
    @Target(ElementType.TYPE)
    @Retention(RetentionPolicy.RUNTIME)
    public static @interface Available {
	String name();
    }

    static final Providers<AudioSystem, Available> prov = new Providers<>("audio-system", audiosystem::get, Available.class, Available::name);
    public static Map<String, Providers.Factory<? extends AudioSystem>> types() {
	return(prov.found());
    }

    public static AudioSystem instance() {
	return(prov.instance());
    }
}
