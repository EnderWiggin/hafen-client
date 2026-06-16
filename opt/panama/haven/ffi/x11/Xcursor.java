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

import haven.*;
import haven.ffi.*;
import java.lang.invoke.*;
import java.lang.foreign.*;
import java.lang.foreign.MemoryLayout.PathElement;
import java.nio.*;
import static haven.ffi.ABI.*;
import static haven.ffi.FUtils.*;
import static haven.ffi.x11.XLib.*;
import static java.lang.foreign.ValueLayout.ADDRESS;

public abstract class Xcursor {
    public static abstract class XcursorImage extends StructInstance {
	XcursorImage(MemorySegment mem) {
	    super(mem);
	}

	MemorySegment mem() {return(mem);}
    }

    public abstract boolean XcursorSupportsARGB(Display dpy);
    public abstract int XcursorGetDefaultSize(Display dpy);
    public abstract XcursorImage XcursorImageCreate(Coord sz, Coord hot, int[] pixels);
    public abstract XID XcursorImageLoadCursor(Display dpy, XcursorImage img);
    public abstract XID XcursorLibraryLoadCursor(Display dpy, String name);

    static class libXcursor_so_1 extends Xcursor {
	private static final MemoryLayout C_XID = libX11_so_6.C_XID;
	private static final MemoryLayout C_XcursorBool = C_INT;
	private static final ValueLayout.OfInt C_XcursorUint = ValueLayout.JAVA_INT;
	private static final ValueLayout.OfInt C_XcursorDim = C_XcursorUint;
	private final SymbolLookup Xcursor = SymbolLookup.libraryLookup("libXcursor.so.1", Arena.global());

	private final MethodHandle XcursorSupportsARGB = ld.downcallHandle(Xcursor.find("XcursorSupportsARGB").get(), FunctionDescriptor.of(C_XcursorBool, ADDRESS));
	public boolean XcursorSupportsARGB(Display dpy) {
	    try {
		return(((long)XcursorSupportsARGB.invoke(dpy.mem())) != 0);
	    } catch(Throwable e) {
		throw(new RuntimeException(e));
	    } finally {
		checkerror();
	    }
	}

	private final MethodHandle XcursorGetDefaultSize = ld.downcallHandle(Xcursor.find("XcursorGetDefaultSize").get(), FunctionDescriptor.of(C_INT, ADDRESS));
	public int XcursorGetDefaultSize(Display dpy) {
	    try {
		return((int)XcursorGetDefaultSize.invoke(dpy.mem()));
	    } catch(Throwable e) {
		throw(new RuntimeException(e));
	    } finally {
		checkerror();
	    }
	}

	private static final StructLayout _XcursorImage = struct(new MemoryLayout[] {
		C_XcursorUint.withName("version"),
		C_XcursorDim.withName("size"),
		C_XcursorDim.withName("width"),
		C_XcursorDim.withName("height"),
		C_XcursorDim.withName("xhot"),
		C_XcursorDim.withName("yhot"),
		C_XcursorDim.withName("delay"),
		ADDRESS.withName("pixels"),
	    });
	public static class XcursorImage extends Xcursor.XcursorImage {
	    private XcursorImage(MemorySegment mem) {
		super(mem);
	    }

	    protected StructLayout $layout() {return(_XcursorImage);}

	    private static final VarHandle xhot = _XcursorImage.varHandle(PathElement.groupElement("xhot"));
	    public XcursorImage xhot(int value) {xhot.set(mem, 0, value); return(this);}
	    private static final VarHandle yhot = _XcursorImage.varHandle(PathElement.groupElement("yhot"));
	    public XcursorImage yhot(int value) {yhot.set(mem, 0, value); return(this);}
	    
	    private static final VarHandle pixels = _XcursorImage.varHandle(PathElement.groupElement("pixels"));
	    public MemorySegment pixels() {return((MemorySegment)pixels.get(mem, 0));}
	}

	private final MethodHandle XcursorImageCreate = ld.downcallHandle(Xcursor.find("XcursorImageCreate").get(), FunctionDescriptor.of(ADDRESS, C_INT, C_INT));
	public Xcursor.XcursorImage XcursorImageCreate(Coord sz, Coord hot, int[] pixels) {
	    if(pixels.length != (sz.x * sz.y))
		throw(new IllegalArgumentException());
	    XcursorImage img;
	    try {
		MemorySegment mem = (MemorySegment)XcursorImageCreate.invoke(sz.x, sz.y);
		img = new XcursorImage(mem);
		Finalizer.finalize(img, () -> XcursorImageDestroy(mem));
	    } catch(Throwable e) {
		throw(new RuntimeException(e));
	    } finally {
		checkerror();
	    }
	    img.xhot(hot.x).yhot(hot.y);
	    MemorySegment pxd = img.pixels().reinterpret(C_XcursorUint.byteSize() * sz.x * sz.y);
	    for(int i = 0; i < pixels.length; i++)
		pxd.set(C_XcursorUint, i * C_XcursorUint.byteSize(), pixels[i]);
	    return(img);
	}

	private final MethodHandle XcursorImageDestroy = ld.downcallHandle(Xcursor.find("XcursorImageDestroy").get(), FunctionDescriptor.ofVoid(ADDRESS));
	public void XcursorImageDestroy(MemorySegment img) {
	    try {
		XcursorImageDestroy.invoke(img);
	    } catch(Throwable e) {
		throw(new RuntimeException(e));
	    } finally {
		checkerror();
	    }
	}

	private final MethodHandle XcursorImageLoadCursor = ld.downcallHandle(Xcursor.find("XcursorImageLoadCursor").get(), FunctionDescriptor.of(C_XID, ADDRESS, ADDRESS));
	public XID XcursorImageLoadCursor(Display dpy, Xcursor.XcursorImage img) {
	    try {
		return(XID.of((long)XcursorImageLoadCursor.invoke(dpy.mem(), img.mem())));
	    } catch(Throwable e) {
		throw(new RuntimeException(e));
	    } finally {
		checkerror();
	    }
	}

	private final MethodHandle XcursorLibraryLoadCursor = ld.downcallHandle(Xcursor.find("XcursorLibraryLoadCursor").get(), FunctionDescriptor.of(C_XID, ADDRESS, ADDRESS));
	public XID XcursorLibraryLoadCursor(Display dpy, String name) {
	    try(Arena st = Arena.ofConfined()) {
		return(XID.of((long)XcursorLibraryLoadCursor.invoke(dpy.mem(), st.allocateFrom(name, C_CHARSET))));
	    } catch(Throwable e) {
		throw(new RuntimeException(e));
	    } finally {
		checkerror();
	    }
	}
    }

    private static Xcursor instance = null;
    public static Xcursor get() {
	if(instance == null) {
	    synchronized(Xcursor.class) {
		if(instance == null) {
		    instance = new libXcursor_so_1();
		}
	    }
	}
	return(instance);
    }
}
