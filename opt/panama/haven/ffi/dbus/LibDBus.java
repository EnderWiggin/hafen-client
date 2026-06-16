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

package haven.ffi.dbus;

import haven.*;
import haven.ffi.*;
import java.lang.invoke.*;
import java.lang.foreign.*;
import java.lang.foreign.MemoryLayout.PathElement;
import java.util.*;
import java.util.function.*;
import static haven.ffi.ABI.*;
import static haven.ffi.FUtils.*;
import static java.lang.foreign.ValueLayout.ADDRESS;

public abstract class LibDBus {
    public static final int DBUS_BUS_SESSION = 0;
    public static final int DBUS_BUS_SYSTEM = 1;
    public static final int DBUS_BUS_STARTER = 2;

    public static final int DBUS_MESSAGE_TYPE_INVALID = 0;
    public static final int DBUS_MESSAGE_TYPE_METHOD_CALL = 1;
    public static final int DBUS_MESSAGE_TYPE_METHOD_RETURN = 2;
    public static final int DBUS_MESSAGE_TYPE_ERROR = 3;
    public static final int DBUS_MESSAGE_TYPE_SIGNAL = 4;

    public static final int DBUS_DISPATCH_DATA_REMAINS = 0;
    public static final int DBUS_DISPATCH_COMPLETE = 1;
    public static final int DBUS_DISPATCH_NEED_MEMORY = 2;

    public static final int DBUS_HANDLER_RESULT_HANDLED = 0;
    public static final int DBUS_HANDLER_RESULT_NOT_YET_HANDLED = 1;
    public static final int DBUS_HANDLER_RESULT_NEED_MEMORY = 2;

    public static final int DBUS_TYPE_INVALID = 0;
    public static final int DBUS_TYPE_BYTE = 'y';
    public static final int DBUS_TYPE_BOOLEAN = 'b';
    public static final int DBUS_TYPE_INT16 = 'n';
    public static final int DBUS_TYPE_UINT16 = 'q';
    public static final int DBUS_TYPE_INT32 = 'i';
    public static final int DBUS_TYPE_UINT32 = 'u';
    public static final int DBUS_TYPE_INT64 = 'x';
    public static final int DBUS_TYPE_UINT64 = 't';
    public static final int DBUS_TYPE_DOUBLE = 'd';
    public static final int DBUS_TYPE_STRING = 's';
    public static final int DBUS_TYPE_OBJECT_PATH = 'o';
    public static final int DBUS_TYPE_SIGNATURE = 'g';
    public static final int DBUS_TYPE_UNIX_FD = 'h';
    public static final int DBUS_TYPE_ARRAY = 'a';
    public static final int DBUS_TYPE_VARIANT = 'v';
    public static final int DBUS_TYPE_STRUCT = 'r';
    public static final int DBUS_TYPE_DICT_ENTRY = 'e';

    public static interface Error {
	public String name();
	public String message();
    }

    public static interface Connection {
    }

    public static interface PendingCall {
    }

    public static interface Message {
    }

    public static interface MessageIter {
    }

    public abstract Error Error();
    public abstract MessageIter MessageIter();

    public abstract boolean dbus_error_is_set(Error error);
    public abstract boolean dbus_pending_call_get_completed(PendingCall call);
    public abstract Message dbus_pending_call_steal_reply(PendingCall call);
    public abstract Connection dbus_bus_get(int type, Error error);
    public abstract String dbus_bus_get_unique_name(LibDBus.Connection conn);
    public abstract void dbus_bus_add_match(Connection conn, String rule, Error error);
    public abstract void dbus_bus_remove_match(Connection conn, String rule, Error error);
    public abstract int dbus_connection_get_unix_fd(Connection conn);
    public abstract PendingCall dbus_connection_send_with_reply(Connection conn, Message msg, int timeout);
    public abstract boolean dbus_connection_has_messages_to_send(Connection conn);
    public abstract boolean dbus_connection_read_write(Connection conn, int timeout);
    public abstract boolean dbus_connection_dispatch(Connection conn);
    public abstract MemorySegment dbus_connection_add_filter(Connection conn, Predicate<Message> cb);
    public abstract void dbus_connection_remove_filter(Connection conn, MemorySegment handlerp);
    public abstract Message dbus_connection_pop_message(Connection conn);
    public abstract Message dbus_message_new_method_call(String destination, String path, String iface, String method);
    public abstract void dbus_message_set_no_reply(Message message, boolean noreply);
    public abstract int dbus_message_get_type(Message msg);
    public abstract String dbus_message_get_signature(Message msg);
    public abstract String dbus_message_get_error_name(Message msg);
    public abstract String dbus_message_get_sender(Message msg);
    public abstract String dbus_message_get_path(Message msg);
    public abstract String dbus_message_get_interface(Message msg);
    public abstract String dbus_message_get_member(Message msg);
    public abstract void dbus_message_iter_init(Message message, MessageIter iter);
    public abstract void dbus_message_iter_init_closed(MessageIter iter);
    public abstract boolean dbus_message_iter_has_next(MessageIter iter);
    public abstract boolean dbus_message_iter_next(MessageIter iter);
    public abstract int dbus_message_iter_get_arg_type(MessageIter iter);
    public abstract int dbus_message_iter_get_element_type(MessageIter iter);
    public abstract void dbus_message_iter_recurse(MessageIter iter, MessageIter sub);
    public abstract String dbus_message_iter_get_signature(MessageIter iter);
    abstract void dbus_message_iter_get_basic(MessageIter iter, MemorySegment buf);
    public abstract void dbus_message_iter_init_append(Message message, MessageIter iter);
    abstract void dbus_message_iter_append_basic(MessageIter iter, int type, MemorySegment value);
    public abstract void dbus_message_iter_open_container(MessageIter iter, int type, String contained_signature, MessageIter sub);
    public abstract void dbus_message_iter_close_container(MessageIter iter, MessageIter sub);

    public void dbus_message_iter_append_basic(MessageIter iter, byte value) {
	try(Arena st = Arena.ofConfined()) {
	    dbus_message_iter_append_basic(iter, DBUS_TYPE_BYTE, st.allocateFrom(ValueLayout.JAVA_BYTE, value));
	}
    }
    public void dbus_message_iter_append_basic(MessageIter iter, boolean value) {
	try(Arena st = Arena.ofConfined()) {
	    dbus_message_iter_append_basic(iter, DBUS_TYPE_BOOLEAN, st.allocateFrom(ValueLayout.JAVA_INT, value ? 1 : 0));
	}
    }
    public void dbus_message_iter_append_basic(MessageIter iter, short value, boolean signed) {
	try(Arena st = Arena.ofConfined()) {
	    dbus_message_iter_append_basic(iter, signed ? DBUS_TYPE_INT16 : DBUS_TYPE_UINT16, st.allocateFrom(ValueLayout.JAVA_SHORT, value));
	}
    }
    public void dbus_message_iter_append_basic(MessageIter iter, int value, boolean signed) {
	try(Arena st = Arena.ofConfined()) {
	    dbus_message_iter_append_basic(iter, signed ? DBUS_TYPE_INT32 : DBUS_TYPE_UINT32, st.allocateFrom(ValueLayout.JAVA_INT, value));
	}
    }
    public void dbus_message_iter_append_basic(MessageIter iter, long value, boolean signed) {
	try(Arena st = Arena.ofConfined()) {
	    dbus_message_iter_append_basic(iter, signed ? DBUS_TYPE_INT64 : DBUS_TYPE_UINT64, st.allocateFrom(ValueLayout.JAVA_LONG, value));
	}
    }
    public void dbus_message_iter_append_basic(MessageIter iter, double value) {
	try(Arena st = Arena.ofConfined()) {
	    dbus_message_iter_append_basic(iter, DBUS_TYPE_DOUBLE, st.allocateFrom(ValueLayout.JAVA_DOUBLE, value));
	}
    }
    public void dbus_message_iter_append_basic(MessageIter iter, String value) {
	try(Arena st = Arena.ofConfined()) {
	    dbus_message_iter_append_basic(iter, DBUS_TYPE_STRING, st.allocateFrom(ADDRESS, st.allocateFrom(value, Utils.utf8)));
	}
    }

    public byte dbus_message_iter_get_byte(MessageIter iter) {
	if(dbus_message_iter_get_arg_type(iter) != DBUS_TYPE_BYTE)
	    throw(new IllegalArgumentException());
	try(Arena st = Arena.ofConfined()) {
	    MemorySegment buf = st.allocate(ValueLayout.JAVA_BYTE);
	    dbus_message_iter_get_basic(iter, buf);
	    return(buf.get(ValueLayout.JAVA_BYTE, 0));
	}
    }

    public boolean dbus_message_iter_get_boolean(MessageIter iter) {
	if(dbus_message_iter_get_arg_type(iter) != DBUS_TYPE_BOOLEAN)
	    throw(new IllegalArgumentException());
	try(Arena st = Arena.ofConfined()) {
	    MemorySegment buf = st.allocate(ValueLayout.JAVA_INT);
	    dbus_message_iter_get_basic(iter, buf);
	    return(buf.get(ValueLayout.JAVA_INT, 0) != 0);
	}
    }

    public short dbus_message_iter_get_short(MessageIter iter) {
	if(!Arrays.asList(DBUS_TYPE_INT16, DBUS_TYPE_UINT16).contains(dbus_message_iter_get_arg_type(iter)))
	    throw(new IllegalArgumentException());
	try(Arena st = Arena.ofConfined()) {
	    MemorySegment buf = st.allocate(ValueLayout.JAVA_SHORT);
	    dbus_message_iter_get_basic(iter, buf);
	    return(buf.get(ValueLayout.JAVA_SHORT, 0));
	}
    }

    public int dbus_message_iter_get_int(MessageIter iter) {
	if(!Arrays.asList(DBUS_TYPE_INT32, DBUS_TYPE_UINT32).contains(dbus_message_iter_get_arg_type(iter)))
	    throw(new IllegalArgumentException());
	try(Arena st = Arena.ofConfined()) {
	    MemorySegment buf = st.allocate(ValueLayout.JAVA_INT);
	    dbus_message_iter_get_basic(iter, buf);
	    return(buf.get(ValueLayout.JAVA_INT, 0));
	}
    }

    public long dbus_message_iter_get_long(MessageIter iter) {
	if(!Arrays.asList(DBUS_TYPE_INT64, DBUS_TYPE_UINT64).contains(dbus_message_iter_get_arg_type(iter)))
	    throw(new IllegalArgumentException());
	try(Arena st = Arena.ofConfined()) {
	    MemorySegment buf = st.allocate(ValueLayout.JAVA_LONG);
	    dbus_message_iter_get_basic(iter, buf);
	    return(buf.get(ValueLayout.JAVA_LONG, 0));
	}
    }

    public double dbus_message_iter_get_double(MessageIter iter) {
	if(dbus_message_iter_get_arg_type(iter) != DBUS_TYPE_DOUBLE)
	    throw(new IllegalArgumentException());
	try(Arena st = Arena.ofConfined()) {
	    MemorySegment buf = st.allocate(ValueLayout.JAVA_DOUBLE);
	    dbus_message_iter_get_basic(iter, buf);
	    return(buf.get(ValueLayout.JAVA_DOUBLE, 0));
	}
    }

    public String dbus_message_iter_get_string(MessageIter iter) {
	if(!Arrays.asList(DBUS_TYPE_STRING, DBUS_TYPE_OBJECT_PATH, DBUS_TYPE_SIGNATURE).contains(dbus_message_iter_get_arg_type(iter)))
	    throw(new IllegalArgumentException());
	try(Arena st = Arena.ofConfined()) {
	    MemorySegment buf = st.allocate(ADDRESS);
	    dbus_message_iter_get_basic(iter, buf);
	    return(buf.get(ADDRESS, 0).reinterpret(Long.MAX_VALUE).getString(0, Utils.utf8));
	}
    }

    public int dbus_message_iter_get_fd(MessageIter iter) {
	if(!Arrays.asList(DBUS_TYPE_UNIX_FD).contains(dbus_message_iter_get_arg_type(iter)))
	    throw(new IllegalArgumentException());
	try(Arena st = Arena.ofConfined()) {
	    MemorySegment buf = st.allocate(ValueLayout.JAVA_INT);
	    dbus_message_iter_get_basic(iter, buf);
	    return(buf.get(ValueLayout.JAVA_INT, 0));
	}
    }

    static class libdbus_1_so_3 extends LibDBus {
	static final ValueLayout DBUS_UINT32_T = ValueLayout.JAVA_INT;
	static final ValueLayout DBUS_BOOL_T = DBUS_UINT32_T;
	private final SymbolLookup dbus = SymbolLookup.libraryLookup("libdbus-1.so.3", Arena.global());

	static final StructLayout _Error = struct(new MemoryLayout[] {
		ADDRESS.withName("name"),
		ADDRESS.withName("message"),
		C_INT.withName("dummy"),
		ADDRESS.withName("padding"),
	    });
	public static class Error extends StructInstance implements LibDBus.Error {
	    Error(MemorySegment mem) {
		super(mem);
	    }

	    Error(libdbus_1_so_3 dbus) {
		this(Arena.ofAuto().allocate(_Error));
		MemorySegment p = mem;
		dbus.dbus_error_init(this);
		Finalizer.finalize(this, () -> dbus.dbus_error_free(p));
	    }

	    protected StructLayout $layout() {return(_Error);}
	    MemorySegment mem() {return(mem);}

	    private static final VarHandle name = _Error.varHandle(PathElement.groupElement("name"));
	    public String name() {return(((MemorySegment)name.get(mem, 0)).reinterpret(Long.MAX_VALUE).getString(0, C_CHARSET));}
	    private static final VarHandle message = _Error.varHandle(PathElement.groupElement("message"));
	    public String message() {return(((MemorySegment)message.get(mem, 0)).reinterpret(Long.MAX_VALUE).getString(0, C_CHARSET));}
	}

	public Error Error() {
	    return(new Error(this));
	}

	public static class Connection implements LibDBus.Connection {
	    final MemorySegment mem;

	    Connection(MemorySegment mem) {
		this.mem = mem;
	    }

	    MemorySegment mem() {return(mem);}
	}

	public static class PendingCall implements LibDBus.PendingCall {
	    final MemorySegment mem;

	    PendingCall(MemorySegment mem) {
		this.mem = mem;
	    }

	    MemorySegment mem() {return(mem);}
	}

	public static class Message implements LibDBus.Message {
	    final MemorySegment mem;

	    Message(MemorySegment mem) {
		this.mem = mem;
	    }

	    MemorySegment mem() {return(mem);}
	}

	static final StructLayout _MessageIter = struct(new MemoryLayout[] {
		ADDRESS.withName("dummy1"),
		ADDRESS.withName("dummy2"),
		DBUS_UINT32_T.withName("dummy3"),
		C_INT.withName("dummy4"),
		C_INT.withName("dummy5"),
		C_INT.withName("dummy5"),
		C_INT.withName("dummy7"),
		C_INT.withName("dummy8"),
		C_INT.withName("dummy9"),
		C_INT.withName("dummy10"),
		C_INT.withName("dummy11"),
		C_INT.withName("pad1"),
		ADDRESS.withName("pad2"),
		ADDRESS.withName("pad3"),
	    });
	public static class MessageIter implements LibDBus.MessageIter {
	    final MemorySegment mem;

	    MessageIter(MemorySegment mem) {
		this.mem = mem;
	    }

	    MessageIter(libdbus_1_so_3 dbus) {
		this(Arena.ofAuto().allocate(_MessageIter));
	    }

	    MemorySegment mem() {return(mem);}
	}

	public MessageIter MessageIter() {
	    return(new MessageIter(this));
	}

	private final MethodHandle dbus_free = ld.downcallHandle(dbus.find("dbus_free").get(), FunctionDescriptor.ofVoid(ADDRESS));
	private void dbus_free(MemorySegment mem) {
	    try {
		dbus_free.invoke(mem);
	    } catch(Throwable e) {
		throw(new RuntimeException(e));
	    }
	}

	private final MethodHandle dbus_error_init = ld.downcallHandle(dbus.find("dbus_error_init").get(), FunctionDescriptor.ofVoid(ADDRESS));
	public void dbus_error_init(LibDBus.Error error) {
	    try {
		dbus_error_init.invoke(((Error)error).mem());
	    } catch(Throwable e) {
		throw(new RuntimeException(e));
	    }
	}

	private final MethodHandle dbus_error_free = ld.downcallHandle(dbus.find("dbus_error_free").get(), FunctionDescriptor.ofVoid(ADDRESS));
	public void dbus_error_free(MemorySegment p) {
	    try {
		dbus_error_free.invoke(p);
	    } catch(Throwable e) {
		throw(new RuntimeException(e));
	    }
	}

	private final MethodHandle dbus_error_is_set = ld.downcallHandle(dbus.find("dbus_error_is_set").get(), FunctionDescriptor.of(DBUS_BOOL_T, ADDRESS));
	public boolean dbus_error_is_set(LibDBus.Error error) {
	    try {
		return((int)dbus_error_is_set.invoke(((Error)error).mem()) != 0);
	    } catch(Throwable e) {
		throw(new RuntimeException(e));
	    }
	}

	private final MethodHandle dbus_pending_call_unref = ld.downcallHandle(dbus.find("dbus_pending_call_unref").get(), FunctionDescriptor.ofVoid(ADDRESS));
	void dbus_pending_call_unref(MemorySegment call) {
	    try {
		dbus_pending_call_unref.invoke(call);
	    } catch(Throwable e) {
		throw(new RuntimeException(e));
	    }
	}

	private final MethodHandle dbus_pending_call_get_completed = ld.downcallHandle(dbus.find("dbus_pending_call_get_completed").get(), FunctionDescriptor.of(DBUS_BOOL_T, ADDRESS));
	public boolean dbus_pending_call_get_completed(LibDBus.PendingCall call) {
	    try {
		return((int)dbus_pending_call_get_completed.invoke(((PendingCall)call).mem()) != 0);
	    } catch(Throwable e) {
		throw(new RuntimeException(e));
	    }
	}

	private final MethodHandle dbus_pending_call_steal_reply = ld.downcallHandle(dbus.find("dbus_pending_call_steal_reply").get(), FunctionDescriptor.of(ADDRESS, ADDRESS));
	public Message dbus_pending_call_steal_reply(LibDBus.PendingCall call) {
	    MemorySegment rv;
	    try {
		rv = (MemorySegment)dbus_pending_call_steal_reply.invoke(((PendingCall)call).mem());
	    } catch(Throwable e) {
		throw(new RuntimeException(e));
	    }
	    if(nullp(rv))
		throw(new IllegalStateException());
	    Message msg = new Message(rv);
	    Finalizer.finalize(msg, () -> dbus_message_unref(rv));
	    return(msg);
	}

	private final MethodHandle dbus_connection_unref = ld.downcallHandle(dbus.find("dbus_connection_unref").get(), FunctionDescriptor.ofVoid(ADDRESS));
	void dbus_connection_unref(MemorySegment conn) {
	    try {
		dbus_connection_unref.invoke(conn);
	    } catch(Throwable e) {
		throw(new RuntimeException(e));
	    }
	}

	private final MethodHandle dbus_connection_get_unix_fd = ld.downcallHandle(dbus.find("dbus_connection_get_unix_fd").get(), FunctionDescriptor.of(DBUS_BOOL_T, ADDRESS, ADDRESS));
	public int dbus_connection_get_unix_fd(LibDBus.Connection conn) {
	    try(Arena st = Arena.ofConfined()) {
		MemorySegment buf = st.allocate(C_INT);
		int rv;
		try {
		    rv = (int)dbus_connection_get_unix_fd.invoke(((Connection)conn).mem(), buf);
		} catch(Throwable e) {
		    throw(new RuntimeException(e));
		}
		if(rv == 0)
		    throw(new UnsupportedOperationException());
		return((int)getint(buf, 0, C_INT, true));
	    }
	}

	private final MethodHandle dbus_connection_send_with_reply = ld.downcallHandle(dbus.find("dbus_connection_send_with_reply").get(), FunctionDescriptor.of(DBUS_BOOL_T, ADDRESS, ADDRESS, ADDRESS, C_INT));
	public PendingCall dbus_connection_send_with_reply(LibDBus.Connection conn, LibDBus.Message msg, int timeout) {
	    try(Arena st = Arena.ofConfined()) {
		MemorySegment rbuf = st.allocate(ADDRESS);
		int rv;
		try {
		    rv = (int)dbus_connection_send_with_reply.invoke(((Connection)conn).mem(), ((Message)msg).mem(), rbuf, timeout);
		} catch(Throwable e) {
		    throw(new RuntimeException(e));
		}
		if(rv == 0)
		    throw(new RuntimeException("dbus allocation failure"));
		MemorySegment pend = rbuf.get(ADDRESS, 0);
		if(nullp(pend))
		    throw(new RuntimeException("dbus call failed to send"));
		PendingCall ret = new PendingCall(pend);
		Finalizer.finalize(ret, () -> dbus_pending_call_unref(pend));
		return(ret);
	    }
	}

	private final MethodHandle dbus_connection_has_messages_to_send = ld.downcallHandle(dbus.find("dbus_connection_has_messages_to_send").get(), FunctionDescriptor.of(DBUS_BOOL_T, ADDRESS));
	public boolean dbus_connection_has_messages_to_send(LibDBus.Connection conn) {
	    int rv;
	    try {
		rv = (int)dbus_connection_has_messages_to_send.invoke(((Connection)conn).mem());
	    } catch(Throwable e) {
		throw(new RuntimeException(e));
	    }
	    return(rv != 0);
	}

	private final MethodHandle dbus_connection_read_write = ld.downcallHandle(dbus.find("dbus_connection_read_write").get(), FunctionDescriptor.of(DBUS_BOOL_T, ADDRESS, C_INT));
	public boolean dbus_connection_read_write(LibDBus.Connection conn, int timeout) {
	    int rv;
	    try {
		rv = (int)dbus_connection_read_write.invoke(((Connection)conn).mem(), timeout);
	    } catch(Throwable e) {
		throw(new RuntimeException(e));
	    }
	    return(rv != 0);
	}

	private final MethodHandle dbus_connection_dispatch = ld.downcallHandle(dbus.find("dbus_connection_dispatch").get(), FunctionDescriptor.of(C_ENUM, ADDRESS));
	public boolean dbus_connection_dispatch(LibDBus.Connection conn) {
	    int rv;
	    try {
		rv = (int)dbus_connection_dispatch.invoke(((Connection)conn).mem());
	    } catch(Throwable e) {
		throw(new RuntimeException(e));
	    }
	    if(rv == DBUS_DISPATCH_DATA_REMAINS)
		return(true);
	    if(rv == DBUS_DISPATCH_COMPLETE)
		return(false);
	    throw(new RuntimeException(Integer.toString(rv)));
	}

	private int filtercallback(Predicate<LibDBus.Message> cb, MemorySegment connp, MemorySegment msgp, MemorySegment pdata) {
	    try {
		Message msg = dbus_message_ref(new Message(msgp));
		Finalizer.finalize(msg, () -> dbus_message_unref(msgp));
		return(cb.test(msg) ? DBUS_HANDLER_RESULT_HANDLED : DBUS_HANDLER_RESULT_NOT_YET_HANDLED);
	    } catch(Throwable t) {
		Thread.UncaughtExceptionHandler h = Thread.currentThread().getUncaughtExceptionHandler();
		if(h == null)
		    new Warning(t, "Uncaught exception in dbus filter").issue();
		else
		    h.uncaughtException(Thread.currentThread(), t);
		return(DBUS_HANDLER_RESULT_NOT_YET_HANDLED);
	    }
	}

	private final MethodHandle dbus_connection_add_filter = ld.downcallHandle(dbus.find("dbus_connection_add_filter").get(), FunctionDescriptor.of(DBUS_BOOL_T, ADDRESS, ADDRESS, ADDRESS, ADDRESS));
	public MemorySegment dbus_connection_add_filter(LibDBus.Connection conn, Predicate<LibDBus.Message> cb) {
	    MethodHandle handler = vlookup(MethodHandles.lookup(), libdbus_1_so_3.class, "filtercallback", Integer.TYPE,
					   Predicate.class, MemorySegment.class, MemorySegment.class, MemorySegment.class);
	    handler = MethodHandles.insertArguments(handler, 0, this, cb);
	    MemorySegment handlerp = ld.upcallStub(handler, FunctionDescriptor.of(C_ENUM, ADDRESS, ADDRESS, ADDRESS), Arena.ofAuto());
	    int rv;
	    try {
		rv = (int)dbus_connection_add_filter.invoke(((Connection)conn).mem(), handlerp, MemorySegment.NULL, MemorySegment.NULL);
	    } catch(Throwable e) {
		throw(new RuntimeException(e));
	    }
	    if(rv == 0)
		throw(new RuntimeException("dbus allocation failure"));
	    return(handlerp);
	}

	private final MethodHandle dbus_connection_remove_filter = ld.downcallHandle(dbus.find("dbus_connection_remove_filter").get(), FunctionDescriptor.ofVoid(ADDRESS, ADDRESS, ADDRESS));
	public void dbus_connection_remove_filter(LibDBus.Connection conn, MemorySegment handlerp) {
	    try {
		dbus_connection_remove_filter.invoke(((Connection)conn).mem(), handlerp, MemorySegment.NULL);
	    } catch(Throwable e) {
		throw(new RuntimeException(e));
	    }
	}

	private final MethodHandle dbus_connection_pop_message = ld.downcallHandle(dbus.find("dbus_connection_pop_message").get(), FunctionDescriptor.of(ADDRESS, ADDRESS));
	public Message dbus_connection_pop_message(LibDBus.Connection conn) {
	    MemorySegment rv;
	    try {
		rv = (MemorySegment)dbus_connection_pop_message.invoke(((Connection)conn).mem());
	    } catch(Throwable e) {
		throw(new RuntimeException(e));
	    }
	    if(nullp(rv))
		return(null);
	    Message msg = new Message(rv);
	    Finalizer.finalize(msg, () -> dbus_message_unref(rv));
	    return(msg);
	}

	private final MethodHandle dbus_bus_get = ld.downcallHandle(dbus.find("dbus_bus_get").get(), FunctionDescriptor.of(ADDRESS, C_ENUM, ADDRESS));
	public Connection dbus_bus_get(int type, LibDBus.Error error) {
	    MemorySegment rv;
	    try {
		rv = (MemorySegment)dbus_bus_get.invoke(type, ((Error)error).mem());
	    } catch(Throwable e) {
		throw(new RuntimeException(e));
	    }
	    if(nullp(rv))
		return(null);
	    Connection conn = new Connection(rv);
	    Finalizer.finalize(conn, () -> dbus_connection_unref(rv));
	    return(conn);
	}

	private final MethodHandle dbus_bus_get_unique_name = ld.downcallHandle(dbus.find("dbus_bus_get_unique_name").get(), FunctionDescriptor.of(ADDRESS, ADDRESS));
	public String dbus_bus_get_unique_name(LibDBus.Connection conn) {
	    MemorySegment rv;
	    try {
		rv = (MemorySegment)dbus_bus_get_unique_name.invoke(((Connection)conn).mem());
	    } catch(Throwable e) {
		throw(new RuntimeException(e));
	    }
	    return(nullp(rv) ? null : rv.reinterpret(Long.MAX_VALUE).getString(0, C_CHARSET));
	}

	private final MethodHandle dbus_bus_add_match = ld.downcallHandle(dbus.find("dbus_bus_add_match").get(), FunctionDescriptor.ofVoid(ADDRESS, ADDRESS, ADDRESS));
	public void dbus_bus_add_match(LibDBus.Connection conn, String rule, LibDBus.Error error) {
	    try(Arena st = Arena.ofConfined()) {
		try {
		    dbus_bus_add_match.invoke(((Connection)conn).mem(), st.allocateFrom(rule, C_CHARSET), ((Error)error).mem());
		} catch(Throwable e) {
		    throw(new RuntimeException(e));
		}
	    }
	}

	private final MethodHandle dbus_bus_remove_match = ld.downcallHandle(dbus.find("dbus_bus_remove_match").get(), FunctionDescriptor.ofVoid(ADDRESS, ADDRESS, ADDRESS));
	public void dbus_bus_remove_match(LibDBus.Connection conn, String rule, LibDBus.Error error) {
	    try(Arena st = Arena.ofConfined()) {
		try {
		    dbus_bus_remove_match.invoke(((Connection)conn).mem(), st.allocateFrom(rule, C_CHARSET), ((Error)error).mem());
		} catch(Throwable e) {
		    throw(new RuntimeException(e));
		}
	    }
	}

	private final MethodHandle dbus_message_ref = ld.downcallHandle(dbus.find("dbus_message_ref").get(), FunctionDescriptor.of(ADDRESS, ADDRESS));
	Message dbus_message_ref(LibDBus.Message message) {
	    try {
		dbus_message_ref.invoke(((Message)message).mem());
	    } catch(Throwable e) {
		throw(new RuntimeException(e));
	    }
	    return((Message)message);
	}

	private final MethodHandle dbus_message_unref = ld.downcallHandle(dbus.find("dbus_message_unref").get(), FunctionDescriptor.ofVoid(ADDRESS));
	void dbus_message_unref(MemorySegment message) {
	    try {
		dbus_message_unref.invoke(message);
	    } catch(Throwable e) {
		throw(new RuntimeException(e));
	    }
	}

	private final MethodHandle dbus_message_new_method_call = ld.downcallHandle(dbus.find("dbus_message_new_method_call").get(), FunctionDescriptor.of(ADDRESS, ADDRESS, ADDRESS, ADDRESS, ADDRESS));
	public Message dbus_message_new_method_call(String destination, String path, String iface, String method) {
	    MemorySegment rv;
	    try(Arena st = Arena.ofConfined()) {
		rv = (MemorySegment)dbus_message_new_method_call.invoke(st.allocateFrom(destination, C_CHARSET), st.allocateFrom(path, C_CHARSET), st.allocateFrom(iface, C_CHARSET), st.allocateFrom(method, C_CHARSET));
	    } catch(Throwable e) {
		throw(new RuntimeException(e));
	    }
	    Message msg = new Message(rv);
	    Finalizer.finalize(msg, () -> dbus_message_unref(rv));
	    return(msg);
	}

	private final MethodHandle dbus_message_set_no_reply = ld.downcallHandle(dbus.find("dbus_message_set_no_reply").get(), FunctionDescriptor.ofVoid(ADDRESS, DBUS_BOOL_T));
	public void dbus_message_set_no_reply(LibDBus.Message message, boolean noreply) {
	    try {
		dbus_message_set_no_reply.invoke(((Message)message).mem(), noreply ? 1 : 0);
	    } catch(Throwable e) {
		throw(new RuntimeException(e));
	    }
	}

	private final MethodHandle dbus_message_get_type = ld.downcallHandle(dbus.find("dbus_message_get_type").get(), FunctionDescriptor.of(C_INT, ADDRESS));
	public int dbus_message_get_type(LibDBus.Message msg) {
	    try {
		return((int)dbus_message_get_type.invoke(((Message)msg).mem()));
	    } catch(Throwable e) {
		throw(new RuntimeException(e));
	    }
	}

	private final MethodHandle dbus_message_get_signature = ld.downcallHandle(dbus.find("dbus_message_get_signature").get(), FunctionDescriptor.of(ADDRESS, ADDRESS));
	public String dbus_message_get_signature(LibDBus.Message msg) {
	    MemorySegment rv;
	    try {
		rv = (MemorySegment)dbus_message_get_signature.invoke(((Message)msg).mem());
	    } catch(Throwable e) {
		throw(new RuntimeException(e));
	    }
	    return(nullp(rv) ? null : rv.reinterpret(Long.MAX_VALUE).getString(0, C_CHARSET));
	}

	private final MethodHandle dbus_message_get_error_name = ld.downcallHandle(dbus.find("dbus_message_get_error_name").get(), FunctionDescriptor.of(ADDRESS, ADDRESS));
	public String dbus_message_get_error_name(LibDBus.Message msg) {
	    MemorySegment rv;
	    try {
		rv = (MemorySegment)dbus_message_get_error_name.invoke(((Message)msg).mem());
	    } catch(Throwable e) {
		throw(new RuntimeException(e));
	    }
	    return(nullp(rv) ? null : rv.reinterpret(Long.MAX_VALUE).getString(0, C_CHARSET));
	}

	private final MethodHandle dbus_message_get_sender = ld.downcallHandle(dbus.find("dbus_message_get_sender").get(), FunctionDescriptor.of(ADDRESS, ADDRESS));
	public String dbus_message_get_sender(LibDBus.Message msg) {
	    MemorySegment rv;
	    try {
		rv = (MemorySegment)dbus_message_get_sender.invoke(((Message)msg).mem());
	    } catch(Throwable e) {
		throw(new RuntimeException(e));
	    }
	    return(nullp(rv) ? null : rv.reinterpret(Long.MAX_VALUE).getString(0, C_CHARSET));
	}

	private final MethodHandle dbus_message_get_path = ld.downcallHandle(dbus.find("dbus_message_get_path").get(), FunctionDescriptor.of(ADDRESS, ADDRESS));
	public String dbus_message_get_path(LibDBus.Message msg) {
	    MemorySegment rv;
	    try {
		rv = (MemorySegment)dbus_message_get_path.invoke(((Message)msg).mem());
	    } catch(Throwable e) {
		throw(new RuntimeException(e));
	    }
	    return(nullp(rv) ? null : rv.reinterpret(Long.MAX_VALUE).getString(0, C_CHARSET));
	}

	private final MethodHandle dbus_message_get_interface = ld.downcallHandle(dbus.find("dbus_message_get_interface").get(), FunctionDescriptor.of(ADDRESS, ADDRESS));
	public String dbus_message_get_interface(LibDBus.Message msg) {
	    MemorySegment rv;
	    try {
		rv = (MemorySegment)dbus_message_get_interface.invoke(((Message)msg).mem());
	    } catch(Throwable e) {
		throw(new RuntimeException(e));
	    }
	    return(nullp(rv) ? null : rv.reinterpret(Long.MAX_VALUE).getString(0, C_CHARSET));
	}

	private final MethodHandle dbus_message_get_member = ld.downcallHandle(dbus.find("dbus_message_get_member").get(), FunctionDescriptor.of(ADDRESS, ADDRESS));
	public String dbus_message_get_member(LibDBus.Message msg) {
	    MemorySegment rv;
	    try {
		rv = (MemorySegment)dbus_message_get_member.invoke(((Message)msg).mem());
	    } catch(Throwable e) {
		throw(new RuntimeException(e));
	    }
	    return(nullp(rv) ? null : rv.reinterpret(Long.MAX_VALUE).getString(0, C_CHARSET));
	}

	private final MethodHandle dbus_message_iter_init = ld.downcallHandle(dbus.find("dbus_message_iter_init").get(), FunctionDescriptor.of(DBUS_BOOL_T, ADDRESS, ADDRESS));
	public void dbus_message_iter_init(LibDBus.Message message, LibDBus.MessageIter iter) {
	    int rv;
	    try {
		rv = (int)dbus_message_iter_init.invoke(((Message)message).mem(), ((MessageIter)iter).mem());
	    } catch(Throwable e) {
		throw(new RuntimeException(e));
	    }
	    if(rv == 0)
		throw(new IllegalArgumentException());
	}

	private final MethodHandle dbus_message_iter_init_closed = ld.downcallHandle(dbus.find("dbus_message_iter_init_closed").get(), FunctionDescriptor.ofVoid(ADDRESS));
	public void dbus_message_iter_init_closed(LibDBus.MessageIter iter) {
	    try {
		dbus_message_iter_init_closed.invoke(((MessageIter)iter).mem());
	    } catch(Throwable e) {
		throw(new RuntimeException(e));
	    }
	}

	private final MethodHandle dbus_message_iter_has_next = ld.downcallHandle(dbus.find("dbus_message_iter_has_next").get(), FunctionDescriptor.of(DBUS_BOOL_T, ADDRESS));
	public boolean dbus_message_iter_has_next(LibDBus.MessageIter iter) {
	    try {
		return((int)dbus_message_iter_has_next.invoke(((MessageIter)iter).mem()) != 0);
	    } catch(Throwable e) {
		throw(new RuntimeException(e));
	    }
	}

	private final MethodHandle dbus_message_iter_next = ld.downcallHandle(dbus.find("dbus_message_iter_next").get(), FunctionDescriptor.of(DBUS_BOOL_T, ADDRESS));
	public boolean dbus_message_iter_next(LibDBus.MessageIter iter) {
	    try {
		return((int)dbus_message_iter_next.invoke(((MessageIter)iter).mem()) != 0);
	    } catch(Throwable e) {
		throw(new RuntimeException(e));
	    }
	}

	private final MethodHandle dbus_message_iter_get_arg_type = ld.downcallHandle(dbus.find("dbus_message_iter_get_arg_type").get(), FunctionDescriptor.of(C_INT, ADDRESS));
	public int dbus_message_iter_get_arg_type(LibDBus.MessageIter iter) {
	    try {
		return((int)dbus_message_iter_get_arg_type.invoke(((MessageIter)iter).mem()));
	    } catch(Throwable e) {
		throw(new RuntimeException(e));
	    }
	}

	private final MethodHandle dbus_message_iter_get_element_type = ld.downcallHandle(dbus.find("dbus_message_iter_get_element_type").get(), FunctionDescriptor.of(C_INT, ADDRESS));
	public int dbus_message_iter_get_element_type(LibDBus.MessageIter iter) {
	    try {
		return((int)dbus_message_iter_get_element_type.invoke(((MessageIter)iter).mem()));
	    } catch(Throwable e) {
		throw(new RuntimeException(e));
	    }
	}

	private final MethodHandle dbus_message_iter_recurse = ld.downcallHandle(dbus.find("dbus_message_iter_recurse").get(), FunctionDescriptor.ofVoid(ADDRESS, ADDRESS));
	public void dbus_message_iter_recurse(LibDBus.MessageIter iter, LibDBus.MessageIter sub) {
	    try {
		dbus_message_iter_recurse.invoke(((MessageIter)iter).mem(), ((MessageIter)sub).mem());
	    } catch(Throwable e) {
		throw(new RuntimeException(e));
	    }
	}

	private final MethodHandle dbus_message_iter_get_signature = ld.downcallHandle(dbus.find("dbus_message_iter_get_signature").get(), FunctionDescriptor.of(ADDRESS, ADDRESS));
	public String dbus_message_iter_get_signature(LibDBus.MessageIter iter) {
	    MemorySegment rv;
	    try {
		rv = (MemorySegment)dbus_message_iter_get_signature.invoke(((MessageIter)iter).mem());
	    } catch(Throwable e) {
		throw(new RuntimeException(e));
	    }
	    String ret = rv.reinterpret(Long.MAX_VALUE).getString(0, C_CHARSET);
	    dbus_free(rv);
	    return(ret);
	}

	private final MethodHandle dbus_message_iter_get_basic = ld.downcallHandle(dbus.find("dbus_message_iter_get_basic").get(), FunctionDescriptor.ofVoid(ADDRESS, ADDRESS));
	void dbus_message_iter_get_basic(LibDBus.MessageIter iter, MemorySegment buf) {
	    try {
		dbus_message_iter_get_basic.invoke(((MessageIter)iter).mem(), buf);
	    } catch(Throwable e) {
		throw(new RuntimeException(e));
	    }
	}

	private final MethodHandle dbus_message_iter_get_element_count = ld.downcallHandle(dbus.find("dbus_message_iter_get_element_count").get(), FunctionDescriptor.of(C_INT, ADDRESS));
	public int dbus_message_iter_get_element_count(LibDBus.MessageIter iter) {
	    try {
		return((int)dbus_message_iter_get_element_count.invoke(((MessageIter)iter).mem()));
	    } catch(Throwable e) {
		throw(new RuntimeException(e));
	    }
	}

	private final MethodHandle dbus_message_iter_init_append = ld.downcallHandle(dbus.find("dbus_message_iter_init_append").get(), FunctionDescriptor.ofVoid(ADDRESS, ADDRESS));
	public void dbus_message_iter_init_append(LibDBus.Message message, LibDBus.MessageIter iter) {
	    try {
		dbus_message_iter_init_append.invoke(((Message)message).mem(), ((MessageIter)iter).mem());
	    } catch(Throwable e) {
		throw(new RuntimeException(e));
	    }
	}

	private final MethodHandle dbus_message_iter_append_basic = ld.downcallHandle(dbus.find("dbus_message_iter_append_basic").get(), FunctionDescriptor.of(DBUS_BOOL_T, ADDRESS, C_INT, ADDRESS));
	void dbus_message_iter_append_basic(LibDBus.MessageIter iter, int type, MemorySegment value) {
	    int rv;
	    try {
		rv = (int)dbus_message_iter_append_basic.invoke(((MessageIter)iter).mem(), type, value);
	    } catch(Throwable e) {
		throw(new RuntimeException(e));
	    }
	    if(rv == 0)
		throw(new RuntimeException("dbus allocation failure"));
	}

	private final MethodHandle dbus_message_iter_open_container = ld.downcallHandle(dbus.find("dbus_message_iter_open_container").get(), FunctionDescriptor.of(DBUS_BOOL_T, ADDRESS, C_INT, ADDRESS, ADDRESS));
	public void dbus_message_iter_open_container(LibDBus.MessageIter iter, int type, String contained_signature, LibDBus.MessageIter sub) {
	    int rv;
	    try(Arena st = Arena.ofConfined()) {
		rv = (int)dbus_message_iter_open_container.invoke(((MessageIter)iter).mem(), type,
								  (contained_signature == null) ? MemorySegment.NULL : st.allocateFrom(contained_signature, Utils.ascii),
								  ((MessageIter)sub).mem());
	    } catch(Throwable e) {
		throw(new RuntimeException(e));
	    }
	    if(rv == 0)
		throw(new RuntimeException("dbus allocation failure"));
	}

	private final MethodHandle dbus_message_iter_close_container = ld.downcallHandle(dbus.find("dbus_message_iter_close_container").get(), FunctionDescriptor.of(DBUS_BOOL_T, ADDRESS, ADDRESS));
	public void dbus_message_iter_close_container(LibDBus.MessageIter iter, LibDBus.MessageIter sub) {
	    int rv;
	    try {
		rv = (int)dbus_message_iter_close_container.invoke(((MessageIter)iter).mem(), ((MessageIter)sub).mem());
	    } catch(Throwable e) {
		throw(new RuntimeException(e));
	    }
	    if(rv == 0)
		throw(new RuntimeException("dbus allocation failure"));
	}
    }

    private static LibDBus instance = null;
    public static LibDBus get() {
	if(instance == null) {
	    synchronized(LibDBus.class) {
		if(instance == null) {
		    instance = new libdbus_1_so_3();
		}
	    }
	}
	return(instance);
    }
}
