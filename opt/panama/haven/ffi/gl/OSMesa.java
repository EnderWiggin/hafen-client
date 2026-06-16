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

public abstract class OSMesa {
    public static final int OSMESA_WIDTH = 0x20;
    public static final int OSMESA_HEIGHT = 0x21;
    public static final int OSMESA_FORMAT = 0x22;
    public static final int OSMESA_TYPE = 0x23;
    public static final int OSMESA_RGBA = OpenGL.GL_RGBA;
    public static final int OSMESA_BGRA = 0x1;
    public static final int OSMESA_ARGB = 0x2;
    public static final int OSMESA_RGB = OpenGL.GL_RGB;
    public static final int OSMESA_BGR = 0x4;
    public static final int OSMESA_RGB_565 = 0x5;

    public static final int OSMESA_DEPTH_BITS = 0x30;
    public static final int OSMESA_STENCIL_BITS = 0x31;
    public static final int OSMESA_ACCUM_BITS = 0x32;
    public static final int OSMESA_PROFILE = 0x33;
    public static final int OSMESA_CORE_PROFILE = 0x34;
    public static final int OSMESA_COMPAT_PROFILE = 0x35;
    public static final int OSMESA_CONTEXT_MAJOR_VERSION = 0x36;
    public static final int OSMESA_CONTEXT_MINOR_VERSION = 0x37;

    public static class OSMesaContext {
	protected final MemorySegment mem;

	OSMesaContext(MemorySegment mem) {
	    this.mem = mem;
	}

	MemorySegment mem() {return(mem);}
    }

    public abstract OSMesaContext OSMesaCreateContextAttribs(int[] attriblist, OSMesaContext sharelist);
    public abstract void OSMesaDestroyContext(OSMesaContext ctx);
    public abstract boolean OSMesaMakeCurrent(OSMesaContext ctx, ByteBuffer buffer, int type, int width, int height);
    abstract MemorySegment OSMesaGetProcAddress(String funcName);

    static class libOSMesa_so_8 extends OSMesa {
	static final ValueLayout C_OSMesaContext = ADDRESS;
	static final ValueLayout GLboolean = OpenGL.Base.GLboolean;
	static final ValueLayout GLenum = OpenGL.Base.GLenum;
	static final ValueLayout GLsizei = OpenGL.Base.GLsizei;
	private final SymbolLookup mesa = SymbolLookup.libraryLookup("libOSMesa.so.8", Arena.global());

	private final MethodHandle OSMesaCreateContextAttribs = ld.downcallHandle(mesa.find("OSMesaCreateContextAttribs").get(), FunctionDescriptor.of(C_OSMesaContext, ADDRESS, C_OSMesaContext));
	public OSMesaContext OSMesaCreateContextAttribs(int[] attriblist, OSMesaContext sharelist) {
	    if(attriblist.length % 2 != 0)
		throw(new IllegalArgumentException());
	    MemorySegment ret;
	    try(Arena st = Arena.ofConfined()) {
		MemorySegment acopy = st.allocate(C_INT, attriblist.length + 2);
		for(int i = 0; i < attriblist.length; i++)
		    setint(acopy, C_INT.byteSize() * i, C_INT, attriblist[i]);
		try {
		    ret = (MemorySegment)OSMesaCreateContextAttribs.invoke(acopy, ornull(sharelist, OSMesaContext::mem));
		} catch(Throwable e) {
		    throw(new RuntimeException(e));
		}
	    }
	    return(nullp(ret) ? null : new OSMesaContext(ret));
	}

	private final MethodHandle OSMesaDestroyContext = ld.downcallHandle(mesa.find("OSMesaDestroyContext").get(), FunctionDescriptor.ofVoid(C_OSMesaContext));
	public void OSMesaDestroyContext(OSMesaContext ctx) {
	    try {
		OSMesaDestroyContext.invoke(ctx.mem());
	    } catch(Throwable e) {
		throw(new RuntimeException(e));
	    }
	}

	private final MethodHandle OSMesaMakeCurrent = ld.downcallHandle(mesa.find("OSMesaMakeCurrent").get(), FunctionDescriptor.of(GLboolean, C_OSMesaContext, ADDRESS, GLenum, GLsizei, GLsizei));
	public boolean OSMesaMakeCurrent(OSMesaContext ctx, ByteBuffer buffer, int type, int width, int height) {
	    try {
		return((int)OSMesaMakeCurrent.invoke(ornull(ctx, OSMesaContext::mem), ornull(buffer, MemorySegment::ofBuffer), type, width, height) != 0);
	    } catch(Throwable e) {
		throw(new RuntimeException(e));
	    }
	}

	private final MethodHandle OSMesaGetProcAddress = ld.downcallHandle(mesa.find("OSMesaGetProcAddress").get(), FunctionDescriptor.of(ADDRESS, ADDRESS));
	MemorySegment OSMesaGetProcAddress(String funcName) {
	    try(Arena st = Arena.ofConfined()) {
		return((MemorySegment)OSMesaGetProcAddress.invoke(st.allocateFrom(funcName, C_CHARSET)));
	    } catch(Throwable e) {
		throw(new RuntimeException(e));
	    }
	}
    }

    class Resolved extends OpenGL.Base {
	protected MethodHandle lookup(String name, FunctionDescriptor sig, Linker.Option... options) {
	    MemorySegment addr = OSMesaGetProcAddress(name);
	    if(nullp(addr))
		return(null);
	    return(ld.downcallHandle(addr, sig, options));
	}
    }

    public OpenGL gl() {
	return(new Resolved());
    }

    private static OSMesa instance = null;
    public static OSMesa get() {
	if(instance == null) {
	    synchronized(OSMesa.class) {
		if(instance == null) {
		    instance = new libOSMesa_so_8();
		}
	    }
	}
	return(instance);
    }
}
