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
import java.io.*;
import java.net.*;

public class Config {
    public static final int MAJOR_VERSION = 1;
    public static final int MINOR_VERSION = 4;
    public final Collection<Resource> include = new ArrayList<>();
    public final Collection<URI> included = new HashSet<>();
    public final Collection<URI> exts = new HashSet<>();
    public final Collection<CommandHandler> mods = new ArrayList<>();
    public Launcher launcher = new JavaLauncher();

    public static class Environment {
	public static final URI opaque = URI.create("urn:nothing");
	public Collection<Validator> val = Collections.emptyList();
	public Map<String, String> par = Collections.emptyMap();
	public URI rel = opaque, src = null;

	public Environment val(Collection<Validator> val) {this.val = val; return(this);}
	public Environment par(Map<String, String> par) {this.par = par; return(this);}
	public Environment rel(URI rel) {this.rel = rel; return(this);}
	public Environment src(URI src) {this.src = src; return(this);}

	public static Environment from(Resource res) {
	    return(new Environment().val(res.val).rel(res.uri).src(res.uri));
	}
    }

    public static int iparcmp(String a, String b) {
	int x, y;
	try {x = Integer.parseInt(a);} catch(NumberFormatException e) {x = Integer.MIN_VALUE;}
	try {y = Integer.parseInt(b);} catch(NumberFormatException e) {y = Integer.MIN_VALUE;}
	return((x < y) ? -1 : (x > y) ? 1 : 0);
    }

    public static List<?> verparse(String ver) {
	List<Object> ret = new ArrayList<>();
	int p = 0;
	while(p < ver.length()) {
	    char c = ver.charAt(p++);
	    if((c >= '0') && (c <= '9')) {
		int n = c - '0';
		while(p < ver.length()) {
		    c = ver.charAt(p);
		    if(!((c >= '0') && (c <= '9')))
			break;
		    p++;
		    n = (n * 10) + (c - '0');
		}
		ret.add(n);
	    } else {
		StringBuilder buf = new StringBuilder();
		buf.append(c);
		while(p < ver.length()) {
		    c = ver.charAt(p);
		    if((c >= '0') && (c <= '9'))
			break;
		    p++;
		    buf.append(c);
		}
		ret.add(buf.toString());
	    }
	}
	return(ret);
    }

    public static int vparcmp(String a, String b) {
	List<?> x = verparse(a), y = verparse(b);
	int p = 0;
	while((p < x.size()) && (p < y.size())) {
	    Object j = x.get(p), k = y.get(p);
	    p++;
	    if((j instanceof Integer) && (k instanceof String)) {
		return(-1);
	    } else if((j instanceof String) && (k instanceof Integer)) {
		return(1);
	    } else if((j instanceof Integer) && (k instanceof Integer)) {
		int c = ((Integer)j).compareTo((Integer)k);
		if(c != 0) return(c);
	    } else if((j instanceof String) && (k instanceof String)) {
		int c = ((String)j).compareTo((String)k);
		if(c != 0) return(c);
	    }
	}
	if(x.size() > p)
	    return(1);
	else if(y.size() > p)
	    return(-1);
	return(0);
    }

    public static String expand(String s, Environment env) {
	StringBuilder buf = new StringBuilder();
	int p = 0;
	while(p < s.length()) {
	    char c = s.charAt(p++);
	    if(c == '$') {
		if(p >= s.length())
		    throw(new RuntimeException("unexpected expansion at end-of-line: " + s));
		char x = s.charAt(p++);
		if(x == '$') {
		    buf.append('$');
		} else if(x == '{') {
		    int p2 = s.indexOf('}', p);
		    if(p2 < 0)
			throw(new RuntimeException("unterminated parameter expansion: " + s));
		    String par = s.substring(p, p2);
		    p = p2 + 1;
		    if(par.startsWith("p:")) {
			buf.append(System.getProperty(par.substring(2), ""));
		    } else {
			buf.append(env.par.getOrDefault(par, ""));
		    }
		    /*
		} else if(x == 's') {
		    char d = s.charAt(p++);
		    int p2 = s.indexOf(d, p), p3 = s.indexOf(d, p2 + 1), p4 = s.indexOf(d, p3 + 1);
		    if((p2 < 0) || (p3 < 0) || (p4 < 0))
			throw(new RuntimeException("unterminated substitution expansion: " + s));
		    String pat = s.substring(p, p2)
		    p = p4 + 1;
		    */
		} else {
		    throw(new RuntimeException("unknown expansion `" + x + "': " + s));
		}
	    } else {
		buf.append(c);
	    }
	}
	return(buf.toString());
    }

    private void when(String[] words, Environment env) {
	int a = 1;
	while(true) {
	    if(a >= words.length)
		throw(new RuntimeException("unterminated `when' stanza: " + Arrays.asList(words)));
	    String w = words[a++];
	    if(w.equals(":")) {
		break;
	    } else if(w.equals("!")) {
		if(a >= words.length) throw(new RuntimeException("unexpected `when' operator at end-of-line: " + Arrays.asList(words)));
		if(!expand(words[a++], env).equals(""))
		    return;
	    } else if(w.equals("==")) {
		if(a >= words.length - 1) throw(new RuntimeException("unexpected `when' operator at end-of-line: " + Arrays.asList(words)));
		if(!expand(words[a++], env).equals(expand(words[a++], env)))
		    return;
	    } else if(w.equals("!=")) {
		if(a >= words.length - 1) throw(new RuntimeException("unexpected `when' operator at end-of-line: " + Arrays.asList(words)));
		if(expand(words[a++], env).equals(expand(words[a++], env)))
		    return;
	    } else if(w.startsWith("~=")) {
		if(a >= words.length - 1) throw(new RuntimeException("unexpected `when' operator at end-of-line: " + Arrays.asList(words)));
		int fl = 0;
		if(w.indexOf('i') >= 0)
		    fl |= Pattern.CASE_INSENSITIVE;
		if(!Pattern.compile(words[a++], fl).matcher(expand(words[a++], env)).matches())
		    return;
	    } else if(w.equals(">")) {
		if(a >= words.length - 1) throw(new RuntimeException("unexpected `when' operator at end-of-line: " + Arrays.asList(words)));
		if(iparcmp(expand(words[a++], env), expand(words[a++], env)) <= 0) return;
	    } else if(w.equals(">=")) {
		if(a >= words.length - 1) throw(new RuntimeException("unexpected `when' operator at end-of-line: " + Arrays.asList(words)));
		if(iparcmp(expand(words[a++], env), expand(words[a++], env)) < 0) return;
	    } else if(w.equals("<")) {
		if(a >= words.length - 1) throw(new RuntimeException("unexpected `when' operator at end-of-line: " + Arrays.asList(words)));
		if(iparcmp(expand(words[a++], env), expand(words[a++], env)) >= 0) return;
	    } else if(w.equals("<=")) {
		if(a >= words.length - 1) throw(new RuntimeException("unexpected `when' operator at end-of-line: " + Arrays.asList(words)));
		if(iparcmp(expand(words[a++], env), expand(words[a++], env)) > 0) return;
	    } else if(w.equals(".>")) {
		if(a >= words.length - 1) throw(new RuntimeException("unexpected `when' operator at end-of-line: " + Arrays.asList(words)));
		if(vparcmp(expand(words[a++], env), expand(words[a++], env)) <= 0) return;
	    } else if(w.equals(".>=")) {
		if(a >= words.length - 1) throw(new RuntimeException("unexpected `when' operator at end-of-line: " + Arrays.asList(words)));
		if(vparcmp(expand(words[a++], env), expand(words[a++], env)) < 0) return;
	    } else if(w.equals(".<")) {
		if(a >= words.length - 1) throw(new RuntimeException("unexpected `when' operator at end-of-line: " + Arrays.asList(words)));
		if(vparcmp(expand(words[a++], env), expand(words[a++], env)) >= 0) return;
	    } else if(w.equals(".<=")) {
		if(a >= words.length - 1) throw(new RuntimeException("unexpected `when' operator at end-of-line: " + Arrays.asList(words)));
		if(vparcmp(expand(words[a++], env), expand(words[a++], env)) > 0) return;
	    } else {
		if(expand(w, env).equals(""))
		    return;
	    }
	}
	command(Arrays.copyOfRange(words, a, words.length), env);
    }

    public static class InvalidVersionException extends RuntimeException implements ErrorMessage {
	public final String required;

	public InvalidVersionException(String required) {
	    super(String.format("invalid version of launcher; launch file requires %s, this is %d.%d", required, MAJOR_VERSION, MINOR_VERSION));
	    this.required = required;
	}

	public String usermessage() {
	    return(String.format("This launcher is outdated; please download the latest version from where you got it. " +
				 "The launch file requires version %s, whereas this launcher is version %d.%d.",
				 required, MAJOR_VERSION, MINOR_VERSION));
	}
    }

    public static class UserError extends RuntimeException implements ErrorMessage {
	public UserError(String message) {
	    super(message);
	}

	public String usermessage() {
	    return(getMessage());
	}
    }

    public void add(CommandHandler mod) {
	mods.add(mod);
    }

    public void command(String[] words, Environment env) {
	    if((words == null) || (words.length < 1))
		return;
	    for(CommandHandler mod : mods) {
		if(mod.command(words, this, env))
		    return;
	    }
	    if(Status.current().command(words, this, env))
		return;
	    if(launcher.command(words, this, env))
		return;
	    switch(words[0]) {
	    case "require": {
		if(words.length < 2)
		    throw(new RuntimeException("usage: require MAJOR.MINOR"));
		int maj, min;
		try {
		    int p = words[1].indexOf('.');
		    if(p < 0)
			throw(new RuntimeException("usage: require MAJOR.MINOR"));
		    maj = Integer.parseInt(words[1].substring(0, p));
		    min = Integer.parseInt(words[1].substring(p + 1));
		} catch(NumberFormatException e) {
		    throw(new RuntimeException("usage: require MAJOR.MINOR", e));
		}
		if((maj != MAJOR_VERSION) || (min > MINOR_VERSION))
		    throw(new InvalidVersionException(maj + "." + min));
		break;
	    }
	    case "error": {
		if(words.length < 2)
		    throw(new RuntimeException("usage: error MESSAGE"));
		throw(new UserError(words[1]));
	    }
	    case "rel": {
		if(words.length < 2)
		    throw(new RuntimeException("usage: rel URI"));
		try {
		    env.rel(new URI(expand(words[1], env)));
		} catch(URISyntaxException e) {
		    throw(new RuntimeException("usage: rel URL", e));
		}
		break;
	    }
	    case "validate": {
		if(words.length < 2)
		    throw(new RuntimeException("usage: validate VALIDATOR..."));
		Collection<Validator> nval = new ArrayList<>();
		for(int i = 1; i < words.length; i++) {
		    Validator v = Validator.parse(expand(words[i], env));
		    if(v != null)
			nval.add(v);
		}
		env.val = nval;
		break;
	    }
	    case "include": {
		if(words.length < 2)
		    throw(new RuntimeException("usage: include URL"));
		try {
		    include.add(new Resource(env.rel.resolve(new URI(expand(words[1], env))), env.val).referrer(env.src));
		} catch(URISyntaxException e) {
		    throw(new RuntimeException("usage: include URL", e));
		}
		break;
	    }
	    case "extension": {
		if(words.length < 2)
		    throw(new RuntimeException("usage: extension URL"));
		URI uri;
		try {
		    uri = env.rel.resolve(new URI(expand(words[1], env)));
		} catch(URISyntaxException e) {
		    throw(new RuntimeException("usage: extension URL", e));
		}
		if(!exts.contains(uri)) {
		    try {
			for(Extension ext : Extension.load(new Resource(uri, env.val).referrer(env.src)))
			    ext.init(this);
		    } catch(IOException e) {
			throw(new RuntimeException("could not load extension: " + String.valueOf(uri), e));
		    }
		    exts.add(uri);
		}
		break;
	    }
	    case "set": {
		if(words.length < 3)
		    throw(new RuntimeException("usage: set VARIABLE VALUE"));
		Map<String, String> par = new HashMap<>(env.par);
		par.put(expand(words[1], env), expand(words[2], env));
		env.par(par);
		break;
	    }
	    case "when": {
		when(words, env);
		break;
	    }
	    case "chain": {
		if(words.length < 2)
		    throw(new RuntimeException("usage: chain URL"));
		try {
		    launcher = new ChainLauncher(new Resource(env.rel.resolve(new URI(expand(words[1], env))), env.val).referrer(env.src));
		} catch(URISyntaxException e) {
		    throw(new RuntimeException("usage: chain URL", e));
		}
		break;
	    }
	    }
    }

    public void read(Reader in, Environment env) throws IOException {
	BufferedReader fp = new BufferedReader(in);
	for(String ln = fp.readLine(); ln != null; ln = fp.readLine()) {
	    if((ln.length() > 0) && (ln.charAt(0) == '#'))
		continue;
	    command(Utils.splitwords(ln), env);
	}
    }
}
