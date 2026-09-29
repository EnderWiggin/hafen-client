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
import java.util.regex.*;
import java.util.jar.*;
import java.io.*;
import java.nio.file.*;
import java.nio.file.attribute.*;
import java.time.*;
import java.net.*;

public class NativeLib {
    public final Pattern os, arch;
    public final Resource jar;
    public final String prefix;

    public NativeLib(Pattern os, Pattern arch, Resource jar, String subdir) {
	this.os = os;
	this.arch = arch;
	this.jar = jar;
	while(subdir.startsWith("/"))
	    subdir = subdir.substring(1);
	while(subdir.endsWith("/"))
	    subdir = subdir.substring(0, subdir.length() - 1);
	if(subdir.length() > 0)
	    subdir = subdir + "/";
	this.prefix = subdir;
    }

    public boolean use() {
	return(os.matcher(System.getProperty("os.name")).matches() && arch.matcher(System.getProperty("os.arch")).matches());
    }

    public Path extract() throws IOException {
	Path jar = this.jar.update();
	Path dir = this.jar.metafile("lib");
	boolean fresh = false;
	if(!Files.isDirectory(dir)) {
	    fresh = true;
	    Files.createDirectories(dir);
	}
	if(fresh || (Files.getLastModifiedTime(jar).compareTo(Files.getLastModifiedTime(dir)) > 0)) {
	    JarFile fp = new JarFile(jar.toFile());
	    for(Enumeration<JarEntry> i = fp.entries(); i.hasMoreElements();) {
		JarEntry ent = i.nextElement();
		if(ent.isDirectory())
		    continue;
		String nm = ent.getName();
		if(nm.charAt(0) == '.')
		    continue;
		if(!nm.startsWith(prefix))
		    continue;
		nm = nm.substring(prefix.length());
		if(nm.indexOf('/') >= 0)
		    continue;
		try(InputStream in = fp.getInputStream(ent)) {
		    try(OutputStream out = Files.newOutputStream(dir.resolve(nm))) {
			byte[] buf = new byte[65536];
			int rv;
			while((rv = in.read(buf)) >= 0)
			    out.write(buf, 0, rv);
		    }
		}
	    }
	    Files.setLastModifiedTime(dir, FileTime.from(Instant.now()));
	}
	return(dir);
    }
}
