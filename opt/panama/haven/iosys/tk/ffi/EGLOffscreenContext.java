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
import java.util.*;
import java.util.function.*;
import haven.ffi.gl.EGL.*;

@Acephal.Available(name = "egl")
public class EGLOffscreenContext implements Providers.Factory<EGLOffscreenContext.EGLOffscreen> {
    private final EGL egl;
    private final Collection<String> clexts;

    private EGLOffscreenContext() {
	try {
	    egl = EGL.get();
	} catch(Exception e) {
	    throw(new Unavailable("EGL libraries not available", e));
	}
	clexts = new HashSet<>(Arrays.asList(egl.eglQueryString(EGLDisplay.NONE, EGL.EGL_EXTENSIONS).split(" ")));
    }

    public class EGLOffscreen implements Acephal {
	private static final int[][] glversions = {
	    {4, 6}, {4, 5}, {4, 4}, {4, 3}, {4, 2}, {4, 1}, {4, 0},
	};
	private static final ByteBuffer backbuffer = ByteBuffer.allocateDirect(4);
	private final EGLDisplay dpy;
	private final EGLSurface surf;
	private final EGLContext ctx;
	private final OpenGL gl;
	private final FFIEnvironment env;
	private final Thread processor;
	private boolean disposed = false;

	private EGLOffscreen(EGLDisplay dpy) {
	    boolean done = false;
	    try {
		try {
		    this.dpy = dpy;
		    egl.eglInitialize(dpy);
		    EGLConfig[] cfg = egl.eglChooseConfig(dpy, new int[] {
			EGL.EGL_SURFACE_TYPE, EGL.EGL_PBUFFER_BIT,
			EGL.EGL_RED_SIZE, 8,  EGL.EGL_GREEN_SIZE, 8,
			EGL.EGL_BLUE_SIZE, 8, EGL.EGL_ALPHA_SIZE, 8,
			EGL.EGL_RENDERABLE_TYPE, EGL.EGL_OPENGL_BIT,
		    });
		    if(cfg.length == 0)
			throw(new Unavailable("no usable EGL configurations available"));
		    surf = egl.eglCreatePbufferSurface(dpy, cfg[0], new int[] {
			EGL.EGL_WIDTH, 1, EGL.EGL_HEIGHT, 1,
		    });
		    egl.eglBindAPI(EGL.EGL_OPENGL_API);
		    EGLContext ctx = null;
		    for(int[] ver : glversions) {
			try {
			    ctx = egl.eglCreateContext(dpy, cfg[0], null, new int[] {
				EGL.EGL_CONTEXT_MAJOR_VERSION, ver[0],
				EGL.EGL_CONTEXT_MINOR_VERSION, ver[1],
			    });
			    if(ctx != null)
				break;
			} catch(EGLException e) {
			    continue;
			}
		    }
		    if(ctx == null)
			throw(new Unavailable("EGL context not available for any usable OpenGL version"));
		    this.ctx = ctx;
		    this.gl = egl.gl();
		} catch(EGLException e) {
		    throw(new Unavailable("could not create EGL context", e));
		}
		this.env = glrun(() -> new FFIEnvironment(gl));
		this.processor = new HackThread(this::loop, "EGL command processor");
		this.processor.setDaemon(true);
		this.processor.start();
		done = true;
	    } finally {
		if(!done)
		    dispose();
	    }
	}

	private <T> T glrun(Supplier<T> task) {
	    egl.eglBindAPI(EGL.EGL_OPENGL_API);
	    egl.eglMakeCurrent(dpy, surf, surf, ctx);
	    try {
		return(task.get());
	    } finally {
		egl.eglMakeCurrent(dpy, null, null, null);
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
		if(ctx != null)
		    egl.eglDestroyContext(dpy, ctx);
		if(surf != null)
		    egl.eglDestroySurface(dpy, surf);
		if(dpy != null)
		    egl.eglTerminate(dpy);
	    }
	}
    }

    public EGLOffscreen open(String... args) {
	EGLDisplay dpy;
	if(args.length < 1) {
	    if(!clexts.contains("EGL_MESA_platform_surfaceless"))
		throw(new Unavailable("EGL_MESA_platform_surfaceless not supported"));
	    try {
		dpy = egl.eglGetPlatformDisplay(new PlatformSurfacelessMesa());
	    } catch(EGLException e) {
		throw(new Unavailable("could get surfaceless Mesa display", e));
	    }
	} else {
	    String spec = args[0], type, arg;
	    int p = spec.indexOf(':');
	    if(p >= 0) {
		type = spec.substring(0, p);
		arg = spec.substring(p + 1);
	    } else {
		type = spec;
		arg = "";
	    }
	    if(type.equals("drm")) {
		if(!clexts.contains("EGL_EXT_device_enumeration")) throw(new Unavailable("EGL_EXT_device_enumeration not supported"));
		if(!clexts.contains("EGL_EXT_device_query")) throw(new Unavailable("EGL_EXT_device_query not supported"));
		EGL.EGLDeviceEXT cdev = null;
		for(EGL.EGLDeviceEXT dev : egl.eglQueryDevicesEXT()) {
		    Collection<String> exts = new HashSet<>(Arrays.asList(egl.eglQueryDeviceStringEXT(dev, EGL.EGL_EXTENSIONS).split(" ")));
		    if(!exts.contains("EGL_EXT_device_drm"))
			continue;
		    if(arg.equals("")) {
			cdev = dev;
			break;
		    }
		    if(egl.eglQueryDeviceStringEXT(dev, EGL.EGL_DRM_DEVICE_FILE_EXT).equals(arg)) {
			cdev = dev;
			break;
		    }
		    if(exts.contains("EGL_EXT_device_drm_render_node")) {
			if(egl.eglQueryDeviceStringEXT(dev, EGL.EGL_DRM_RENDER_NODE_FILE_EXT).equals(arg)) {
			    cdev = dev;
			    break;
			}
		    }
		}
		if(cdev == null)
		    throw(new Unavailable("no such drm device found: " + arg));
		dpy = egl.eglGetPlatformDisplay(cdev);
	    } else {
		throw(new Unavailable("no such EGL display type: " + type));
	    }
	}
	return(new EGLOffscreen(dpy));
    }

    private static EGLOffscreenContext instance = null;
    public static EGLOffscreenContext get() {
	if(instance == null) {
	    synchronized(EGLOffscreenContext.class) {
		if(instance == null)
		    instance = new EGLOffscreenContext();
	    }
	}
	return(instance);
    }
}
