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
import static haven.launcher.Config.expand;

public class TTYStatus implements Status {
    private static final char[] progc = "|/-\\".toCharArray();
    private static final int INTERVAL = 50;
    private final PrintStream out;
    private String curmsg = "";
    private int prog;
    private long lastprog;

    public TTYStatus(PrintStream out) {
	this.out = out;
    }

    private static PrintStream tty() throws IOException {
	return(new PrintStream(new BufferedOutputStream(Files.newOutputStream(Utils.path("/dev/tty")))));
    }

    public TTYStatus() throws IOException {
	this(tty());
    }

    public void dispose() {
	out.close();
    }

    private void reprint(String text) {
	out.print("\r" + text + "\033[K");
	out.flush();
    }

    public void message(String text) {
	reprint(curmsg = (text + " "));
	prog = 0;
	lastprog = 0;
    }

    public void transfer(long size, long cur) {
	long now = System.currentTimeMillis();
	if((now - lastprog) > INTERVAL) {
	    reprint(String.format("%s(%d%%)", curmsg, (100 * cur) / size));
	    lastprog = now;
	}
    }

    public void progress() {
	long now = System.currentTimeMillis();
	if((now - lastprog) > INTERVAL) {
	    reprint(String.format("%s%c", curmsg, progc[prog++ % progc.length]));
	    lastprog = now;
	}
    }

    public void close() {
	reprint("");
	curmsg = "";
    }

    public boolean command(String[] argv, Config cfg, Config.Environment env) {
	switch(argv[0]) {
	case "title": {
	    if(argv.length < 2)
		throw(new RuntimeException("usage: title TITLE"));
	    String title = expand(argv[1], env);
	    out.printf("Launching %s...\n", title);
	    out.flush();
	    return(true);
	}
	}
	return(false);
    }

    public void error(Throwable exc) {
	exc.printStackTrace(out);
	out.flush();
    }
}
