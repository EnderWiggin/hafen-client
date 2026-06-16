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
import java.util.*;
import static haven.ffi.ABI.*;
import static haven.ffi.FUtils.*;
import static haven.ffi.x11.XLib.*;
import static java.lang.foreign.ValueLayout.ADDRESS;

public abstract class Xrandr {
    public static final int RR_Connected = 0;
    public static final int RR_Disconnected = 1;
    public static final int RR_UnknownConnection = 2;

    public static final int SubPixelUnknown = 0;
    public static final int SubPixelHorizontalRGB = 1;
    public static final int SubPixelHorizontalBGR = 2;
    public static final int SubPixelVerticalRGB = 3;
    public static final int SubPixelVerticalBGR = 4;
    public static final int SubPixelNone = 5;

    public static final int RR_Rotate_0 = 1;
    public static final int RR_Rotate_90 = 2;
    public static final int RR_Rotate_180 = 4;
    public static final int RR_Rotate_270 = 8;
    public static final int RR_Reflect_X = 16;
    public static final int RR_Reflect_Y = 32;

    public static class XRRExtensionInfo {
	public final int event, error, major, minor;

	public XRRExtensionInfo(int event, int error, int major, int minor) {
	    this.event = event;
	    this.error = error;
	    this.major = major;
	    this.minor = minor;
	}
    }

    public static abstract class XRRScreenResources extends StructInstance {
	protected XRRScreenResources(MemorySegment mem) {
	    super(mem);
	}

	MemorySegment mem() {return(mem);}

	public abstract long timestamp();
	public abstract long configTimestamp();
	public abstract List<XID> crtcs();
	public abstract List<XID> outputs();
    }

    public static abstract class XRROutputInfo extends StructInstance {
	protected XRROutputInfo(MemorySegment mem) {
	    super(mem);
	}

	MemorySegment mem() {return(mem);}

	public abstract long timestamp();
	public abstract XID crtc();
	public abstract String name();
	public abstract Coord mm_size();
	public abstract int connection();
	public abstract int subpixel_order();
	public abstract List<XID> crtcs();
	public abstract List<XID> clones();
    }

    public static abstract class XRRCrtcInfo extends StructInstance {
	protected XRRCrtcInfo(MemorySegment mem) {
	    super(mem);
	}

	MemorySegment mem() {return(mem);}

	public abstract long timestamp();
	public abstract Coord pos();
	public abstract Coord size();
	public abstract int rotation();
	public abstract List<XID> outputs();
	public abstract int rotations();
	public abstract List<XID> possible();
    }

    public abstract XRRExtensionInfo XRRQueryExtension(Display dpy);
    public abstract XRRScreenResources XRRGetScreenResources(XLib.Display dpy, XID window);
    public abstract XRROutputInfo XRRGetOutputInfo(XLib.Display dpy, XRRScreenResources resources, XID output);
    public abstract XRRCrtcInfo XRRGetCrtcInfo(XLib.Display dpy, XRRScreenResources resources, XID output);

    static class libXrandr_so_2 extends Xrandr {
	private static final MemoryLayout C_XBool = libX11_so_6.C_XBool;
	private static final MemoryLayout C_Status = libX11_so_6.C_Status;
	private static final MemoryLayout C_Time = libX11_so_6.C_Time;
	private static final MemoryLayout C_XID = libX11_so_6.C_XID;
	private static final MemoryLayout C_XRREnum = C_SHORT;
	private final SymbolLookup Xrandr = SymbolLookup.libraryLookup("libXrandr.so.2", Arena.global());

	private final MethodHandle XRRQueryExtension = ld.downcallHandle(Xrandr.find("XRRQueryExtension").get(), FunctionDescriptor.of(C_XBool, ADDRESS, ADDRESS, ADDRESS));
	private final MethodHandle XRRQueryVersion = ld.downcallHandle(Xrandr.find("XRRQueryVersion").get(), FunctionDescriptor.of(C_Status, ADDRESS, ADDRESS, ADDRESS));
	public XRRExtensionInfo XRRQueryExtension(Display dpy) {
	    try(Arena st = Arena.ofConfined()) {
		MemorySegment evbuf = st.allocate(C_INT), erbuf = st.allocate(C_INT);
		MemorySegment mabuf = st.allocate(C_INT), mibuf = st.allocate(C_INT);
		try {
		    if(((int)XRRQueryExtension.invoke(dpy.mem(), evbuf, erbuf)) == 0)
			return(null);
		    XRRQueryVersion.invoke(dpy.mem(), mabuf, mibuf);
		} catch(Throwable e) {
		    throw(new RuntimeException(e));
		} finally {
		    checkerror();
		}
		return(new XRRExtensionInfo((int)getint(evbuf, 0, C_INT, true), (int)getint(erbuf, 0, C_INT, true),
					    (int)getint(mabuf, 0, C_INT, true), (int)getint(mibuf, 0, C_INT, true)));
	    }
	}

	static final StructLayout _XRRScreenResources = struct(new MemoryLayout[] {
		C_Time.withName("timestamp"),
		C_Time.withName("configTimestamp"),
		C_INT.withName("ncrtc"),
		ADDRESS.withName("crtcs"),
		C_INT.withName("noutput"),
		ADDRESS.withName("outputs"),
		C_INT.withName("nmode"),
		ADDRESS.withName("modes"),
	    });
	public static class XRRScreenResources extends Xrandr.XRRScreenResources {
	    XRRScreenResources(MemorySegment mem) {
		super(mem);
	    }

	    protected StructLayout $layout() {return(_XRRScreenResources);}

	    private static final VarHandle timestamp = _XRRScreenResources.varHandle(PathElement.groupElement("timestamp"));
	    public long timestamp() {return((long)timestamp.get(mem, 0));}
	    private static final VarHandle configTimestamp = _XRRScreenResources.varHandle(PathElement.groupElement("configTimestamp"));
	    public long configTimestamp() {return((long)configTimestamp.get(mem, 0));}
	    private static final VarHandle ncrtc = _XRRScreenResources.varHandle(PathElement.groupElement("ncrtc"));
	    private static final VarHandle crtcs = _XRRScreenResources.varHandle(PathElement.groupElement("crtcs"));
	    public List<XID> crtcs() {return(new MemArray<XID>((MemorySegment)crtcs.get(mem, 0), C_XID, (int)ncrtc.get(mem, 0), p -> XID.of(getint(p, 0, C_XID, false))));}
	    private static final VarHandle noutput = _XRRScreenResources.varHandle(PathElement.groupElement("noutput"));
	    private static final VarHandle outputs = _XRRScreenResources.varHandle(PathElement.groupElement("outputs"));
	    public List<XID> outputs() {return(new MemArray<XID>((MemorySegment)outputs.get(mem, 0), C_XID, (int)noutput.get(mem, 0), p -> XID.of(getint(p, 0, C_XID, false))));}
	}

	private final MethodHandle XRRGetScreenResources = ld.downcallHandle(Xrandr.find("XRRGetScreenResources").get(), FunctionDescriptor.of(ADDRESS, ADDRESS, C_XID));
	public XRRScreenResources XRRGetScreenResources(XLib.Display dpy, XID window) {
	    MemorySegment p;
	    try {
		if(C_XID instanceof ValueLayout.OfLong)
		    p = (MemorySegment)XRRGetScreenResources.invoke(dpy.mem(), (long)window.bits);
		else
		    p = (MemorySegment)XRRGetScreenResources.invoke(dpy.mem(), (int)window.bits);
	    } catch(Throwable e) {
		throw(new RuntimeException(e));
	    } finally {
		checkerror();
	    }
	    XRRScreenResources rv = new XRRScreenResources(p);
	    Finalizer.finalize(rv, () -> XRRFreeScreenResources(p));
	    return(rv);
	}

	private final MethodHandle XRRFreeScreenResources = ld.downcallHandle(Xrandr.find("XRRFreeScreenResources").get(), FunctionDescriptor.ofVoid(ADDRESS));
	private void XRRFreeScreenResources(MemorySegment p) {
	    try {
		XRRFreeScreenResources.invoke(p);
	    } catch(Throwable e) {
		throw(new RuntimeException(e));
	    } finally {
		checkerror();
	    }
	}

	static final StructLayout _XRROutputInfo = struct(new MemoryLayout[] {
		C_Time.withName("timestamp"),
		C_XID.withName("crtc"),
		ADDRESS.withName("name"),
		C_INT.withName("nameLen"),
		C_LONG.withName("mm_width"),
		C_LONG.withName("mm_height"),
		C_XRREnum.withName("connection"),
		C_XRREnum.withName("subpixel_order"),
		C_INT.withName("ncrtc"),
		ADDRESS.withName("crtcs"),
		C_INT.withName("nclone"),
		ADDRESS.withName("clones"),
		C_INT.withName("nmode"),
		C_INT.withName("npreferred"),
		ADDRESS.withName("modes"),
	    });
	public static class XRROutputInfo extends Xrandr.XRROutputInfo {
	    XRROutputInfo(MemorySegment mem) {
		super(mem);
	    }

	    protected StructLayout $layout() {return(_XRROutputInfo);}

	    private static final VarHandle timestamp = _XRROutputInfo.varHandle(PathElement.groupElement("timestamp"));
	    public long timestamp() {return((long)timestamp.get(mem, 0));}
	    private static final VarHandle crtc = _XRROutputInfo.varHandle(PathElement.groupElement("crtc"));
	    public XID crtc() {return(XID.of((long)crtc.get(mem, 0)));}
	    private static final VarHandle name = _XRROutputInfo.varHandle(PathElement.groupElement("name"));
	    private static final VarHandle nameLen = _XRROutputInfo.varHandle(PathElement.groupElement("nameLen"));
	    public String name() {return(nstring((MemorySegment)name.get(mem, 0), 0, (int)nameLen.get(mem, 0), Utils.utf8));}
	    private static final VarHandle mm_width = _XRROutputInfo.varHandle(PathElement.groupElement("mm_width"));
	    private static final VarHandle mm_height = _XRROutputInfo.varHandle(PathElement.groupElement("mm_height"));
	    public Coord mm_size() {return(Coord.of((int)(long)mm_width.get(mem, 0), (int)(long)mm_height.get(mem, 0)));}
	    private static final VarHandle connection = _XRROutputInfo.varHandle(PathElement.groupElement("connection"));
	    public int connection() {return((int)connection.get(mem, 0));}
	    private static final VarHandle subpixel_order = _XRROutputInfo.varHandle(PathElement.groupElement("subpixel_order"));
	    public int subpixel_order() {return((int)subpixel_order.get(mem, 0));}
	    private static final VarHandle ncrtc = _XRROutputInfo.varHandle(PathElement.groupElement("ncrtc"));
	    private static final VarHandle crtcs = _XRROutputInfo.varHandle(PathElement.groupElement("crtcs"));
	    public List<XID> crtcs() {return(new MemArray<XID>((MemorySegment)crtcs.get(mem, 0), C_XID, (int)ncrtc.get(mem, 0), p -> XID.of(getint(p, 0, C_XID, false))));}
	    private static final VarHandle nclone = _XRROutputInfo.varHandle(PathElement.groupElement("nclone"));
	    private static final VarHandle clones = _XRROutputInfo.varHandle(PathElement.groupElement("clones"));
	    public List<XID> clones() {return(new MemArray<XID>((MemorySegment)clones.get(mem, 0), C_XID, (int)nclone.get(mem, 0), p -> XID.of(getint(p, 0, C_XID, false))));}
	}

	private final MethodHandle XRRGetOutputInfo = ld.downcallHandle(Xrandr.find("XRRGetOutputInfo").get(), FunctionDescriptor.of(ADDRESS, ADDRESS, ADDRESS, C_XID));
	public XRROutputInfo XRRGetOutputInfo(XLib.Display dpy, Xrandr.XRRScreenResources resources, XID output) {
	    MemorySegment p;
	    try {
		if(C_XID instanceof ValueLayout.OfLong)
		    p = (MemorySegment)XRRGetOutputInfo.invoke(dpy.mem(), resources.mem(), (long)output.bits);
		else
		    p = (MemorySegment)XRRGetOutputInfo.invoke(dpy.mem(), resources.mem(), (int)output.bits);
	    } catch(Throwable e) {
		throw(new RuntimeException(e));
	    } finally {
		checkerror();
	    }
	    XRROutputInfo rv = new XRROutputInfo(p);
	    Finalizer.finalize(rv, () -> XRRFreeOutputInfo(p));
	    return(rv);
	}

	private final MethodHandle XRRFreeOutputInfo = ld.downcallHandle(Xrandr.find("XRRFreeOutputInfo").get(), FunctionDescriptor.ofVoid(ADDRESS));
	private void XRRFreeOutputInfo(MemorySegment p) {
	    try {
		XRRFreeOutputInfo.invoke(p);
	    } catch(Throwable e) {
		throw(new RuntimeException(e));
	    } finally {
		checkerror();
	    }
	}

	static final StructLayout _XRRCrtcInfo = struct(new MemoryLayout[] {
		C_Time.withName("timestamp"),
		C_INT.withName("x"),
		C_INT.withName("y"),
		C_INT.withName("width"),
		C_INT.withName("height"),
		C_XID.withName("mode"),
		C_XRREnum.withName("rotation"),
		C_INT.withName("noutput"),
		ADDRESS.withName("outputs"),
		C_XRREnum.withName("rotations"),
		C_INT.withName("npossible"),
		ADDRESS.withName("possible"),
	    });
	public static class XRRCrtcInfo extends Xrandr.XRRCrtcInfo {
	    XRRCrtcInfo(MemorySegment mem) {
		super(mem);
	    }

	    protected StructLayout $layout() {return(_XRRCrtcInfo);}

	    private static final VarHandle timestamp = _XRRCrtcInfo.varHandle(PathElement.groupElement("timestamp"));
	    public long timestamp() {return((long)timestamp.get(mem, 0));}
	    private static final VarHandle x = _XRRCrtcInfo.varHandle(PathElement.groupElement("x"));
	    private static final VarHandle y = _XRRCrtcInfo.varHandle(PathElement.groupElement("y"));
	    public Coord pos() {return(Coord.of((int)(long)x.get(mem, 0), (int)(long)y.get(mem, 0)));}
	    private static final VarHandle width = _XRRCrtcInfo.varHandle(PathElement.groupElement("width"));
	    private static final VarHandle height = _XRRCrtcInfo.varHandle(PathElement.groupElement("height"));
	    public Coord size() {return(Coord.of((int)(long)width.get(mem, 0), (int)(long)height.get(mem, 0)));}
	    private static final VarHandle rotation = _XRRCrtcInfo.varHandle(PathElement.groupElement("rotation"));
	    public int rotation() {return((int)rotation.get(mem, 0));}
	    private static final VarHandle noutput = _XRRCrtcInfo.varHandle(PathElement.groupElement("noutput"));
	    private static final VarHandle outputs = _XRRCrtcInfo.varHandle(PathElement.groupElement("outputs"));
	    public List<XID> outputs() {return(new MemArray<XID>((MemorySegment)outputs.get(mem, 0), C_XID, (int)noutput.get(mem, 0), p -> XID.of(getint(p, 0, C_XID, false))));}
	    private static final VarHandle rotations = _XRRCrtcInfo.varHandle(PathElement.groupElement("rotations"));
	    public int rotations() {return((int)rotations.get(mem, 0));}
	    private static final VarHandle npossible = _XRRCrtcInfo.varHandle(PathElement.groupElement("npossible"));
	    private static final VarHandle possible = _XRRCrtcInfo.varHandle(PathElement.groupElement("possible"));
	    public List<XID> possible() {return(new MemArray<XID>((MemorySegment)possible.get(mem, 0), C_XID, (int)npossible.get(mem, 0), p -> XID.of(getint(p, 0, C_XID, false))));}
	}

	private final MethodHandle XRRGetCrtcInfo = ld.downcallHandle(Xrandr.find("XRRGetCrtcInfo").get(), FunctionDescriptor.of(ADDRESS, ADDRESS, ADDRESS, C_XID));
	public XRRCrtcInfo XRRGetCrtcInfo(XLib.Display dpy, Xrandr.XRRScreenResources resources, XID output) {
	    MemorySegment p;
	    try {
		if(C_XID instanceof ValueLayout.OfLong)
		    p = (MemorySegment)XRRGetCrtcInfo.invoke(dpy.mem(), resources.mem(), (long)output.bits);
		else
		    p = (MemorySegment)XRRGetCrtcInfo.invoke(dpy.mem(), resources.mem(), (int)output.bits);
	    } catch(Throwable e) {
		throw(new RuntimeException(e));
	    } finally {
		checkerror();
	    }
	    XRRCrtcInfo rv = new XRRCrtcInfo(p);
	    Finalizer.finalize(rv, () -> XRRFreeCrtcInfo(p));
	    return(rv);
	}

	private final MethodHandle XRRFreeCrtcInfo = ld.downcallHandle(Xrandr.find("XRRFreeCrtcInfo").get(), FunctionDescriptor.ofVoid(ADDRESS));
	private void XRRFreeCrtcInfo(MemorySegment p) {
	    try {
		XRRFreeCrtcInfo.invoke(p);
	    } catch(Throwable e) {
		throw(new RuntimeException(e));
	    } finally {
		checkerror();
	    }
	}
    }

    private static Xrandr instance = null;
    public static Xrandr get() {
	if(instance == null) {
	    synchronized(Xrandr.class) {
		if(instance == null) {
		    instance = new libXrandr_so_2();
		}
	    }
	}
	return(instance);
    }
}
