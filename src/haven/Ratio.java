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

import java.util.*;
import static haven.Utils.*;

public class Ratio extends Number implements Comparable<Ratio>{
    public static final Ratio z = new Ratio(0, 1);
    public final long p, q;

    private Ratio(long p, long q) {
	if(q < 0) {
	    this.p = -p;
	    this.q = -q;
	} else if(q > 0) {
	    this.p = p;
	    this.q = q;
	} else {
	    throw(new ArithmeticException());
	}
    }
    public static Ratio of(long p, long q) {
	return((p == 0) ? z : new Ratio(p, q));
    }
    public static Ratio of(long n) {
	return(of(n, 1));
    }
    public static Ratio of(double v) {
	long b = Double.doubleToLongBits(v);
	long m = (b & 0x000fffffffffffffl) | 0x0010000000000000l;
	long e = (b >>> 52) & 0x7ff;
	boolean s = (b >>> 63) != 0;
	if(e == 0) {
	    return(z);
	} else if(e == 2047) {
	    return(null);
	}
	e = e - 1023 - 52;
	int z = Long.numberOfTrailingZeros(m);
	m >>>= z; e += z;
	if(e >= 0)
	    return(of(m << e, 1));
	else
	    return(of(m, 1l << -e));
    }

    private static long M(long a, long b) {return(Math.multiplyExact(a, b));}
    private static long A(long a, long b) {return(Math.addExact(a, b));}
    private static long S(long a, long b) {return(Math.subtractExact(a, b));}

    private byte simplep = 0;
    public boolean simplep() {
	if(simplep == 0)
	    simplep = (gcd(p, q) == 1) ? (byte)2 : (byte)1;
	return(simplep == 2);
    }

    private int hash = 0;
    public int hashCode() {
	if(hash == 0) {
	    if(simplep())
		hash = (int)((q * 0x1f1f1f1f) + p);
	    else
		hash = red().hashCode();
	    if(hash == 0)
		hash = 1;
	}
	return(hash);
    }

    public boolean equals(long P, long Q) {
	return(compareTo(P, Q) == 0);
    }
    public boolean equals(Ratio b) {
	return(equals(b.p, b.q));
    }
    public boolean equals(Object x) {
	return((x instanceof Ratio) && equals((Ratio)x));
    }

    private int longcmp(long a, long b) {
	return((a < b) ? -1 : (a > b) ? 1 : 0);
    }
    public int compareTo(long P, long Q) {
	if(q == Q)
	    return(longcmp(p, P));
	if(q > Q) {
	    if((q % Q) == 0)
		return(longcmp(p, M(P, (q / Q))));
	} else {
	    if((Q % q) == 0)
		return(longcmp(M(p, (Q / q)), P));
	}
	long m = lcm(q, Q);
	return(longcmp(M(p, (m / q)), M(P, (m / Q))));
    }
    public int compareTo(Ratio b) {
	return(compareTo(b.p, b.q));
    }

    public boolean lt(Ratio b) {return(compareTo(b) <  0);}
    public boolean le(Ratio b) {return(compareTo(b) <= 0);}
    public boolean gt(Ratio b) {return(compareTo(b) >  0);}
    public boolean ge(Ratio b) {return(compareTo(b) >= 0);}

    public Ratio inv() {
	return(of(-p, q));
    }
    public Ratio rcp() {
	return(of(q, p));
    }
    public Ratio red() {
	long d = gcd(p, q);
	return(of(p / d, q / d));
    }

    public Ratio add(long n) {
	return(of(A(p, M(n, q)), q));
    }
    public Ratio add(long P, long Q) {
	if(q == Q)
	    return(of(A(p, P), q));
	long d = lcm(q, Q);
	return(of(A(M(p, (d / q)), M(P, (d / Q))), d));
    }
    public Ratio add(Ratio b) {
	return(add(b.p, b.q));
    }
    public Ratio sub(long n) {
	return(of(S(p, M(n, q)), q));
    }
    public Ratio sub(long P, long Q) {
	return(add(-P, Q));
    }
    public Ratio sub(Ratio b) {
	return(sub(b.p, b.q));
    }

    public Ratio mul(long P, long Q) {
	long d = gcd(p, Q), D = gcd(P, q);
	return(of(M((p / d), (P / D)), M((q / D), (Q / d))));
    }
    public Ratio mul(Ratio b) {
	return(mul(b.p, b.q));
    }
    public Ratio div(long P, long Q) {
	return(mul(Q, P));
    }
    public Ratio div(Ratio b) {
	return(div(b.p, b.q));
    }
    public Ratio mod(Ratio b) {
	return(sub(b.mul(div(b).floor())));
    }
    public Ratio mod(long P, long Q) {
	return(mod(of(P, Q)));
    }
    public Ratio mul(long f) {
	return(mul(f, 1));
    }
    public Ratio div(long f) {
	return(div(f, 1));
    }

    public long trunc() {
	return(p / q);
    }
    public long floor() {
	return(Utils.floordiv(p, q));
    }
    public long round() {
	return(of(A(p, (q / 2)), q).floor());
    }
    public long ceil() {
	return(of(A(p, (q - 1)), q).floor());
    }

    public long longValue() {
	return(p / q);
    }
    public double doubleValue() {
	return((double)p / (double)q);
    }
    public int intValue() {return((int)longValue());}
    public float floatValue() {return((float)doubleValue());}

    private static final long rtimeoff = System.nanoTime();
    public static Ratio rtime() {
	return(of(System.nanoTime() - rtimeoff, 1000000000));
    }

    public static Ratio parse(String s) {
	int p = s.indexOf('/');
	if(p < 0)
	    return(of(Long.parseLong(s)));
	return(of(Long.parseLong(s.substring(0, p)), Long.parseLong(s.substring(p + 1))));
    }

    public String toString() {
	return(p + "/" + q);
    }
}
