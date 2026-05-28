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

public interface Status extends CommandHandler, AutoCloseable {
    public void message(String text);
    public default void messagef(String fmt, Object... args) {message(String.format(fmt, args));}
    public void transfer(long size, long cur);
    public void progress();
    public void error(Throwable exc);

    public default void launch(Launcher l) {}

    public default void close() {}
    public default void dispose() {}

    static final ThreadLocal<Status> current = new ThreadLocal<>();
    public static Status current() {
	Status ret = current.get();
	return((ret == null) ? dummy : ret);
    }
    public static void use(Status st) {
	Status cur = current.get();
	current.set(st);
	if(cur != null)
	    cur.dispose();
    }

    public static final Status dummy = new Status() {
	public void message(String text) {}
	public void transfer(long size, long cur) {}
	public void progress() {}
	public boolean command(String[] argv, Config cfg, Config.Environment env) {return(false);}
	public void error(Throwable exc) {
	    exc.printStackTrace();
	}
    };
}
