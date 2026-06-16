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

package haven.ffi.x11;

import haven.*;
import haven.ffi.gl.*;
import java.util.*;
import java.lang.invoke.*;
import java.lang.foreign.*;

public class Test {
    public static void main(String[] args) {
	XLib xlib = XLib.get();
	GLX glx = GLX.get();
	OpenGL gl = glx.gl();

	XLib.Display dpy = xlib.XOpenDisplay(null);
	xlib.XSetErrorHandler();
	XLib.Atom WM_PROTOCOLS = xlib.XInternAtom(dpy, "WM_PROTOCOLS", false);
	XLib.Atom WM_DELETE_WINDOW = xlib.XInternAtom(dpy, "WM_DELETE_WINDOW", false);
	List<String> ext = Arrays.asList(glx.glXQueryExtensionsString(dpy, xlib.DefaultScreen(dpy)).split(" "));
	if(!ext.contains("GLX_ARB_create_context_profile"))
	    throw(new RuntimeException("GLX_ARB_create_context_profile not supported"));
	
	GLX.GLXFBConfig fb = glx.glXChooseFBConfig(dpy, xlib.DefaultScreen(dpy),
						   new int[] {GLX.GLX_DRAWABLE_TYPE, GLX.GLX_WINDOW_BIT,    GLX.GLX_RENDER_TYPE,  GLX.GLX_RGBA_BIT,
							      GLX.GLX_DEPTH_SIZE,    24,                    GLX.GLX_DOUBLEBUFFER, 1,
							      GLX.GLX_RED_SIZE,      8,                     GLX.GLX_GREEN_SIZE,   8,                GLX.GLX_BLUE_SIZE, 8,
							      XLib.None})[0];
	Debug.dump("GLX_RENDER_TYPE: ", fb.attrib(GLX.GLX_RENDER_TYPE));
	XLib.XVisualInfo vis = glx.glXGetVisualFromFBConfig(dpy, fb);
	XLib.XID root = xlib.DefaultRootWindow(dpy);
	XLib.XID cmap = xlib.XCreateColormap(dpy, root, vis.visual(), XLib.AllocNone);
	XLib.XSetWindowAttributes attrs = xlib.XSetWindowAttributes();
	attrs.colormap(cmap).event_mask(XLib.ExposureMask | XLib.KeyPressMask | XLib.FocusChangeMask);
	GLX.GLXContext ctx = null;
	for(int[] ver : new int[][] {{5, 2}, {4, 6}, {3, 2}}) {
	    try {
		ctx = glx.glXCreateContextAttribsARB(dpy, fb, null, true, new int[] {
			GLX.GLX_CONTEXT_PROFILE_MASK_ARB, GLX.GLX_CONTEXT_CORE_PROFILE_BIT_ARB,
			GLX.GLX_CONTEXT_MAJOR_VERSION_ARB, ver[0],
			GLX.GLX_CONTEXT_MINOR_VERSION_ARB, ver[1],
			XLib.None
		    });
		Debug.dump(ver);
		break;
	    } catch(XException e) {
		Debug.dump(e.getMessage());
	    }
	}
	if(ctx == null)
	    throw(new NullPointerException());
	XLib.XID wnd = xlib.XCreateWindow(dpy, root, 0, 0, 600, 600, 0, vis.depth(), XLib.InputOutput, vis.visual(), XLib.CWColormap | XLib.CWEventMask | XLib.CWBackPixel, attrs);
	Debug.dump(glx.glXMakeCurrent(dpy, wnd, ctx));
	int[] buf = {0};
	gl.glGetIntegerv(gl.GL_MAJOR_VERSION, buf);
	Debug.dump(buf[0]);
	gl.glGetIntegerv(gl.GL_MINOR_VERSION, buf);
	Debug.dump(buf[0]);
	gl.glGetIntegerv(gl.GL_CONTEXT_PROFILE_MASK, buf);
	Debug.dump(buf[0]);
	gl.glGetIntegerv(gl.GL_CONTEXT_FLAGS, buf);
	Debug.dump(buf[0]);
	
	xlib.XMapWindow(dpy, wnd);
	xlib.XStoreName(dpy, wnd, "Barda");
	evloop: while(true) {
	    XLib.XEvent ev = xlib.XEvent();
	    xlib.XNextEvent(dpy, ev);
	    switch(ev.type()) {
	    case XLib.Expose: {
		glx.glXSwapBuffers(dpy, wnd);
		Debug.dump(ev.xexpose());
		break;
	    }
	    case XLib.ConfigureNotify: {
		Debug.dump(ev.xconfigure());
		break;
	    }
	    case XLib.KeyPress: {
		XLib.XKeyEvent kev = ev.xkey();
		Debug.dump(kev);
		if(kev.keycode() == 9)
		    break evloop;
		break;
	    }
	    case XLib.ClientMessage: {
		XLib.XClientMessageEvent cev = ev.xclient();
		if(cev.message_type().equals(WM_PROTOCOLS)) {
		    XLib.Atom msg = cev.a()[0];
		    if(msg.equals(WM_DELETE_WINDOW))
			break evloop;
		}
		break;
	    }
	    default:
		Debug.dump(ev.type());
		break;
	    }
	}
	glx.glXMakeCurrent(dpy, XLib.XID.None, null);
	glx.glXDestroyContext(dpy, ctx);
	xlib.XDestroyWindow(dpy, wnd);
	xlib.XCloseDisplay(dpy);
    }
}
