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

public abstract class LibPulse {
    public static final int PA_CONTEXT_UNCONNECTED  = 0;
    public static final int PA_CONTEXT_CONNECTING   = 1;
    public static final int PA_CONTEXT_AUTHORIZING  = 2;
    public static final int PA_CONTEXT_SETTING_NAME = 3;
    public static final int PA_CONTEXT_READY        = 4;
    public static final int PA_CONTEXT_FAILED       = 5;
    public static final int PA_CONTEXT_TERMINATED   = 6;

    public static final int PA_STREAM_UNCONNECTED = 0;
    public static final int PA_STREAM_CREATING = 1;
    public static final int PA_STREAM_READY = 2;
    public static final int PA_STREAM_FAILED = 3;
    public static final int PA_STREAM_TERMINATED = 4;

    public static final int PA_CONTEXT_NOFLAGS     = 0x0000;
    public static final int PA_CONTEXT_NOAUTOSPAWN = 0x0001;
    public static final int PA_CONTEXT_NOFAIL      = 0x0002;

    public static final int PA_SEEK_RELATIVE         = 0;
    public static final int PA_SEEK_ABSOLUTE         = 1;
    public static final int PA_SEEK_RELATIVE_ON_READ = 2;
    public static final int PA_SEEK_RELATIVE_END     = 3;

    public static final int PA_SAMPLE_U8        =  0;
    public static final int PA_SAMPLE_ALAW      =  1;
    public static final int PA_SAMPLE_ULAW      =  2;
    public static final int PA_SAMPLE_S16LE     =  3;
    public static final int PA_SAMPLE_S16BE     =  4;
    public static final int PA_SAMPLE_FLOAT32LE =  5;
    public static final int PA_SAMPLE_FLOAT32BE =  6;
    public static final int PA_SAMPLE_S32LE     =  7;
    public static final int PA_SAMPLE_S32BE     =  8;
    public static final int PA_SAMPLE_S24LE     =  9;
    public static final int PA_SAMPLE_S24BE     = 10;
    public static final int PA_SAMPLE_S24_32LE  = 11;
    public static final int PA_SAMPLE_S24_32BE  = 12;
    public static final int PA_SAMPLE_INVALID   = -1;

    public static final int PA_STREAM_NOFLAGS                   = 0x00000;
    public static final int PA_STREAM_START_CORKED              = 0x00001;
    public static final int PA_STREAM_INTERPOLATE_TIMING        = 0x00002;
    public static final int PA_STREAM_NOT_MONOTONIC             = 0x00004;
    public static final int PA_STREAM_AUTO_TIMING_UPDATE        = 0x00008;
    public static final int PA_STREAM_NO_REMAP_CHANNELS         = 0x00010;
    public static final int PA_STREAM_NO_REMIX_CHANNELS         = 0x00020;
    public static final int PA_STREAM_FIX_FORMAT                = 0x00040;
    public static final int PA_STREAM_FIX_RATE                  = 0x00080;
    public static final int PA_STREAM_FIX_CHANNELS              = 0x00010;
    public static final int PA_STREAM_DONT_MOVE                 = 0x00200;
    public static final int PA_STREAM_VARIABLE_RATE             = 0x00400;
    public static final int PA_STREAM_PEAK_DETECT               = 0x00800;
    public static final int PA_STREAM_START_MUTED               = 0x01000;
    public static final int PA_STREAM_ADJUST_LATENCY            = 0x02000;
    public static final int PA_STREAM_EARLY_REQUESTS            = 0x04000;
    public static final int PA_STREAM_DONT_INHIBIT_AUTO_SUSPEND = 0x08000;
    public static final int PA_STREAM_START_UNMUTED             = 0x10000;
    public static final int PA_STREAM_FAIL_ON_SUSPEND           = 0x20000;
    public static final int PA_STREAM_RELATIVE_VOLUME           = 0x40000;
    public static final int PA_STREAM_PASSTHROUGH               = 0x80000;

    public static final String PA_PROP_MEDIA_NAME = "media.name";
    public static final String PA_PROP_MEDIA_TITLE = "media.title";
    public static final String PA_PROP_MEDIA_ARTIST = "media.artist";
    public static final String PA_PROP_MEDIA_COPYRIGHT = "media.copyright";
    public static final String PA_PROP_MEDIA_SOFTWARE = "media.software";
    public static final String PA_PROP_MEDIA_LANGUAGE = "media.language";
    public static final String PA_PROP_MEDIA_FILENAME = "media.filename";
    public static final String PA_PROP_MEDIA_ICON = "media.icon";
    public static final String PA_PROP_MEDIA_ICON_NAME = "media.icon_name";
    public static final String PA_PROP_MEDIA_ROLE = "media.role";
    public static final String PA_PROP_FILTER_WANT = "filter.want";
    public static final String PA_PROP_FILTER_APPLY = "filter.apply";
    public static final String PA_PROP_FILTER_SUPPRESS = "filter.suppress";
    public static final String PA_PROP_EVENT_ID = "event.id";
    public static final String PA_PROP_EVENT_DESCRIPTION = "event.description";
    public static final String PA_PROP_EVENT_MOUSE_X = "event.mouse.x";
    public static final String PA_PROP_EVENT_MOUSE_Y = "event.mouse.y";
    public static final String PA_PROP_EVENT_MOUSE_HPOS = "event.mouse.hpos";
    public static final String PA_PROP_EVENT_MOUSE_VPOS = "event.mouse.vpos";
    public static final String PA_PROP_EVENT_MOUSE_BUTTON = "event.mouse.button";
    public static final String PA_PROP_WINDOW_NAME = "window.name";
    public static final String PA_PROP_WINDOW_ID = "window.id";
    public static final String PA_PROP_WINDOW_ICON = "window.icon";
    public static final String PA_PROP_WINDOW_ICON_NAME = "window.icon_name";
    public static final String PA_PROP_WINDOW_X = "window.x";
    public static final String PA_PROP_WINDOW_Y = "window.y";
    public static final String PA_PROP_WINDOW_WIDTH = "window.width";
    public static final String PA_PROP_WINDOW_HEIGHT = "window.height";
    public static final String PA_PROP_WINDOW_HPOS = "window.hpos";
    public static final String PA_PROP_WINDOW_VPOS = "window.vpos";
    public static final String PA_PROP_WINDOW_DESKTOP = "window.desktop";
    public static final String PA_PROP_WINDOW_X11_DISPLAY = "window.x11.display";
    public static final String PA_PROP_WINDOW_X11_SCREEN = "window.x11.screen";
    public static final String PA_PROP_WINDOW_X11_MONITOR = "window.x11.monitor";
    public static final String PA_PROP_WINDOW_X11_XID = "window.x11.xid";
    public static final String PA_PROP_APPLICATION_NAME = "application.name";
    public static final String PA_PROP_APPLICATION_ID = "application.id";
    public static final String PA_PROP_APPLICATION_VERSION = "application.version";
    public static final String PA_PROP_APPLICATION_ICON = "application.icon";
    public static final String PA_PROP_APPLICATION_ICON_NAME = "application.icon_name";
    public static final String PA_PROP_APPLICATION_LANGUAGE = "application.language";
    public static final String PA_PROP_APPLICATION_PROCESS_ID = "application.process.id";
    public static final String PA_PROP_APPLICATION_PROCESS_BINARY = "application.process.binary";
    public static final String PA_PROP_APPLICATION_PROCESS_USER = "application.process.user";
    public static final String PA_PROP_APPLICATION_PROCESS_HOST = "application.process.host";
    public static final String PA_PROP_APPLICATION_PROCESS_MACHINE_ID = "application.process.machine_id";
    public static final String PA_PROP_APPLICATION_PROCESS_SESSION_ID = "application.process.session_id";
    public static final String PA_PROP_DEVICE_STRING = "device.string";
    public static final String PA_PROP_DEVICE_API = "device.api";
    public static final String PA_PROP_DEVICE_DESCRIPTION = "device.description";
    public static final String PA_PROP_DEVICE_BUS_PATH = "device.bus_path";
    public static final String PA_PROP_DEVICE_SERIAL = "device.serial";
    public static final String PA_PROP_DEVICE_VENDOR_ID = "device.vendor.id";
    public static final String PA_PROP_DEVICE_VENDOR_NAME = "device.vendor.name";
    public static final String PA_PROP_DEVICE_PRODUCT_ID = "device.product.id";
    public static final String PA_PROP_DEVICE_PRODUCT_NAME = "device.product.name";
    public static final String PA_PROP_DEVICE_CLASS = "device.class";
    public static final String PA_PROP_DEVICE_FORM_FACTOR = "device.form_factor";
    public static final String PA_PROP_DEVICE_BUS = "device.bus";
    public static final String PA_PROP_DEVICE_ICON = "device.icon";
    public static final String PA_PROP_DEVICE_ICON_NAME = "device.icon_name";
    public static final String PA_PROP_DEVICE_ACCESS_MODE = "device.access_mode";
    public static final String PA_PROP_DEVICE_MASTER_DEVICE = "device.master_device";
    public static final String PA_PROP_DEVICE_BUFFERING_BUFFER_SIZE = "device.buffering.buffer_size";
    public static final String PA_PROP_DEVICE_BUFFERING_FRAGMENT_SIZE = "device.buffering.fragment_size";
    public static final String PA_PROP_DEVICE_PROFILE_NAME = "device.profile.name";
    public static final String PA_PROP_DEVICE_INTENDED_ROLES = "device.intended_roles";
    public static final String PA_PROP_DEVICE_PROFILE_DESCRIPTION = "device.profile.description";
    public static final String PA_PROP_MODULE_AUTHOR = "module.author";
    public static final String PA_PROP_MODULE_DESCRIPTION = "module.description";
    public static final String PA_PROP_MODULE_USAGE = "module.usage";
    public static final String PA_PROP_MODULE_VERSION = "module.version";
    public static final String PA_PROP_FORMAT_SAMPLE_FORMAT = "format.sample_format";
    public static final String PA_PROP_FORMAT_RATE = "format.rate";
    public static final String PA_PROP_FORMAT_CHANNELS = "format.channels";
    public static final String PA_PROP_FORMAT_CHANNEL_MAP = "format.channel_map";
    public static final String PA_PROP_CONTEXT_FORCE_DISABLE_SHM = "context.force.disable.shm";
    public static final String PA_PROP_BLUETOOTH_CODEC = "bluetooth.codec";

    public static interface pa_threaded_mainloop {
	public LibPulse lib();

	public default pa_mainloop_api get_api() {return(lib().pa_threaded_mainloop_get_api(this));}
	public default void start() {lib().pa_threaded_mainloop_start(this);}
	public default void stop() {lib().pa_threaded_mainloop_stop(this);}

	public class Lock implements AutoCloseable {
	    private final pa_threaded_mainloop m;

	    public Lock(pa_threaded_mainloop m) {
		this.m = m;
		m.lib().pa_threaded_mainloop_lock(m);
	    }

	    public void close() {
		m.lib().pa_threaded_mainloop_unlock(m);
	    }
	}
	public default Lock lock() {return(new Lock(this));}
    }

    public static interface pa_mainloop_api {
	public LibPulse lib();
    }

    public static interface pa_context {
	public LibPulse lib();

	public default int errno() {return(lib().pa_context_errno(this));}
	public default int get_state() {return(lib().pa_context_get_state(this));}
	public default void connect(String server, int flags) {lib().pa_context_connect(this, server, flags);}
	public default void disconnect() {lib().pa_context_disconnect(this);}
	public default void set_state_callback(Runnable cb) {lib().pa_context_set_state_callback(this, cb);}
	public default void set_event_callback(BiConsumer<? super String, ? super pa_proplist> cb) {lib().pa_context_set_event_callback(this, cb);}
    }

    public static interface pa_proplist {
	public LibPulse lib();
	public default void set(String key, byte[] data) {lib().pa_proplist_set(this, key, data);}
	public default void sets(String key, String val) {lib().pa_proplist_sets(this, key, val);}
    }

    public static interface pa_stream {
	public LibPulse lib();

	public default int get_state() {return(lib().pa_stream_get_state(this));}
	public default void set_state_callback(Runnable cb) {lib().pa_stream_set_state_callback(this, cb);}
	public default void set_write_callback(LongConsumer cb) {lib().pa_stream_set_write_callback(this, cb);}
	public default void connect_playback(String dev, LibPulse.pa_buffer_attr attr, int flags) {lib().pa_stream_connect_playback(this, dev, attr, flags);}
	public default ByteBuffer begin_write(long wanted) {return(lib().pa_stream_begin_write(this, wanted));}
	public default void write(ByteBuffer data, long offset, int seek) {lib().pa_stream_write(this, data, offset, seek);}
	public default void cancel_write() {lib().pa_stream_cancel_write(this);}
	public default void disconnect() {lib().pa_stream_disconnect(this);}
    }

    public static interface pa_sample_spec {
	public pa_sample_spec format(int value);
	public pa_sample_spec rate(int value);
	public pa_sample_spec channels(int value);
	public int channels();
    }

    public static interface pa_buffer_attr {
	public pa_buffer_attr maxlength(int value);
	public pa_buffer_attr tlength(int value);
	public pa_buffer_attr prebuf(int value);
	public pa_buffer_attr minreq(int value);
	public pa_buffer_attr fragsize(int value);
    }

    public static class PulseException extends RuntimeException {
	public PulseException(String msg) {
	    super(msg);
	}
    }

    public static class ContextException extends PulseException {
	public final int errno;

	public ContextException(String msg, int errno) {
	    super(msg);
	    this.errno = errno;
	}
    }

    public abstract pa_threaded_mainloop pa_threaded_mainloop_new();
    public abstract pa_mainloop_api pa_threaded_mainloop_get_api(LibPulse.pa_threaded_mainloop m);
    public abstract void pa_threaded_mainloop_lock(LibPulse.pa_threaded_mainloop m);
    public abstract void pa_threaded_mainloop_unlock(LibPulse.pa_threaded_mainloop m);
    public abstract void pa_threaded_mainloop_start(LibPulse.pa_threaded_mainloop m);
    public abstract void pa_threaded_mainloop_stop(LibPulse.pa_threaded_mainloop m);
    public abstract pa_context pa_context_new(LibPulse.pa_mainloop_api mainloop, String name);
    public abstract pa_context pa_context_new_with_proplist(LibPulse.pa_mainloop_api mainloop, String name, LibPulse.pa_proplist props);
    public abstract int pa_context_errno(LibPulse.pa_context c);
    public abstract int pa_context_get_state(LibPulse.pa_context c);
    public abstract void pa_context_connect(LibPulse.pa_context c, String server, int flags);
    public abstract void pa_context_disconnect(LibPulse.pa_context c);
    public abstract void pa_context_set_state_callback(LibPulse.pa_context gc, Runnable cb);
    public abstract void pa_context_set_event_callback(LibPulse.pa_context gc, BiConsumer<? super String, ? super LibPulse.pa_proplist> cb);
    public abstract pa_proplist pa_proplist_new();
    public abstract void pa_proplist_set(LibPulse.pa_proplist p, String key, byte[] data);
    public abstract void pa_proplist_sets(LibPulse.pa_proplist p, String key, String val);
    public abstract pa_sample_spec pa_sample_spec();
    public abstract long pa_usec_to_bytes(long t, LibPulse.pa_sample_spec spec);
    public abstract pa_stream pa_stream_new(LibPulse.pa_context c, String name, LibPulse.pa_sample_spec ss);
    public abstract pa_stream pa_stream_new_with_proplist(LibPulse.pa_context c, String name, LibPulse.pa_sample_spec ss, LibPulse.pa_proplist props);
    public abstract void pa_stream_set_state_callback(LibPulse.pa_stream gs, Runnable cb);
    public abstract void pa_stream_set_write_callback(LibPulse.pa_stream gs, LongConsumer cb);
    public abstract pa_buffer_attr pa_buffer_attr();
    public abstract void pa_stream_connect_playback(LibPulse.pa_stream s, String dev, LibPulse.pa_buffer_attr attr, int flags);
    public abstract int pa_stream_get_state(LibPulse.pa_stream c);
    public abstract ByteBuffer pa_stream_begin_write(LibPulse.pa_stream s, long wanted);
    public abstract void pa_stream_write(LibPulse.pa_stream s, ByteBuffer data, long offset, int seek);
    public abstract void pa_stream_cancel_write(LibPulse.pa_stream s);
    public abstract void pa_stream_disconnect(LibPulse.pa_stream c);

    public boolean PA_CONTEXT_IS_GOOD(int x) {
	return((x == PA_CONTEXT_CONNECTING) ||
	       (x == PA_CONTEXT_AUTHORIZING) ||
	       (x == PA_CONTEXT_SETTING_NAME) ||
	       (x == PA_CONTEXT_READY));
    }

    public boolean PA_STREAM_IS_GOOD(int x) {
	return((x == PA_STREAM_CREATING) ||
	       (x == PA_STREAM_READY));
    }

    public static class libpulse_so_0 extends LibPulse {
	private static final MemoryLayout PA_USEC_T = ValueLayout.JAVA_LONG;
	private final SymbolLookup libpulse = loadlib("libpulse.so.0", Arena.global());

	class pa_threaded_mainloop implements LibPulse.pa_threaded_mainloop {
	    final MemorySegment mem;

	    pa_threaded_mainloop(MemorySegment mem) {
		this.mem = mem;
		libpulse_so_0 lib = libpulse_so_0.this;
		Finalizer.finalize(this, () -> lib.pa_threaded_mainloop_free(mem));
	    }

	    public libpulse_so_0 lib() {return(libpulse_so_0.this);}
	}

	private final MethodHandle pa_threaded_mainloop_free = ld.downcallHandle(libpulse.find("pa_threaded_mainloop_free").get(), FunctionDescriptor.ofVoid(ADDRESS));
	void pa_threaded_mainloop_free(MemorySegment m) {
	    try {
		pa_threaded_mainloop_free.invoke(m);
	    } catch(Throwable e) {
		throw(new InvocationException(e));
	    }
	}

	private final MethodHandle pa_threaded_mainloop_new = ld.downcallHandle(libpulse.find("pa_threaded_mainloop_new").get(), FunctionDescriptor.of(ADDRESS));
	public pa_threaded_mainloop pa_threaded_mainloop_new() {
	    MemorySegment rv;
	    try {
		rv = (MemorySegment)pa_threaded_mainloop_new.invoke();
	    } catch(Throwable e) {
		throw(new InvocationException(e));
	    }
	    return(nullp(rv) ? null : new pa_threaded_mainloop(rv));
	}

	private final MethodHandle pa_threaded_mainloop_lock = ld.downcallHandle(libpulse.find("pa_threaded_mainloop_lock").get(), FunctionDescriptor.ofVoid(ADDRESS));
	public void pa_threaded_mainloop_lock(LibPulse.pa_threaded_mainloop m) {
	    try {
		pa_threaded_mainloop_lock.invoke(((pa_threaded_mainloop)m).mem);
	    } catch(Throwable e) {
		throw(new InvocationException(e));
	    }
	}

	private final MethodHandle pa_threaded_mainloop_unlock = ld.downcallHandle(libpulse.find("pa_threaded_mainloop_unlock").get(), FunctionDescriptor.ofVoid(ADDRESS));
	public void pa_threaded_mainloop_unlock(LibPulse.pa_threaded_mainloop m) {
	    try {
		pa_threaded_mainloop_unlock.invoke(((pa_threaded_mainloop)m).mem);
	    } catch(Throwable e) {
		throw(new InvocationException(e));
	    }
	}

	private final MethodHandle pa_threaded_mainloop_start = ld.downcallHandle(libpulse.find("pa_threaded_mainloop_start").get(), FunctionDescriptor.of(C_INT, ADDRESS));
	public void pa_threaded_mainloop_start(LibPulse.pa_threaded_mainloop m) {
	    int rv;
	    try {
		rv = (int)pa_threaded_mainloop_start.invoke(((pa_threaded_mainloop)m).mem);
	    } catch(Throwable e) {
		throw(new InvocationException(e));
	    }
	    if(rv < 0)
		throw(new PulseException("pa_threaded_mainloop_start: " + rv));
	}

	private final MethodHandle pa_threaded_mainloop_stop = ld.downcallHandle(libpulse.find("pa_threaded_mainloop_stop").get(), FunctionDescriptor.ofVoid(ADDRESS));
	public void pa_threaded_mainloop_stop(LibPulse.pa_threaded_mainloop m) {
	    try {
		pa_threaded_mainloop_start.invoke(((pa_threaded_mainloop)m).mem);
	    } catch(Throwable e) {
		throw(new InvocationException(e));
	    }
	}

	class pa_mainloop_api implements LibPulse.pa_mainloop_api {
	    final MemorySegment mem;
	    private final Object retain;

	    pa_mainloop_api(MemorySegment mem, Object retain) {
		this.mem = mem;
		this.retain = retain;
	    }

	    public libpulse_so_0 lib() {return(libpulse_so_0.this);}
	}

	private final MethodHandle pa_threaded_mainloop_get_api = ld.downcallHandle(libpulse.find("pa_threaded_mainloop_get_api").get(), FunctionDescriptor.of(ADDRESS, ADDRESS));
	public pa_mainloop_api pa_threaded_mainloop_get_api(LibPulse.pa_threaded_mainloop m) {
	    MemorySegment rv;
	    try {
		rv = (MemorySegment)pa_threaded_mainloop_get_api.invoke(((pa_threaded_mainloop)m).mem);
	    } catch(Throwable e) {
		throw(new InvocationException(e));
	    }
	    return(nullp(rv) ? null : new pa_mainloop_api(rv, m));
	}

	class pa_context implements LibPulse.pa_context {
	    final MemorySegment mem;
	    private final Object retain;
	    Object state_cb, event_cb;

	    pa_context(MemorySegment mem, Object retain) {
		this.mem = mem;
		this.retain = retain;
		libpulse_so_0 lib = libpulse_so_0.this;
		Finalizer.finalize(this, () -> lib.pa_context_unref(mem));
	    }

	    public libpulse_so_0 lib() {return(libpulse_so_0.this);}
	}

	private final MethodHandle pa_context_unref = ld.downcallHandle(libpulse.find("pa_context_unref").get(), FunctionDescriptor.ofVoid(ADDRESS));
	void pa_context_unref(MemorySegment m) {
	    try {
		pa_context_unref.invoke(m);
	    } catch(Throwable e) {
		throw(new InvocationException(e));
	    }
	}

	private final MethodHandle pa_context_new = ld.downcallHandle(libpulse.find("pa_context_new").get(), FunctionDescriptor.of(ADDRESS, ADDRESS, ADDRESS));
	public pa_context pa_context_new(LibPulse.pa_mainloop_api mainloop, String name) {
	    MemorySegment rv;
	    try(Arena st = Arena.ofConfined()) {
		rv = (MemorySegment)pa_context_new.invoke(((pa_mainloop_api)mainloop).mem, st.allocateFrom(name, C_CHARSET));
	    } catch(Throwable e) {
		throw(new InvocationException(e));
	    }
	    return(nullp(rv) ? null : new pa_context(rv, mainloop));
	}

	private final MethodHandle pa_context_new_with_proplist = ld.downcallHandle(libpulse.find("pa_context_new_with_proplist").get(), FunctionDescriptor.of(ADDRESS, ADDRESS, ADDRESS, ADDRESS));
	public pa_context pa_context_new_with_proplist(LibPulse.pa_mainloop_api mainloop, String name, LibPulse.pa_proplist props) {
	    MemorySegment rv;
	    try(Arena st = Arena.ofConfined()) {
		rv = (MemorySegment)pa_context_new_with_proplist.invoke(((pa_mainloop_api)mainloop).mem, st.allocateFrom(name, C_CHARSET), ((pa_proplist)props).mem);
	    } catch(Throwable e) {
		throw(new InvocationException(e));
	    }
	    return(nullp(rv) ? null : new pa_context(rv, mainloop));
	}

	private final MethodHandle pa_context_errno = ld.downcallHandle(libpulse.find("pa_context_errno").get(), FunctionDescriptor.of(C_INT, ADDRESS));
	public int pa_context_errno(LibPulse.pa_context c) {
	    try {
		return((int)pa_context_errno.invoke(((pa_context)c).mem));
	    } catch(Throwable e) {
		throw(new InvocationException(e));
	    }
	}

	private final MethodHandle pa_context_get_state = ld.downcallHandle(libpulse.find("pa_context_get_state").get(), FunctionDescriptor.of(C_ENUM, ADDRESS));
	public int pa_context_get_state(LibPulse.pa_context c) {
	    try {
		return((int)pa_context_get_state.invoke(((pa_context)c).mem));
	    } catch(Throwable e) {
		throw(new InvocationException(e));
	    }
	}

	private final MethodHandle pa_context_connect = ld.downcallHandle(libpulse.find("pa_context_connect").get(), FunctionDescriptor.of(C_INT, ADDRESS, ADDRESS, C_ENUM, ADDRESS));
	public void pa_context_connect(LibPulse.pa_context c, String server, int flags) {
	    int rv;
	    try(Arena st = Arena.ofConfined()) {
		rv = (int)pa_context_connect.invoke(((pa_context)c).mem, server == null ? MemorySegment.NULL : st.allocateFrom(server, C_CHARSET), flags, MemorySegment.NULL);
	    } catch(Throwable e) {
		throw(new InvocationException(e));
	    }
	    if(rv < 0)
		throw(new ContextException("pa_context_connect", pa_context_errno(c)));
	}

	private final MethodHandle pa_context_disconnect = ld.downcallHandle(libpulse.find("pa_context_disconnect").get(), FunctionDescriptor.ofVoid(ADDRESS));
	public void pa_context_disconnect(LibPulse.pa_context c) {
	    try {
		pa_context_disconnect.invoke(((pa_context)c).mem);
	    } catch(Throwable e) {
		throw(new InvocationException(e));
	    }
	}

	private static void context_notify_cb(Runnable cb, MemorySegment ctxp, MemorySegment pdata) {
	    upcallwrap(() -> cb.run());
	}
	private final MethodHandle context_notify_cb = slookup(MethodHandles.lookup(), libpulse_so_0.class, "context_notify_cb",
							Void.TYPE, Runnable.class, MemorySegment.class, MemorySegment.class);
	private final FunctionDescriptor context_notify_cb_sig = FunctionDescriptor.ofVoid(ADDRESS, ADDRESS);

	private final MethodHandle pa_context_set_state_callback = ld.downcallHandle(libpulse.find("pa_context_set_state_callback").get(), FunctionDescriptor.ofVoid(ADDRESS, ADDRESS, ADDRESS));
	public void pa_context_set_state_callback(LibPulse.pa_context gc, Runnable cb) {
	    pa_context c = (pa_context)gc;
	    MemorySegment cbt = ld.upcallStub(MethodHandles.insertArguments(context_notify_cb, 0, cb), context_notify_cb_sig, Arena.ofAuto());
	    try {
		pa_context_set_state_callback.invoke(c.mem, cbt, MemorySegment.NULL);
	    } catch(Throwable e) {
		throw(new InvocationException(e));
	    }
	    c.state_cb = cbt;
	}

	private static void context_event_cb(libpulse_so_0 lib, BiConsumer<? super String, ? super LibPulse.pa_proplist> cb, MemorySegment ctxp, MemorySegment namep, MemorySegment propsp, MemorySegment pdata) {
	    upcallwrap(() -> {
		String name = namep.reinterpret(Long.MAX_VALUE).getString(0, C_CHARSET);
		pa_proplist props = lib.new pa_proplist(propsp);
		try {
		    cb.accept(name, props);
		} finally {
		    props.mem = null;
		}
	    });
	}
	private final MethodHandle context_event_cb = slookup(MethodHandles.lookup(), libpulse_so_0.class, "context_event_cb",
							Void.TYPE, libpulse_so_0.class, BiConsumer.class, MemorySegment.class, MemorySegment.class, MemorySegment.class, MemorySegment.class);
	private final FunctionDescriptor context_event_cb_sig = FunctionDescriptor.ofVoid(ADDRESS, ADDRESS, ADDRESS, ADDRESS);

	private final MethodHandle pa_context_set_event_callback = ld.downcallHandle(libpulse.find("pa_context_set_event_callback").get(), FunctionDescriptor.ofVoid(ADDRESS, ADDRESS, ADDRESS));
	public void pa_context_set_event_callback(LibPulse.pa_context gc, BiConsumer<? super String, ? super LibPulse.pa_proplist> cb) {
	    pa_context c = (pa_context)gc;
	    MemorySegment cbt = ld.upcallStub(MethodHandles.insertArguments(context_event_cb, 0, this, cb), context_event_cb_sig, Arena.ofAuto());
	    try {
		pa_context_set_event_callback.invoke(c.mem, cbt, MemorySegment.NULL);
	    } catch(Throwable e) {
		throw(new InvocationException(e));
	    }
	    c.event_cb = cbt;
	}

	class pa_proplist implements LibPulse.pa_proplist {
	    MemorySegment mem;

	    pa_proplist(MemorySegment mem) {
		this.mem = mem;
	    }

	    public libpulse_so_0 lib() {return(libpulse_so_0.this);}
	}

	private final MethodHandle pa_proplist_free = ld.downcallHandle(libpulse.find("pa_proplist_free").get(), FunctionDescriptor.ofVoid(ADDRESS));
	void pa_proplist_free(MemorySegment mem) {
	    try {
		pa_proplist_free.invoke(mem);
	    } catch(Throwable e) {
		throw(new InvocationException(e));
	    }
	}

	private final MethodHandle pa_proplist_new = ld.downcallHandle(libpulse.find("pa_proplist_new").get(), FunctionDescriptor.of(ADDRESS));
	public pa_proplist pa_proplist_new() {
	    MemorySegment rv;
	    try {
		rv = (MemorySegment)pa_proplist_new.invoke();
	    } catch(Throwable e) {
		throw(new InvocationException(e));
	    }
	    pa_proplist ret = new pa_proplist(rv);
	    Finalizer.finalize(ret, () -> pa_proplist_free(rv));
	    return(ret);
	}

	private final MethodHandle pa_proplist_set = ld.downcallHandle(libpulse.find("pa_proplist_set").get(), FunctionDescriptor.of(C_INT, ADDRESS, ADDRESS, ADDRESS, SIZE_T),
								       Linker.Option.critical(true));
	public void pa_proplist_set(LibPulse.pa_proplist p, String key, byte[] data) {
	    int rv;
	    try(Arena st = Arena.ofConfined()) {
		rv = (int)pa_proplist_set.invoke(((pa_proplist)p).mem, st.allocateFrom(key, Utils.utf8), MemorySegment.ofArray(data), data.length);
	    } catch(Throwable e) {
		throw(new InvocationException(e));
	    }
	    if(rv != 0)
		throw(new PulseException("pa_proplist_set: " + rv));
	}

	private final MethodHandle pa_proplist_sets = ld.downcallHandle(libpulse.find("pa_proplist_sets").get(), FunctionDescriptor.of(C_INT, ADDRESS, ADDRESS, ADDRESS));
	public void pa_proplist_sets(LibPulse.pa_proplist p, String key, String val) {
	    int rv;
	    try(Arena st = Arena.ofConfined()) {
		rv = (int)pa_proplist_sets.invoke(((pa_proplist)p).mem, st.allocateFrom(key, Utils.utf8), st.allocateFrom(val, Utils.utf8));
	    } catch(Throwable e) {
		throw(new InvocationException(e));
	    }
	    if(rv != 0)
		throw(new PulseException("pa_proplist_sets: " + rv));
	}

	static final StructLayout _pa_sample_spec = struct(new MemoryLayout[] {
	    C_ENUM.withName("format"),
	    ValueLayout.JAVA_INT.withName("rate"),
	    ValueLayout.JAVA_BYTE.withName("channels"),
	});
	class pa_sample_spec extends StructInstance implements LibPulse.pa_sample_spec {
	    pa_sample_spec(MemorySegment mem) {
		super(mem);
	    }

	    public StructLayout $layout() {return(_pa_sample_spec);}
	    public MemorySegment mem() {return(mem);}

	    private static final VarHandle format = _pa_sample_spec.varHandle(PathElement.groupElement("format"));
	    public pa_sample_spec format(int value) {format.set(mem, 0, value); return(this);}
	    private static final VarHandle rate = _pa_sample_spec.varHandle(PathElement.groupElement("rate"));
	    public pa_sample_spec rate(int value) {rate.set(mem, 0, value); return(this);}
	    private static final VarHandle channels = _pa_sample_spec.varHandle(PathElement.groupElement("channels"));
	    public pa_sample_spec channels(int value) {channels.set(mem, 0, (byte)value); return(this);}
	    public int channels() {return((int)channels.get(mem, 0));}
	}
	public pa_sample_spec pa_sample_spec() {return(new pa_sample_spec(Arena.ofAuto().allocate(_pa_sample_spec)));}

	private final MethodHandle pa_usec_to_bytes = ld.downcallHandle(libpulse.find("pa_usec_to_bytes").get(), FunctionDescriptor.of(SIZE_T, PA_USEC_T, ADDRESS));
	public long pa_usec_to_bytes(long t, LibPulse.pa_sample_spec spec) {
	    try {
		return((long)pa_usec_to_bytes.invoke(t, ((pa_sample_spec)spec).mem()));
	    } catch(Throwable e) {
		throw(new InvocationException(e));
	    }
	}

	static final StructLayout _pa_buffer_attr = struct(new MemoryLayout[] {
	    ValueLayout.JAVA_INT.withName("maxlength"),
	    ValueLayout.JAVA_INT.withName("tlength"),
	    ValueLayout.JAVA_INT.withName("prebuf"),
	    ValueLayout.JAVA_INT.withName("minreq"),
	    ValueLayout.JAVA_INT.withName("fragsize"),
	});
	class pa_buffer_attr extends StructInstance implements LibPulse.pa_buffer_attr {
	    pa_buffer_attr(MemorySegment mem) {
		super(mem);
	    }

	    public StructLayout $layout() {return(_pa_buffer_attr);}
	    public MemorySegment mem() {return(mem);}

	    private static final VarHandle maxlength = _pa_buffer_attr.varHandle(PathElement.groupElement("maxlength"));
	    public pa_buffer_attr maxlength(int value) {maxlength.set(mem, 0, value); return(this);}
	    private static final VarHandle tlength = _pa_buffer_attr.varHandle(PathElement.groupElement("tlength"));
	    public pa_buffer_attr tlength(int value) {tlength.set(mem, 0, value); return(this);}
	    private static final VarHandle prebuf = _pa_buffer_attr.varHandle(PathElement.groupElement("prebuf"));
	    public pa_buffer_attr prebuf(int value) {prebuf.set(mem, 0, value); return(this);}
	    private static final VarHandle minreq = _pa_buffer_attr.varHandle(PathElement.groupElement("minreq"));
	    public pa_buffer_attr minreq(int value) {minreq.set(mem, 0, value); return(this);}
	    private static final VarHandle fragsize = _pa_buffer_attr.varHandle(PathElement.groupElement("fragsize"));
	    public pa_buffer_attr fragsize(int value) {fragsize.set(mem, 0, value); return(this);}
	}
	public pa_buffer_attr pa_buffer_attr() {return(new pa_buffer_attr(Arena.ofAuto().allocate(_pa_buffer_attr)));}

	class pa_stream implements LibPulse.pa_stream {
	    final MemorySegment mem;
	    private final Object retain;
	    Object state_cb, write_cb;

	    pa_stream(MemorySegment mem, Object retain) {
		this.mem = mem;
		this.retain = retain;
		libpulse_so_0 lib = libpulse_so_0.this;
		Finalizer.finalize(this, () -> lib.pa_stream_unref(mem));
	    }

	    public libpulse_so_0 lib() {return(libpulse_so_0.this);}
	}

	private final MethodHandle pa_stream_unref = ld.downcallHandle(libpulse.find("pa_stream_unref").get(), FunctionDescriptor.ofVoid(ADDRESS));
	void pa_stream_unref(MemorySegment m) {
	    try {
		pa_stream_unref.invoke(m);
	    } catch(Throwable e) {
		throw(new InvocationException(e));
	    }
	}

	private final MethodHandle pa_stream_new = ld.downcallHandle(libpulse.find("pa_stream_new").get(), FunctionDescriptor.of(ADDRESS, ADDRESS, ADDRESS, ADDRESS, ADDRESS));
	public pa_stream pa_stream_new(LibPulse.pa_context c, String name, LibPulse.pa_sample_spec ss) {
	    MemorySegment rv;
	    try(Arena st = Arena.ofConfined()) {
		rv = (MemorySegment)pa_stream_new.invoke(((pa_context)c).mem, st.allocateFrom(name, C_CHARSET), ((pa_sample_spec)ss).mem(), MemorySegment.NULL);
	    } catch(Throwable e) {
		throw(new InvocationException(e));
	    }
	    return(nullp(rv) ? null : new pa_stream(rv, c));
	}

	private final MethodHandle pa_stream_new_with_proplist = ld.downcallHandle(libpulse.find("pa_stream_new_with_proplist").get(), FunctionDescriptor.of(ADDRESS, ADDRESS, ADDRESS, ADDRESS, ADDRESS, ADDRESS));
	public pa_stream pa_stream_new_with_proplist(LibPulse.pa_context c, String name, LibPulse.pa_sample_spec ss, LibPulse.pa_proplist props) {
	    MemorySegment rv;
	    try(Arena st = Arena.ofConfined()) {
		rv = (MemorySegment)pa_stream_new_with_proplist.invoke(((pa_context)c).mem, st.allocateFrom(name, C_CHARSET), ((pa_sample_spec)ss).mem(), MemorySegment.NULL, ((pa_proplist)props).mem);
	    } catch(Throwable e) {
		throw(new InvocationException(e));
	    }
	    return(nullp(rv) ? null : new pa_stream(rv, c));
	}

	private static void stream_notify_cb(Runnable cb, MemorySegment ctxp, MemorySegment pdata) {
	    upcallwrap(() -> cb.run());
	}
	private final MethodHandle stream_notify_cb = slookup(MethodHandles.lookup(), libpulse_so_0.class, "stream_notify_cb",
							Void.TYPE, Runnable.class, MemorySegment.class, MemorySegment.class);
	private final FunctionDescriptor stream_notify_cb_sig = FunctionDescriptor.ofVoid(ADDRESS, ADDRESS);

	private final MethodHandle pa_stream_set_state_callback = ld.downcallHandle(libpulse.find("pa_stream_set_state_callback").get(), FunctionDescriptor.ofVoid(ADDRESS, ADDRESS, ADDRESS));
	public void pa_stream_set_state_callback(LibPulse.pa_stream gs, Runnable cb) {
	    pa_stream s = (pa_stream)gs;
	    MemorySegment cbt = ld.upcallStub(MethodHandles.insertArguments(stream_notify_cb, 0, cb), stream_notify_cb_sig, Arena.ofAuto());
	    try {
		pa_stream_set_state_callback.invoke(s.mem, cbt, MemorySegment.NULL);
	    } catch(Throwable e) {
		throw(new InvocationException(e));
	    }
	    s.state_cb = cbt;
	}

	private static void stream_request_cb(LongConsumer cb, MemorySegment ctxp, long nbytes, MemorySegment pdata) {
	    upcallwrap(() -> cb.accept(nbytes));
	}
	private final MethodHandle stream_request_cb = slookup(MethodHandles.lookup(), libpulse_so_0.class, "stream_request_cb",
							Void.TYPE, LongConsumer.class, MemorySegment.class, Long.TYPE, MemorySegment.class);
	private final FunctionDescriptor stream_request_cb_sig = FunctionDescriptor.ofVoid(ADDRESS, SIZE_T, ADDRESS);

	private final MethodHandle pa_stream_set_write_callback = ld.downcallHandle(libpulse.find("pa_stream_set_write_callback").get(), FunctionDescriptor.ofVoid(ADDRESS, ADDRESS, ADDRESS));
	public void pa_stream_set_write_callback(LibPulse.pa_stream gs, LongConsumer cb) {
	    pa_stream s = (pa_stream)gs;
	    MemorySegment cbt = ld.upcallStub(MethodHandles.insertArguments(stream_request_cb, 0, cb), stream_request_cb_sig, Arena.ofAuto());
	    try {
		pa_stream_set_write_callback.invoke(s.mem, cbt, MemorySegment.NULL);
	    } catch(Throwable e) {
		throw(new InvocationException(e));
	    }
	    s.write_cb = cbt;
	}
	private final MethodHandle pa_stream_connect_playback = ld.downcallHandle(libpulse.find("pa_stream_connect_playback").get(), FunctionDescriptor.of(C_INT, ADDRESS, ADDRESS, ADDRESS, C_ENUM, ADDRESS, ADDRESS));
	public void pa_stream_connect_playback(LibPulse.pa_stream s, String dev, LibPulse.pa_buffer_attr attr, int flags) {
	    int rv;
	    try(Arena st = Arena.ofConfined()) {
		rv = (int)pa_stream_connect_playback.invoke(((pa_stream)s).mem, (dev == null) ? MemorySegment.NULL : st.allocateFrom(dev, C_CHARSET), ((pa_buffer_attr)attr).mem(), flags, MemorySegment.NULL, MemorySegment.NULL);
	    } catch(Throwable e) {
		throw(new InvocationException(e));
	    }
	    if(rv != 0)
		throw(new PulseException("pa_stream_connect_playback: " + rv));
	}

	private final MethodHandle pa_stream_get_state = ld.downcallHandle(libpulse.find("pa_stream_get_state").get(), FunctionDescriptor.of(C_ENUM, ADDRESS));
	public int pa_stream_get_state(LibPulse.pa_stream s) {
	    try {
		return((int)pa_stream_get_state.invoke(((pa_stream)s).mem));
	    } catch(Throwable e) {
		throw(new InvocationException(e));
	    }
	}

	private final MethodHandle pa_stream_begin_write = ld.downcallHandle(libpulse.find("pa_stream_begin_write").get(), FunctionDescriptor.of(C_INT, ADDRESS, ADDRESS, ADDRESS));
	public ByteBuffer pa_stream_begin_write(LibPulse.pa_stream s, long wanted) {
	    try(Arena st = Arena.ofConfined()) {
		MemorySegment datap = st.allocate(ADDRESS), nbytesp = st.allocate(SIZE_T);
		setint(nbytesp, 0, SIZE_T, wanted);
		int rv;
		try {
		    rv = (int)pa_stream_begin_write.invoke(((pa_stream)s).mem, datap, nbytesp);
		} catch(Throwable e) {
		    throw(new InvocationException(e));
		}
		if(rv != 0)
		    throw(new PulseException("pa_stream_begin_write: " + rv));
		MemorySegment data = datap.get(ADDRESS, 0);
		if(nullp(data))
		    throw(new PulseException("pa_stream_begin_write: NULL data"));
		return(data.reinterpret(getint(nbytesp, 0, SIZE_T, false)).asByteBuffer());
	    }
	}

	private final MethodHandle pa_stream_write = ld.downcallHandle(libpulse.find("pa_stream_write").get(), FunctionDescriptor.of(C_INT, ADDRESS, ADDRESS, SIZE_T, ADDRESS, ValueLayout.JAVA_LONG, C_ENUM));
	public void pa_stream_write(LibPulse.pa_stream s, ByteBuffer data, long offset, int seek) {
	    int rv;
	    try {
		rv = (int)pa_stream_write.invoke(((pa_stream)s).mem, MemorySegment.ofBuffer(data), data.remaining(), MemorySegment.NULL, offset, seek);
	    } catch(Throwable e) {
		throw(new InvocationException(e));
	    }
	    if(rv != 0)
		throw(new PulseException("pa_stream_begin_write: " + rv));
	}

	private final MethodHandle pa_stream_cancel_write = ld.downcallHandle(libpulse.find("pa_stream_cancel_write").get(), FunctionDescriptor.of(C_INT, ADDRESS));
	public void pa_stream_cancel_write(LibPulse.pa_stream s) {
	    int rv;
	    try {
		rv = (int)pa_stream_cancel_write.invoke(((pa_stream)s).mem);
	    } catch(Throwable e) {
		throw(new InvocationException(e));
	    }
	    if(rv != 0)
		throw(new PulseException("pa_stream_begin_cancel_write: " + rv));
	}

	private final MethodHandle pa_stream_disconnect = ld.downcallHandle(libpulse.find("pa_stream_disconnect").get(), FunctionDescriptor.of(C_INT, ADDRESS));
	public void pa_stream_disconnect(LibPulse.pa_stream s) {
	    int rv;
	    try {
		rv = (int)pa_stream_disconnect.invoke(((pa_stream)s).mem);
	    } catch(Throwable e) {
		throw(new InvocationException(e));
	    }
	    if(rv != 0)
		throw(new PulseException("pa_stream_disconnect: " + rv));
	}
    }

    private static LibPulse instance = null;
    public static LibPulse get() {
	if(instance == null) {
	    synchronized(LibPulse.class) {
		if(instance == null)
		    instance = tryload("libpulse", libpulse_so_0::new);
	    }
	}
	return(instance);
    }
}
