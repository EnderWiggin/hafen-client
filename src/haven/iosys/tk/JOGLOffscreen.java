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

package haven.iosys.tk;

import haven.*;
import haven.iosys.*;
import haven.render.*;
import haven.render.gl.*;
import haven.render.jogl.*;
import com.jogamp.opengl.*;
import haven.render.gl.GL;

@Acephal.Available(name = "jogl")
public class JOGLOffscreen implements Acephal {
    public final GLProfile prof;
    public final GLAutoDrawable buf;
    private Thread processor = null;
    private JOGLEnvironment benv = null;
    private final Environment penv;

    public JOGLOffscreen() {
	try {
	    prof = GLProfile.getMaxProgrammableCore(true);
	} catch(Throwable e) {
	    throw(new Unavailable("could not initialize JOGL", e));
	}
	GLDrawableFactory df = GLDrawableFactory.getFactory(prof);
	this.buf = df.createOffscreenAutoDrawable(null, caps(prof), null, 1, 1);
	buf.addGLEventListener(new GLEventListener() {
		public void display(GLAutoDrawable d) {
		    process(d.getGL().getGL3());
		}

		public void init(GLAutoDrawable d) {
		}

		public void reshape(GLAutoDrawable d, int x, int y, int w, int h) {
		}

		public void dispose(GLAutoDrawable d) {
		}
	    });
	try {
	    buf.display();
	} catch(com.jogamp.opengl.GLException e) {
	    throw(new Unavailable("could not initialize JOGL", e));
	}
	if(benv == null)
	    throw(new AssertionError("offscreen display call was not honored"));
	penv = new ProxyEnv();
    }

    private class ProxyEnv extends Environment.Proxy {
	public Environment back() {return(benv);}

	public void submit(Render r) {
	    super.submit(r);
	    ckproc();
	}
    }

    protected GLCapabilities caps(GLProfile prof) {
	GLCapabilities ret = new GLCapabilities(prof);
	ret.setDoubleBuffered(true);
	ret.setAlphaBits(8);
	ret.setRedBits(8);
	ret.setGreenBits(8);
	ret.setBlueBits(8);
	return(ret);
    }

    private void process(GL3 gl) {
	// gl = new TraceGL3(gl, System.err);
	GLContext ctx = gl.getContext();
	if(benv == null)
	    benv = new JOGLEnvironment(gl, ctx, Area.sized(Coord.z, new Coord(1, 1)));
	if(benv.ctx != ctx)
	    throw(new AssertionError());
	benv.process(new JOGLWrap(gl));
	try {
	    benv.finish(new JOGLWrap(gl));
	} catch(InterruptedException e) {
	    Thread.currentThread().interrupt();
	    throw(new RuntimeException(e));
	}
    }

    private void loop() {
	try {
	    while(true) {
		benv.submitwait();
		buf.display();
	    }
	} catch(InterruptedException e) {
	}
    }

    private void ckproc() {
	synchronized(this) {
	    if(processor == null) {
		processor = new HackThread(this::loop, "JOGL offscreen processor");
		processor.setDaemon(true);
		processor.start();
	    }
	}
    }

    public Environment env() {
	return(penv);
    }

    public void dispose() {
    }

    private static Factory factory = new Factory() {
	private JOGLOffscreen instance = null;

	public Acephal open(String... args) {
	    synchronized(this) {
		if(instance == null)
		    instance = new JOGLOffscreen();
	    }
	    return(instance);
	}

	public int priority() {return(-10);}
    };
    public static Factory get() {
	return(factory);
    }
}
