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

package haven.ffi.posix;

import haven.ffi.*;
import java.nio.*;
import java.util.function.*;
import java.lang.invoke.*;
import java.lang.foreign.*;
import java.lang.foreign.MemoryLayout.PathElement;
import static haven.ffi.ABI.*;
import static haven.ffi.FUtils.*;
import static java.lang.foreign.ValueLayout.ADDRESS;

public abstract class LibC {
    public static final int POLLIN = 1;
    public static final int POLLPRI = 2;
    public static final int POLLOUT = 4;
    public static final int POLLERR = 8;
    public static final int POLLHUP = 16;
    public static final int POLLNVAL = 32;

    public static final int EPERM = 1;
    public static final int ENOENT = 2;
    public static final int ESRCH = 3;
    public static final int EINTR = 4;
    public static final int EIO = 5;
    public static final int ENXIO = 6;
    public static final int E2BIG = 7;
    public static final int ENOEXEC = 8;
    public static final int EBADF = 9;
    public static final int ECHILD = 10;
    public static final int EAGAIN = 11;
    public static final int ENOMEM = 12;
    public static final int EACCES = 13;
    public static final int EFAULT = 14;
    public static final int ENOTBLK = 15;
    public static final int EBUSY = 16;
    public static final int EEXIST = 17;
    public static final int EXDEV = 18;
    public static final int ENODEV = 19;
    public static final int ENOTDIR = 20;
    public static final int EISDIR = 21;
    public static final int EINVAL = 22;
    public static final int ENFILE = 23;
    public static final int EMFILE = 24;
    public static final int ENOTTY = 25;
    public static final int ETXTBSY = 26;
    public static final int EFBIG = 27;
    public static final int ENOSPC = 28;
    public static final int ESPIPE = 29;
    public static final int EROFS = 30;
    public static final int EMLINK = 31;
    public static final int EPIPE = 32;
    public static final int EDOM = 33;
    public static final int ERANGE = 34;

    public abstract static class PollFD {
	public final int length;
	protected final MemorySegment mem;

	PollFD(MemorySegment mem, int length) {
	    this.mem = mem.reinterpret($layout().byteSize() * length);
	    this.length = length;
	}

	protected abstract StructLayout $layout();

	protected int ck(int idx) {
	    if((idx < 0) || (idx >= length))
		throw(new ArrayIndexOutOfBoundsException(idx));
	    return(idx);
	}

	public abstract PollFD fd(int idx, int value);
	public abstract PollFD events(int idx, int value);
	public abstract PollFD revents(int idx, int value);
	public abstract int fd(int idx);
	public abstract int events(int idx);
	public abstract int revents(int idx);
    }

    public abstract String strerror(int errnum);
    public abstract int getpid();
    public abstract void close(int fd);
    public abstract byte[] read(int fd, int count);
    public abstract int write(int fd, ByteBuffer buf, int count);
    public abstract int[] pipe();
    public abstract PollFD pollfd(int length);
    public abstract int poll(LibC.PollFD fds, int nfds, int timeout);

    public int write(int fd, byte[] buf, int off, int len) {
	ByteBuffer w = ByteBuffer.wrap(buf);
	w.position(off).limit(off + len);
	return(write(fd, w, len));
    }
    public int write(int fd, byte[] buf) {
	return(write(fd, buf, 0, buf.length));
    }

    static class Builtin extends LibC {
	private static final MemoryLayout NFDS_T = C_LONG;
	private static final MemoryLayout PID_T = C_INT;
	private static final Linker.Option errcall = Linker.Option.captureCallState("errno");
	private static final MemoryLayout errnofmt = Linker.Option.captureStateLayout();
	private static final VarHandle errnovar = errnofmt.varHandle(PathElement.groupElement("errno"));
	private static final ThreadLocal<Integer> errno = new ThreadLocal<>();
	private final SymbolLookup libc = ld.defaultLookup();

	interface NativeCall<T> {
	    T call(Arena stack, MemorySegment errnobuf) throws Throwable;
	}

	private <T> T stdcall(NativeCall<T> fun) {
	    try(Arena st = Arena.ofConfined()) {
		MemorySegment errnobuf = st.allocate(errnofmt);
		T ret;
		try {
		    ret = fun.call(st, errnobuf);
		} catch(Throwable t) {
		    throw(new RuntimeException(t));
		}
		errno.set((int)errnovar.get(errnobuf, 0));
		return(ret);
	    }
	}

	private void zerocall(NativeCall<Integer> fun) {
	    if(stdcall(fun) != 0)
		throw(new StdError(errno.get()));
	}

	private int poscall(NativeCall<Integer> fun) {
	    int ret = stdcall(fun);
	    if(ret < 0)
		throw(new StdError(errno.get()));
	    return(ret);
	}

	private final MethodHandle strerror = ld.downcallHandle(libc.find("strerror").get(), FunctionDescriptor.of(ADDRESS, C_INT));
	public String strerror(int errnum) {
	    MemorySegment ret;
	    try {
		ret = (MemorySegment)strerror.invoke(errnum);
	    } catch(Throwable t) {
		throw(new RuntimeException(t));
	    }
	    return(nullp(ret) ? null : ret.reinterpret(Long.MAX_VALUE).getString(0, C_CHARSET));
	}

	private final MethodHandle getpid = ld.downcallHandle(libc.find("getpid").get(), FunctionDescriptor.of(PID_T));
	public int getpid() {
	    try {
		return((int)getpid.invoke());
	    } catch(Throwable t) {
		throw(new RuntimeException(t));
	    }
	}

	private final MethodHandle close = ld.downcallHandle(libc.find("close").get(), FunctionDescriptor.of(C_INT, C_INT), errcall);
	public void close(int fd) {
	    zerocall((st, err) -> (int)close.invoke(err, fd));
	}

	private final MethodHandle pipe = ld.downcallHandle(libc.find("pipe").get(), FunctionDescriptor.of(C_INT, ADDRESS), errcall);
	public int[] pipe() {
	    int[] ret = new int[2];
	    zerocall((st, err) -> {
		MemorySegment buf = st.allocate(C_INT, 2);
		if((int)pipe.invoke(err, buf) != 0)
		    return(-1);
		for(int i = 0; i < 2; i++)
		    ret[i] = (int)getint(buf, i * C_INT.byteSize(), C_INT, true);
		return(0);
	    });;
	    return(ret);
	}

	private final MethodHandle read = ld.downcallHandle(libc.find("read").get(), FunctionDescriptor.of(SIZE_T, C_INT, ADDRESS, SIZE_T), errcall);
	public byte[] read(int fd, int count) {
	    return(stdcall((st, err) -> {
		MemorySegment buf = st.allocate(C_CHAR, count);
		long rv = (long)read.invoke(err, fd, buf, count);
		if(rv < 0)
		    throw(new StdError(errno.get()));
		return(memcpy(new byte[(int)rv], buf, 0, 0, (int)rv));
	    }));
	}

	private final MethodHandle write = ld.downcallHandle(libc.find("write").get(), FunctionDescriptor.of(SIZE_T, C_INT, ADDRESS, SIZE_T), errcall);
	public int write(int fd, ByteBuffer buf, int count) {
	    return(stdcall((st, err) -> {
		long rv = (long)write.invoke(err, fd, bufcpy(st, buf, count), count);
		if(rv < 0)
		    throw(new StdError(errno.get()));
		return((int)rv);
	    }));
	}

	static final StructLayout _pollfd = struct(new MemoryLayout[] {
		C_INT.withName("fd"),
		C_SHORT.withName("events"),
		C_SHORT.withName("revents"),
	    });
	public static class PollFD extends LibC.PollFD {
	    PollFD(MemorySegment mem, int length) {
		super(mem, length);
	    }

	    PollFD(int length) {
		this(Arena.ofAuto().allocate(_pollfd, length), length);
	    }

	    protected StructLayout $layout() {return(_pollfd);}

	    private static final VarHandle fd = _pollfd.varHandle(PathElement.groupElement("fd"));
	    public int fd(int idx) {return((int)fd.get(mem, _pollfd.byteSize() * ck(idx)));}
	    public PollFD fd(int idx, int value) {fd.set(mem, _pollfd.byteSize() * ck(idx), value); return(this);}
	    private static final VarHandle events = _pollfd.varHandle(PathElement.groupElement("events"));
	    public int events(int idx) {return((int)events.get(mem, _pollfd.byteSize() * ck(idx)));}
	    public PollFD events(int idx, int value) {events.set(mem, _pollfd.byteSize() * ck(idx), (short)value); return(this);}
	    private static final VarHandle revents = _pollfd.varHandle(PathElement.groupElement("revents"));
	    public int revents(int idx) {return((int)revents.get(mem, _pollfd.byteSize() * ck(idx)));}
	    public PollFD revents(int idx, int value) {revents.set(mem, _pollfd.byteSize() * ck(idx), (short)value); return(this);}
	}

	public PollFD pollfd(int length) {return(new PollFD(length));}

	private final MethodHandle poll = ld.downcallHandle(libc.find("poll").get(), FunctionDescriptor.of(C_INT, ADDRESS, NFDS_T, C_INT), errcall);
	public int poll(LibC.PollFD fds, int nfds, int timeout) {
	    if((nfds < 0) || (nfds > fds.length))
		throw(new ArrayIndexOutOfBoundsException(nfds));
	    return(poscall((st, err) -> (int)poll.invoke(err, fds.mem, nfds, timeout)));
	}
    }

    private static LibC instance = null;
    public static LibC get() {
	if(instance == null) {
	    synchronized(LibC.class) {
		if(instance == null) {
		    instance = new Builtin();
		}
	    }
	}
	return(instance);
    }
}
