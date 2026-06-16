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

package haven.ffi.objc;

import haven.*;
import haven.ffi.*;
import haven.ffi.objc.Runtime.*;
import java.lang.invoke.*;
import java.lang.foreign.*;
import java.lang.foreign.MemoryLayout.PathElement;
import java.util.*;
import static haven.ffi.ABI.*;
import static haven.ffi.FUtils.*;
import static java.lang.foreign.ValueLayout.ADDRESS;

public abstract class CoreGraphics {
    public static interface CGPoint {
	MemorySegment mem();

	public double x();
	public CGPoint x(double val);
	public double y();
	public CGPoint y(double val);
    }

    public abstract MemoryLayout C_CGPoint();
    public abstract CGPoint CGPoint(MemorySegment mem);
    public abstract CGPoint CGPoint(Arena alloc);
    public abstract CGPoint CGPoint();

    public static interface CGSize {
	MemorySegment mem();

	public double width();
	public CGSize width(double val);
	public double height();
	public CGSize height(double val);
    }

    public abstract MemoryLayout C_CGSize();
    public abstract CGSize CGSize(MemorySegment mem);
    public abstract CGSize CGSize(Arena alloc);
    public abstract CGSize CGSize();

    public static interface CGRect {
	MemorySegment mem();

	public CGPoint origin();
	public CGSize size();
    }

    public abstract MemoryLayout C_CGRect();
    public abstract CGRect CGRect();

    static class VersionA extends CoreGraphics {
	static final MemoryLayout CGFloat = C_DOUBLE;
	private final SymbolLookup dylib = SymbolLookup.libraryLookup("/System/Library/Frameworks/CoreGraphics.framework/CoreGraphics", Arena.global());

	static final StructLayout _CGPoint = struct(new MemoryLayout[] {
		CGFloat.withName("x"),
		CGFloat.withName("y"),
	    });
	static class CGPoint extends StructInstance implements CoreGraphics.CGPoint {
	    CGPoint(MemorySegment mem) {
		super(mem);
	    }
	    CGPoint(Arena alloc) {
		this(alloc.allocate(_CGPoint));
	    }
	    CGPoint() {
		this(Arena.ofAuto());
	    }

	    protected StructLayout $layout() {return(_CGPoint);}
	    public MemorySegment mem() {return(mem);}

	    private static final VarHandle x = _CGPoint.varHandle(PathElement.groupElement("x"));
	    public double x() {return((double)x.get(mem, 0));}
	    public CGPoint x(double val) {x.set(mem, 0, (float)val); return(this);}
	    private static final VarHandle y = _CGPoint.varHandle(PathElement.groupElement("y"));
	    public double y() {return((double)y.get(mem, 0));}
	    public CGPoint y(double val) {y.set(mem, 0, (float)val); return(this);}
	}
	public MemoryLayout C_CGPoint() {return(_CGPoint);}
	public CGPoint CGPoint(MemorySegment mem) {return(new CGPoint(mem));}
	public CGPoint CGPoint(Arena alloc) {return(new CGPoint(alloc));}
	public CGPoint CGPoint() {return(new CGPoint());}

	static final StructLayout _CGSize = struct(new MemoryLayout[] {
		CGFloat.withName("width"),
		CGFloat.withName("height"),
	    });
	static class CGSize extends StructInstance implements CoreGraphics.CGSize {
	    CGSize(MemorySegment mem) {
		super(mem);
	    }
	    CGSize(Arena alloc) {
		this(alloc.allocate(_CGSize));
	    }
	    CGSize() {
		this(Arena.ofAuto());
	    }

	    protected StructLayout $layout() {return(_CGSize);}
	    public MemorySegment mem() {return(mem);}

	    private static final VarHandle width = _CGSize.varHandle(PathElement.groupElement("width"));
	    public double width() {return((double)width.get(mem, 0));}
	    public CGSize width(double val) {width.set(mem, 0, (float)val); return(this);}
	    private static final VarHandle height = _CGSize.varHandle(PathElement.groupElement("height"));
	    public double height() {return((double)height.get(mem, 0));}
	    public CGSize height(double val) {height.set(mem, 0, (float)val); return(this);}
	}
	public MemoryLayout C_CGSize() {return(_CGSize);}
	public CGSize CGSize(MemorySegment mem) {return(new CGSize(mem));}
	public CGSize CGSize(Arena alloc) {return(new CGSize(alloc));}
	public CGSize CGSize() {return(new CGSize());}

	static final StructLayout _CGRect = struct(new MemoryLayout[] {
		_CGPoint.withName("origin"),
		_CGSize.withName("size"),
	    });
	static class CGRect extends StructInstance implements CoreGraphics.CGRect {
	    CGRect(MemorySegment mem) {
		super(mem);
	    }
	    CGRect(Arena alloc) {
		this(alloc.allocate(_CGRect));
	    }
	    CGRect() {
		this(Arena.ofAuto());
	    }

	    protected StructLayout $layout() {return(_CGRect);}
	    public MemorySegment mem() {return(mem);}

	    private static final long origin = _CGRect.byteOffset(PathElement.groupElement("origin"));
	    public CGPoint origin() {return(new CGPoint(mem.asSlice(origin, _CGPoint)));}
	    private static final long size = _CGRect.byteOffset(PathElement.groupElement("size"));
	    public CGSize size() {return(new CGSize(mem.asSlice(size, _CGSize)));}
	}
	public MemoryLayout C_CGRect() {return(_CGRect);}
	public CGRect CGRect() {return(new CGRect());}
    }

    private static CoreGraphics instance = null;
    public static CoreGraphics get() {
	if(instance == null) {
	    synchronized(CoreGraphics.class) {
		if(instance == null) {
		    instance = new VersionA();
		}
	    }
	}
	return(instance);
    }
}
