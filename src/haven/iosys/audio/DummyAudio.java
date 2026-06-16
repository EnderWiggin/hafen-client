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
import java.util.*;
import java.util.function.*;
import haven.Audio.*;

@AudioSystem.Available(name = "dummy")
public class DummyAudio implements AudioSystem {
    public static final DummyAudio instance = new DummyAudio();

    public static class DummyPlayer extends HackThread implements Player {
	public final CS stream;
	private boolean stop = false;

	public DummyPlayer(CS stream) {
	    super("Dummy audio player");
	    this.stream = stream;
	    setDaemon(true);
	    start();
	}

	public void run() {
	    double then = Utils.rtime();
	    try {
		double[][] buf = new double[2][1024];
		while(!stop) {
		    Thread.sleep((1000 * 1024) / Audio.SAMPLE_RATE);
		    if(stream.get(buf, 1024) <= 0)
			return;
		}
	    } catch(InterruptedException e) {
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

    public static class DummySink implements SinkLine {
	public static final SinkLine instance = new DummySink();

	public Player open(CS stream) {
	    return(new DummyPlayer(stream));
	}
	public Player open(CS stream, int bufsize) {
	    return(open(stream));
	}
    }

    public SinkLine sinkline(Map<?, ?> spec) {
	return(DummySink.instance);
    }

    public List<SinkDevice> sinkdevs() {
	return(Collections.emptyList());
    }

    private static Factory factory = new Factory() {
	public AudioSystem open(String... args) {
	    return(instance);
	}

	public int priority() {return(-999);}
	public boolean autouse() {return(false);}
    };
    public static Factory get() {
	return(factory);
    }
}
