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

package haven.iosys.audio;

import haven.*;
import haven.iosys.*;
import java.util.*;
import java.util.function.*;
import javax.sound.sampled.*;
import haven.Audio.*;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Mixer;
import static haven.iosys.audio.AudioSystem.*;

@haven.iosys.audio.AudioSystem.Available(name = "java")
public class JavaSound implements haven.iosys.audio.AudioSystem {
    private static Factory factory = new Factory() {
	private JavaSound instance = null;

	public haven.iosys.audio.AudioSystem open(String... args) {
	    synchronized(this) {
		if(instance == null)
		    instance = new JavaSound();
	    }
	    return(instance);
	}

	public int priority() {return(-10);}
    };
    public static Factory get() {
	return(factory);
    }

    public JavaSound() {
	/* Basic availability test */
	sinkline(Utils.map().put(SPEC_RATE, 44100).map());
    }

    private static class Player extends HackThread implements haven.iosys.audio.AudioSystem.Player {
	private final SourceDataLine line;
	private final AudioFormat fmt;
	private final CS stream;
	private final int nch;
	private volatile boolean stop = false;

	Player(SourceDataLine line, AudioFormat fmt, CS stream) {
	    super("Haven audio player");
	    this.line = line;
	    this.fmt = fmt;
	    this.stream = stream;
	    nch = fmt.getChannels();
	    setDaemon(true);
	    start();
	}

	private int fillbuf(byte[] dst, int off, int len) {
	    int ns = len / (2 * nch);
	    double[][] val = new double[nch][ns];
	    int left = ns, wr = 0;
	    while(left > 0) {
		int ret = stream.get(val, left);
		if(ret <= 0)
		    return((wr > 0)?wr:-1);
		for(int i = 0; i < ret; i++) {
		    for(int o = 0; o < nch; o++) {
			int iv = (int)(val[o][i] * 32767.0);
			if(iv < 0) {
			    if(iv < -32768)
				iv = -32768;
			    iv += 65536;
			} else {
			    if(iv > 32767)
				iv = 32767;
			}
			dst[off++] = (byte)(iv & 0xff);
			dst[off++] = (byte)((iv & 0xff00) >> 8);
			wr += 2;
		    }
		}
		left -= ret;
	    }
	    return(wr);
	}

	public void run() {
	    try {
		byte[] buf = new byte[line.getBufferSize() / 2];
		while(true) {
		    if(Thread.interrupted())
			throw(new InterruptedException());
		    int ret = fillbuf(buf, 0, buf.length);
		    if(ret < 0)
			return;
		    for(int off = 0; off < ret; off += line.write(buf, off, ret - off));
		    if(stop)
			break;
		}
	    } catch(InterruptedException e) {
	    } finally {
		if(line != null)
		    line.close();
	    }
	}

	public void stop(boolean async) {
	    try {
		synchronized(this) {
		    stop = true;
		    while(!async && isAlive())
			this.join();
		}
	    } catch(InterruptedException e) {
		Thread.currentThread().interrupt();
	    }
	}
    }

    public static class JavaSink implements SinkLine {
	public final Mixer mixer;
	public final AudioFormat fmt;
	public final int defbuf;

	public JavaSink(Mixer mixer, AudioFormat fmt, int defbuf) {
	    this.mixer = mixer;
	    this.fmt = fmt;
	    this.defbuf = defbuf;
	}

	public Player open(CS stream, int bufsize) {
	    SourceDataLine line;
	    try {
		line = (SourceDataLine)mixer.getLine(new DataLine.Info(SourceDataLine.class, fmt));
		line.open(fmt, bufsize);
	    } catch(LineUnavailableException e) {
		throw(new Unavailable(e));
	    }
	    line.start();
	    return(new Player(line, fmt, stream));
	}

	public Player open(CS stream) {
	    return(open(stream, defbuf));
	}
    }

    public static class JavaSinkDev implements SinkDevice {
	public final Mixer.Info info;

	public JavaSinkDev(Mixer.Info info) {
	    this.info = info;
	}

	public String id() {return(info.getName());}
	public String desc() {return(info.getDescription());}

	public String toString() {
	    return(String.format("%s: %s", id(), desc()));
	}
    }

    public List<SinkDevice> sinkdevs() {
	List<SinkDevice> ret = new ArrayList<>();
	for(Mixer.Info info : AudioSystem.getMixerInfo())
	    ret.add(new JavaSinkDev(info));
	return(ret);
    }

    public SinkLine sinkline(Map<?, ?> spec) {
	int rate = intspec(spec, SPEC_RATE, null);
	int ch = intspec(spec, SPEC_CHANNELS, 2);
	int buf = intspec(spec, SPEC_BUFSIZE, 1024);
	AudioFormat fmt = new AudioFormat(rate, 16, ch, true, false);
	int bufsize = buf * ch * 2;
	Object outspec = spec.get(SPEC_SINKDEV);
	Mixer mixer;
	if(outspec == null) {
	    try {
		mixer = AudioSystem.getMixer(null);
	    } catch(IllegalArgumentException e) {
		throw(new Unavailable(e));
	    }
	} else if(outspec instanceof JavaSinkDev) {
	    mixer = AudioSystem.getMixer(((JavaSinkDev)outspec).info);
	} else if(outspec instanceof Number) {
	    int idx = ((Number)outspec).intValue();
	    Mixer.Info[] byid = AudioSystem.getMixerInfo();
	    if((idx < 0) || (idx >= byid.length))
		throw(new IllegalArgumentException(SPEC_SINKDEV + ": " + outspec));
	    mixer = AudioSystem.getMixer(byid[idx]);
	} else if(outspec instanceof String) {
	    String ss = (String)outspec;
	    Mixer.Info f = null, f2 = null;
	    int fs = 0;
	    for(Mixer.Info info : AudioSystem.getMixerInfo()) {
		String nm = info.getName();
		int s = 0;
		if(nm.equals(ss))
		    s = 3;
		else if(nm.equalsIgnoreCase(ss))
		    s = 2;
		else if(nm.toLowerCase().indexOf(ss.toLowerCase()) >= 0)
		    s = 1;
		if(s > fs) {
		    f = info;
		    f2 = null;
		    fs = s;
		} else if((s > 0) && (s == fs)) {
		    f2 = info;
		}
	    }
	    if(f == null)
		throw(new IllegalArgumentException(String.format("no mixer found by name: %s", spec)));
	    else if(f2 != null)
		throw(new IllegalArgumentException(String.format("multiple mixers found by name `%s': %s and %s", spec, f.getName(), f2.getName())));
	    mixer = AudioSystem.getMixer(f);
	} else {
	    throw(new IllegalArgumentException(SPEC_SINKDEV + ": " + outspec));
	}
	SourceDataLine line;
	try {
	    /* Basic availability test */
	    line = (SourceDataLine)mixer.getLine(new DataLine.Info(SourceDataLine.class, fmt));
	} catch(LineUnavailableException | IllegalArgumentException e) {
	    throw(new Unavailable(e));
	}
	line.close();
	return(new JavaSink(mixer, fmt, bufsize));
    }
}
