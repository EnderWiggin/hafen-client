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

package haven.resutil;

import haven.*;
import me.ender.timer.Timer;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.image.BufferedImage;

import static haven.QualityList.SingleType.*;

public class Curiosity extends ItemInfo.Tip implements GItem.ColorInfo {
    public final Color better = new Color(0, 255, 0, 64), worse = new Color(255, 0, 0, 64);
    public final int exp, mw, enc, time;
    public transient final int lph;
    public final UI ui;

    public Curiosity(Owner owner, int exp, int mw, int enc, int time) {
	super(owner);
	this.exp = exp;
	this.mw = mw;
	this.enc = enc;
	this.time = time;
	this.lph = (exp > 0 && time > 0) ? (3600 * exp / time) : 0;
	UI ui = null;
	if(owner instanceof Widget) {
	    Widget wdg = (Widget)owner;
	    if(wdg.getparent(CharWnd.class) != null)
		ui = wdg.ui;
	}
	this.ui = ui;
    }

    static String[] units = {"s", "m", "h", "d"};
    static int[] div = {60, 60, 24};
    static String timefmt(int time) {
	if(CFG.REAL_TIME_CURIO.get()) {
	    time = (int) (time / Timer.SERVER_RATIO);
	}
	int[] vals = new int[units.length];
	vals[0] = time;
	for(int i = 0; i < div.length; i++) {
	    vals[i + 1] = vals[i] / div[i];
	    vals[i] = vals[i] % div[i];
	}
	StringBuilder buf = new StringBuilder();
	for(int i = units.length - 1; i >= 0; i--) {
	    if(vals[i] > 0) {
		if(buf.length() > 0) {
		    buf.append(String.format(" %02d", vals[i]));
		} else {
		    buf.append(vals[i]);
		}
		buf.append(units[i]);
	    }
	}
	return(buf.toString());
    }
    
    public static int lph(int lph){
        return CFG.REAL_TIME_CURIO.get() ? ((int) (Timer.SERVER_RATIO * lph)) : lph;
    }

    public BufferedImage tipimg() {
	StringBuilder buf = new StringBuilder();
	if(exp > 0)
	    buf.append(String.format("Learning points: $col[192,192,255]{%s}\n", Utils.thformat(exp)));
	if(time > 0)
	    buf.append(String.format("Study time: $col[192,255,192]{%s}\n", timefmt(time)));
	String remaining = remainingLongTip();
	if(remaining != null) {
	    buf.append(String.format("Remaining: $col[255,224,192]{%s}\n", remaining));
	}
	if(CFG.SHOW_CURIO_LPH.get() && lph > 0) {
	    buf.append(String.format("LP/H: $col[192,255,255]{%d}\n", lph(this.lph)));
	    buf.append(String.format("LP/H/Weight: $col[192,255,255]{%d}\n", lph(this.lph / mw)));
	}
	if(mw > 0)
	    buf.append(String.format("Mental weight: $col[255,192,255]{%d}\n", mw));
	if(enc > 0)
	    buf.append(String.format("Experience cost: $col[255,255,192]{%d}\n", enc));
	return(RichText.render(buf.toString(), 0).img);
    }

    public Color olcol() {
	Object tip = (ui == null) ? null : ui.lasttip;
	if(tip instanceof ItemInfo.InfoTip) {
	    Curiosity that = ItemInfo.find(Curiosity.class, ((ItemInfo.InfoTip)tip).info());
	    if(that != null) {
		double crate = (double)that.exp / (double)that.time;
		double trate = (double)this.exp / (double)this.time;
		if(Debug.ff)
		    Debug.dump(trate, crate);
		double ε = 0.5 / 3600.0;
		if(trate < crate - ε)
		    return(worse);
		if(trate > crate + ε)
		    return(better);
	    }
	}
	return(null);
    }

    private String remainingLongTip() {
        if(CFG.SHOW_CURIO_REMAINING_TT.get()) {
	    return remainingLongTip(remaining());
	} else {
            return null;
	}
    }
    
    private String remainingLongTip(int remaining) {
	if(remaining >= 0) {
	    return timefmt(remaining);
	}
	return null;
    }
    
    private String remainingShortTip(int time) {
	if(!CFG.SHOW_CURIO_REMAINING_METER.get() || time < 0) {return null;}
	time = (int) (time / Timer.SERVER_RATIO); //short tip is always in real time
	if(time >= 60) {
	    if(time > 3600) {
		time = time / 60;
	    }
	    return String.format("%d:%02d", time / 60, time % 60);
	} else {
	    return String.format("%02d", time);
	}
    }
    
    public Pair<String, String> remainingTip() {
	int time = remaining();
	return new Pair<>(remainingShortTip(time), remainingLongTip(time));
    }
    
    //return remaining study time in server seconds
    public int remaining() {
	if(owner instanceof GItem) {
	    GItem item = ((GItem) owner);
	    GItem.MeterInfo m = ItemInfo.find(GItem.MeterInfo.class, item.info());
	    double meter = (m != null) ? m.meter() : 0;
	    if(meter > 0) {
		long now = System.currentTimeMillis();
		long remStudy = (long) ((1.0 - meter) * time);
		long elapsed = (long) (Timer.SERVER_RATIO * (now - item.meterUpdated) / 1000);
		return (int) (remStudy - elapsed);
	    }
	}
	return -1;
    }
    
    public static class Data implements ItemData.ITipData {
	public final int lp, weight, xp, time;

	public Data(Curiosity ii, QualityList q) {
	    QualityList.Quality single = q.single(Quality);
	    if(single == null) {
		single = QualityList.DEFAULT;
	    }
	    lp = (int) Math.round(ii.exp / single.multiplier);
	    weight = ii.mw;
	    xp = ii.enc;
	    time = ii.time;
	}

	@Override
	public ItemInfo create(Session sess) {
	    return new Curiosity(null, lp, weight, xp, time);
	}
    }
}
