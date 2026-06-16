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

package haven;

import java.util.*;
import java.net.*;
import static haven.PType.*;

public class SessWidget extends AWidget {
    private final Defer.Future<Result> conn;
    private boolean rep = false;

    @RName("sess")
    public static class $_ implements Factory {
	public Widget create(UI ui, Object[] args) {
	    String host = STR.of(args[0]);
	    int port = INT.of(args[1]);
	    byte[] cookie = BYTES.is(args[2]) ? BYTES.of(args[2]) : Utils.hex.dec(STR.of(args[2]));
	    Object[] sargs = Utils.splice(args, 3);
	    return(new SessWidget(host, port, ui.sess, cookie, sargs));
	}
    }

    static class Result {
	final Session sess;
	final Connection.SessionError error;

	Result(Session sess, Connection.SessionError error) {
	    this.sess = sess;
	    this.error = error;
	}
    }

    public SessWidget(String host, int port, Session cursess, byte[] cookie, Object... args) {
	Session.User acct = cursess.user.copy().alias(null);
	boolean encrypt = (cursess.conn instanceof Connection) && ((Connection)cursess.conn).encrypted();
	SocketAddress curaddr = (cursess.conn instanceof Connection) ? ((Connection)cursess.conn).server : null;
	conn = Defer.later(() -> {
	    List<InetSocketAddress> addrs = new ArrayList<>();
	    try {
		for(InetAddress addr : InetAddress.getAllByName(host))
		    addrs.add(new InetSocketAddress(addr, port));
	    } catch(UnknownHostException e) {
		return(new Result(null, new Connection.SessionConnError()));
	    }
	    Bootstrap.preferhost(addrs, curaddr);
	    Connection.SessionError error = null;
	    for(int i = 0; i < addrs.size(); i++) {
		try {
		    return(new Result(Session.connect(addrs.get(i), acct, encrypt, cookie, args), null));
		} catch(Connection.SessionConnError err) {
		    error = err;
		} catch(Connection.SessionError err) {
		    return(new Result(null, err));
		}
	    }
	    return(new Result(null, error));
	});
    }

    public void tick(double dt) {
	super.tick(dt);
	if(!rep && conn.done()) {
	    Result r = conn.get();
	    wdgmsg("res", (r.error == null) ? 0 : r.error.code);
	    rep = true;
	}
    }

    public void uimsg(String name, Object... args) {
	if(name == "exec") {
	    ((RemoteUI)ui.rcvr).ret(conn.get().sess);
	} else {
	    super.uimsg(name, args);
	}
    }

    public void destroy() {
	super.destroy();
	/* XXX: There's a race condition here, but I admit I'm not
	 * sure what can properly be done about it, and it ought at
	 * least be uncommon. */
	if(conn.done()) {
	    Session sess = conn.get().sess;
	    if(sess != null)
		sess.close();
	} else {
	    conn.cancel();
	}
    }
}
