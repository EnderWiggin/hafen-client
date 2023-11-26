package me.ender;

import haven.Debug;
import haven.Message;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

public class IDPool {
    private final long min;
    private final long max;
    private long next;
    private final Set<Long> sparse = new HashSet<>();
    
    public IDPool(final long min, final long max) {
	this.min = min;
	this.max = max;
	next = min;
    }
    
    public IDPool(final Message fp) {
	this.min = fp.int64();
	this.max = fp.int64();
	this.next = fp.int64();
	final int len = fp.int32();
	for (int i = 0; i < len; ++i) {
	    sparse.add(fp.int64());
	}
    }
    
    public long next() {
	if(sparse.isEmpty()) {
	    if(next <= max) {
		return next++;
	    } else {
		throw new RuntimeException("Ran out of IDs in IDPool");
	    }
	} else {
	    Iterator<Long> itr = sparse.iterator();
	    final long id = itr.next();
	    itr.remove();
	    return id;
	}
    }
    
    public void claim(final long id) {
	if(id == next) {
	    next++;
	} else if(id > next) {
	    for (long missed = next; missed < id; ++missed) {
		sparse.add(missed);
	    }
	    next = id + 1;
	} else if(sparse.contains(id)) {
	    sparse.remove(id);
	} else {
	    Debug.log.println("Attempted to claim an ID already claimed");
	}
    }
    
    public void release(final long id) {
	if(id == next - 1) {
	    next--;
	} else {
	    sparse.add(id);
	}
    }
    
    public void save(final Message fp) {
	fp.addint64(min);
	fp.addint64(max);
	fp.addint64(next);
	fp.addint32(sparse.size());
	for (final Long id : sparse) {
	    fp.addint64(id);
	}
    }
}
