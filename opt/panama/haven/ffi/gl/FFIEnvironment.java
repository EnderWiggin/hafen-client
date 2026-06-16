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

package haven.ffi.gl;

import haven.*;
import haven.ffi.*;
import haven.render.gl.*;
import java.nio.*;
import java.lang.foreign.*;

public class FFIEnvironment extends GLEnvironment {
    public FFIEnvironment(OpenGL gl, Area wnd) {
	super(gl, wnd);
    }

    public static class FFIBuffer extends GLObject implements SysBuffer {
	public static final boolean LEAK_CHECK = false;
	private final SharedArena.Block chunk;
	private final ByteBuffer buf;
	private final Cleanup clean;
	private boolean deleted = false;

	private static class Cleanup implements Finalizer.Cleaner, Disposable {
	    private final Throwable init = LEAK_CHECK ? new Throwable() : null;
	    private final SharedArena.Block chunk;
	    private final Runnable fin;
	    private boolean clean;

	    Cleanup(FFIBuffer ob) {
		this.chunk = ob.chunk;
		fin = Finalizer.finalize(ob, this);
	    }

	    public void clean() {
		if(!clean)
		    new Warning(init , "Native buffer leaked (" + chunk.size() + " bytes)").issue();
		chunk.dispose();
	    }

	    public void dispose() {
		clean = true;
		fin.run();
	    }
	}

	public FFIBuffer(FFIEnvironment env, int sz) {
	    super(env);
	    chunk = SharedArena.get().malloc(sz);
	    buf = chunk.mem().asByteBuffer().order(ByteOrder.nativeOrder());
	    clean = new Cleanup(this);
	}

	public ByteBuffer data() {
	    if(deleted)
		throw(new IllegalStateException("already disposed"));
	    return(buf);
	}

	public void create(GL gl) {throw(new UnsupportedOperationException());}

	protected void delete(GL gl) {
	    if(deleted)
		throw(new IllegalStateException("already disposed"));
	    deleted = true;
	    clean.dispose();
	}
    }

    public static class HeapBuffer implements SysBuffer {
	public final ByteBuffer data;

	public HeapBuffer(int sz) {
	    this.data = ByteBuffer.allocate(sz).order(ByteOrder.nativeOrder());
	}

	public HeapBuffer(ByteBuffer data) {
	    this.data = data;
	}

	public ByteBuffer data() {return(data);}
	public void dispose() {}
    }

    public static class FFICaps extends Caps {
	public final boolean coreprof;

	public FFICaps(GL gl, FFIEnvironment env) {
	    super(gl);
	    if((major > 3) || ((major == 3) && (minor >= 2)))
		this.coreprof = glgeti(gl, GL.GL_CONTEXT_PROFILE_MASK) == GL.GL_CONTEXT_CORE_PROFILE_BIT;
	    else
		this.coreprof = false;
	}

	public void checkreq() {
	    super.checkreq();
	    if(!coreprof || ((major < 3) || ((major == 3) && (minor < 2))))
		throw(new HardwareException("Graphics context is not a core OpenGL profile.", this));
	}
    }

    public Caps mkcaps(GL initgl) {
	return(new FFICaps(initgl, this));
    }

    public SysBuffer malloc(int sz) {
	return(new HeapBuffer(sz));
    }

    public SysBuffer subsume(ByteBuffer data, int sz) {
	if(data.remaining() < sz) {
	    String msg = data.remaining() + " < " + sz;
	    throw(new BufferUnderflowException() {
		    public String getMessage() {return(msg);}
		});
	}
	SysBuffer ret = new HeapBuffer(data.duplicate());
	data.position(data.position() + sz);
	return(ret);
    }
}
