/*
 *  This file is part of the Haven Java Launcher.
 *  Copyright (C) 2019 Fredrik Tolf <fredrik@dolda2000.com>, and
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

package haven.launcher;

import java.util.*;

public interface Validator {
    public void validate(Cached file) throws ValidationException;

    static class TlsKeyValidator implements Validator {
	final String key;

	TlsKeyValidator(String key) {this.key = key;}

	public void validate(Cached file) {
	    if(file.props.containsKey("tls-certs") &&
	       Arrays.asList(((String)file.props.get("tls-certs")).split(" ")).contains(this.key))
		return;
	    throw(new ValidationException("file not downloaded over tls connection signed with " + key));
	}
    }

    static class JarKeyValidator implements Validator {
	final String key;

	JarKeyValidator(String key) {this.key = key;}

	public void validate(Cached file) {
	    if(file.props.containsKey("jar-certs") &&
	       Arrays.asList(((String)file.props.get("jar-certs")).split(" ")).contains(this.key))
		return;
	    throw(new ValidationException("Jar file not signed with " + key));
	}
    }

    public static Validator parse(String spec) {
	int p = spec.indexOf(':');
	String type, arg;
	if(p < 0) {
	    type = spec;
	    arg = "";
	} else {
	    type = spec.substring(0, p);
	    arg = spec.substring(p + 1);
	}
	switch(type) {
	case "tls-cert":
	    return(new TlsKeyValidator(arg));
	case "jar-cert":
	    return(new JarKeyValidator(arg));
	case "always":
	    return(file -> {});
	default:
	    return(null);
	}
    }
}
