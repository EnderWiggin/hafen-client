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
import java.nio.file.*;
import java.net.*;
import static haven.launcher.Config.expand;
import static haven.launcher.Utils.*;

public class JavaLauncher implements Launcher {
    public final Collection<Resource> classpath = new ArrayList<>();
    public final Collection<String> jvmargs = new ArrayList<>();
    public final Collection<String> cmdargs = new ArrayList<>();
    public final Collection<NativeLib> libraries = new ArrayList<>();
    public final Map<String, String> sysprops = new HashMap<>();
    public String mainclass = null;
    public Resource execjar = null;
    public int heapsize = 0;
    public String runCmdName = null;

    public static Path findjvm() {
	Path jvm, javadir = pj(path(System.getProperty("java.home")), "bin");
	if(Files.exists(jvm = pj(javadir, "java")))
	    return(jvm);
	if(Files.exists(jvm = pj(javadir, "javaw.exe")))
	    return(jvm);
	if(Files.exists(jvm = pj(javadir, "java.exe")))
	    return(jvm);
	throw(new RuntimeException("could not find a Java executable"));
    }

    public void launch() throws IOException {
	List<String> args = new ArrayList<>();
	args.add(findjvm().toFile().toString());
	Collection<Path> classpath = new ArrayList<>();
	for(Resource res : this.classpath) {
	    classpath.add(res.update());
	}
	if(heapsize > 0) {
	    if (!Utils.is64BitVM()) { // Limit heap on not x64-bit runtimes
		heapsize = Math.min(1024, heapsize);
	    }
	    args.add(String.format("-Xmx%dm", heapsize));
	}
	for(String arg : jvmargs)
	    args.add(arg);
	for(Map.Entry<String, String> prop : sysprops.entrySet())
	    args.add(String.format("-D%s=%s", prop.getKey(), prop.getValue()));
	if(!classpath.isEmpty()) {
	    args.add("-classpath");
	    args.add(String.join(File.pathSeparator, (Iterable<String>)classpath.stream().map(Path::toFile).map(File::toString)::iterator));
	}

	{
	    Collection<String> libdirs = new ArrayList<>();
	    for(NativeLib lib : libraries) {
		if(lib.use())
		    libdirs.add(lib.extract().toFile().toString());
	    }
	    if(libdirs.size() > 0) {
		String dirs = String.join(File.pathSeparator, libdirs);
		String cur = System.getProperty("java.library.path");
		if((cur != null) && (cur.length() > 0))
		    dirs = dirs + File.pathSeparator + cur;
		args.add(String.format("-Djava.library.path=%s", dirs));
	    }
	}

	if(mainclass != null) {
	    args.add(mainclass);
	} else if(execjar != null) {
	    args.add("-jar");
	    args.add(execjar.update().toString());
	} else {
	    throw(new RuntimeException("neither main-class nor exec-jar specified for Java launcher"));
	}
	for(String arg : cmdargs)
	    args.add(arg);
	try(Status st = Status.current()) {
	    st.message("Launching...");
	    ProcessBuilder spec = new ProcessBuilder(args);
	    spec.inheritIO();
	    Utils.saveRunBat(spec, runCmdName);
	    spec.start();
	}
    }

    public boolean command(String[] words, Config cfg, Config.Environment env) {
	switch(words[0]) {
	case "main-class": {
	    if(words.length < 2)
		throw(new RuntimeException("usage: main-class CLASS-NAME"));
	    mainclass = expand(words[1], env);
	    return(true);
	}
	case "exec-jar": {
	    if(words.length < 2)
		throw(new RuntimeException("usage: exec-jar URL"));
	    try {
		execjar = new Resource(env.rel.resolve(new URI(expand(words[1], env))), env.val).referrer(env.src);
	    } catch(URISyntaxException e) {
		throw(new RuntimeException("usage: exec-jar URL", e));
	    }
	    return(true);
	}
	case "class-path": {
	    if(words.length < 2)
		throw(new RuntimeException("usage: classpath URL"));
	    try {
		classpath.add(new Resource(env.rel.resolve(new URI(expand(words[1], env))), env.val).referrer(env.src));
	    } catch(URISyntaxException e) {
		throw(new RuntimeException("usage: classpath URL", e));
	    }
	    return(true);
	}
	case "property": {
	    if(words.length < 3)
		throw(new RuntimeException("usage: property NAME VALUE"));
	    sysprops.put(expand(words[1], env), expand(words[2], env));
	    return(true);
	}
	case "heap-size": {
	    if(words.length < 2)
		throw(new RuntimeException("usage: heap-size MBYTES"));
	    try {
		heapsize = Integer.parseInt(expand(words[1], env));
	    } catch(NumberFormatException e) {
		throw(new RuntimeException("usage: heap-size MBYTES", e));
	    }
	    return(true);
	}
	case "jvm-arg": {
	    if(words.length < 2)
		throw(new RuntimeException("usage: jvm-arg ARG..."));
	    for(int i = 1; i < words.length; i++)
		jvmargs.add(expand(words[i], env));
	    return(true);
	}
	case "arguments": {
	    if(words.length < 2)
		throw(new RuntimeException("usage: arguments ARG..."));
	    for(int i = 1; i < words.length; i++)
		cmdargs.add(expand(words[i], env));
	    return(true);
	}
	case "native-lib": {
	    if(words.length < 4)
		throw(new RuntimeException("usage: native-lib OS ARCH URL [SUB-DIR]"));
	    try {
		Pattern os = Pattern.compile(words[1], Pattern.CASE_INSENSITIVE);
		Pattern arch = Pattern.compile(words[2], Pattern.CASE_INSENSITIVE);
		Resource lib = new Resource(env.rel.resolve(new URI(expand(words[3], env))), env.val).referrer(env.src);
		String subdir = "";
		if(words.length > 4)
		    subdir = expand(words[4], env);
		libraries.add(new NativeLib(os, arch, lib, subdir));
	    } catch(PatternSyntaxException | URISyntaxException e) {
		throw(new RuntimeException("usage: native-lib OS ARCH URL [SUB-DIR]", e));
	    }
	    return(true);
	}
	    case "command-file": {
		if(words.length < 2)
		    throw(new RuntimeException("usage: command-file FILE-NAME"));
		runCmdName = words[1];
		return true;
	    }
	}
	return(false);
    }
}
