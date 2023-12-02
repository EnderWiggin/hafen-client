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
import java.util.jar.*;
import java.io.*;
import java.nio.file.*;
import java.net.*;
import java.security.cert.*;

public class Utils {
    public static final java.nio.charset.Charset utf8 = java.nio.charset.Charset.forName("UTF-8");

    public static <E> E pop(Collection<E> c) {
	Iterator<E> i = c.iterator();
	E ret = i.next();
	i.remove();
	return(ret);
    }

    public static char num2hex(int num) {
	if(num < 10)
	    return((char)('0' + num));
	else
	    return((char)('A' + num - 10));
    }

    public static String byte2hex(byte[] in) {
	StringBuilder buf = new StringBuilder();
	for(byte b : in) {
	    buf.append(num2hex((b & 0xf0) >> 4));
	    buf.append(num2hex(b & 0x0f));
	}
	return(buf.toString());
    }

    public static String basename(URI uri) {
	String path = uri.getPath();
	int p = path.lastIndexOf('/');
	if(p < 0)
	    return(path);
	return(path.substring(p + 1));
    }

    public static String[] splitwords(String text) {
	ArrayList<String> words = new ArrayList<String>();
	StringBuilder buf = new StringBuilder();
	String st = "ws";
	int i = 0;
	while(i < text.length()) {
	    char c = text.charAt(i);
	    if(st == "ws") {
		if(!Character.isWhitespace(c))
		    st = "word";
		else
		    i++;
	    } else if(st == "word") {
		if(c == '"') {
		    st = "quote";
		    i++;
		} else if(c == '\\') {
		    st = "squote";
		    i++;
		} else if(Character.isWhitespace(c)) {
		    words.add(buf.toString());
		    buf = new StringBuilder();
		    st = "ws";
		} else {
		    buf.append(c);
		    i++;
		}
	    } else if(st == "quote") {
		if(c == '"') {
		    st = "word";
		    i++;
		} else if(c == '\\') {
		    st = "sqquote";
		    i++;
		} else {
		    buf.append(c);
		    i++;
		}
	    } else if(st == "squote") {
		buf.append(c);
		i++;
		st = "word";
	    } else if(st == "sqquote") {
		buf.append(c);
		i++;
		st = "quote";
	    }
	}
	if(st == "word")
	    words.add(buf.toString());
	if((st != "ws") && (st != "word"))
	    return(null);
	return(words.toArray(new String[0]));
    }

    public static Certificate[] checkjar(Path path, Status prog) throws IOException {
	Set<Certificate> ret = null;
	try(JarFile jar = new JarFile(path.toFile())) {
	    if(jar.getManifest() == null)
		return(new Certificate[0]);
	    byte[] buf = new byte[65536];
	    for(Enumeration<JarEntry> i = jar.entries(); i.hasMoreElements();) {
		JarEntry ent = i.nextElement();
		if(ent.isDirectory())
		    continue;
		if(prog != null)
		    prog.progress();
		try(InputStream st = jar.getInputStream(ent)) {
		    while(st.read(buf, 0, buf.length) >= 0);
		}
		if(ent.getName().startsWith("META-INF"))
		    continue;
		Certificate[] entc = ent.getCertificates();
		if((entc == null) || (entc.length < 1))
		    return(new Certificate[0]);
		if(ret == null) {
		    ret = new HashSet<>(Arrays.asList(entc));
		} else {
		    ret.retainAll(Arrays.asList(entc));
		    if(ret.size() < 1)
			return(new Certificate[0]);
		}
	    }
	}
	if(ret == null)
	    return(new Certificate[0]);
	return(ret.toArray(new Certificate[0]));
    }

    public static Path path(String path) {
	return(FileSystems.getDefault().getPath(path));
    }

    public static Path pj(Path base, String... els) {
	for(String el : els)
	    base = base.resolve(el);
	return(base);
    }

    public static boolean canWrite(Path path) {
	Path tmp = pj(path, String.format("tmp-%d", System.currentTimeMillis()));
	try {
	    Files.createFile(tmp);
	    Files.delete(tmp);
	    return true;
	} catch (IOException e) {
	    return false;
	}
    }
    
    public static boolean is64BitVM() {
	String bits = System.getProperty("sun.arch.data.model", "?");
	if (bits.equals("64")) {
	    return true;
	}
	if (bits.equals("?")) {
	    // probably sun.arch.data.model isn't available
	    // maybe not a Sun JVM?
	    // try with the vm.name property
	    return System.getProperty("java.vm.name")
		    .toLowerCase().contains("64");
	}
	// probably 32bit
	return false;
    }

    static String addQuotes(String s) {
	if (s.contains(" ")) {
	    return String.format("\"%s\"", s);
	}
	return s;
    }

    static void saveRunBat(ProcessBuilder cmd, String name) {
	if (name == null) {
	    return;
	}
	Path dst = pj(getLauncherLocation(), name);
	try (OutputStream out = Files.newOutputStream(dst)) {
	    List<String> commands = new ArrayList<>(cmd.command());
	    String java = commands.get(0);
	    commands.remove(0);
	    out.write(String.format("start \"\" \"%s\" %s", java, String.join(" ", commands)).getBytes());
	} catch (Exception ignored) {
	}
    }
    public static Path getLauncherLocation() {
	Path root = path(".");
	try {
	    URL location = getLocation(Config.class);
	    root = urlToFile(location).getParentFile().toPath();
	} catch (Exception ignored) {
	}
	return root;
    }

    /** Whether the operating system is Windows-based. */
    public static boolean isWindows() {
	return System.getProperty("os.name", "Unknown").startsWith("Win");
    }

    /**
     * Gets the base location of the given class.
     * <p>
     * If the class is directly on the file system (e.g.,
     * "/path/to/my/package/MyClass.class") then it will return the base directory
     * (e.g., "file:/path/to").
     * </p>
     * <p>
     * If the class is within a JAR file (e.g.,
     * "/path/to/my-jar.jar!/my/package/MyClass.class") then it will return the
     * path to the JAR (e.g., "file:/path/to/my-jar.jar").
     * </p>
     *
     * @param c The class whose location is desired.
     * @see Utils#urlToFile(URL) to convert the result to a {@link File}.
     */
    public static URL getLocation(final Class<?> c) {
	if (c == null) return null; // could not load the class

	// try the easy way first
	try {
	    final URL codeSourceLocation =
		    c.getProtectionDomain().getCodeSource().getLocation();
	    if (codeSourceLocation != null) return codeSourceLocation;
	}
	catch (final SecurityException e) {
	    // NB: Cannot access protection domain.
	}
	catch (final NullPointerException e) {
	    // NB: Protection domain or code source is null.
	}

	// NB: The easy way failed, so we try the hard way. We ask for the class
	// itself as a resource, then strip the class's path from the URL string,
	// leaving the base path.

	// get the class's raw resource path
	final URL classResource = c.getResource(c.getSimpleName() + ".class");
	if (classResource == null) return null; // cannot find class resource

	final String url = classResource.toString();
	final String suffix = c.getCanonicalName().replace('.', '/') + ".class";
	if (!url.endsWith(suffix)) return null; // weird URL

	// strip the class's path from the URL string
	final String base = url.substring(0, url.length() - suffix.length());

	String path = base;

	// remove the "jar:" prefix and "!/" suffix, if present
	if (path.startsWith("jar:")) path = path.substring(4, path.length() - 2);

	try {
	    return new URL(path);
	}
	catch (final MalformedURLException e) {
	    e.printStackTrace();
	    return null;
	}
    }

    /**
     * Converts the given {@link URL} to its corresponding {@link File}.
     * <p>
     * This method is similar to calling {@code new File(url.toURI())} except that
     * it also handles "jar:file:" URLs, returning the path to the JAR file.
     * </p>
     *
     * @param url The URL to convert.
     * @return A file path suitable for use with e.g. {@link FileInputStream}
     * @throws IllegalArgumentException if the URL does not correspond to a file.
     */
    public static File urlToFile(final URL url) {
	return url == null ? null : urlToFile(url.toString());
    }

    /**
     * Converts the given URL string to its corresponding {@link File}.
     *
     * @param url The URL to convert.
     * @return A file path suitable for use with e.g. {@link FileInputStream}
     * @throws IllegalArgumentException if the URL does not correspond to a file.
     */
    public static File urlToFile(final String url) {
	String path = url;
	if (path.startsWith("jar:")) {
	    // remove "jar:" prefix and "!/" suffix
	    final int index = path.indexOf("!/");
	    path = path.substring(4, index);
	}
	try {
	    if (isWindows() && path.matches("file:[A-Za-z]:.*")) {
		path = "file:/" + path.substring(5);
	    }
	    return new File(new URL(path).toURI());
	}
	catch (final MalformedURLException e) {
	    // NB: URL is not completely well-formed.
	}
	catch (final URISyntaxException e) {
	    // NB: URL is not completely well-formed.
	}
	if (path.startsWith("file:")) {
	    // pass through the URL as-is, minus "file:" prefix
	    path = path.substring(5);
	    return new File(path);
	}
	throw new IllegalArgumentException("Invalid URL: " + url);
    }
}
