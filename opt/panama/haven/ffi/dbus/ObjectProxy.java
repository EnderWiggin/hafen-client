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

package haven.ffi.dbus;

import java.util.*;
import haven.*;
import haven.ffi.dbus.DBus.*;
import static haven.PType.*;

public class ObjectProxy {
    public static final String IF_PROPS = "org.freedesktop.DBus.Properties";
    public static final String IF_INTROSPECT = "org.freedesktop.DBus.Introspectable";
    public final DBus bus;
    public final String name, path;

    public ObjectProxy(DBus bus, String name, String path) {
	this.bus = bus;
	this.name = name;
	this.path = path;
    }

    public Promise<List<Object>> call(List<Type> sig, String iface, String member, Object... args) {
	return(bus.call(sig, name, path, iface, member, args));
    }

    private Introspec spec = null;
    public Introspec introspect() {
	if(spec == null) {
	    Promise<List<Object>> call = call(Collections.emptyList(), IF_INTROSPECT, "Introspect");
	    String desc = STR.of(LIST.of(call.waitfor()).get(0));
	    spec = new Introspec(desc);
	}
	return(spec);
    }
}
