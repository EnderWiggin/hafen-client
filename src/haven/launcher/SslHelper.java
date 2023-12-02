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
import java.net.*;
import java.security.*;
import java.security.cert.*;
import java.security.cert.Certificate;
import javax.net.ssl.*;
import java.nio.channels.SocketChannel;
import java.nio.channels.UnresolvedAddressException;

public class SslHelper {
    private SSLContext ctx = null;
    private SSLSocketFactory sfac = null;
    private HostnameVerifier ver = null;

    public SslHelper() {
    }

    private synchronized SSLContext ctx() {
	if(ctx == null) {
	    TrustManagerFactory tmf;
	    KeyManagerFactory kmf;
	    try {
		ctx = SSLContext.getInstance("TLS");
		ctx.init(null, new TrustManager[] {new X509TrustManager() {
			public void checkClientTrusted(X509Certificate[] certs, String type) {
			    throw(new RuntimeException());
			}
			public void checkServerTrusted(X509Certificate[] certs, String type) {
			    /* Always return. */
			}
			public X509Certificate[] getAcceptedIssuers() {
			    return(new X509Certificate[0]);
			}
		    }}, new SecureRandom());
	    } catch(NoSuchAlgorithmException e) {
		throw(new Error(e));
	    } catch(KeyManagementException e) {
		throw(new RuntimeException(e));
	    }
	}
	return(ctx);
    }

    private synchronized SSLSocketFactory sfac() {
	if(sfac == null)
	    sfac = ctx().getSocketFactory();
	return(sfac);
    }

    public HttpsURLConnection connect(URL url) throws IOException {
	if(!url.getProtocol().equals("https"))
	    return(null);
	HttpsURLConnection conn = (HttpsURLConnection)url.openConnection();
	conn.setSSLSocketFactory(sfac());
	if(ver != null)
	    conn.setHostnameVerifier(ver);
	return(conn);
    }

    public HttpsURLConnection connect(String url) throws IOException {
	return(connect(new URL(url)));
    }

    public SslHelper ignorename() {
	ver = new HostnameVerifier() {
		public boolean verify(String hostname, SSLSession sess) {
		    return(true);
		}
	    };
	return(this);
    }
}
