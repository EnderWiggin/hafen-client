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

package haven;

import java.util.*;
import java.util.function.*;

public class Timeout {
    private final Queue<Future<?>> scheduled = new BinHeap<>(Comparator.comparing(f -> f.time));
    private final ThreadGroup tg;
    private Runner runner;

    private Timeout(ThreadGroup tg) {
	this.tg = tg;
    }

    public static class NotYetException extends Loading {
	public final transient Future<?> future;

	public NotYetException(Future<?> future) {
	    this.future = future;
	}

	public void waitfor(Runnable callback, Consumer<Waitable.Waiting> reg) {
	    synchronized(future) {
		if(future.done()) {
		    reg.accept(Waitable.Waiting.dummy);
		    callback.run();
		} else {
		    reg.accept(new Waitable.Checker(callback) {
			    protected Object monitor() {return(future);}
			    protected boolean check() {return(future.done());}
			    protected Waitable.Waiting add() {return(future.wq.add(this));}
			}.addi());
		}
	    }
	}
    }

    public class Future<T> implements haven.Future<T> {
	public final double time;
	public final Supplier<T> task;
	private final Waitable.Queue wq = new Waitable.Queue();
	private T val;
	private String st = "";

	private Future(double time, Supplier<T> task) {
	    this.time = time;
	    this.task = task;
	}

	private void run() {
	    T val = task.get();
	    synchronized(this) {
		if(this.st == "") {
		    this.val = val;
		    this.st = "done";
		    wq.wnotify();
		}
	    }
	}

	public T get() {
	    synchronized(this) {
		if(st == "done")
		    return(val);
		else if(st == "")
		    throw(new NotYetException(this));
		else
		    throw(new IllegalStateException(st));
	    }
	}

	public boolean done() {
	    synchronized(this) {
		return(st == "done");
	    }
	}

	public void cancel() {
	    synchronized(scheduled) {
		scheduled.remove(this);
	    }
	    synchronized(this) {
		if(st != "done") {
		    st = "cancelled";
		    wq.wnotify();
		}
	    }
	}
    }

    public class Runner extends HackThread {
	private Runner() {
	    super(tg, null, "Timer scheduler");
	    setDaemon(true);
	}

	public void run() {
	    try {
		double now = Utils.rtime(), emptied = 0;
		while(true) {
		    double next;
		    synchronized(scheduled) {
			if(scheduled.isEmpty()) {
			    if(emptied == 0)
				emptied = now;
			    if(now > (next = emptied + 5)) {
				runner = null;
				break;
			    }
			} else {
			    emptied = 0;
			    next = scheduled.element().time;
			}
			if(next > now)
			    scheduled.wait(Math.max((long)Math.ceil((next - now) * 1000), 0));
			now = Utils.rtime();
		    }
		    while(true) {
			Future<?> t;
			synchronized(scheduled) {
			    t = scheduled.peek();
			    if((t == null) || (t.time > now))
				break;
			    scheduled.remove(t);
			}
			t.run();
		    }
		}
	    } catch(InterruptedException e) {
	    }
	}

	public Timeout pool() {return(Timeout.this);}
    }

    public <T> Future<T> defer(double time, Supplier<T> task) {
	Future<T> f = new Future<T>(time, task);
	synchronized(scheduled) {
	    scheduled.add(f);
	    scheduled.notifyAll();
	    if(runner == null) {
		runner = new Runner();
		runner.start();
	    }
	}
	return(f);
    }

    private static final Map<ThreadGroup, Timeout> groups = new WeakHashMap<ThreadGroup, Timeout>();
    private static Timeout getgroup() {
	if(Thread.currentThread() instanceof Runner)
	    return(((Runner)Thread.currentThread()).pool());
	ThreadGroup tg = Thread.currentThread().getThreadGroup();
	Timeout p;
	synchronized(groups) {
	    if((p = groups.get(tg)) == null)
		groups.put(tg, p = new Timeout(tg));
	}
	return(p);
    }

    public static <T> Future<T> later(double time, Supplier<T> task) {
	return(getgroup().defer(time, task));
    }

    public static <T> Future<T> later(double time, Runnable task, T result) {
	return(later(time, () -> {
	    task.run();
	    return(result);
	}));
    }
}
