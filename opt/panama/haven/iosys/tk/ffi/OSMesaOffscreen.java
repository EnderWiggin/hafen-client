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

package haven.iosys.tk.ffi;

import haven.*;
import haven.iosys.*;
import haven.iosys.tk.*;
import haven.render.*;
import haven.render.gl.*;
import haven.ffi.gl.*;
import java.nio.*;
import java.util.function.*;
import haven.ffi.gl.OSMesa.*;

@Acephal.Available(name = "osmesa")
public class OSMesaOffscreen implements Acephal {
    private static final int[][] glversions = {
	{4, 6}, {4, 5}, {4, 4}, {4, 3}, {4, 2}, {4, 1}, {4, 0},
    };
    private static final ByteBuffer backbuffer = ByteBuffer.allocateDirect(4);
    private final OSMesa osmesa;
    private final OSMesaContext ctx;
    private final OpenGL gl;
    private final FFIEnvironment env;
    private final Thread processor;
    private boolean disposed = false;

    public OSMesaOffscreen() {
	try {
	    osmesa = OSMesa.get();
	} catch(Exception e) {
	    throw(new Unavailable("OSMesa libraries not available", e));
	}
	OSMesaContext ctx = null;
	for(int[] ver : glversions) {
	    ctx = osmesa.OSMesaCreateContextAttribs(new int[] {
		    OSMesa.OSMESA_FORMAT, OSMesa.OSMESA_RGBA,
		    OSMesa.OSMESA_PROFILE, OSMesa.OSMESA_CORE_PROFILE,
		    OSMesa.OSMESA_CONTEXT_MAJOR_VERSION, ver[0],
		    OSMesa.OSMESA_CONTEXT_MINOR_VERSION, ver[1],
		}, null);
	}
	if(ctx == null)
	    throw(new Unavailable("no OSMesa context available"));
	this.ctx = ctx;
	this.gl = osmesa.gl();
	this.env = glrun(() -> new FFIEnvironment(gl, Area.sized(Coord.of(1, 1))));
	this.processor = new HackThread(this::loop, "OSMesa GL processor");
	this.processor.setDaemon(true);
	this.processor.start();
    }

    private <T> T glrun(Supplier<T> task) {
	if(!osmesa.OSMesaMakeCurrent(ctx, backbuffer, GL.GL_UNSIGNED_BYTE, 1, 1))
	    throw(new RuntimeException("could not make context current"));
	try {
	    return(task.get());
	} finally {
	    osmesa.OSMesaMakeCurrent(null, null, GL.GL_UNSIGNED_BYTE, 0, 0);
	}
    }

    private Object process() {
	env.process(gl);
	try {
	    env.finish(gl);
	} catch(InterruptedException e) {
	    Thread.currentThread().interrupt();
	    throw(new RuntimeException(e));
	}
	return(null);
    }

    private void loop() {
	try {
	    while(true) {
		env.submitwait();
		glrun(this::process);
	    }
	} catch(InterruptedException e) {
	}
    }

    public Environment env() {
	return(env);
    }

    public void dispose() {
	if(!disposed) {
	    disposed = true;
	    if(processor != null)
		processor.interrupt();
	    osmesa.OSMesaDestroyContext(ctx);
	}
    }

    private static Factory factory = new Factory() {
	public Acephal open(String... args) {
	    return(new OSMesaOffscreen());
	}

	public int priority() {return(-5);}
    };
    public static Factory get() {
	return(factory);
    }
}
