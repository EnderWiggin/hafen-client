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
import static haven.ffi.dbus.ObjectProxy.*;

public class IfaceProxy {
    public final ObjectProxy obj;
    public final String iface;

    public IfaceProxy(ObjectProxy obj, String iface) {
	this.obj = obj;
	this.iface = iface;
    }

    public Promise<List<Object>> call(List<Type> sig, String member, Object... args) {
	return(obj.call(sig, iface, member, args));
    }

    private static final List<Type> sig_getprop = Arrays.asList(Type.STRING, Type.STRING);
    public Object property(String name) {
	Variant rv = (Variant)obj.call(sig_getprop, IF_PROPS, "Get", iface, name).waitfor().get(0);
	return(rv.value);
    }
}
