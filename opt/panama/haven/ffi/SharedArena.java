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

package haven.ffi;

import haven.*;
import java.util.*;
import java.util.concurrent.atomic.*;
import java.lang.foreign.*;

/* Using actual Arena.ofShared()'s is apparently just too slow. */
public class SharedArena {
    private static final Comparator<Pair<Long, Long>> poolcmp = Comparator.<Pair<Long, Long>, Long>comparing(p -> p.a).thenComparing(Comparator.comparing(p -> p.b));
    private final NavigableMap<Pair<Long, Long>, Pool> pools = new TreeMap<>(poolcmp);
    private long vsz, asz;

    public static class Slot {
	public final long base, size;

	public Slot(long base, long size) {
	    this.base = base;
	    this.size = size;
	}

	public String toString() {
	    return(String.format("%x-%x", base, base + size));
	}
    }

    public static class Pool {
	private static final Comparator<Slot> addrcmp = (a, b) -> (a.base < b.base) ? -1 : (a.base > b.base) ? 1 : 0;
	private static final Comparator<Slot> sizecmp = (a, b) -> (a.size < b.size) ? -1 : (a.size > b.size) ? 1 : 0;
	private final NavigableSet<Slot> byaddr = new TreeSet<>(addrcmp);
	private final NavigableSet<Slot> bysize = new TreeSet<>(sizecmp.thenComparing(addrcmp));
	private final Arena arena;
	private final MemorySegment space;

	public Pool(long size) {
	    arena = Arena.ofShared();
	    space = arena.allocate(size);
	    Slot all = new Slot(0, size);
	    byaddr.add(all);
	    bysize.add(all);
	}

	public long id() {
	    return(space.address());
	}

	public long size() {
	    return(space.byteSize());
	}

	public boolean empty() {
	    synchronized(this) {
		if(byaddr.size() != 1)
		    return(false);
		return((byaddr.first().base == 0) && (byaddr.first().size == space.byteSize()));
	    }
	}

	public boolean full() {
	    synchronized(this) {
		return(byaddr.isEmpty());
	    }
	}

	public Slot malloc(long size) {
	    synchronized(this) {
		size = Math.max(size, 16);
		size = (size + 15) & ~15;
		Slot s = bysize.ceiling(new Slot(0, size));
		if(s == null)
		    return(null);
		byaddr.remove(s); bysize.remove(s);
		if(size < s.size) {
		    Slot r = new Slot(s.base + size, s.size - size);
		    byaddr.add(r);
		    bysize.add(r);
		}
		// check();
		return(new Slot(s.base, size));
	    }
	}

	public void free(Slot s) {
	    synchronized(this) {
		Slot lnb = byaddr.floor(s);
		Slot hnb = byaddr.ceiling(s);
		long base = s.base;
		long size = s.size;
		if(lnb != null) {
		    if(lnb.base + lnb.size == base) {
			byaddr.remove(lnb); bysize.remove(lnb);
			base = lnb.base;
			size = lnb.size + size;
		    } else if(lnb.base + lnb.size > base) {
			throw(new RuntimeException(String.format("(%,d-%,d)\u2229(%,d-%,d)", lnb.base, lnb.base + lnb.size, base, base + size)));
		    }
		}
		if(hnb != null) {
		    if(base + size == hnb.base) {
			byaddr.remove(hnb); bysize.remove(hnb);
			size = size + hnb.size;
		    } else if(base + size > hnb.base) {
			throw(new RuntimeException(String.format("(%,d-%,d)\u2229(%,d-%,d)", base, base + size, hnb.base, hnb.base + hnb.size)));
		    }
		}
		s = new Slot(base, size);
		byaddr.add(s); bysize.add(s);
		// check();
	    }
	}

	public void dispose() {
	    arena.close();
	}

	private void check() {
	    Slot p = null;
	    for(Slot s : byaddr) {
		if(p != null) {
		    if(s.base < p.base)
			throw(new RuntimeException("Bad chunk order: " + p + " & " + s));
		    if(p.base + p.size == s.base) {
			throw(new RuntimeException("Unmerged chunks: " + p + " & " + s));
		    } else if(p.base + p.size > s.base) {
			throw(new RuntimeException("Overlapping chunks: " + p + " & " + s));
		    }
		}
		p = s;
	    }
	}

	public String toString() {
	    return(String.format("#<pool %x B @ %x>", size(), id()));
	}
    }

    public class Block implements Disposable {
	private final Pool pool;
	private final Slot slot;
	private final MemorySegment mem;
	private boolean freed = false;

	private Block(Pool pool, Slot slot) {
	    this.pool = pool;
	    this.slot = slot;
	    this.mem = pool.space.asSlice(slot.base, slot.size, 16);
	    asz += slot.size;
	}

	public MemorySegment mem() {return(mem);}
	public long size() {return(slot.size);}

	public void dispose() {
	    synchronized(SharedArena.this) {
		if(freed)
		    throw(new IllegalStateException());
		freed = true;
		pool.free(slot);
		if(pool.empty()) {
		    if(pools.remove(Pair.of(pool.size(), pool.id())) != pool)
			throw(new AssertionError());
		    pool.dispose();
		    vsz -= pool.size();
		}
		asz -= slot.size;
	    }
	}

	public String toString() {
	    return(slot.toString());
	}
    }

    public Block malloc(long size) {
	if(size < 0)
	    throw(new IllegalArgumentException(Long.toString(size)));
	synchronized(this) {
	    for(Pool p : pools.tailMap(Pair.of(size, 0l)).values()) {
		Slot s = p.malloc(size);
		if(s != null)
		    return(new Block(p, s));
	    }
	    Pool p = new Pool(Math.max(1 << 20, Long.highestOneBit(size) << 2));
	    vsz += p.size();
	    pools.put(Pair.of(p.size(), p.id()), p);
	    return(new Block(p, p.malloc(size)));
	}
    }

    public long vsz() {return(vsz);}
    public long asz() {return(asz);}

    private static SharedArena instance = new SharedArena();
    public static SharedArena get() {
	return(instance);
    }
}
