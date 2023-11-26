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

package haven;

import java.awt.Color;
import java.util.Arrays;

public class Astronomy {
    public final double dt, mp, yt, sp, sd;
    public final double years, ym, md;
    public final boolean night;
    public final Color mc;
    public final int is;
    
    public final int hh, mm, day, srday, srhh, srmm, scday;
    public static final String[] phase = {
	"New Moon",
	"Waxing Crescent",
	"First Quarter",
	"Waxing Gibbous",
	"Full Moon",
	"Waning Gibbous",
	"Last Quarter",
	"Waning Crescent"
    };
    private static final int MINUTE = 60;
    private static final int HOUR = 60 * MINUTE;
    private static final int DAY = 24 * HOUR;
    private static final int YEAR_DAYS = Season.yearLength();
    
    public Astronomy(double dt, double mp, double yt, boolean night, Color mc, int is, double sp, double sd, double years, double ym, double md) {
	this.dt = dt;
	this.mp = mp;
	this.yt = yt;
	this.night = night;
	this.mc = mc;
	this.is = is;
	this.sp = sp;
	this.sd = sd;
	this.years = years;
	this.ym = ym;
	this.md = md;
	this.hh = (int) (24 * dt);
	this.mm = (int) (60 * (24 * dt - hh));
	this.day = (int) (YEAR_DAYS * yt);
    
	scday = (int) (season().length * sp);
	
	int seasonTs = (int) (season().length * DAY * (1 - sp)); //seconds remaining in season
	srday = seasonTs / DAY;
	srhh = (seasonTs - srday * DAY) / HOUR;
	srmm = (seasonTs - srday * DAY - srhh * HOUR) / MINUTE;
    }
    
    public Season season() { return Season.values()[is]; }
    
    enum Season {
	Spring(30), Summer(105), Autumn(30), Winter(15);
	
	public final int length;
	
	Season(int length) {
	    this.length = length;
	}
    
	public static int yearLength() { return Arrays.stream(values()).mapToInt(season -> season.length).sum(); }
    }
}
