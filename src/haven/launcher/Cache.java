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
import java.nio.channels.*;
import java.util.*;
import javax.net.ssl.*;
import java.security.*;
import java.security.cert.*;
import java.security.interfaces.*;
import java.security.cert.Certificate;
import static haven.launcher.Utils.path;
import static haven.launcher.Utils.pj;
import static haven.launcher.Utils.canWrite;

public class Cache {
    public static final String USER_AGENT;
    private final Path base;

    static {
	StringBuilder buf = new StringBuilder();
	buf.append(String.format("Haven-Launcher/%d.%d", Config.MAJOR_VERSION, Config.MINOR_VERSION));
	String jv = System.getProperty("java.version");
	if((jv != null) && (jv.length() > 0))
	    buf.append(String.format(" Java/%s", jv));
	USER_AGENT = buf.toString();
    }

    private static Path findbase() {
	Path dir = path(".");
	if (!canWrite(dir)) {
	    return findbase2();
	}
	return dir;
    }
    
    private static Path findbase2() {
	try {
	    windows: {
		String path = System.getenv("APPDATA");
		if(path == null)
		    break windows;
		Path appdata = path(path);
		if(!Files.exists(appdata) || !Files.isDirectory(appdata) || !Files.isReadable(appdata) || !Files.isWritable(appdata))
		    break windows;
		Path base = pj(appdata, "Haven Launcher");
		if(!Files.exists(base)) {
		    try {
			Files.createDirectories(base);
		    } catch(IOException e) {
			break windows;
		    }
		}
		return(base);
	    }
	    fallback: {
		String path = System.getProperty("user.home", null);
		if(path == null)
		    break fallback;
		Path home = path(path);
		if(!Files.exists(home) || !Files.isDirectory(home) || !Files.isReadable(home) || !Files.isWritable(home))
		    break fallback;
		Path base = pj(home, ".cache", "haven-launcher");
		if(!Files.exists(base)) {
		    try {
			Files.createDirectories(base);
		    } catch(IOException e) {
			break fallback;
		    }
		}
		return(base);
	    }
	} catch(SecurityException e) {
	}
	throw(new UnsupportedOperationException("Found no reasonable place to store local files"));
    }

    public Cache() {
	this.base = findbase();
    }

    private static Cache global = null;
    public synchronized static Cache get() {
	if(global == null)
	    global = new Cache();
	return(global);
    }

    private static final String safe =
	"ABCDEFGHIJKLMNOPQRSTUVWXYZ" +
	"abcdefghijklmnopqrstuvwxyz" +
	"0123456789-_. ";
    private static String mangle(String el) {
	StringBuilder buf = new StringBuilder();
	for(int i = 0; i < el.length(); i++) {
	    char c = el.charAt(i);
	    if((safe.indexOf(c) < 0) || ((i == 0) && (c == '.'))) {
		int cc = (int)c;
		if(c < 256)
		    buf.append(String.format("%%%02x", (int)c));
		else
		    buf.append(String.format("%%%04x", (int)c));
	    } else {
		buf.append(c);
	    }
	}
	return(buf.toString());
    }

    public Path mangle(URI uri) {
	Path ret = pj(base, "cache", mangle(uri.getScheme()));
	if(uri.getAuthority() != null)
	    ret = pj(ret, mangle(uri.getAuthority()));
	String path = uri.getPath();
	int p = 0;
	while(true) {
	    int n = path.indexOf('/', p);
	    if(n < 0)
		n = path.length();
	    if(n > p)
		ret = pj(ret, mangle(path.substring(p, n)));
	    if(n >= path.length())
		break;
	    p = n + 1;
	}
	if(uri.getQuery() != null)
	    ret = pj(ret, mangle(uri.getQuery()));
	return(ret);
    }

    public Path metafile(URI uri, String var) {
	Path ret = mangle(uri);
	return(ret.resolveSibling("." + ret.getFileName() + "." + var));
    }

    private void addcert(Collection<String> buf, Certificate cert) {
	try {
	    buf.add("dig:sha256:" + Utils.byte2hex(MessageDigest.getInstance("SHA-256").digest(cert.getEncoded())));
	    PublicKey key = cert.getPublicKey();
	    if(key instanceof RSAPublicKey) {
		RSAPublicKey rsakey = (RSAPublicKey)key;
		MessageDigest dig = MessageDigest.getInstance("SHA-256");
		dig.update(rsakey.getPublicExponent().toString().getBytes(Utils.utf8));
		dig.update(new byte[] {':'});
		dig.update(rsakey.getModulus().toString().getBytes(Utils.utf8));
		buf.add("key:rsa:" + Utils.byte2hex(dig.digest()));
	    }
	    if(key instanceof DSAPublicKey) {
		DSAPublicKey dsakey = (DSAPublicKey)key;
		MessageDigest dig = MessageDigest.getInstance("SHA-256");
		DSAParams dsapar = dsakey.getParams();
		dig.update(dsapar.getG().toString().getBytes(Utils.utf8));
		dig.update(new byte[] {':'});
		dig.update(dsapar.getP().toString().getBytes(Utils.utf8));
		dig.update(new byte[] {':'});
		dig.update(dsapar.getQ().toString().getBytes(Utils.utf8));
		dig.update(new byte[] {':'});
		dig.update(dsakey.getY().toString().getBytes(Utils.utf8));
		buf.add("key:dsa:" + Utils.byte2hex(dig.digest()));
	    }
	} catch(CertificateEncodingException e) {
	} catch(NoSuchAlgorithmException e) {
	    throw(new AssertionError(e));
	}
    }

    private static boolean dokludgerepl() {
	/*
	 * XXX?! Windows is dumb in that there exists no good way to
	 * replace files while they're in use. It is, however,
	 * possible to overwrite Jar files in-place even while they're
	 * in use by a running JVM. Doing so is real ugly since the
	 * currently running JVM will certainly not expect them to
	 * change under it, but it's (probably) better than just being
	 * utterly unable to replace Jar files while a previous
	 * instance of the program is running.
	 *
	 * Funnily enough, good ol' Java Web Start apparently had no
	 * issues replacing Jar files currently in use. I do wonder if
	 * it was doing the same thing, or if it had some clever
	 * file-system virtualization going on.
	 */
	String os = System.getProperty("os.name");
	return((os != null) && os.startsWith("Windows"));
    }

    private static void overwrite(Path dst, Path src) throws IOException {
	try(InputStream in = Files.newInputStream(src)) {
	    try(OutputStream out = Files.newOutputStream(dst)) {
		byte[] buf = new byte[65536];
		for(int rv = in.read(buf); rv >= 0; rv = in.read(buf))
		    out.write(buf, 0, rv);
	    }
	}
    }

    public static class FileReplaceException extends IOException implements ErrorMessage {
	public FileReplaceException(Throwable cause) {
	    super("could not replace out-of-date file with newly downloaded file", cause);
	}

	public String usermessage() {
	    return("Could not replace out-of-date file with newly downloaded file. " +
		   "If the program is currently running, please quit it and try again.");
	}
    }

    private static final SslHelper ssl = new SslHelper();
    private Cached update0(Resource res, boolean force) throws IOException {
	URI uri = res.uri;
	try(Status st = Status.current()) {
	    st.messagef("Checking %s...", Utils.basename(uri));
	    Path path = mangle(uri);
	    Path infop = metafile(uri, "info");
	    Path newp = metafile(uri, "new");
	    Path dir = path.getParent();
	    if(!Files.isDirectory(dir))
		Files.createDirectories(dir);
	    FileChannel fp = FileChannel.open(infop, StandardOpenOption.READ, StandardOpenOption.WRITE, StandardOpenOption.CREATE);
	    Properties props = new Properties();
	    Properties nprops = new Properties();
	    nprops.put("source", uri.toString());
	    try(FileLock lk = fp.lock()) {
		fp.position(0);
		props.load(new BufferedReader(new InputStreamReader(Channels.newInputStream(fp), Utils.utf8)));
		/* Set up connection parameters */
		URL url = uri.toURL();
		URLConnection conn = null;
		if(conn == null)
		    conn = ssl.connect(url);
		if(conn == null)
		    conn = uri.toURL().openConnection();
		conn.setConnectTimeout(5000);
		conn.setReadTimeout(5000);
		HttpURLConnection http = (conn instanceof HttpURLConnection) ? ((HttpURLConnection)conn) : null;
		conn.addRequestProperty("User-Agent", USER_AGENT);
		if(res.referrer != null)
		    conn.addRequestProperty("Referer", String.valueOf(res.referrer));
		if(http != null) {
		    http.setUseCaches(false);
		    if(!force && props.containsKey("mtime"))
			http.setRequestProperty("If-Modified-Since", (String)props.get("mtime"));
		}
		conn.connect();
		/* Inspect connection state */
		if(conn instanceof HttpsURLConnection) {
		    Collection<String> certinfo = new ArrayList<>();
		    for(Certificate cert : ((HttpsURLConnection)conn).getServerCertificates())
			addcert(certinfo, cert);
		    nprops.put("tls-certs", String.join(" ", certinfo));
		}
		long bytes = 0, expected = -1;
		try(InputStream in = conn.getInputStream()) {
		    if(http != null) {
			expected = http.getContentLengthLong();
			if(!force && (http.getResponseCode() == HttpURLConnection.HTTP_NOT_MODIFIED)) {
			    return(new Cached(path, props, false));
			}
			if(http.getResponseCode() != HttpURLConnection.HTTP_OK)
			    throw(new IOException("Unexpected HTTP response code: " + http.getResponseCode()));
		    }
		    /* Fetch file */
		    st.messagef("Fetching %s...", Utils.basename(uri));
		    st.transfer(expected, 0);
		    byte[] buf = new byte[65536];
		    try(OutputStream out = Files.newOutputStream(newp)) {
			for(int rv = in.read(buf); rv >= 0; rv = in.read(buf)) {
			    out.write(buf, 0, rv);
			    bytes += rv;
			    st.transfer(expected, bytes);
			}
		    }
		}
		/* Check completion parameters */
		if(http != null) {
		    long clen = http.getContentLengthLong();
		    /* Because, apparently, Java doesn't make this check itself. */
		    if(clen != bytes)
			throw(new IOException("Premature EOF"));
		    String mtime = http.getHeaderField("Last-Modified");
		    if(mtime != null)
			nprops.put("mtime", mtime);
		}
		String ctype = conn.getContentType();
		if(ctype != null)
		    nprops.put("ctype", ctype);
		if(ctype.equals("application/java-archive")) {
		    st.messagef("Verifying %s...", Utils.basename(uri));
		    Collection<String> certinfo = new ArrayList<>();
		    for(Certificate cert : Utils.checkjar(newp, st))
			addcert(certinfo, cert);
		    if(!certinfo.isEmpty())
			nprops.put("jar-certs", String.join(" ", certinfo));
		}
		/* Commit file */
		fp.position(0); fp.truncate(0);
		try {
		    try {
			Files.move(newp, path, StandardCopyOption.ATOMIC_MOVE);
		    } catch(AtomicMoveNotSupportedException e) {
			Files.move(newp, path, StandardCopyOption.REPLACE_EXISTING);
		    }
		} catch(IOException e) {
		    if(!dokludgerepl())
			throw(e);
		    try {
			overwrite(path, newp);
			try {
			    Files.delete(newp);
			} catch(IOException ign) {
			}
		    } catch(IOException e2) {
			e2.addSuppressed(e);
			throw(new FileReplaceException(e2));
		    }
		}
		Writer propout = new BufferedWriter(new OutputStreamWriter(Channels.newOutputStream(fp), Utils.utf8));
		nprops.store(propout, null);
		propout.flush();
		return(new Cached(path, nprops, true));
	    }
	}
    }

    public Cached update(Resource res, boolean force) throws IOException {
	List<IOException> errors = new ArrayList<>();
	for(int retry = 0; retry < 3; retry++) {
	    try {
		return(update0(res, force));
	    } catch(IOException e) {
		errors.add(e);
	    }
	    force = true;
	}
	IOException first = errors.get(0);
	for(int i = 1; i < errors.size(); i++)
	    first.addSuppressed(errors.get(i));
	throw(first);
    }
}
