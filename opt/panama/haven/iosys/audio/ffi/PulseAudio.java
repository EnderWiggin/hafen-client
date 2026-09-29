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

package haven.iosys.audio.ffi;

import java.util.*;
import java.io.*;
import java.nio.*;
import haven.*;
import haven.iosys.*;
import haven.iosys.audio.*;
import haven.ffi.misc.LibPulse;
import haven.Audio.*;
import static haven.iosys.audio.AudioSystem.*;
import static haven.ffi.misc.LibPulse.*;

@AudioSystem.Available(name = "pulse")
public class PulseAudio implements AudioSystem {
    private static final String NAME = "Haven & Hearth";
    private final LibPulse pulse;
    private final pa_threaded_mainloop loop;
    private final pa_context ctx;
    private int ctxst;

    private static Providers.Factory<PulseAudio> factory = new Providers.Factory<PulseAudio>() {
	private PulseAudio instance = null;

	public PulseAudio open(String... args) {
	    synchronized(this) {
		if(instance == null)
		    instance = new PulseAudio();
	    }
	    return(instance);
	}

	public boolean experimental() {return(true);}
	public int priority() {return(10);}
    };
    public static Providers.Factory<PulseAudio> get() {
	return(factory);
    }

    private void ctxstate() {
	synchronized(ctx) {
	    ctxst = ctx.get_state();
	    ctx.notifyAll();
	}
    }

    private void ctxevent(String name, pa_proplist props) {
	Debug.dump(name);
    }

    public PulseAudio() {
	boolean done = false;
	try {
	    pulse = LibPulse.get();
	    loop = pulse.pa_threaded_mainloop_new();
	    try(var _lk = loop.lock()) {
		pa_proplist props = pulse.pa_proplist_new();
		props.sets(PA_PROP_APPLICATION_ID, "se.seatribe.hafen");
		/*
		try(InputStream icon = Client.class.getResourceAsStream("icon.png")) {
		    props.set(PA_PROP_APPLICATION_ICON, Utils.readall(icon));
		} catch(IOException e) {
		    new Warning(e, "could not read pulse-audio icon").issue();
		}
		*/
		ctx = pulse.pa_context_new_with_proplist(loop.get_api(), NAME, props);
		ctx.set_state_callback(this::ctxstate);
		ctx.set_event_callback(this::ctxevent);
		loop.start();
		ctx.connect(null, PA_CONTEXT_NOFLAGS);
	    }
	    synchronized(ctx) {
		while(true) {
		    if(ctxst == PA_CONTEXT_READY)
			break;
		    if(!pulse.PA_CONTEXT_IS_GOOD(ctxst))
			throw(new RuntimeException("pulseaudio connect failed: " + ctxst));
		    ctx.wait();
		}
	    }
	    done = true;
	} catch(Exception e) {
	    throw(new Unavailable("PulseAudio library not avilable", e));
	} finally {
	    if(!done)
		dispose();
	}
    }

    public class PulseSink implements SinkLine {
	public final pa_sample_spec spec;
	public final int defbuf;

	public PulseSink(pa_sample_spec spec, int defbuf) {
	    this.spec = spec;
	    this.defbuf = defbuf;
	}

	public class Stream implements Player {
	    public final CS stream;
	    public final pa_stream ps;
	    private int state;

	    private void state() {
		synchronized(ps) {
		    this.state = ps.get_state();
		    ps.notifyAll();
		}
	    }

	    private void fill(long len) {
		int nch = spec.channels();
		ByteBuffer buf = ps.begin_write(len);
		buf.order(ByteOrder.LITTLE_ENDIAN);
		int nf = buf.remaining() / (nch * 4);
		double[][] val = new double[nch][nf];
		int ret = stream.get(val, nf);
		if(ret <= 0) {
		    ps.cancel_write();
		    ps.disconnect();
		    Debug.dump(1);
		    return;
		}
		for(int f = 0; f < nf; f++) {
		    for(int c = 0; c < nch; c++)
			buf.putFloat((float)val[c][f]);
		}
		buf.flip();
		ps.write(buf, 0, PA_SEEK_RELATIVE);
	    }

	    public Stream(CS stream, int bufsize) {
		this.stream = stream;
		pa_stream ps = null;
		boolean clean = false;
		try {
		    try(var _lk = loop.lock()) {
			pa_proplist props = pulse.pa_proplist_new();
			props.sets(PA_PROP_MEDIA_ROLE, "game");
			this.ps = ps = pulse.pa_stream_new_with_proplist(ctx, "Audio output", spec, props);
			ps.set_state_callback(this::state);
			ps.set_write_callback(this::fill);
			pa_buffer_attr attr = pulse.pa_buffer_attr();
			attr.maxlength(-1);
			attr.tlength(bufsize * spec.channels() * 4);
			attr.prebuf(-1);
			attr.minreq(-1);
			attr.fragsize(-1);
			ps.connect_playback(null, attr, PA_STREAM_INTERPOLATE_TIMING | PA_STREAM_AUTO_TIMING_UPDATE | PA_STREAM_ADJUST_LATENCY);
		    }
		    synchronized(ps) {
			while(true) {
			    if(state == PA_STREAM_READY)
				break;
			    if(!pulse.PA_STREAM_IS_GOOD(state))
				throw(new RuntimeException("stream connection failed: " + state));
			    ps.wait();
			}
		    }
		} catch(InterruptedException e) {
		    Thread.currentThread().interrupt();
		    throw(new Unavailable(e));
		} catch(PulseException e) {
		    throw(new Unavailable(e));
		} finally {
		    if(clean)
			ps.disconnect();
		}
	    }

	    public void stop(boolean async) {
		try(var _lk = loop.lock()) {
		    ps.disconnect();
		}
	    }
	}

	public Player open(CS stream, int bufsize) {
	    return(new Stream(stream, bufsize));
	}

	public Player open(CS stream) {
	    return(open(stream, defbuf));
	}
    }

    public List<SinkDevice> sinkdevs() {
	return(Collections.emptyList());
    }

    public SinkLine sinkline(Map<?, ?> spec) {
	int buf = intspec(spec, SPEC_BUFSIZE, 1024);
	pa_sample_spec ss = pulse.pa_sample_spec();
	ss.format(PA_SAMPLE_FLOAT32LE);
	ss.rate(intspec(spec, SPEC_RATE, null));
	ss.channels(intspec(spec, SPEC_CHANNELS, 2));
	return(new PulseSink(ss, buf));
    }

    public void dispose() {
	if(loop != null) {
	    try(var _lk = loop.lock()) {
		loop.stop();
		if(ctx != null)
		    ctx.disconnect();
	    }
	}
    }
}
