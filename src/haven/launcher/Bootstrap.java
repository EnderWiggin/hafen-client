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

import java.io.*;
import java.nio.file.*;
import java.net.*;
import java.util.*;
import java.util.zip.*;

public class Bootstrap {
    public static void bootstrap(OutputStream out, InputStream cfg) throws IOException {
	URL srcjar;
	try {
	    srcjar = Bootstrap.class.getProtectionDomain().getCodeSource().getLocation();
	} catch(Exception e) {
	    throw(new RuntimeException("Could not locate source Jar file", e));
	}
	ZipOutputStream outjar = new ZipOutputStream(out);
	byte[] buf = new byte[65536];
	Collection<String> seen = new ArrayList<>();
	try(ZipInputStream injar = new ZipInputStream(srcjar.openConnection().getInputStream())) {
	    ZipEntry ent;
	    while((ent = injar.getNextEntry()) != null) {
		if(ent.getName().equals("haven/launcher/bootstrap.hl")) {
		    System.err.println("launcher: warning: removing current bootstrap");
		} else {
		    outjar.putNextEntry(ent);
		    for(int rv = injar.read(buf); rv >= 0; rv = injar.read(buf))
			outjar.write(buf, 0, rv);
		    seen.add(ent.getName().toLowerCase());
		}
		injar.closeEntry();
	    }
	}
	if(!seen.contains("meta-inf/manifest.mf") || !seen.contains("haven/launcher/driver.class"))
	    throw(new RuntimeException("Source Jar file appears corrupt or incomplete"));
	outjar.putNextEntry(new ZipEntry("haven/launcher/bootstrap.hl"));
	for(int rv = cfg.read(buf); rv >= 0; rv = cfg.read(buf))
	    outjar.write(buf, 0, rv);
	outjar.finish();
    }

    private static void usage(PrintStream out) {
	out.println("usage: Bootstrap [-h] [BOOT-CONFIG|-] OUTPUT");
    }

    public static void main(String[] args) {
	PosixArgs opt = PosixArgs.getopt(args, "h");
	if(opt == null) {
	    usage(System.err);
	    System.exit(1);
	}
	for(char c : opt.parsed()) {
	    switch(c) {
	    case 'h':
		usage(System.out);
		System.exit(0);
		break;
	    }
	}
	if(opt.rest.length < 2) {
	    usage(System.err);
	    System.exit(1);
	}
	try {
	    InputStream cfg, cl = null;
	    if(opt.rest[0].equals("-")) {
		cfg = System.in;
	    } else {
		cl = cfg = new BufferedInputStream(Files.newInputStream(Utils.path(opt.rest[0])));
	    }
	    Path outnm = Utils.path(opt.rest[1]), temp = outnm.resolveSibling(outnm.getFileName() + ".new");
	    try {
		boolean done = false;
		try {
		    try(OutputStream out = new BufferedOutputStream(Files.newOutputStream(temp))) {
			bootstrap(out, cfg);
		    }
		    Files.move(temp, outnm, StandardCopyOption.ATOMIC_MOVE);
		    done = true;
		} finally {
		    if(!done)
			Files.delete(temp);
		}
	    } finally {
		if(cl != null)
		    cl.close();
	    }
	} catch(Exception e) {
	    e.printStackTrace();
	    System.exit(1);
	}
    }
}
