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

package haven.ffi.misc;

import haven.*;
import haven.ffi.*;
import haven.ffi.posix.*;
import java.lang.invoke.*;
import java.lang.foreign.*;
import java.lang.foreign.MemoryLayout.PathElement;
import java.util.*;
import java.util.function.*;
import java.nio.*;
import static haven.ffi.ABI.*;
import static haven.ffi.FUtils.*;
import static java.lang.foreign.ValueLayout.ADDRESS;

public abstract class Alsa {
    public static final int SND_PCM_STREAM_PLAYBACK = 0;
    public static final int SND_PCM_STREAM_CAPTURE  = 1;

    public static final int SND_PCM_NONBLOCK = 1;
    public static final int SND_PCM_ASYNC    = 2;

    public static final int SND_PCM_ACCESS_MMAP_INTERLEAVED    = 0;
    public static final int SND_PCM_ACCESS_MMAP_NONINTERLEAVED = 1;
    public static final int SND_PCM_ACCESS_MMAP_COMPLEX        = 2;
    public static final int SND_PCM_ACCESS_RW_INTERLEAVED      = 3;
    public static final int SND_PCM_ACCESS_RW_NONINTERLEAVED   = 4;

    public static final int SND_PCM_FORMAT_UNKNOWN  = -1;
    public static final int SND_PCM_FORMAT_SB       =  0;
    public static final int SND_PCM_FORMAT_UB       =  1;
    public static final int SND_PCM_FORMAT_S16_LE   =  2;
    public static final int SND_PCM_FORMAT_S16_BE   =  3;
    public static final int SND_PCM_FORMAT_U16_LE   =  4;
    public static final int SND_PCM_FORMAT_U16_BE   =  5;
    public static final int SND_PCM_FORMAT_FLOAT_LE = 14;
    public static final int SND_PCM_FORMAT_FLOAT_BE = 15;

    public static interface Pcm extends AutoCloseable {
	public void close();
    }

    public static interface DeviceHints {
	public int size();
	public String get(int idx, String id);
    }

    public static interface HwParams {
	public HwParams copy();
	public HwParams any(Pcm pcm);

	public int rate();
	public Ratio rate_numden();
	public int channels();
	public int period_time();
	public int period_time_min();
	public int period_time_max();
	public long period_size();
	public long period_size_min();
	public long period_size_max();
	public int periods();
	public int periods_min();
	public int periods_max();
	public long buffer_size();

	public HwParams access(Pcm pcm, int val);
	public HwParams format(Pcm pcm, int val);
	public HwParams channels(Pcm pcm, int val);
	public HwParams rate(Pcm pcm, int val, int dir);
	public HwParams rate_near(Pcm pcm, int val, int dir);
	public HwParams buffer_size(Pcm pcm, int val);
	public HwParams buffer_size_min(Pcm pcm, int val);
	public HwParams buffer_size_near(Pcm pcm, int val);
    }

    public static interface SwParams {
	public SwParams copy();
	public SwParams current(Pcm pcm);

	public long avail_min();

	public SwParams avail_min(Pcm pcm, int val);
	public SwParams start_threshold(Pcm pcm, int val);
    }

    public static interface PcmInfo {
	public int device();
	public int subdevice();
	public int card();
	public String id();
	public String name();
	public String subdevice_name();
    }

    public static class AlsaException extends RuntimeException {
	public final int code;

	public AlsaException(String msg, int code) {
	    super(msg);
	    this.code = code;
	}
    }

    public abstract Pcm snd_pcm_open(String name, int stream, int mode);
    public abstract void snd_pcm_prepare(Pcm pcm);
    public abstract String snd_pcm_name(Pcm pcm);
    public abstract long snd_pcm_avail_update(Pcm pcm);
    public abstract boolean snd_pcm_wait(Pcm pcm, int timeout);
    public abstract int snd_pcm_writei(Pcm pcm, ByteBuffer buffer, int samples);
    public abstract HwParams snd_pcm_hw_params();
    public abstract void snd_pcm_hw_params_any(Pcm pcm, HwParams params);
    public abstract void snd_pcm_hw_params(Pcm pcm, HwParams params);
    public abstract SwParams snd_pcm_sw_params();
    public abstract void snd_pcm_sw_params(Pcm pcm, SwParams params);
    public abstract PcmInfo snd_pcm_info(Pcm pcm);
    public abstract int snd_card_next(int card);
    public abstract DeviceHints snd_device_name_hint(int card, String iface);

    public static class libasound_so_2 extends Alsa {
	static final MemoryLayout SFRAMES_T = C_LONG;
	static final MemoryLayout UFRAMES_T = C_LONG;
	private final SymbolLookup asound = loadlib("libasound.so.2", Arena.global());

	public static class Pcm implements Alsa.Pcm {
	    final MemorySegment mem;
	    private final Runnable clean;

	    Pcm(libasound_so_2 lib, MemorySegment mem) {
		this.mem = mem;
		clean = Finalizer.finalize(this, () -> lib.snd_pcm_close(mem));
	    }

	    public void close() {
		clean.run();
	    }

	    MemorySegment mem() {return(mem);}
	}

	public static class DeviceHints implements Alsa.DeviceHints {
	    public final libasound_so_2 lib;
	    final MemorySegment mem;
	    private final int n;
	    private final Runnable clean;

	    DeviceHints(libasound_so_2 lib, MemorySegment mem) {
		this.lib = lib;
		int n;
		for(n = 0; !nullp(mem.reinterpret(Long.MAX_VALUE).get(ADDRESS, ADDRESS.byteSize() * n)); n++);
		this.n = n;
		this.mem = mem.reinterpret(ADDRESS.byteSize() * n);
		clean = Finalizer.finalize(this, () -> lib.snd_device_name_free_hint(mem));
	    }

	    public int size() {
		return(n);
	    }

	    public String get(int idx, String id) {
		if((idx < 0) || (idx >= n))
		    throw(new ArrayIndexOutOfBoundsException(Integer.toString(idx)));
		return(lib.snd_device_name_get_hint(mem.get(ADDRESS, ADDRESS.byteSize() * idx), id));
	    }

	    MemorySegment mem() {return(mem);}
	}

	public static class HwParams implements Alsa.HwParams {
	    public final libasound_so_2 lib;
	    final MemorySegment mem;

	    HwParams(libasound_so_2 lib, MemorySegment mem) {
		this.lib = lib;
		this.mem = mem;
	    }
	    HwParams(libasound_so_2 lib) {
		this(lib, Arena.ofAuto().allocate(lib.snd_pcm_hw_params_sizeof()));
	    }

	    MemorySegment mem() {return(mem);}

	    public HwParams copy() {
		HwParams ret = lib.snd_pcm_hw_params();
		lib.snd_pcm_hw_params_copy(ret, this);
		return(ret);
	    }

	    public int rate() {return(lib.snd_pcm_hw_params_get_rate(this));}
	    public Ratio rate_numden() {return(lib.snd_pcm_hw_params_get_rate_numden(this));}
	    public int channels() {return(lib.snd_pcm_hw_params_get_channels(this));}
	    public int period_time() {return(lib.snd_pcm_hw_params_get_period_time(this));}
	    public int period_time_min() {return(lib.snd_pcm_hw_params_get_period_time_min(this));}
	    public int period_time_max() {return(lib.snd_pcm_hw_params_get_period_time_max(this));}
	    public long period_size() {return(lib.snd_pcm_hw_params_get_period_size(this));}
	    public long period_size_min() {return(lib.snd_pcm_hw_params_get_period_size_min(this));}
	    public long period_size_max() {return(lib.snd_pcm_hw_params_get_period_size_max(this));}
	    public int periods() {return(lib.snd_pcm_hw_params_get_periods(this));}
	    public int periods_min() {return(lib.snd_pcm_hw_params_get_periods_min(this));}
	    public int periods_max() {return(lib.snd_pcm_hw_params_get_periods_max(this));}
	    public long buffer_size() {return(lib.snd_pcm_hw_params_get_buffer_size(this));}

	    public HwParams any(Alsa.Pcm pcm) {lib.snd_pcm_hw_params_any(pcm, this); return(this);}
	    public HwParams access(Alsa.Pcm pcm, int val) {lib.snd_pcm_hw_params_set_access(pcm, this, val); return(this);}
	    public HwParams format(Alsa.Pcm pcm, int val) {lib.snd_pcm_hw_params_set_format(pcm, this, val); return(this);}
	    public HwParams channels(Alsa.Pcm pcm, int val) {lib.snd_pcm_hw_params_set_channels(pcm, this, val); return(this);}
	    public HwParams rate(Alsa.Pcm pcm, int val, int dir) {lib.snd_pcm_hw_params_set_rate(pcm, this, val, dir); return(this);}
	    public HwParams rate_near(Alsa.Pcm pcm, int val, int dir) {lib.snd_pcm_hw_params_set_rate_near(pcm, this, val, dir); return(this);}
	    public HwParams buffer_size(Alsa.Pcm pcm, int val) {lib.snd_pcm_hw_params_set_buffer_size(pcm, this, val); return(this);}
	    public HwParams buffer_size_min(Alsa.Pcm pcm, int val) {lib.snd_pcm_hw_params_set_buffer_size_min(pcm, this, val); return(this);}
	    public HwParams buffer_size_near(Alsa.Pcm pcm, int val) {lib.snd_pcm_hw_params_set_buffer_size_near(pcm, this, val); return(this);}
	}

	public static class SwParams implements Alsa.SwParams {
	    public final libasound_so_2 lib;
	    final MemorySegment mem;

	    SwParams(libasound_so_2 lib, MemorySegment mem) {
		this.lib = lib;
		this.mem = mem;
	    }
	    SwParams(libasound_so_2 lib) {
		this(lib, Arena.ofAuto().allocate(lib.snd_pcm_sw_params_sizeof()));
	    }

	    MemorySegment mem() {return(mem);}

	    public SwParams copy() {
		SwParams ret = lib.snd_pcm_sw_params();
		lib.snd_pcm_sw_params_copy(ret, this);
		return(ret);
	    }

	    public long avail_min() {return(lib.snd_pcm_sw_params_get_avail_min(this));}

	    public SwParams current(Alsa.Pcm pcm) {lib.snd_pcm_sw_params_current(pcm, this); return(this);}
	    public SwParams avail_min(Alsa.Pcm pcm, int val) {lib.snd_pcm_sw_params_set_avail_min(pcm, this, val); return(this);}
	    public SwParams start_threshold(Alsa.Pcm pcm, int val) {lib.snd_pcm_sw_params_set_start_threshold(pcm, this, val); return(this);}
	}

	public static class PcmInfo implements Alsa.PcmInfo {
	    public final libasound_so_2 lib;
	    final MemorySegment mem;

	    PcmInfo(libasound_so_2 lib, MemorySegment mem) {
		this.lib = lib;
		this.mem = mem;
	    }
	    PcmInfo(libasound_so_2 lib) {
		this(lib, Arena.ofAuto().allocate(lib.snd_pcm_info_sizeof()));
	    }

	    MemorySegment mem() {return(mem);}

	    public int device() {return(lib.snd_pcm_info_get_device(this));}
	    public int subdevice() {return(lib.snd_pcm_info_get_subdevice(this));}
	    public int card() {return(lib.snd_pcm_info_get_card(this));}
	    public String id() {return(lib.snd_pcm_info_get_id(this));}
	    public String name() {return(lib.snd_pcm_info_get_name(this));}
	    public String subdevice_name() {return(lib.snd_pcm_info_get_subdevice_name(this));}
	}

	private final MethodHandle snd_strerror = ld.downcallHandle(asound.find("snd_strerror").get(), FunctionDescriptor.of(ADDRESS, C_INT));
	public String snd_strerror(int errnum) {
	    MemorySegment rv;
	    try {
		rv = (MemorySegment)snd_strerror.invoke(errnum);
	    } catch(Throwable e) {
		throw(new InvocationException(e));
	    }
	    return(nullp(rv) ? null : rv.reinterpret(Long.MAX_VALUE).getString(0, C_CHARSET));
	}

	private final MethodHandle snd_pcm_close = ld.downcallHandle(asound.find("snd_pcm_close").get(), FunctionDescriptor.of(C_INT, ADDRESS));
	void snd_pcm_close(MemorySegment mem) {
	    try(Arena st = Arena.ofConfined()) {
		MemorySegment buf = st.allocate(ADDRESS);
		int rv;
		try {
		    rv = (int)snd_pcm_close.invoke(mem);
		} catch(Throwable e) {
		    throw(new InvocationException(e));
		}
		if(rv < 0)
		    throw(new AlsaException(snd_strerror(rv), rv));
	    }
	}

	private final MethodHandle snd_pcm_open = ld.downcallHandle(asound.find("snd_pcm_open").get(), FunctionDescriptor.of(C_INT, ADDRESS, ADDRESS, C_ENUM, C_INT));
	public Pcm snd_pcm_open(String name, int stream, int mode) {
	    try(Arena st = Arena.ofConfined()) {
		MemorySegment buf = st.allocate(ADDRESS);
		int rv;
		try {
		    rv = (int)snd_pcm_open.invoke(buf, st.allocateFrom(name, C_CHARSET), stream, mode);
		} catch(Throwable e) {
		    throw(new InvocationException(e));
		}
		if(rv < 0)
		    throw(new AlsaException(snd_strerror(rv), rv));
		Pcm pcm = new Pcm(this, buf.get(ADDRESS, 0));
		return(pcm);
	    }
	}

	private final MethodHandle snd_pcm_prepare = ld.downcallHandle(asound.find("snd_pcm_prepare").get(), FunctionDescriptor.of(C_INT, ADDRESS));
	public void snd_pcm_prepare(Alsa.Pcm pcm) {
	    int rv;
	    try {
		rv = (int)snd_pcm_prepare.invoke(((Pcm)pcm).mem());
	    } catch(Throwable e) {
		throw(new InvocationException(e));
	    }
	    if(rv < 0)
		throw(new AlsaException(snd_strerror(rv), rv));
	}

	private final MethodHandle snd_pcm_name = ld.downcallHandle(asound.find("snd_pcm_name").get(), FunctionDescriptor.of(ADDRESS, ADDRESS));
	public String snd_pcm_name(Alsa.Pcm pcm) {
	    MemorySegment rv;
	    try {
		rv = (MemorySegment)snd_pcm_name.invoke(((Pcm)pcm).mem());
	    } catch(Throwable e) {
		throw(new InvocationException(e));
	    }
	    return(nullp(rv) ? null : rv.reinterpret(Long.MAX_VALUE).getString(0, C_CHARSET));
	}

	private final MethodHandle snd_pcm_avail_update = ld.downcallHandle(asound.find("snd_pcm_avail_update").get(), FunctionDescriptor.of(SFRAMES_T, ADDRESS));
	public long snd_pcm_avail_update(Alsa.Pcm pcm) {
	    long rv;
	    try {
		rv = (long)snd_pcm_avail_update.invoke(((Pcm)pcm).mem());
	    } catch(Throwable e) {
		throw(new InvocationException(e));
	    }
	    if(rv < 0)
		throw(new AlsaException(snd_strerror((int)rv), (int)rv));
	    return(rv);
	}

	private final MethodHandle snd_pcm_wait = ld.downcallHandle(asound.find("snd_pcm_wait").get(), FunctionDescriptor.of(C_INT, ADDRESS, C_INT));
	public boolean snd_pcm_wait(Alsa.Pcm pcm, int timeout) {
	    int rv;
	    try {
		rv = (int)snd_pcm_wait.invoke(((Pcm)pcm).mem(), timeout);
	    } catch(Throwable e) {
		throw(new InvocationException(e));
	    }
	    if(rv < 0)
		throw(new AlsaException(snd_strerror(rv), rv));
	    return(rv != 0);
	}

	private final MethodHandle snd_pcm_writei = ld.downcallHandle(asound.find("snd_pcm_writei").get(), FunctionDescriptor.of(SFRAMES_T, ADDRESS, ADDRESS, UFRAMES_T));
	public int snd_pcm_writei(Alsa.Pcm pcm, ByteBuffer buffer, int samples) {
	    try(Arena st = Arena.ofConfined()) {
		long rv;
		try {
		    rv = (long)snd_pcm_writei.invoke(((Pcm)pcm).mem(), bufcpy(st, buffer, buffer.remaining()), samples);
		} catch(Throwable e) {
		    throw(new InvocationException(e));
		}
		if(rv < 0)
		    throw(new AlsaException(snd_strerror((int)rv), (int)rv));
		if(rv > Integer.MAX_VALUE)
		    throw(new AssertionError());
		return((int)rv);
	    }
	}

	private final MethodHandle snd_pcm_hw_params_sizeof = ld.downcallHandle(asound.find("snd_pcm_hw_params_sizeof").get(), FunctionDescriptor.of(SIZE_T));
	long snd_pcm_hw_params_sizeof() {
	    try {
		return((long)snd_pcm_hw_params_sizeof.invoke());
	    } catch(Throwable e) {
		throw(new InvocationException(e));
	    }
	}

	public HwParams snd_pcm_hw_params() {
	    return(new HwParams(this));
	}

	private final MethodHandle snd_pcm_sw_params_sizeof = ld.downcallHandle(asound.find("snd_pcm_sw_params_sizeof").get(), FunctionDescriptor.of(SIZE_T));
	long snd_pcm_sw_params_sizeof() {
	    try {
		return((long)snd_pcm_sw_params_sizeof.invoke());
	    } catch(Throwable e) {
		throw(new InvocationException(e));
	    }
	}

	public SwParams snd_pcm_sw_params() {
	    return(new SwParams(this));
	}

	private final MethodHandle snd_pcm_info_sizeof = ld.downcallHandle(asound.find("snd_pcm_info_sizeof").get(), FunctionDescriptor.of(SIZE_T));
	long snd_pcm_info_sizeof() {
	    try {
		return((long)snd_pcm_info_sizeof.invoke());
	    } catch(Throwable e) {
		throw(new InvocationException(e));
	    }
	}

	private final MethodHandle snd_pcm_hw_params_copy = ld.downcallHandle(asound.find("snd_pcm_hw_params_copy").get(), FunctionDescriptor.ofVoid(ADDRESS, ADDRESS));
	public void snd_pcm_hw_params_copy(Alsa.HwParams dst, Alsa.HwParams src) {
	    try {
		snd_pcm_hw_params_copy.invoke(((HwParams)dst).mem(), ((HwParams)src).mem());
	    } catch(Throwable e) {
		throw(new InvocationException(e));
	    }
	}

	private final MethodHandle snd_pcm_hw_params_any = ld.downcallHandle(asound.find("snd_pcm_hw_params_any").get(), FunctionDescriptor.of(C_INT, ADDRESS, ADDRESS));
	public void snd_pcm_hw_params_any(Alsa.Pcm pcm, Alsa.HwParams params) {
	    int rv;
	    try {
		rv = (int)snd_pcm_hw_params_any.invoke(((Pcm)pcm).mem(), ((HwParams)params).mem());
	    } catch(Throwable e) {
		throw(new InvocationException(e));
	    }
	    if(rv < 0)
		throw(new AlsaException(snd_strerror(rv), rv));
	}

	private final MethodHandle snd_pcm_hw_params = ld.downcallHandle(asound.find("snd_pcm_hw_params").get(), FunctionDescriptor.of(C_INT, ADDRESS, ADDRESS));
	public void snd_pcm_hw_params(Alsa.Pcm pcm, Alsa.HwParams params) {
	    int rv;
	    try {
		rv = (int)snd_pcm_hw_params.invoke(((Pcm)pcm).mem(), ((HwParams)params).mem());
	    } catch(Throwable e) {
		throw(new InvocationException(e));
	    }
	    if(rv < 0)
		throw(new AlsaException(snd_strerror(rv), rv));
	}

	private final MethodHandle snd_pcm_hw_params_get_channels = ld.downcallHandle(asound.find("snd_pcm_hw_params_get_channels").get(), FunctionDescriptor.of(C_INT, ADDRESS, ADDRESS));
	public int snd_pcm_hw_params_get_channels(Alsa.HwParams params) {
	    try(Arena st = Arena.ofConfined()) {
		MemorySegment vbuf = st.allocate(C_INT);
		int rv;
		try {
		    rv = (int)snd_pcm_hw_params_get_channels.invoke(((HwParams)params).mem(), vbuf);
		} catch(Throwable e) {
		    throw(new InvocationException(e));
		}
		if(rv < 0)
		    throw(new AlsaException(snd_strerror(rv), rv));
		return((int)getint(vbuf, 0, C_INT, false));
	    }
	}

	private final MethodHandle snd_pcm_hw_params_get_rate = ld.downcallHandle(asound.find("snd_pcm_hw_params_get_rate").get(), FunctionDescriptor.of(C_INT, ADDRESS, ADDRESS, ADDRESS));
	public int snd_pcm_hw_params_get_rate(Alsa.HwParams params) {
	    try(Arena st = Arena.ofConfined()) {
		MemorySegment vbuf = st.allocate(C_INT), dbuf = st.allocate(C_INT);
		int rv;
		try {
		    rv = (int)snd_pcm_hw_params_get_rate.invoke(((HwParams)params).mem(), vbuf, dbuf);
		} catch(Throwable e) {
		    throw(new InvocationException(e));
		}
		if(rv < 0)
		    throw(new AlsaException(snd_strerror(rv), rv));
		return((int)getint(vbuf, 0, C_INT, false));
	    }
	}

	private final MethodHandle snd_pcm_hw_params_get_rate_numden = ld.downcallHandle(asound.find("snd_pcm_hw_params_get_rate_numden").get(), FunctionDescriptor.of(C_INT, ADDRESS, ADDRESS, ADDRESS));
	public Ratio snd_pcm_hw_params_get_rate_numden(Alsa.HwParams params) {
	    try(Arena st = Arena.ofConfined()) {
		MemorySegment nbuf = st.allocate(C_INT), dbuf = st.allocate(C_INT);
		int rv;
		try {
		    rv = (int)snd_pcm_hw_params_get_rate_numden.invoke(((HwParams)params).mem(), nbuf, dbuf);
		} catch(Throwable e) {
		    throw(new InvocationException(e));
		}
		if(rv < 0)
		    throw(new AlsaException(snd_strerror(rv), rv));
		return(Ratio.of(getint(nbuf, 0, C_INT, false), getint(dbuf, 0, C_INT, false)));
	    }
	}

	private final MethodHandle snd_pcm_hw_params_get_period_time = ld.downcallHandle(asound.find("snd_pcm_hw_params_get_period_time").get(), FunctionDescriptor.of(C_INT, ADDRESS, ADDRESS, ADDRESS));
	public int snd_pcm_hw_params_get_period_time(Alsa.HwParams params) {
	    try(Arena st = Arena.ofConfined()) {
		MemorySegment vbuf = st.allocate(C_INT), dbuf = st.allocate(C_INT);
		int rv;
		try {
		    rv = (int)snd_pcm_hw_params_get_period_time.invoke(((HwParams)params).mem(), vbuf, dbuf);
		} catch(Throwable e) {
		    throw(new InvocationException(e));
		}
		if(rv < 0)
		    throw(new AlsaException(snd_strerror(rv), rv));
		return((int)getint(vbuf, 0, C_INT, false));
	    }
	}

	private final MethodHandle snd_pcm_hw_params_get_period_time_min = ld.downcallHandle(asound.find("snd_pcm_hw_params_get_period_time_min").get(), FunctionDescriptor.of(C_INT, ADDRESS, ADDRESS, ADDRESS));
	public int snd_pcm_hw_params_get_period_time_min(Alsa.HwParams params) {
	    try(Arena st = Arena.ofConfined()) {
		MemorySegment vbuf = st.allocate(C_INT), dbuf = st.allocate(C_INT);
		int rv;
		try {
		    rv = (int)snd_pcm_hw_params_get_period_time_min.invoke(((HwParams)params).mem(), vbuf, dbuf);
		} catch(Throwable e) {
		    throw(new InvocationException(e));
		}
		if(rv < 0)
		    throw(new AlsaException(snd_strerror(rv), rv));
		return((int)getint(vbuf, 0, C_INT, false));
	    }
	}

	private final MethodHandle snd_pcm_hw_params_get_period_time_max = ld.downcallHandle(asound.find("snd_pcm_hw_params_get_period_time_max").get(), FunctionDescriptor.of(C_INT, ADDRESS, ADDRESS, ADDRESS));
	public int snd_pcm_hw_params_get_period_time_max(Alsa.HwParams params) {
	    try(Arena st = Arena.ofConfined()) {
		MemorySegment vbuf = st.allocate(C_INT), dbuf = st.allocate(C_INT);
		int rv;
		try {
		    rv = (int)snd_pcm_hw_params_get_period_time_max.invoke(((HwParams)params).mem(), vbuf, dbuf);
		} catch(Throwable e) {
		    throw(new InvocationException(e));
		}
		if(rv < 0)
		    throw(new AlsaException(snd_strerror(rv), rv));
		return((int)getint(vbuf, 0, C_INT, false));
	    }
	}

	private final MethodHandle snd_pcm_hw_params_get_period_size = ld.downcallHandle(asound.find("snd_pcm_hw_params_get_period_size").get(), FunctionDescriptor.of(C_INT, ADDRESS, ADDRESS, ADDRESS));
	public long snd_pcm_hw_params_get_period_size(Alsa.HwParams params) {
	    try(Arena st = Arena.ofConfined()) {
		MemorySegment vbuf = st.allocate(UFRAMES_T), dbuf = st.allocate(C_INT);
		int rv;
		try {
		    rv = (int)snd_pcm_hw_params_get_period_size.invoke(((HwParams)params).mem(), vbuf, dbuf);
		} catch(Throwable e) {
		    throw(new InvocationException(e));
		}
		if(rv < 0)
		    throw(new AlsaException(snd_strerror(rv), rv));
		return(getint(vbuf, 0, UFRAMES_T, false));
	    }
	}

	private final MethodHandle snd_pcm_hw_params_get_period_size_min = ld.downcallHandle(asound.find("snd_pcm_hw_params_get_period_size_min").get(), FunctionDescriptor.of(C_INT, ADDRESS, ADDRESS, ADDRESS));
	public long snd_pcm_hw_params_get_period_size_min(Alsa.HwParams params) {
	    try(Arena st = Arena.ofConfined()) {
		MemorySegment vbuf = st.allocate(UFRAMES_T), dbuf = st.allocate(C_INT);
		int rv;
		try {
		    rv = (int)snd_pcm_hw_params_get_period_size_min.invoke(((HwParams)params).mem(), vbuf, dbuf);
		} catch(Throwable e) {
		    throw(new InvocationException(e));
		}
		if(rv < 0)
		    throw(new AlsaException(snd_strerror(rv), rv));
		return(getint(vbuf, 0, UFRAMES_T, false));
	    }
	}

	private final MethodHandle snd_pcm_hw_params_get_period_size_max = ld.downcallHandle(asound.find("snd_pcm_hw_params_get_period_size_max").get(), FunctionDescriptor.of(C_INT, ADDRESS, ADDRESS, ADDRESS));
	public int snd_pcm_hw_params_get_period_size_max(Alsa.HwParams params) {
	    try(Arena st = Arena.ofConfined()) {
		MemorySegment vbuf = st.allocate(UFRAMES_T), dbuf = st.allocate(C_INT);
		int rv;
		try {
		    rv = (int)snd_pcm_hw_params_get_period_size_max.invoke(((HwParams)params).mem(), vbuf, dbuf);
		} catch(Throwable e) {
		    throw(new InvocationException(e));
		}
		if(rv < 0)
		    throw(new AlsaException(snd_strerror(rv), rv));
		return((int)getint(vbuf, 0, UFRAMES_T, false));
	    }
	}

	private final MethodHandle snd_pcm_hw_params_get_periods = ld.downcallHandle(asound.find("snd_pcm_hw_params_get_periods").get(), FunctionDescriptor.of(C_INT, ADDRESS, ADDRESS, ADDRESS));
	public int snd_pcm_hw_params_get_periods(Alsa.HwParams params) {
	    try(Arena st = Arena.ofConfined()) {
		MemorySegment vbuf = st.allocate(C_INT), dbuf = st.allocate(C_INT);
		int rv;
		try {
		    rv = (int)snd_pcm_hw_params_get_periods.invoke(((HwParams)params).mem(), vbuf, dbuf);
		} catch(Throwable e) {
		    throw(new InvocationException(e));
		}
		if(rv < 0)
		    throw(new AlsaException(snd_strerror(rv), rv));
		return((int)getint(vbuf, 0, C_INT, false));
	    }
	}

	private final MethodHandle snd_pcm_hw_params_get_periods_min = ld.downcallHandle(asound.find("snd_pcm_hw_params_get_periods_min").get(), FunctionDescriptor.of(C_INT, ADDRESS, ADDRESS, ADDRESS));
	public int snd_pcm_hw_params_get_periods_min(Alsa.HwParams params) {
	    try(Arena st = Arena.ofConfined()) {
		MemorySegment vbuf = st.allocate(C_INT), dbuf = st.allocate(C_INT);
		int rv;
		try {
		    rv = (int)snd_pcm_hw_params_get_periods_min.invoke(((HwParams)params).mem(), vbuf, dbuf);
		} catch(Throwable e) {
		    throw(new InvocationException(e));
		}
		if(rv < 0)
		    throw(new AlsaException(snd_strerror(rv), rv));
		return((int)getint(vbuf, 0, C_INT, false));
	    }
	}

	private final MethodHandle snd_pcm_hw_params_get_periods_max = ld.downcallHandle(asound.find("snd_pcm_hw_params_get_periods_max").get(), FunctionDescriptor.of(C_INT, ADDRESS, ADDRESS, ADDRESS));
	public int snd_pcm_hw_params_get_periods_max(Alsa.HwParams params) {
	    try(Arena st = Arena.ofConfined()) {
		MemorySegment vbuf = st.allocate(C_INT), dbuf = st.allocate(C_INT);
		int rv;
		try {
		    rv = (int)snd_pcm_hw_params_get_periods_max.invoke(((HwParams)params).mem(), vbuf, dbuf);
		} catch(Throwable e) {
		    throw(new InvocationException(e));
		}
		if(rv < 0)
		    throw(new AlsaException(snd_strerror(rv), rv));
		return((int)getint(vbuf, 0, C_INT, false));
	    }
	}

	private final MethodHandle snd_pcm_hw_params_get_buffer_size = ld.downcallHandle(asound.find("snd_pcm_hw_params_get_buffer_size").get(), FunctionDescriptor.of(C_INT, ADDRESS, ADDRESS));
	public long snd_pcm_hw_params_get_buffer_size(Alsa.HwParams params) {
	    try(Arena st = Arena.ofConfined()) {
		MemorySegment vbuf = st.allocate(UFRAMES_T);
		int rv;
		try {
		    rv = (int)snd_pcm_hw_params_get_buffer_size.invoke(((HwParams)params).mem(), vbuf);
		} catch(Throwable e) {
		    throw(new InvocationException(e));
		}
		if(rv < 0)
		    throw(new AlsaException(snd_strerror(rv), rv));
		return(getint(vbuf, 0, UFRAMES_T, false));
	    }
	}

	private final MethodHandle snd_pcm_hw_params_set_access = ld.downcallHandle(asound.find("snd_pcm_hw_params_set_access").get(), FunctionDescriptor.of(C_INT, ADDRESS, ADDRESS, C_ENUM));
	public void snd_pcm_hw_params_set_access(Alsa.Pcm pcm, Alsa.HwParams params, int access) {
	    int rv;
	    try {
		rv = (int)snd_pcm_hw_params_set_access.invoke(((Pcm)pcm).mem(), ((HwParams)params).mem(), access);
	    } catch(Throwable e) {
		throw(new InvocationException(e));
	    }
	    if(rv < 0)
		throw(new AlsaException(snd_strerror(rv), rv));
	}

	private final MethodHandle snd_pcm_hw_params_set_format = ld.downcallHandle(asound.find("snd_pcm_hw_params_set_format").get(), FunctionDescriptor.of(C_INT, ADDRESS, ADDRESS, C_ENUM));
	public void snd_pcm_hw_params_set_format(Alsa.Pcm pcm, Alsa.HwParams params, int format) {
	    int rv;
	    try {
		rv = (int)snd_pcm_hw_params_set_format.invoke(((Pcm)pcm).mem(), ((HwParams)params).mem(), format);
	    } catch(Throwable e) {
		throw(new InvocationException(e));
	    }
	    if(rv < 0)
		throw(new AlsaException(snd_strerror(rv), rv));
	}

	private final MethodHandle snd_pcm_hw_params_set_channels = ld.downcallHandle(asound.find("snd_pcm_hw_params_set_channels").get(), FunctionDescriptor.of(C_INT, ADDRESS, ADDRESS, C_INT));
	public void snd_pcm_hw_params_set_channels(Alsa.Pcm pcm, Alsa.HwParams params, int channels) {
	    int rv;
	    try {
		rv = (int)snd_pcm_hw_params_set_channels.invoke(((Pcm)pcm).mem(), ((HwParams)params).mem(), channels);
	    } catch(Throwable e) {
		throw(new InvocationException(e));
	    }
	    if(rv < 0)
		throw(new AlsaException(snd_strerror(rv), rv));
	}

	private final MethodHandle snd_pcm_hw_params_set_rate = ld.downcallHandle(asound.find("snd_pcm_hw_params_set_rate").get(), FunctionDescriptor.of(C_INT, ADDRESS, ADDRESS, C_INT, C_INT));
	public void snd_pcm_hw_params_set_rate(Alsa.Pcm pcm, Alsa.HwParams params, int rate, int dir) {
	    int rv;
	    try {
		rv = (int)snd_pcm_hw_params_set_rate.invoke(((Pcm)pcm).mem(), ((HwParams)params).mem(), rate, dir);
	    } catch(Throwable e) {
		throw(new InvocationException(e));
	    }
	    if(rv < 0)
		throw(new AlsaException(snd_strerror(rv), rv));
	}

	private final MethodHandle snd_pcm_hw_params_set_rate_near = ld.downcallHandle(asound.find("snd_pcm_hw_params_set_rate_near").get(), FunctionDescriptor.of(C_INT, ADDRESS, ADDRESS, ADDRESS, ADDRESS));
	public int snd_pcm_hw_params_set_rate_near(Alsa.Pcm pcm, Alsa.HwParams params, int rate, int dir) {
	    try(Arena st = Arena.ofConfined()) {
		MemorySegment vbuf = setint(st.allocate(C_INT), 0, C_INT, rate);
		MemorySegment dbuf = setint(st.allocate(C_INT), 0, C_INT, dir);
		int rv;
		try {
		    rv = (int)snd_pcm_hw_params_set_rate_near.invoke(((Pcm)pcm).mem(), ((HwParams)params).mem(), vbuf, dbuf);
		} catch(Throwable e) {
		    throw(new InvocationException(e));
		}
		if(rv < 0)
		    throw(new AlsaException(snd_strerror(rv), rv));
		return((int)getint(vbuf, 0, C_INT, false));
	    }
	}

	private final MethodHandle snd_pcm_hw_params_set_buffer_size = ld.downcallHandle(asound.find("snd_pcm_hw_params_set_buffer_size").get(), FunctionDescriptor.of(C_INT, ADDRESS, ADDRESS, UFRAMES_T));
	public void snd_pcm_hw_params_set_buffer_size(Alsa.Pcm pcm, Alsa.HwParams params, int size) {
	    int rv;
	    try {
		rv = (int)snd_pcm_hw_params_set_buffer_size.invoke(((Pcm)pcm).mem(), ((HwParams)params).mem(), size);
	    } catch(Throwable e) {
		throw(new InvocationException(e));
	    }
	    if(rv < 0)
		throw(new AlsaException(snd_strerror(rv), rv));
	}

	private final MethodHandle snd_pcm_hw_params_set_buffer_size_min = ld.downcallHandle(asound.find("snd_pcm_hw_params_set_buffer_size_min").get(), FunctionDescriptor.of(C_INT, ADDRESS, ADDRESS, ADDRESS));
	public int snd_pcm_hw_params_set_buffer_size_min(Alsa.Pcm pcm, Alsa.HwParams params, int size) {
	    try(Arena st = Arena.ofConfined()) {
		MemorySegment vbuf = setint(st.allocate(UFRAMES_T), 0, UFRAMES_T, size);
		int rv;
		try {
		    rv = (int)snd_pcm_hw_params_set_buffer_size_min.invoke(((Pcm)pcm).mem(), ((HwParams)params).mem(), vbuf);
		} catch(Throwable e) {
		    throw(new InvocationException(e));
		}
		if(rv < 0)
		    throw(new AlsaException(snd_strerror(rv), rv));
		return((int)getint(vbuf, 0, UFRAMES_T, false));
	    }
	}

	private final MethodHandle snd_pcm_hw_params_set_buffer_size_near = ld.downcallHandle(asound.find("snd_pcm_hw_params_set_buffer_size_near").get(), FunctionDescriptor.of(C_INT, ADDRESS, ADDRESS, ADDRESS));
	public int snd_pcm_hw_params_set_buffer_size_near(Alsa.Pcm pcm, Alsa.HwParams params, int size) {
	    try(Arena st = Arena.ofConfined()) {
		MemorySegment vbuf = setint(st.allocate(UFRAMES_T), 0, UFRAMES_T, size);
		int rv;
		try {
		    rv = (int)snd_pcm_hw_params_set_buffer_size_near.invoke(((Pcm)pcm).mem(), ((HwParams)params).mem(), vbuf);
		} catch(Throwable e) {
		    throw(new InvocationException(e));
		}
		if(rv < 0)
		    throw(new AlsaException(snd_strerror(rv), rv));
		return((int)getint(vbuf, 0, UFRAMES_T, false));
	    }
	}

	private final MethodHandle snd_pcm_sw_params_copy = ld.downcallHandle(asound.find("snd_pcm_sw_params_copy").get(), FunctionDescriptor.ofVoid(ADDRESS, ADDRESS));
	public void snd_pcm_sw_params_copy(Alsa.SwParams dst, Alsa.SwParams src) {
	    try {
		snd_pcm_sw_params_copy.invoke(((SwParams)dst).mem(), ((SwParams)src).mem());
	    } catch(Throwable e) {
		throw(new InvocationException(e));
	    }
	}

	private final MethodHandle snd_pcm_sw_params_current = ld.downcallHandle(asound.find("snd_pcm_sw_params_current").get(), FunctionDescriptor.of(C_INT, ADDRESS, ADDRESS));
	public void snd_pcm_sw_params_current(Alsa.Pcm pcm, Alsa.SwParams params) {
	    int rv;
	    try {
		rv = (int)snd_pcm_sw_params_current.invoke(((Pcm)pcm).mem(), ((SwParams)params).mem());
	    } catch(Throwable e) {
		throw(new InvocationException(e));
	    }
	    if(rv < 0)
		throw(new AlsaException(snd_strerror(rv), rv));
	}

	private final MethodHandle snd_pcm_sw_params = ld.downcallHandle(asound.find("snd_pcm_sw_params").get(), FunctionDescriptor.of(C_INT, ADDRESS, ADDRESS));
	public void snd_pcm_sw_params(Alsa.Pcm pcm, Alsa.SwParams params) {
	    int rv;
	    try {
		rv = (int)snd_pcm_sw_params.invoke(((Pcm)pcm).mem(), ((SwParams)params).mem());
	    } catch(Throwable e) {
		throw(new InvocationException(e));
	    }
	    if(rv < 0)
		throw(new AlsaException(snd_strerror(rv), rv));
	}

	private final MethodHandle snd_pcm_sw_params_get_avail_min = ld.downcallHandle(asound.find("snd_pcm_sw_params_get_avail_min").get(), FunctionDescriptor.of(C_INT, ADDRESS, ADDRESS));
	public long snd_pcm_sw_params_get_avail_min(Alsa.SwParams params) {
	    try(Arena st = Arena.ofConfined()) {
		MemorySegment vbuf = st.allocate(UFRAMES_T);
		int rv;
		try {
		    rv = (int)snd_pcm_sw_params_get_avail_min.invoke(((SwParams)params).mem(), vbuf);
		} catch(Throwable e) {
		    throw(new InvocationException(e));
		}
		if(rv < 0)
		    throw(new AlsaException(snd_strerror(rv), rv));
		return(getint(vbuf, 0, UFRAMES_T, false));
	    }
	}

	private final MethodHandle snd_pcm_sw_params_set_avail_min = ld.downcallHandle(asound.find("snd_pcm_sw_params_set_avail_min").get(), FunctionDescriptor.of(C_INT, ADDRESS, ADDRESS, UFRAMES_T));
	public void snd_pcm_sw_params_set_avail_min(Alsa.Pcm pcm, Alsa.SwParams params, int size) {
	    int rv;
	    try {
		rv = (int)snd_pcm_sw_params_set_avail_min.invoke(((Pcm)pcm).mem(), ((SwParams)params).mem(), size);
	    } catch(Throwable e) {
		throw(new InvocationException(e));
	    }
	    if(rv < 0)
		throw(new AlsaException(snd_strerror(rv), rv));
	}

	private final MethodHandle snd_pcm_sw_params_set_start_threshold = ld.downcallHandle(asound.find("snd_pcm_sw_params_set_start_threshold").get(), FunctionDescriptor.of(C_INT, ADDRESS, ADDRESS, UFRAMES_T));
	public void snd_pcm_sw_params_set_start_threshold(Alsa.Pcm pcm, Alsa.SwParams params, int size) {
	    int rv;
	    try {
		rv = (int)snd_pcm_sw_params_set_start_threshold.invoke(((Pcm)pcm).mem(), ((SwParams)params).mem(), size);
	    } catch(Throwable e) {
		throw(new InvocationException(e));
	    }
	    if(rv < 0)
		throw(new AlsaException(snd_strerror(rv), rv));
	}

	private final MethodHandle snd_pcm_info = ld.downcallHandle(asound.find("snd_pcm_info").get(), FunctionDescriptor.of(C_INT, ADDRESS, ADDRESS));
	public PcmInfo snd_pcm_info(Alsa.Pcm pcm) {
	    PcmInfo ret = new PcmInfo(this);
	    int rv;
	    try {
		rv = (int)snd_pcm_info.invoke(((Pcm)pcm).mem(), ret.mem());
	    } catch(Throwable e) {
		throw(new InvocationException(e));
	    }
	    if(rv < 0)
		throw(new AlsaException(snd_strerror(rv), rv));
	    return(ret);
	}

	private final MethodHandle snd_pcm_info_get_device = ld.downcallHandle(asound.find("snd_pcm_info_get_device").get(), FunctionDescriptor.of(C_INT, ADDRESS));
	public int snd_pcm_info_get_device(Alsa.PcmInfo info) {
	    try {
		return((int)snd_pcm_info_get_device.invoke(((PcmInfo)info).mem()));
	    } catch(Throwable e) {
		throw(new InvocationException(e));
	    }
	}

	private final MethodHandle snd_pcm_info_get_subdevice = ld.downcallHandle(asound.find("snd_pcm_info_get_subdevice").get(), FunctionDescriptor.of(C_INT, ADDRESS));
	public int snd_pcm_info_get_subdevice(Alsa.PcmInfo info) {
	    try {
		return((int)snd_pcm_info_get_subdevice.invoke(((PcmInfo)info).mem()));
	    } catch(Throwable e) {
		throw(new InvocationException(e));
	    }
	}

	private final MethodHandle snd_pcm_info_get_card = ld.downcallHandle(asound.find("snd_pcm_info_get_card").get(), FunctionDescriptor.of(C_INT, ADDRESS));
	public int snd_pcm_info_get_card(Alsa.PcmInfo info) {
	    try {
		return((int)snd_pcm_info_get_card.invoke(((PcmInfo)info).mem()));
	    } catch(Throwable e) {
		throw(new InvocationException(e));
	    }
	}

	private final MethodHandle snd_pcm_info_get_id = ld.downcallHandle(asound.find("snd_pcm_info_get_id").get(), FunctionDescriptor.of(ADDRESS, ADDRESS));
	public String snd_pcm_info_get_id(Alsa.PcmInfo info) {
	    MemorySegment rv;
	    try {
		rv = (MemorySegment)snd_pcm_info_get_id.invoke(((PcmInfo)info).mem());
	    } catch(Throwable e) {
		throw(new InvocationException(e));
	    }
	    return(rv.reinterpret(Long.MAX_VALUE).getString(0, C_CHARSET));
	}

	private final MethodHandle snd_pcm_info_get_name = ld.downcallHandle(asound.find("snd_pcm_info_get_name").get(), FunctionDescriptor.of(ADDRESS, ADDRESS));
	public String snd_pcm_info_get_name(Alsa.PcmInfo info) {
	    MemorySegment rv;
	    try {
		rv = (MemorySegment)snd_pcm_info_get_name.invoke(((PcmInfo)info).mem());
	    } catch(Throwable e) {
		throw(new InvocationException(e));
	    }
	    return(rv.reinterpret(Long.MAX_VALUE).getString(0, C_CHARSET));
	}

	private final MethodHandle snd_pcm_info_get_subdevice_name = ld.downcallHandle(asound.find("snd_pcm_info_get_subdevice_name").get(), FunctionDescriptor.of(ADDRESS, ADDRESS));
	public String snd_pcm_info_get_subdevice_name(Alsa.PcmInfo info) {
	    MemorySegment rv;
	    try {
		rv = (MemorySegment)snd_pcm_info_get_subdevice_name.invoke(((PcmInfo)info).mem());
	    } catch(Throwable e) {
		throw(new InvocationException(e));
	    }
	    return(rv.reinterpret(Long.MAX_VALUE).getString(0, C_CHARSET));
	}

	private final MethodHandle snd_card_next = ld.downcallHandle(asound.find("snd_card_next").get(), FunctionDescriptor.of(C_INT, ADDRESS));
	public int snd_card_next(int card) {
	    try(Arena st = Arena.ofConfined()) {
		MemorySegment buf = setint(st.allocate(C_INT), 0, C_INT, card);
		int rv;
		try {
		    rv = (int)snd_card_next.invoke(buf);
		} catch(Throwable e) {
		    throw(new InvocationException(e));
		}
		if(rv < 0)
		    throw(new AlsaException(snd_strerror(rv), rv));
		return((int)getint(buf, 0, C_INT, true));
	    }
	}

	private final MethodHandle snd_device_name_free_hint = ld.downcallHandle(asound.find("snd_device_name_free_hint").get(), FunctionDescriptor.of(C_INT, ADDRESS));
	void snd_device_name_free_hint(MemorySegment mem) {
	    int rv;
	    try {
		rv = (int)snd_device_name_free_hint.invoke(mem);
	    } catch(Throwable e) {
		throw(new InvocationException(e));
	    }
	    if(rv < 0)
		throw(new AlsaException(snd_strerror(rv), rv));
	}

	private final MethodHandle snd_device_name_hint = ld.downcallHandle(asound.find("snd_device_name_hint").get(), FunctionDescriptor.of(C_INT, C_INT, ADDRESS, ADDRESS));
	public DeviceHints snd_device_name_hint(int card, String iface) {
	    try(Arena st = Arena.ofConfined()) {
		MemorySegment buf = st.allocate(ADDRESS);
		int rv;
		try {
		    rv = (int)snd_device_name_hint.invoke(card, st.allocateFrom(iface, C_CHARSET), buf);
		} catch(Throwable e) {
		    throw(new InvocationException(e));
		}
		if(rv < 0)
		    throw(new AlsaException(snd_strerror(rv), rv));
		return(new DeviceHints(this, buf.get(ADDRESS, 0)));
	    }
	}

	private final MethodHandle snd_device_name_get_hint = ld.downcallHandle(asound.find("snd_device_name_get_hint").get(), FunctionDescriptor.of(ADDRESS, ADDRESS, ADDRESS));
	String snd_device_name_get_hint(MemorySegment hint, String id) {
	    try(Arena st = Arena.ofConfined()) {
		MemorySegment rv;
		try {
		    rv = (MemorySegment)snd_device_name_get_hint.invoke(hint, st.allocateFrom(id, C_CHARSET));
		} catch(Throwable e) {
		    throw(new InvocationException(e));
		}
		if(nullp(rv))
		    return(null);
		String ret = rv.reinterpret(Long.MAX_VALUE).getString(0, C_CHARSET);
		LibC.get().free(rv);
		return(ret);
	    }
	}
    }

    private static Alsa instance = null;
    public static Alsa get() {
	if(instance == null) {
	    synchronized(Alsa.class) {
		if(instance == null)
		    instance = tryload("libasound", libasound_so_2::new);
	    }
	}
	return(instance);
    }
}
