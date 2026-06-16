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

package haven.ffi.x11;

import haven.ffi.*;
import haven.ffi.gl.*;
import java.lang.invoke.*;
import java.lang.foreign.*;
import static haven.ffi.ABI.*;
import static haven.ffi.FUtils.*;
import static haven.ffi.x11.XLib.*;
import static java.lang.foreign.ValueLayout.ADDRESS;

public abstract class GLX {
    public static final int GLX_DONT_CARE = 0xFFFFFFFF;
    public static final int GLX_NONE = 0x8000;

    public static final int GLX_USE_GL = 1;
    public static final int GLX_BUFFER_SIZE = 2;
    public static final int GLX_LEVEL = 3;
    public static final int GLX_RGBA = 4;
    public static final int GLX_DOUBLEBUFFER = 5;
    public static final int GLX_STEREO = 6;
    public static final int GLX_AUX_BUFFERS = 7;
    public static final int GLX_RED_SIZE = 8;
    public static final int GLX_GREEN_SIZE = 9;
    public static final int GLX_BLUE_SIZE = 10;
    public static final int GLX_ALPHA_SIZE = 11;
    public static final int GLX_DEPTH_SIZE = 12;
    public static final int GLX_STENCIL_SIZE = 13;
    public static final int GLX_ACCUM_RED_SIZE = 14;
    public static final int GLX_ACCUM_GREEN_SIZE = 15;
    public static final int GLX_ACCUM_BLUE_SIZE = 16;
    public static final int GLX_ACCUM_ALPHA_SIZE = 17;
    public static final int GLX_DRAWABLE_TYPE = 0x8010;
    public static final int GLX_RENDER_TYPE = 0x8011;
    public static final int GLX_WINDOW = 0x8022;
    public static final int GLX_PBUFFER = 0x8023;
    public static final int GLX_SAMPLE_BUFFERS = 0x186a0;
    public static final int GLX_SAMPLES = 0x186a1;

    public static final int GLX_CONFIG_CAVEAT = 0x20;
    public static final int GLX_SLOW_CONFIG = 0x8001;
    public static final int GLX_TRUE_COLOR = 0x8002;
    public static final int GLX_DIRECT_COLOR = 0x8003;
    public static final int GLX_PSEUDO_COLOR = 0x8004;
    public static final int GLX_STATIC_COLOR = 0x8005;
    public static final int GLX_GRAY_SCALE = 0x8006;
    public static final int GLX_STATIC_GRAY = 0x8007;
    public static final int GLX_TRANSPARENT_RGB = 0x8008;
    public static final int GLX_TRANSPARENT_INDEX = 0x8009;
    public static final int GLX_VISUAL_ID = 0x800B;
    public static final int GLX_SCREEN = 0x800C;
    public static final int GLX_NON_CONFORMANT_CONFIG = 0x800D;
    public static final int GLX_X_RENDERABLE = 0x8012;
    public static final int GLX_FBCONFIG_ID = 0x8013;
    public static final int GLX_RGBA_TYPE = 0x8014;

    public static final int GLX_RGBA_BIT = 0x01;
    public static final int GLX_COLOR_INDEX_BIT = 0x02;

    public static final int GLX_WINDOW_BIT = 0x00000001;
    public static final int GLX_PIXMAP_BIT = 0x00000002;
    public static final int GLX_PBUFFER_BIT = 0x00000004;

    public static final int GLX_CONTEXT_MAJOR_VERSION_ARB = 0x2091;
    public static final int GLX_CONTEXT_MINOR_VERSION_ARB = 0x2092;
    public static final int GLX_CONTEXT_FLAGS_ARB = 0x2094;
    public static final int GLX_CONTEXT_PROFILE_MASK_ARB = 0x9126;

    public static final int GLX_CONTEXT_CORE_PROFILE_BIT_ARB = 0x00000001;
    public static final int GLX_CONTEXT_COMPATIBILITY_PROFILE_BIT_ARB = 0x00000002;
    public static final int GLX_CONTEXT_DEBUG_BIT_ARB = 0x00000001;
    public static final int GLX_CONTEXT_FORWARD_COMPATIBLE_BIT_ARB = 0x00000002;

    public static class GLXContext {
	protected final MemorySegment mem;

	GLXContext(MemorySegment mem) {
	    this.mem = mem;
	}

	MemorySegment mem() {return(mem);}
    }

    public static class GLXFBConfig {
	protected final MemorySegment mem;
	private final GLX glx;
	private final Display dpy;

	GLXFBConfig(MemorySegment mem, GLX glx, Display dpy) {
	    this.mem = mem;
	    this.dpy = dpy;
	    this.glx = glx;
	}

	MemorySegment mem() {return(mem);}

	public int attrib(int attrib) {
	    return(glx.glXGetFBConfigAttrib(dpy, this, attrib));
	}

	public String toString() {
	    return(String.format("#<fbconfig 0x%x r%dg%db%da%d>", attrib(GLX_FBCONFIG_ID),
				 attrib(GLX_RED_SIZE), attrib(GLX_GREEN_SIZE), attrib(GLX_BLUE_SIZE), attrib(GLX_ALPHA_SIZE)));
	}
    }

    public abstract MemorySegment glXGetProcAddress(String name);
    public abstract String glXQueryExtensionsString(Display dpy, int screen);
    public abstract XVisualInfo glXChooseVisual(Display dpy, int screen, int[] attriblist);
    public abstract GLXFBConfig[] glXChooseFBConfig(Display dpy, int screen, int[] attriblist);
    public abstract XVisualInfo glXGetVisualFromFBConfig(Display dpy, GLXFBConfig config);
    public abstract int glXGetFBConfigAttrib(Display dpy, GLXFBConfig config, int attrib);
    public abstract GLXContext glXCreateContext(Display dpy, XVisualInfo vis, GLXContext sharelist, boolean direct);
    public abstract GLXContext glXCreateNewContext(Display dpy, GLXFBConfig config, int type, GLXContext sharelist, boolean direct);
    public abstract GLXContext glXCreateContextAttribsARB(Display dpy, GLXFBConfig config, GLXContext sharelist, boolean direct, int[] attriblist);
    public abstract void glXSwapIntervalEXT(Display dpy, XID drawable, int interval);
    public abstract void glXDestroyContext(Display dpy, GLXContext ctx);
    public abstract boolean glXMakeCurrent(Display dpy, XID drawable, GLXContext ctx);
    public abstract void glXSwapBuffers(Display dpy, XID drawable);

    static class libGLX_so_0 extends GLX {
	private static final MemoryLayout C_XBool = libX11_so_6.C_XBool;
	private static final MemoryLayout C_XID = libX11_so_6.C_XID;
	private static final VarHandle attribary = C_INT.arrayElementVarHandle();
	private final XLib xlib = XLib.get();
	private final SymbolLookup glx = SymbolLookup.libraryLookup("libGLX.so.0", Arena.global());

	private final MethodHandle glXGetProcAddress = ld.downcallHandle(glx.find("glXGetProcAddress").get(), FunctionDescriptor.of(ADDRESS, ADDRESS));
	public MemorySegment glXGetProcAddress(String name) {
	    try(Arena st = Arena.ofConfined()) {
		MemorySegment stname = st.allocateFrom(name, C_CHARSET);
		try {
		    return((MemorySegment)glXGetProcAddress.invoke(stname));
		} catch(Throwable e) {
		    throw(new RuntimeException(e));
		} finally {
		    checkerror();
		}
	    }
	}

	private final MethodHandle glXQueryExtensionsString = ld.downcallHandle(glx.find("glXQueryExtensionsString").get(), FunctionDescriptor.of(ADDRESS, ADDRESS, C_INT));
	public String glXQueryExtensionsString(Display dpy, int screen) {
	    MemorySegment retp;
	    try {
		retp = (MemorySegment)glXQueryExtensionsString.invoke(dpy.mem(), screen);
	    } catch(Throwable e) {
		throw(new RuntimeException(e));
	    } finally {
		checkerror();
	    }
	    String ret = retp.reinterpret(Long.MAX_VALUE).getString(0, C_CHARSET);
	    return(ret);
	}

	private final MethodHandle glXChooseVisual = ld.downcallHandle(glx.find("glXChooseVisual").get(), FunctionDescriptor.of(ADDRESS.withTargetLayout(libX11_so_6._XVisualInfo), ADDRESS, C_INT, ADDRESS));
	public XVisualInfo glXChooseVisual(Display dpy, int screen, int[] attriblist) {
	    MemorySegment ret;
	    if(attriblist[attriblist.length - 1] != XLib.None)
		throw(new IllegalArgumentException());
	    try(Arena st = Arena.ofConfined()) {
		MemorySegment acopy = st.allocate(C_INT, attriblist.length);
		for(int i = 0; i < attriblist.length; i++)
		    attribary.set(acopy, 0, i, attriblist[i]);
		try {
		    ret = (MemorySegment)glXChooseVisual.invoke(dpy.mem(), screen, acopy);
		} catch(Throwable e) {
		    throw(new RuntimeException(e));
		} finally {
		    checkerror();
		}
	    }
	    return(nullp(ret) ? null : new libX11_so_6.XVisualInfo(ret));
	}

	private final MethodHandle glXChooseFBConfig = ld.downcallHandle(glx.find("glXChooseFBConfig").get(), FunctionDescriptor.of(ADDRESS, ADDRESS, C_INT, ADDRESS, ADDRESS));
	public GLXFBConfig[] glXChooseFBConfig(Display dpy, int screen, int[] attriblist) {
	    if(attriblist.length % 2 != 1)
		throw(new IllegalArgumentException());
	    if(attriblist[attriblist.length - 1] != XLib.None)
		throw(new IllegalArgumentException());
	    try(Arena st = Arena.ofConfined()) {
		MemorySegment acopy = st.allocate(C_INT, attriblist.length);
		for(int i = 0; i < attriblist.length; i++)
		    attribary.set(acopy, 0, i, attriblist[i]);
		MemorySegment nelements = st.allocate(C_INT);
		MemorySegment retp;
		try {
		    retp = (MemorySegment)glXChooseFBConfig.invoke(dpy.mem(), screen, acopy, nelements);
		} catch(Throwable e) {
		    throw(new RuntimeException(e));
		} finally {
		    checkerror();
		}
		if(nullp(retp))
		    return(new GLXFBConfig[0]);
		GLXFBConfig[] ret = new GLXFBConfig[(int)getint(nelements, 0, C_INT, true)];
		retp = retp.reinterpret(ADDRESS.byteSize() * ret.length);
		for(int i = 0; i < ret.length; i++)
		    ret[i] = new GLXFBConfig(retp.getAtIndex(ADDRESS, i), this, dpy);
		xlib.XFree(retp);
		return(ret);
	    }
	}

	private final MethodHandle glXGetVisualFromFBConfig = ld.downcallHandle(glx.find("glXGetVisualFromFBConfig").get(), FunctionDescriptor.of(ADDRESS, ADDRESS, ADDRESS));
	public XVisualInfo glXGetVisualFromFBConfig(Display dpy, GLXFBConfig config) {
	    MemorySegment ret;
	    try {
		ret = (MemorySegment)glXGetVisualFromFBConfig.invoke(dpy.mem(), config.mem());
	    } catch(Throwable e) {
		throw(new RuntimeException(e));
	    } finally {
		checkerror();
	    }
	    return(nullp(ret) ? null : new libX11_so_6.XVisualInfo(ret));
	}

	private final MethodHandle glXGetFBConfigAttrib = ld.downcallHandle(glx.find("glXGetFBConfigAttrib").get(), FunctionDescriptor.of(C_INT, ADDRESS, ADDRESS, C_INT, ADDRESS));
	public int glXGetFBConfigAttrib(Display dpy, GLXFBConfig config, int attrib) {
	    try(Arena st = Arena.ofConfined()) {
		MemorySegment buf = st.allocate(C_INT);
		int rv;
		try {
		    rv = (int)glXGetFBConfigAttrib.invoke(dpy.mem(), config.mem(), attrib, buf);
		} catch(Throwable e) {
		    throw(new RuntimeException(e));
		} finally {
		    checkerror();
		}
		if(rv != XLib.Success)
		    throw(new RuntimeException("glXGetFBConfigAttrib: " + rv));
		return((int)getint(buf, 0, C_INT, true));
	    }
	}

	private final MethodHandle glXCreateContext = ld.downcallHandle(glx.find("glXCreateContext").get(), FunctionDescriptor.of(ADDRESS, ADDRESS, ADDRESS, ADDRESS, C_XBool));
	public GLXContext glXCreateContext(Display dpy, XVisualInfo vis, GLXContext sharelist, boolean direct) {
	    MemorySegment ret;
	    try {
		ret = (MemorySegment)glXCreateContext.invoke(dpy.mem(), vis.mem(), ornull(sharelist, GLXContext::mem), direct ? 1 : 0);
	    } catch(Throwable e) {
		throw(new RuntimeException(e));
	    } finally {
		checkerror();
	    }
	    return(nullp(ret) ? null : new GLXContext(ret));
	}

	private final MethodHandle glXCreateNewContext = ld.downcallHandle(glx.find("glXCreateNewContext").get(), FunctionDescriptor.of(ADDRESS, ADDRESS, ADDRESS, C_INT, ADDRESS, C_XBool));
	public GLXContext glXCreateNewContext(Display dpy, GLXFBConfig config, int type, GLXContext sharelist, boolean direct) {
	    MemorySegment ret;
	    try {
		ret = (MemorySegment)glXCreateNewContext.invoke(dpy.mem(), config.mem(), type, ornull(sharelist, GLXContext::mem), direct ? 1 : 0);
	    } catch(Throwable e) {
		throw(new RuntimeException(e));
	    } finally {
		checkerror();
	    }
	    return(nullp(ret) ? null : new GLXContext(ret));
	}

	private final MethodHandle glXCreateContextAttribsARB = ld.downcallHandle(glXGetProcAddress("glXCreateContextAttribsARB"), FunctionDescriptor.of(ADDRESS, ADDRESS, ADDRESS, ADDRESS, C_XBool, ADDRESS));
	public GLXContext glXCreateContextAttribsARB(Display dpy, GLXFBConfig config, GLXContext sharelist, boolean direct, int[] attriblist) {
	    if(attriblist.length % 2 != 1)
		throw(new IllegalArgumentException());
	    if(attriblist[attriblist.length - 1] != XLib.None)
		throw(new IllegalArgumentException());
	    MemorySegment ret;
	    try(Arena st = Arena.ofConfined()) {
		MemorySegment acopy = st.allocate(C_INT, attriblist.length);
		for(int i = 0; i < attriblist.length; i++)
		    attribary.set(acopy, 0, i, attriblist[i]);
		try {
		    ret = (MemorySegment)glXCreateContextAttribsARB.invoke(dpy.mem(), config.mem(), ornull(sharelist, GLXContext::mem), direct ? 1 : 0, acopy);
		} catch(Throwable e) {
		    throw(new RuntimeException(e));
		} finally {
		    checkerror();
		}
	    }
	    return(nullp(ret) ? null : new GLXContext(ret));
	}

	private final MethodHandle glXSwapIntervalEXT = ld.downcallHandle(glXGetProcAddress("glXSwapIntervalEXT"), FunctionDescriptor.ofVoid(ADDRESS, C_XID, C_INT));
	public void glXSwapIntervalEXT(Display dpy, XID drawable, int interval) {
	    try {
		if(C_XID instanceof ValueLayout.OfLong)
		    glXSwapIntervalEXT.invoke(dpy.mem(), (long)drawable.bits, interval);
		else
		    glXSwapIntervalEXT.invoke(dpy.mem(), (int)drawable.bits, interval);
	    } catch(Throwable e) {
		throw(new RuntimeException(e));
	    } finally {
		checkerror();
	    }
	}

	private final MethodHandle glXDestroyContext = ld.downcallHandle(glx.find("glXDestroyContext").get(), FunctionDescriptor.ofVoid(ADDRESS, ADDRESS));
	public void glXDestroyContext(Display dpy, GLXContext ctx) {
	    try {
		glXDestroyContext.invoke(dpy.mem(), ctx.mem());
	    } catch(Throwable e) {
		throw(new RuntimeException(e));
	    } finally {
		checkerror();
	    }
	}

	private final MethodHandle glXMakeCurrent = ld.downcallHandle(glx.find("glXMakeCurrent").get(), FunctionDescriptor.of(C_XBool, ADDRESS, C_XID, ADDRESS));
	public boolean glXMakeCurrent(Display dpy, XID drawable, GLXContext ctx) {
	    try {
		if(C_XID instanceof ValueLayout.OfLong)
		    return(((int)glXMakeCurrent.invoke(dpy.mem(), (long)drawable.bits, ornull(ctx, GLXContext::mem))) != 0);
		else
		    return(((int)glXMakeCurrent.invoke(dpy.mem(), (int)drawable.bits, ornull(ctx, GLXContext::mem))) != 0);
	    } catch(Throwable e) {
		throw(new RuntimeException(e));
	    } finally {
		checkerror();
	    }
	}

	private final MethodHandle glXSwapBuffers = ld.downcallHandle(glx.find("glXSwapBuffers").get(), FunctionDescriptor.ofVoid(ADDRESS, C_XID));
	public void glXSwapBuffers(Display dpy, XID drawable) {
	    try {
		if(C_XID instanceof ValueLayout.OfLong)
		    glXSwapBuffers.invoke(dpy.mem(), (long)drawable.bits);
		else
		    glXSwapBuffers.invoke(dpy.mem(), (int)drawable.bits);
	    } catch(Throwable e) {
		throw(new RuntimeException(e));
	    } finally {
		checkerror();
	    }
	}
    }

    class Resolved extends OpenGL.Base {
	protected MethodHandle lookup(String name, FunctionDescriptor sig, Linker.Option... options) {
	    MemorySegment addr = glXGetProcAddress(name);
	    if(nullp(addr))
		return(null);
	    return(ld.downcallHandle(addr, sig, options));
	}
    }

    public OpenGL gl() {
	return(new Resolved());
    }

    private static GLX instance = null;
    public static GLX get() {
	if(instance == null) {
	    synchronized(GLX.class) {
		if(instance == null) {
		    instance = new libGLX_so_0();
		}
	    }
	}
	return(instance);
    }
}
