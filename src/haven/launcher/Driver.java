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

public class Driver {
    public static void execute(Config cfg) {
	try {
	    Launcher l = cfg.launcher;
	    Status.current().launch(l);
	    l.launch();
	} catch(Exception e) {
	    /* XXX */
	    throw(new RuntimeException(e));
	}
    }

    public static void run(Config cfg) {
	try {
	    while(!cfg.include.isEmpty()) {
		Resource res = Utils.pop(cfg.include);
		if(cfg.included.contains(res.uri))
		    continue;
		cfg.included.add(res.uri);
		Path path = res.update();
		try(InputStream src = Files.newInputStream(path)) {
		    cfg.read(new InputStreamReader(src, Utils.utf8), Config.Environment.from(res));
		}
	    }
	} catch(IOException e) {
	    /* XXX */
	    throw(new RuntimeException(e));
	}
	execute(cfg);
    }

    private static void usage(PrintStream out) {
	out.println("usage: launcher.jar [-hq] [CONFIG-URL|FILE]");
    }

    public static void main(String[] args) {
	try {
	    boolean quiet = false;
	    PosixArgs opt = PosixArgs.getopt(args, "hq");
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
		case 'q':
		    quiet = true;
		    break;
		}
	    }
	    if(!quiet) {
		status: {
		    try {
			Status.use(new TTYStatus());
			break status;
		    } catch(IOException e) {
		    }
		    try {
			Status.use(new AWTStatus());
			break status;
		    } catch(java.awt.HeadlessException e) {
		    }
		}
	    }
	    Config cfg = new Config();
	    if(opt.rest.length > 0) {
		try {
		    if(opt.rest[0].indexOf("://") < 0) {
			Path p = Utils.path(opt.rest[0]);
			try(InputStream src = Files.newInputStream(p)) {
			    cfg.read(new InputStreamReader(src, Utils.utf8), new Config.Environment().rel(p.toUri()));
			}
		    } else {
			URI uri;
			try {
			    uri = new URI(opt.rest[0]);
			} catch(URISyntaxException e) {
			    System.err.printf("launcher: invalid url: %s\n", opt.rest[0]);
			    System.exit(1); return;
			}
			Resource res = new Resource(uri, Collections.emptyList());
			try(InputStream src = Files.newInputStream(res.update())) {
			    cfg.read(new InputStreamReader(src, Utils.utf8), Config.Environment.from(res));
			}
		    }
		} catch(IOException e) {
		    System.err.printf("launcher: could not read %s: %s\n", opt.rest[0], e);
		    System.exit(1); return;
		}
	    } else {
		InputStream src = Driver.class.getResourceAsStream("bootstrap.hl");
		if(src == null) {
		    System.err.println("launcher: no bootstreap config found\n");
		    usage(System.err);
		    System.exit(1);
		}
		try {
		    try {
			cfg.read(new InputStreamReader(src, Utils.utf8), new Config.Environment());
		    } finally {
			src.close();
		    }
		} catch(IOException e) {
		    throw(new AssertionError(e));
		}
	    }
	    run(cfg);
	} catch(Throwable t) {
	    Status.current().error(t);
	}
	System.exit(0);
    }
}
