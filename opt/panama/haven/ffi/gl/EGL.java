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

package haven.ffi.gl;

import haven.*;
import haven.ffi.*;
import java.lang.invoke.*;
import java.lang.foreign.*;
import java.lang.foreign.MemoryLayout.PathElement;
import java.nio.*;
import static haven.ffi.ABI.*;
import static haven.ffi.FUtils.*;
import static java.lang.foreign.ValueLayout.ADDRESS;

public abstract class EGL {
    public static final MemorySegment EGL_DEFAULT_DISPLAY = MemorySegment.ofAddress(0);
    public static final int EGL_DONT_CARE = -1;
    public static final int EGL_FALSE = 0;
    public static final int EGL_TRUE = 1;
    public static final int EGL_NONE = 0x3038;
    public static final int EGL_SURFACE_TYPE = 0x3033;
    public static final int EGL_RENDERABLE_TYPE = 0x3040;
    public static final int EGL_RED_SIZE = 0x3024;
    public static final int EGL_GREEN_SIZE = 0x3023;
    public static final int EGL_BLUE_SIZE = 0x3022;
    public static final int EGL_ALPHA_SIZE = 0x3021;
    public static final int EGL_WIDTH = 0x3057;
    public static final int EGL_HEIGHT = 0x3056;
    public static final int EGL_PBUFFER_BIT = 0x0001;
    public static final int EGL_PIXMAP_BIT = 0x0002;
    public static final int EGL_WINDOW_BIT = 0x0004;
    public static final int EGL_OPENGL_API = 0x30A2;
    public static final int EGL_OPENGL_ES_BIT = 0x0001;
    public static final int EGL_OPENVG_BIT = 0x0002;
    public static final int EGL_OPENGL_ES2_BIT = 0x0004;
    public static final int EGL_OPENGL_BIT = 0x0008;
    public static final int EGL_CLIENT_APIS = 0x308D;
    public static final int EGL_VENDOR = 0x3053;
    public static final int EGL_VERSION = 0x3054;
    public static final int EGL_EXTENSIONS = 0x3055;
    public static final int EGL_CONTEXT_MAJOR_VERSION = 0x3098;
    public static final int EGL_CONTEXT_MINOR_VERSION = 0x30FB;
    public static final int EGL_CONTEXT_OPENGL_PROFILE_MASK = 0x30FD;
    public static final int EGL_CONTEXT_OPENGL_DEBUG = 0x31B0;
    public static final int EGL_CONTEXT_OPENGL_FORWARD_COMPATIBLE = 0x31B1;
    public static final int EGL_CONTEXT_OPENGL_ROBUST_ACCESS = 0x31B2;
    public static final int EGL_CONTEXT_OPENGL_RESET_NOTIFICATION_STRATEGY = 0x31BD;
    public static final int EGL_CONTEXT_OPENGL_CORE_PROFILE_BIT = 0x00000001;
    public static final int EGL_CONTEXT_OPENGL_COMPATIBILITY_PROFILE_BIT = 0x00000002;
    public static final int EGL_NO_RESET_NOTIFICATION = 0x31BE;
    public static final int EGL_LOSE_CONTEXT_ON_RESET = 0x31BF;
    public static final int EGL_PLATFORM_GBM_MESA = 0x31D7;
    public static final int EGL_PLATFORM_SURFACELESS_MESA = 0x31DD;

    public static final int EGL_SUCCESS = 0x3000;
    public static final int EGL_NOT_INITIALIZED = 0x3001;
    public static final int EGL_BAD_ACCESS = 0x3002;
    public static final int EGL_BAD_ALLOC = 0x3003;
    public static final int EGL_BAD_ATTRIBUTE = 0x3004;
    public static final int EGL_BAD_CONFIG = 0x3005;
    public static final int EGL_BAD_CONTEXT = 0x3006;
    public static final int EGL_BAD_CURRENT_SURFACE = 0x3007;
    public static final int EGL_BAD_DISPLAY = 0x3008;
    public static final int EGL_BAD_MATCH = 0x3009;
    public static final int EGL_BAD_NATIVE_PIXMAP = 0x300A;
    public static final int EGL_BAD_NATIVE_WINDOW = 0x300B;
    public static final int EGL_BAD_PARAMETER = 0x300C;
    public static final int EGL_BAD_SURFACE = 0x300D;
    public static final int EGL_CONTEXT_LOST = 0x300E;

    public static class EGLDisplay {
	protected final MemorySegment mem;

	EGLDisplay(MemorySegment mem) {
	    this.mem = mem;
	}

	MemorySegment mem() {return(mem);}
    }

    public static class EGLConfig {
	protected final MemorySegment mem;

	EGLConfig(MemorySegment mem) {
	    this.mem = mem;
	}

	MemorySegment mem() {return(mem);}
    }

    public static class EGLSurface {
	protected final MemorySegment mem;

	EGLSurface(MemorySegment mem) {
	    this.mem = mem;
	}

	MemorySegment mem() {return(mem);}
    }

    public static class EGLContext {
	protected final MemorySegment mem;

	EGLContext(MemorySegment mem) {
	    this.mem = mem;
	}

	MemorySegment mem() {return(mem);}
    }

    abstract MemorySegment eglGetProcAddress(String funcName);
    abstract EGLDisplay eglGetDisplay(MemorySegment native_display);
    abstract EGLDisplay eglGetPlatformDisplay(int platform, MemorySegment native_display, int[] attriblist);
    public abstract int[] eglInitialize(EGLDisplay dpy);
    public abstract int eglGetError();
    public abstract String eglQueryString(EGLDisplay dpy, int name);
    public abstract EGLConfig[] eglChooseConfig(EGLDisplay dpy, int[] attriblist);
    public abstract EGLSurface eglCreatePbufferSurface(EGLDisplay dpy, EGLConfig conf, int[] attriblist);
    public abstract EGLContext eglCreateContext(EGLDisplay dpy, EGLConfig conf, EGLContext share, int[] attriblist);
    public abstract void eglBindAPI(int api);
    public abstract void eglMakeCurrent(EGLDisplay dpy, EGLSurface draw, EGLSurface read, EGLContext ctx);
    public abstract void eglDestroyContext(EGLDisplay dpy, EGLContext ctx);
    public abstract void eglDestroySurface(EGLDisplay dpy, EGLSurface srf);
    public abstract void eglTerminate(EGLDisplay dpy);

    public EGLDisplay eglGetDisplay() {
	return(eglGetDisplay(EGL_DEFAULT_DISPLAY));
    }

    public static class PlatformSurfacelessMesa {
	public PlatformSurfacelessMesa() {}
    }

    public EGLDisplay eglGetPlatformDisplay(PlatformSurfacelessMesa spec) {
	return(eglGetPlatformDisplay(EGL_PLATFORM_SURFACELESS_MESA, MemorySegment.NULL, new int[0]));
    }

    EGLException lasterror() {
	return(new EGLException(eglGetError()));
    }

    static class libEGL_so_1 extends EGL {
	static final ValueLayout EGLenum = OpenGL.Base.GLenum;
	static final ValueLayout EGLBoolean = OpenGL.Base.GLboolean;
	static final ValueLayout EGLint = ValueLayout.JAVA_INT;
	static final ValueLayout EGLNativeDisplayType = ADDRESS;
	private final SymbolLookup egl = SymbolLookup.libraryLookup("libEGL.so.1", Arena.global());

	private final MethodHandle eglGetProcAddress = ld.downcallHandle(egl.find("eglGetProcAddress").get(), FunctionDescriptor.of(ADDRESS, ADDRESS));
	MemorySegment eglGetProcAddress(String funcName) {
	    try(Arena st = Arena.ofConfined()) {
		return((MemorySegment)eglGetProcAddress.invoke(st.allocateFrom(funcName, C_CHARSET)));
	    } catch(Throwable e) {
		throw(new RuntimeException(e));
	    }
	}

	private final MethodHandle eglGetDisplay = ld.downcallHandle(egl.find("eglGetDisplay").get(), FunctionDescriptor.of(ADDRESS, ADDRESS));
	EGLDisplay eglGetDisplay(MemorySegment native_display) {
	    MemorySegment rv;
	    try {
		rv = (MemorySegment)eglGetDisplay.invoke(native_display);
	    } catch(Throwable e) {
		throw(new RuntimeException(e));
	    }
	    if(nullp(rv))
		throw(lasterror());
	    return(new EGLDisplay(rv));
	}

	private final MethodHandle eglGetPlatformDisplay = ld.downcallHandle(egl.find("eglGetPlatformDisplay").get(), FunctionDescriptor.of(ADDRESS, EGLenum, ADDRESS, ADDRESS));
	EGLDisplay eglGetPlatformDisplay(int platform, MemorySegment native_display, int[] attriblist) {
	    if(attriblist.length % 2 != 0)
		throw(new IllegalArgumentException());
	    try(Arena st = Arena.ofConfined()) {
		MemorySegment acopy = st.allocate(EGLint, attriblist.length + 2);
		for(int i = 0; i < attriblist.length; i++)
		    setint(acopy, EGLint.byteSize() * i, EGLint, attriblist[i]);
		setint(acopy, EGLint.byteSize() * attriblist.length, EGLint, EGL_NONE);
		MemorySegment rv;
		try {
		    rv = (MemorySegment)eglGetPlatformDisplay.invoke(platform, native_display, acopy);
		} catch(Throwable e) {
		    throw(new RuntimeException(e));
		}
		if(nullp(rv))
		    throw(lasterror());
		return(new EGLDisplay(rv));
	    }
	}

	private final MethodHandle eglInitialize = ld.downcallHandle(egl.find("eglInitialize").get(), FunctionDescriptor.of(EGLBoolean, ADDRESS, ADDRESS, ADDRESS));
	public int[] eglInitialize(EGLDisplay dpy) {
	    try(Arena st = Arena.ofConfined()) {
		MemorySegment major = st.allocate(EGLint), minor = st.allocate(EGLint);
		int rv;
		try {
		    rv = (int)eglInitialize.invoke(dpy.mem(), major, minor);
		} catch(Throwable e) {
		    throw(new RuntimeException(e));
		}
		if(rv == 0)
		    throw(lasterror());
		return(new int[] {(int)getint(major, 0, EGLint, true), (int)getint(minor, 0, EGLint, true)});
	    }
	}

	private final MethodHandle eglGetError = ld.downcallHandle(egl.find("eglGetError").get(), FunctionDescriptor.of(EGLint));
	public int eglGetError() {
	    try {
		return((int)eglGetError.invoke());
	    } catch(Throwable e) {
		throw(new RuntimeException(e));
	    }
	}

	private final MethodHandle eglQueryString = ld.downcallHandle(egl.find("eglQueryString").get(), FunctionDescriptor.of(ADDRESS, ADDRESS, EGLint));
	public String eglQueryString(EGLDisplay dpy, int name) {
	    MemorySegment rv;
	    try {
		rv = (MemorySegment)eglQueryString.invoke(dpy.mem(), name);
	    } catch(Throwable e) {
		throw(new RuntimeException(e));
	    }
	    if(nullp(rv))
		return(null);
	    return(rv.reinterpret(Long.MAX_VALUE).getString(0, C_CHARSET));
	}

	private final MethodHandle eglChooseConfig = ld.downcallHandle(egl.find("eglChooseConfig").get(), FunctionDescriptor.of(EGLBoolean, ADDRESS, ADDRESS, ADDRESS, EGLint, ADDRESS));
	public EGLConfig[] eglChooseConfig(EGLDisplay dpy, int[] attriblist) {
	    if(attriblist.length % 2 != 0)
		throw(new IllegalArgumentException());
	    int maxret = 128;
	    try(Arena st = Arena.ofConfined()) {
		MemorySegment acopy = st.allocate(EGLint, attriblist.length + 2);
		for(int i = 0; i < attriblist.length; i++)
		    setint(acopy, EGLint.byteSize() * i, EGLint, attriblist[i]);
		setint(acopy, EGLint.byteSize() * attriblist.length, EGLint, EGL_NONE);
		MemorySegment confbuf = st.allocate(ADDRESS, maxret), numbuf = st.allocate(EGLint);
		int rv;
		try {
		    rv = (int)eglChooseConfig.invoke(dpy.mem(), acopy, confbuf, maxret, numbuf);
		} catch(Throwable e) {
		    throw(new RuntimeException(e));
		}
		if(rv == 0)
		    throw(lasterror());
		int n = (int)getint(numbuf, 0, EGLint, true);
		EGLConfig[] ret = new EGLConfig[n];
		for(int i = 0; i < n; i++)
		    ret[i] = new EGLConfig(confbuf.get(ADDRESS, i * ADDRESS.byteSize()));
		return(ret);
	    }
	}

	private final MethodHandle eglCreatePbufferSurface = ld.downcallHandle(egl.find("eglCreatePbufferSurface").get(), FunctionDescriptor.of(ADDRESS, ADDRESS, ADDRESS, ADDRESS));
	public EGLSurface eglCreatePbufferSurface(EGLDisplay dpy, EGLConfig conf, int[] attriblist) {
	    if(attriblist.length % 2 != 0)
		throw(new IllegalArgumentException());
	    try(Arena st = Arena.ofConfined()) {
		MemorySegment acopy = st.allocate(EGLint, attriblist.length + 2);
		for(int i = 0; i < attriblist.length; i++)
		    setint(acopy, EGLint.byteSize() * i, EGLint, attriblist[i]);
		setint(acopy, EGLint.byteSize() * attriblist.length, EGLint, EGL_NONE);
		MemorySegment rv;
		try {
		    rv = (MemorySegment)eglCreatePbufferSurface.invoke(dpy.mem(), conf.mem(), acopy);
		} catch(Throwable e) {
		    throw(new RuntimeException(e));
		}
		if(nullp(rv))
		    throw(lasterror());
		return(new EGLSurface(rv));
	    }
	}

	private final MethodHandle eglCreateContext = ld.downcallHandle(egl.find("eglCreateContext").get(), FunctionDescriptor.of(ADDRESS, ADDRESS, ADDRESS, ADDRESS, ADDRESS));
	public EGLContext eglCreateContext(EGLDisplay dpy, EGLConfig conf, EGLContext share, int[] attriblist) {
	    if(attriblist.length % 2 != 0)
		throw(new IllegalArgumentException());
	    try(Arena st = Arena.ofConfined()) {
		MemorySegment acopy = st.allocate(EGLint, attriblist.length + 2);
		for(int i = 0; i < attriblist.length; i++)
		    setint(acopy, EGLint.byteSize() * i, EGLint, attriblist[i]);
		setint(acopy, EGLint.byteSize() * attriblist.length, EGLint, EGL_NONE);
		MemorySegment rv;
		try {
		    rv = (MemorySegment)eglCreateContext.invoke(dpy.mem(), conf.mem(), (share == null) ? MemorySegment.NULL : share.mem(), acopy);
		} catch(Throwable e) {
		    throw(new RuntimeException(e));
		}
		if(nullp(rv))
		    throw(lasterror());
		return(new EGLContext(rv));
	    }
	}

	private final MethodHandle eglBindAPI = ld.downcallHandle(egl.find("eglBindAPI").get(), FunctionDescriptor.of(EGLBoolean, EGLenum));
	public void eglBindAPI(int api) {
	    int rv;
	    try {
		rv = (int)eglBindAPI.invoke(api);
	    } catch(Throwable e) {
		throw(new RuntimeException(e));
	    }
	    if(rv == 0)
		throw(lasterror());
	}

	private final MethodHandle eglMakeCurrent = ld.downcallHandle(egl.find("eglMakeCurrent").get(), FunctionDescriptor.of(EGLBoolean, ADDRESS, ADDRESS, ADDRESS, ADDRESS));
	public void eglMakeCurrent(EGLDisplay dpy, EGLSurface draw, EGLSurface read, EGLContext ctx) {
	    int rv;
	    try {
		rv = (int)eglMakeCurrent.invoke(dpy.mem(),
						(draw == null) ? MemorySegment.NULL : draw.mem(),
						(read == null) ? MemorySegment.NULL : read.mem(),
						(ctx == null) ? MemorySegment.NULL : ctx.mem());
	    } catch(Throwable e) {
		throw(new RuntimeException(e));
	    }
	    if(rv == 0)
		throw(lasterror());
	}

	private final MethodHandle eglDestroyContext = ld.downcallHandle(egl.find("eglDestroyContext").get(), FunctionDescriptor.of(EGLBoolean, ADDRESS, ADDRESS));
	public void eglDestroyContext(EGLDisplay dpy, EGLContext ctx) {
	    int rv;
	    try {
		rv = (int)eglDestroyContext.invoke(dpy.mem(), ctx.mem());
	    } catch(Throwable e) {
		throw(new RuntimeException(e));
	    }
	    if(rv == 0)
		throw(lasterror());
	}

	private final MethodHandle eglDestroySurface = ld.downcallHandle(egl.find("eglDestroySurface").get(), FunctionDescriptor.of(EGLBoolean, ADDRESS, ADDRESS));
	public void eglDestroySurface(EGLDisplay dpy, EGLSurface srf) {
	    int rv;
	    try {
		rv = (int)eglDestroySurface.invoke(dpy.mem(), srf.mem());
	    } catch(Throwable e) {
		throw(new RuntimeException(e));
	    }
	    if(rv == 0)
		throw(lasterror());
	}

	private final MethodHandle eglTerminate = ld.downcallHandle(egl.find("eglTerminate").get(), FunctionDescriptor.of(EGLBoolean, ADDRESS));
	public void eglTerminate(EGLDisplay dpy) {
	    int rv;
	    try {
		rv = (int)eglTerminate.invoke(dpy.mem());
	    } catch(Throwable e) {
		throw(new RuntimeException(e));
	    }
	    if(rv == 0)
		throw(lasterror());
	}
    }

    class Resolved extends OpenGL.Base {
	protected MethodHandle lookup(String name, FunctionDescriptor sig, Linker.Option... options) {
	    MemorySegment addr = eglGetProcAddress(name);
	    if(nullp(addr))
		return(null);
	    return(ld.downcallHandle(addr, sig, options));
	}
    }

    public OpenGL gl() {
	return(new Resolved());
    }

    private static EGL instance = null;
    public static EGL get() {
	if(instance == null) {
	    synchronized(EGL.class) {
		if(instance == null) {
		    instance = new libEGL_so_1();
		}
	    }
	}
	return(instance);
    }
}
