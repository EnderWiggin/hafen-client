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

import java.util.*;
import java.util.function.*;
import haven.*;
import haven.ffi.posix.*;
import haven.ffi.dbus.LibDBus.Connection;
import haven.ffi.dbus.LibDBus.Message;
import haven.ffi.dbus.LibDBus.Error;
import static haven.ffi.dbus.LibDBus.*;

public class DBus {
    public final String name;
    private final Connection conn;
    private final LibC libc = LibC.get();
    private final LibDBus lib = LibDBus.get();
    private final Queue<Runnable> tasks = new LinkedList<>();
    private final Collection<Pending> pending = new ArrayList<>();
    private final Collection<Listener> listening = new ArrayList<>();
    private Thread thread = null;
    private final Object sighandler;

    public DBus(Connection conn) {
	this.conn = conn;
	this.name = lib.dbus_bus_get_unique_name(conn);
	this.sighandler = lib.dbus_connection_add_filter(conn, this::handlesignal);
    }

    private final FileDescriptor rwake, wwake; {
	int[] pipe = libc.pipe();
	rwake = FileDescriptor.of(pipe[0]);
	wwake = FileDescriptor.of(pipe[1]);
    }

    public static class Signal {
	public final String sender, path, iface, member;
	public final List<Object> args;

	public Signal(String sender, String path, String iface, String member, List<Object> args) {
	    this.sender = sender;
	    this.path = path;
	    this.iface = iface;
	    this.member = member;
	    this.args = args;
	}
    }

    public class Listener {
	public final String sender, path, iface, member;
	public final Predicate<Signal> cb;
	private final String rule;
	private boolean removed = false;

	private Listener(String sender, String path, String iface, String member, Predicate<Signal> cb) {
	    StringBuilder rbuf = new StringBuilder();
	    rbuf.append("type='signal'");
	    if(sender != null) rbuf.append(",sender='" + sender + "'");
	    if(path != null) rbuf.append(",path='" + path + "'");
	    if(iface != null) rbuf.append(",interface='" + iface + "'");
	    if(member != null) rbuf.append(",member='" + member + "'");
	    this.rule = rbuf.toString();
	    this.sender = sender;
	    this.path = path;
	    this.iface = iface;
	    this.member = member;
	    this.cb = cb;
	    dtrun(() -> {
		Error err = lib.Error();
		lib.dbus_bus_add_match(conn, rule, err);
		if(lib.dbus_error_is_set(err))
		    throw(new DBusError(err.name(), err.message()));
		listening.add(this);
	    });
	}

	private void unregister() {
	    Error err = lib.Error();
	    lib.dbus_bus_remove_match(conn, rule, err);
	    if(lib.dbus_error_is_set(err))
		throw(new DBusError(err.name(), err.message()));
	}

	boolean check(Signal sig) {
	    if(((sender == null) || Utils.eq(sender, sig.sender)) && ((path == null) || Utils.eq(path, sig.path)) &&
	       ((iface == null) || Utils.eq(iface, sig.iface)) && ((member == null) || Utils.eq(member, sig.member)))
	    {
		if(!cb.test(sig)) {
		    if(!removed) {
			unregister();
			removed = true;
		    }
		    return(false);
		}
	    }
	    return(true);
	}

	public void remove() {
	    dtrun(() -> {
		if(!removed) {
		    listening.remove(this);
		    unregister();
		    removed = true;
		}
	    });
	}
    }

    public static class Pending {
	public final PendingCall call;
	public final Consumer<Message> cb;

	public Pending(PendingCall call, Consumer<Message> cb) {
	    this.call = call;
	    this.cb = cb;
	}
    }

    private boolean handlesignal(Message msg) {
	if(lib.dbus_message_get_type(msg) == DBUS_MESSAGE_TYPE_SIGNAL) {
	    Signal sig = new Signal(lib.dbus_message_get_sender(msg),
				    lib.dbus_message_get_path(msg),
				    lib.dbus_message_get_interface(msg),
				    lib.dbus_message_get_member(msg),
				    decodemsg(msg));
	    synchronized(this) {
		for(Iterator<Listener> i = listening.iterator(); i.hasNext();) {
		    Listener l = i.next();
		    if(!l.check(sig))
			i.remove();
		}
	    }
	}
	return(false);
    }

    private void run() {	
	try {
	    LibC.PollFD pfd = libc.pollfd(2);
	    double now = Utils.rtime(), last = now;
	    while(true) {
		while(lib.dbus_connection_dispatch(conn));
		List<Runnable> tasks;
		synchronized(this) {
		    tasks = new ArrayList<>(this.tasks);
		    this.tasks.clear();
		    for(Iterator<Pending> i = pending.iterator(); i.hasNext();) {
			Pending pend = i.next();
			if(lib.dbus_pending_call_get_completed(pend.call)) {
			    Message reply = lib.dbus_pending_call_steal_reply(pend.call);
			    tasks.add(() -> pend.cb.accept(reply));
			    i.remove();
			}
		    }
		}
		for(Runnable task : tasks)
		    task.run();
		double timeout;
		synchronized(this) {
		    if(!pending.isEmpty() || !listening.isEmpty()) {
			timeout = -1;
		    } else {
			if(now - last > 5) {
			    thread = null;
			    return;
			}
			timeout = last + 5 - now;
		    }
		}
		int ev = LibC.POLLIN | (lib.dbus_connection_has_messages_to_send(conn) ? LibC.POLLOUT : 0);
		pfd.fd(0, lib.dbus_connection_get_unix_fd(conn)).events(0, ev).revents(0, 0);
		pfd.fd(1, rwake.fileno).events(1, LibC.POLLIN).revents(1, 0);
		try {
		    libc.poll(pfd, 2, (timeout < 0) ? -1 : (int)Math.round(timeout * 1000));
		} catch(StdError e) {
		    if(e.errno == LibC.EINTR)
			continue;
		    throw(e);
		}
		now = Utils.rtime();
		if(pfd.events(0) != 0) {
		    if(!lib.dbus_connection_read_write(conn, 0))
			return;
		}
		if((pfd.revents(1) & LibC.POLLIN) != 0)
		    libc.read(rwake.fileno, 1024);
	    }
	} finally {
	    synchronized(this) {
		if(thread == Thread.currentThread())
		    thread = null;
	    }
	}
    }

    private void ckthread() {
	if(thread == null) {
	    thread = new HackThread(this::run, "DBus dispatch thread");
	    thread.setDaemon(true);
	    thread.start();
	}
    }

    void dtrun(Runnable task) {
	synchronized(this) {
	    tasks.add(task);
	    ckthread();
	}
	while(libc.write(wwake.fileno, new byte[] {0}) == 0);
    }

    public static class Type {
	public static final Type BYTE    = new Basic(DBUS_TYPE_BYTE);
	public static final Type BOOLEAN = new Basic(DBUS_TYPE_BOOLEAN);
	public static final Type SINT16  = new Basic(DBUS_TYPE_INT16);
	public static final Type UINT16  = new Basic(DBUS_TYPE_UINT16);
	public static final Type SINT32  = new Basic(DBUS_TYPE_INT32);
	public static final Type UINT32  = new Basic(DBUS_TYPE_UINT32);
	public static final Type SINT64  = new Basic(DBUS_TYPE_INT64);
	public static final Type UINT64  = new Basic(DBUS_TYPE_UINT64);
	public static final Type DOUBLE  = new Basic(DBUS_TYPE_DOUBLE);
	public static final Type STRING  = new Basic(DBUS_TYPE_STRING);
	public static final Type PATH    = new Basic(DBUS_TYPE_OBJECT_PATH);
	public static final Type SIG     = new Basic(DBUS_TYPE_SIGNATURE);
	public static final Type UNIXFD  = new Basic(DBUS_TYPE_UNIX_FD);
	public static final Type VARIANT = new Type() {
	    public String toString() {return(Character.toString(DBUS_TYPE_VARIANT));}
	};

	public static class Basic extends Type {
	    public final int code;

	    private Basic(int code) {
		this.code = code;
	    }

	    public String toString() {
		return(Character.toString(code));
	    }
	}

	public static class Array extends Type {
	    public final Type of;

	    public Array(Type of) {
		this.of = of;
	    }

	    public String toString() {
		return("a" + of);
	    }
	}

	public static class Struct extends Type {
	    public final Type[] of;

	    public Struct(Type... of) {
		this.of = of;
	    }

	    public String toString() {
		StringBuilder buf = new StringBuilder();
		buf.append('(');
		for(Type el : of)
		    buf.append(el.toString());
		buf.append(')');
		return(buf.toString());
	    }
	}

	public static class Mapping extends Type {
	    public final Type key, val;

	    public Mapping(Type key, Type val) {
		if(!(key instanceof Basic))
		    throw(new IllegalArgumentException(String.valueOf(key)));
		this.key = key;
		this.val = val;
	    }

	    public String toString() {
		return("{" + key + val + "}");
	    }
	}
    }

    public static class Variant {
	public final Type type;
	public final Object value;

	public Variant(Type type, Object value) {
	    this.type = type;
	    this.value = value;
	}

	public static Variant of(byte v) {return(new Variant(Type.BYTE, v));}
	public static Variant of(boolean v) {return(new Variant(Type.BOOLEAN, v));}
	public static Variant of(short v, boolean signed) {return(new Variant(signed ? Type.SINT16 : Type.UINT16, v));}
	public static Variant of(int v,   boolean signed) {return(new Variant(signed ? Type.SINT32 : Type.UINT32, v));}
	public static Variant of(long v,  boolean signed) {return(new Variant(signed ? Type.SINT64 : Type.UINT64, v));}
	public static Variant of(String v) {return(new Variant(Type.STRING, v));}
	public static Variant of(Variant v) {return(new Variant(Type.VARIANT, v));}

	public static Variant array(Type elt, List<?> v) {return(new Variant(new Type.Array(elt), v));}
	public static Variant array(Type elt, Object... v) {return(new Variant(new Type.Array(elt), v));}
	public static Variant struct(Type.Struct t, Object... v) {return(new Variant(t, v));}

	public String toString() {
	    return("v(" + type + ", " + value + ")");
	}
    }

    public static class ArrayValue extends ArrayList<Object> {
	public ArrayValue(Collection<Object> from) {
	    super(from);
	}

	public String toString() {
	    StringBuilder buf = new StringBuilder();
	    buf.append('[');
	    boolean f = true;
	    for(Object val : this) {
		if(!f)
		    buf.append(", ");
		buf.append(String.valueOf(val));
		f = false;
	    }
	    buf.append(']');
	    return(buf.toString());
	}
    }

    public static class DictValue extends HashMap<Object, Object> {
	public DictValue(Collection<Object> from) {
	    for(Object obj : from)
		put(((Entry)obj).getKey(), ((Entry)obj).getValue());
	}

	public String toString() {
	    StringBuilder buf = new StringBuilder();
	    buf.append('{');
	    boolean f = true;
	    for(Map.Entry ent : this.entrySet()) {
		if(!f)
		    buf.append(", ");
		buf.append(String.valueOf(ent.getKey()));
		buf.append(String.valueOf(": "));
		buf.append(String.valueOf(ent.getValue()));
		f = false;
	    }
	    buf.append('}');
	    return(buf.toString());
	}
    }

    public static class StructValue extends ArrayList<Object> {
	public StructValue(Collection<Object> from) {
	    super(from);
	}

	public String toString() {
	    StringBuilder buf = new StringBuilder();
	    buf.append('(');
	    boolean f = true;
	    for(Object val : this) {
		if(!f)
		    buf.append(", ");
		buf.append(String.valueOf(val));
		f = false;
	    }
	    buf.append(')');
	    return(buf.toString());
	}
    }

    public static class Entry extends AbstractMap.SimpleEntry<Object, Object> {
	public Entry(Object key, Object value) {
	    super(key, value);
	}
    }

    private MessageIter recurse(MessageIter iter) {
	MessageIter sub = lib.MessageIter();
	lib.dbus_message_iter_recurse(iter, sub);
	return(sub);
    }

    private Object decode(MessageIter iter) {
	switch(lib.dbus_message_iter_get_arg_type(iter)) {
	case DBUS_TYPE_BYTE:    return(lib.dbus_message_iter_get_byte(iter));
	case DBUS_TYPE_BOOLEAN: return(lib.dbus_message_iter_get_boolean(iter));
	case DBUS_TYPE_INT16:   return(lib.dbus_message_iter_get_short(iter));
	case DBUS_TYPE_UINT16:  return(lib.dbus_message_iter_get_short(iter));
	case DBUS_TYPE_INT32:   return(lib.dbus_message_iter_get_int(iter));
	case DBUS_TYPE_UINT32:  return(lib.dbus_message_iter_get_int(iter));
	case DBUS_TYPE_INT64:   return(lib.dbus_message_iter_get_long(iter));
	case DBUS_TYPE_UINT64:  return(lib.dbus_message_iter_get_long(iter));
	case DBUS_TYPE_DOUBLE:  return(lib.dbus_message_iter_get_double(iter));
	case DBUS_TYPE_STRING: case DBUS_TYPE_OBJECT_PATH: case DBUS_TYPE_SIGNATURE:
	    return(lib.dbus_message_iter_get_string(iter));
	case DBUS_TYPE_ARRAY: {
	    List<Object> sub = decodeall(recurse(iter));
	    if(lib.dbus_message_iter_get_element_type(iter) == DBUS_TYPE_DICT_ENTRY)
		return(new DictValue(sub));
	    return(new ArrayValue(sub));
	}
	case DBUS_TYPE_STRUCT:
	    return(new StructValue(decodeall(recurse(iter))));
	case DBUS_TYPE_DICT_ENTRY: {
	    List<Object> sub = decodeall(recurse(iter));
	    return(new Entry(sub.get(0), sub.get(1)));
	}
	case DBUS_TYPE_VARIANT: {
	    List<Object> sub = decodeall(recurse(iter));
	    return(new Variant(null, sub.get(0)));
	}
	case DBUS_TYPE_UNIX_FD:
	    return(FileDescriptor.of(lib.dbus_message_iter_get_fd(iter)));
	default:
	    throw(new IllegalArgumentException(Integer.toString(lib.dbus_message_iter_get_arg_type(iter))));
	}
    }

    private List<Object> decodeall(MessageIter iter) {
	List<Object> ret = new ArrayList<>();
	while(lib.dbus_message_iter_get_arg_type(iter) != DBUS_TYPE_INVALID) {
	    ret.add(decode(iter));
	    lib.dbus_message_iter_next(iter);
	}
	return(ret);
    }

    private List<Object> decodemsg(Message msg) {
	MessageIter iter = lib.MessageIter();
	try {
	    lib.dbus_message_iter_init(msg, iter);
	} catch(IllegalArgumentException e) {
	    return(Collections.emptyList());
	}
	return(new StructValue(decodeall(iter)));
    }

    public class Container implements AutoCloseable {
	public final MessageIter parent, iter;

	public Container(MessageIter parent, int type, Type sig) {
	    this.parent = parent;
	    this.iter = lib.MessageIter();
	    lib.dbus_message_iter_open_container(parent, type, (sig == null) ? null : sig.toString(), this.iter);
	}

	public void close() {
	    lib.dbus_message_iter_close_container(parent, iter);
	}
    }

    private void append(MessageIter iter, Type type, Object val) {
	if(type instanceof Type.Basic) {
	    switch(((Type.Basic)type).code) {
	    case DBUS_TYPE_BYTE:    lib.dbus_message_iter_append_basic(iter, ((Number)val).byteValue());         break;
	    case DBUS_TYPE_BOOLEAN: lib.dbus_message_iter_append_basic(iter, (boolean)val);                      break;
	    case DBUS_TYPE_INT16:   lib.dbus_message_iter_append_basic(iter, ((Number)val).shortValue(), true);  break;
	    case DBUS_TYPE_UINT16:  lib.dbus_message_iter_append_basic(iter, ((Number)val).shortValue(), false); break;
	    case DBUS_TYPE_INT32:   lib.dbus_message_iter_append_basic(iter, ((Number)val).intValue(),   true);  break;
	    case DBUS_TYPE_UINT32:  lib.dbus_message_iter_append_basic(iter, ((Number)val).intValue(),   false); break;
	    case DBUS_TYPE_INT64:   lib.dbus_message_iter_append_basic(iter, ((Number)val).longValue(),  true);  break;
	    case DBUS_TYPE_UINT64:  lib.dbus_message_iter_append_basic(iter, ((Number)val).longValue(),  false); break;
	    case DBUS_TYPE_DOUBLE:  lib.dbus_message_iter_append_basic(iter, ((Number)val).doubleValue());       break;
	    case DBUS_TYPE_STRING:  lib.dbus_message_iter_append_basic(iter, (String)val);                       break;
	    default: throw(new IllegalArgumentException(type.toString()));
	    }
	} else if(type instanceof Type.Array) {
	    Type.Array ary = (Type.Array)type;
	    try(Container sub = new Container(iter, DBUS_TYPE_ARRAY, ary.of)) {
		Iterable<?> args;
		if(val instanceof Object[])
		    args = Arrays.asList((Object[])val);
		else if(val instanceof Map)
		    args = ((Map)val).entrySet();
		else
		    args = (Iterable<?>)val;
		for(Object arg : args)
		    append(sub.iter, ary.of, arg);
	    }
	} else if(type instanceof Type.Struct) {
	    Type.Struct st = (Type.Struct)type;
	    try(Container sub = new Container(iter, DBUS_TYPE_STRUCT, null)) {
		List<?> args;
		if(val instanceof Object[])
		    args = Arrays.asList((Object[])val);
		else
		    args = (List<?>)val;
		if(args.size() != st.of.length)
		    throw(new IllegalArgumentException(String.valueOf(args)));
		for(int i = 0; i < st.of.length; i++)
		    append(sub.iter, st.of[i], args.get(i));
	    }
	} else if(type instanceof Type.Mapping) {
	    Type.Mapping mpt = (Type.Mapping)type;
	    try(Container sub = new Container(iter, DBUS_TYPE_DICT_ENTRY, null)) {
		List<?> args;
		if(val instanceof Object[])
		    args = Arrays.asList((Object[])val);
		else if(val instanceof Map.Entry)
		    args = Arrays.asList(((Map.Entry)val).getKey(), ((Map.Entry)val).getValue());
		else
		    args = (List<?>)val;
		if(args.size() != 2)
		    throw(new IllegalArgumentException(String.valueOf(args)));
		append(sub.iter, mpt.key, args.get(0));
		append(sub.iter, mpt.val, args.get(1));
	    }
	} else if(type == Type.VARIANT) {
	    Variant var = (Variant)val;
	    try(Container sub = new Container(iter, DBUS_TYPE_VARIANT, var.type)) {
		append(sub.iter, var.type, var.value);
	    }
	} else {
	    throw(new IllegalArgumentException(String.valueOf(type)));
	}
    }

    private void append(MessageIter iter, List<Type> sig, Object... values) {
	if(sig.size() != values.length)
	    throw(new IllegalArgumentException());
	for(int i = 0; i < sig.size(); i++)
	    append(iter, sig.get(i), values[i]);
    }

    public Promise<List<Object>> call(List<Type> sig, String dest, String path, String iface, String name, Object... args) {
	Promise<List<Object>> ret  = new Promise<>();
	Message msg = lib.dbus_message_new_method_call(dest, path, iface, name);
	MessageIter root = lib.MessageIter();
	lib.dbus_message_iter_init_append(msg, root);
	append(root, sig, args);
	dtrun(() -> {
	    PendingCall call = lib.dbus_connection_send_with_reply(conn, msg, -1);
	    synchronized(this) {
		pending.add(new Pending(call, reply -> {
		    int typ = lib.dbus_message_get_type(reply);
		    try {
			if(typ == DBUS_MESSAGE_TYPE_METHOD_RETURN) {
			    ret.resolve(decodemsg(reply));
			} else if(typ == DBUS_MESSAGE_TYPE_ERROR) {
			    String descr = (String)decodemsg(reply).get(0);
			    throw(new DBusError(lib.dbus_message_get_error_name(reply), descr));
			} else {
			    throw(new RuntimeException("unexpected dbus reply type: " + typ));
			}
		    } catch(Throwable t) {
			ret.reject(t);
		    }
		}));
	    }
	});
	return(ret);
    }

    public Listener listen(String sender, String path, String iface, String member, Predicate<Signal> cb) {
	return(new Listener(sender, path, iface, member, cb));
    }

    public Promise<List<Object>> signal(String sender, String path, String iface, String member) {
	Promise<List<Object>> ret = new Promise<>();
	listen(sender, path, iface, member, sig -> {ret.resolve(sig.args); return(false);});
	return(ret);
    }

    public static enum Std {
	SYSTEM(DBUS_BUS_SYSTEM), SESSION(DBUS_BUS_SESSION);

	public final int dbustype;

	private Std(int dbustype) {
	    this.dbustype = dbustype;
	}
    }

    private static final Map<Std, DBus> std = new EnumMap<>(Std.class);
    public static DBus get(Std type) {
	synchronized(std) {
	    DBus ret = std.get(type);
	    if(ret == null) {
		LibDBus lib = LibDBus.get();
		Error err = lib.Error();
		Connection conn = lib.dbus_bus_get(type.dbustype, err);
		if(lib.dbus_error_is_set(err))
		    throw(new DBusError(err.name(), err.message()));
		std.put(type, ret = new DBus(conn));
	    }
	    return(ret);
	}
    }
}
