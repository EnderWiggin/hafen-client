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

package haven.ffi.windows;

import haven.*;
import haven.ffi.*;
import java.nio.*;
import java.lang.invoke.*;
import java.lang.foreign.*;
import static haven.ffi.ABI.*;
import static haven.ffi.FUtils.*;
import static java.lang.foreign.ValueLayout.ADDRESS;
import static haven.ffi.windows.Win32.*;

public abstract class SHCore {
    public static final int MDT_EFFECTIVE_DPI = 0;
    public static final int MDT_ANGULAR_DPI = 1;
    public static final int MDT_RAW_DPI = 2;

    public abstract Coord GetDpiForMonitor(Handle hmonitor, int dpiType);
    public abstract int GetScaleFactorForMonitor(Handle hmonitor);

    static class Win64Unicode extends SHCore {
	private static final MemoryLayout HRESULT = Win32.Win64Unicode.HRESULT;
	private static final MemoryLayout HMONITOR = Win32.Win64Unicode.HMONITOR;
	private static final MemoryLayout UINT = Win32.Win64Unicode.UINT;
	private final SymbolLookup shcore = SymbolLookup.libraryLookup("SHCORE.DLL", Arena.global());
	private final Win32 win = Win32.get();

	private final MethodHandle GetDpiForMonitor = ld.downcallHandle(shcore.find("GetDpiForMonitor").get(), FunctionDescriptor.of(HRESULT, HMONITOR, C_ENUM, ADDRESS, ADDRESS));
	public Coord GetDpiForMonitor(Handle hmonitor, int dpiType) {
	    try(Arena st = Arena.ofConfined()) {
		MemorySegment xbuf = st.allocate(UINT), ybuf = st.allocate(UINT);
		int rv;
		try {
		    rv = (int)GetDpiForMonitor.invoke(hmonitor.bits, dpiType, xbuf, ybuf);
		} catch(Throwable e) {
		    throw(new RuntimeException(e));
		}
		if(rv != Win32.S_OK)
		    throw(new HResultError(rv));
		return(Coord.of((int)getint(xbuf, 0, UINT, false), (int)getint(ybuf, 0, UINT, false)));
	    }
	}

	private final MethodHandle GetScaleFactorForMonitor = ld.downcallHandle(shcore.find("GetScaleFactorForMonitor").get(), FunctionDescriptor.of(HRESULT, HMONITOR, ADDRESS));
	public int GetScaleFactorForMonitor(Handle hmonitor) {
	    try(Arena st = Arena.ofConfined()) {
		MemorySegment buf = st.allocate(C_ENUM);
		int rv;
		try {
		    rv = (int)GetScaleFactorForMonitor.invoke(hmonitor.bits, buf);
		} catch(Throwable e) {
		    throw(new RuntimeException(e));
		}
		if(rv != Win32.S_OK)
		    throw(new HResultError(rv));
		return((int)getint(buf, 0, C_ENUM, true));
	    }
	}
    }

    private static SHCore instance = null;
    public static SHCore get() {
	if(instance == null) {
	    synchronized(SHCore.class) {
		if(instance == null) {
		    instance = new Win64Unicode();
		}
	    }
	}
	return(instance);
    }
}
