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

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.function.*;
import java.text.*;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.awt.Font;
import java.awt.Color;
import java.awt.font.*;
import static java.text.AttributedCharacterIterator.Attribute;

public class RichText extends Text {
    public static final Parser std;
    public static final Foundry stdf;
    public final Part parts;

    static {
	Map<Attribute, Object> a = new HashMap<Attribute, Object>();
	a.put(TextAttribute.FAMILY, "SansSerif");
	a.put(TextAttribute.SIZE, UI.scale(10.0f));
	std = new Parser(a);
	stdf = new Foundry(std);
    }
    
    private RichText(String text, BufferedImage img, Part parts) {
	super(text, img);
	this.parts = parts;
    }

    private static class RState {
	FontRenderContext frc;
	
	RState(FontRenderContext frc) {
	    this.frc = frc;
	}
    }
    
    public static class FormatException extends RuntimeException {
	public FormatException(String msg) {
	    super(msg);
	}
    }

    public static class Part {
	public Part next = null;
	public int x, y;
	public RState rs;
	
	public void append(Part p) {
	    if(next == null)
		next = p;
	    else
		next.append(p);
	}
	
	public void prepare(RState rs) {
	    this.rs = rs;
	    if(next != null)
		next.prepare(rs);
	}
	
	public int width() {return(0);}
	public int height() {return(0);}
	public int baseline() {return(0);}
	public void render(Graphics2D g) {}
	public Part split(int w) {
	    return(this);
	}
    }
    
    public static interface ImageSource {
	public Image get(String[] args, int[] ap);

	public static ImageSource chain(ImageSource... srcs) {
	    return((args, ap) -> {
		for(ImageSource src : srcs) {
		    Image ret = src.get(args, ap);
		    if(ret != null)
			return(ret);
		}
		return(null);
	    });
	}

	public static ImageSource id(String id, Supplier<? extends Image> img) {
	    return((args, ap) -> {
		if(args[ap[0]].equals(id)) {
		    ap[0]++;
		    /* XXX? The Image cast indeed shouldn't be
		     * necessary, but there seems to be a javac bug
		     * that causes it not to emit the implicit cast
		     * from the generic call in the bytecode, causing
		     * verification errors. I have *no* idea what's so
		     * special about this specific line that causes it
		     * to trigger the bug, but it seems to happen
		     * anyway. */
		    return((Image)img.get());
		}
		return(null);
	    });
	}

	public static ImageSource res(Resource res) {
	    return((args, ap) -> {
		try {
		    int id = Integer.parseInt(args[ap[0]++]);
		    return(new Image(res.flayer(Resource.imgc, id)));
		} catch(NumberFormatException e) {
		    return(null);
		}
	    });
	}

	public static ImageSource res(Resource.Pool pool) {
	    return((args, ap) -> {
		Resource res = pool.loadwait(args[ap[0]++]);
		int id = -1;
		if(args.length > ap[0]) {
		    try {
			id = Integer.parseInt(args[ap[0]]);
			ap[0]++;
		    } catch(NumberFormatException e) {}
		}
		for(Resource.Image img : res.layers(Resource.imgc)) {
		    if(img.id == id)
			return(new Image(img));
		}
		throw(new RuntimeException("Found no image with id " + id + " in " + res.toString()));
	    });
	}
    }

    public static class Image extends Part {
	public BufferedImage img;
	public int h = -1;
	public double lh = -1, bh = -1;
	public Map<? extends Attribute, ?> attrs;
	public float imgscale = 1.0f;
	public boolean center = false;
	private Coord sz = null;
	
	public Image(BufferedImage img) {
	    this.img = img;
	}

	public Image(Resource.Image img) {
	    this.img = img.img;
	    this.imgscale = img.scale;
	}

	public Image(Resource res, int id) {
	    for(Resource.Image img : res.layers(Resource.imgc)) {
		if(img.id == id) {
		    this.img = img.img;
		    this.imgscale = img.scale;
		    break;
		}
	    }
	    if(this.img == null)
		throw(new RuntimeException("Found no image with id " + id + " in " + res.toString()));
	}

	private LineMetrics lm() {
	    Font f = (Font)attrs.get(TextAttribute.FONT);
	    if(f == null)
		f = new Font(attrs);
	    return(f.getLineMetrics("", rs.frc));
	}

	public void prepare(RState rs) {
	    super.prepare(rs);
	    sz = new Coord(Math.round(UI.scale(img.getWidth() / imgscale)), Math.round(UI.scale(img.getHeight() / imgscale)));
	    if(lh >= 0) {
		h = (int)Math.round(lh * lm().getHeight());
	    } else if(bh >= 0) {
		h = (int)Math.round(bh * lm().getAscent());
	    }
	    if(h >= 0)
		sz = new Coord((img.getWidth() * h) / img.getHeight(), h);
	}
	
	public int width() {return(sz.x);}
	public int height() {return(sz.y);}
    
	public int baseline() {
	    if(center) {
		LineMetrics lm = lm();
		float ascent = lm.getAscent();
		float height = lm.getHeight();
		return (int) (ascent + (sz.y - height) / 2);
	    }
	    return sz.y - 1;
	}

	public void render(Graphics2D g) {
	    g.drawImage(PUtils.uiscale(img, sz), x, y, null);
	}
    }

    public static class Newline extends Part {
	private Map<? extends Attribute, ?> attrs;
	private LineMetrics lm;
	
	public Newline(Map<? extends Attribute, ?> attrs) {
	    this.attrs = attrs;
	}
	
	private LineMetrics lm() {
	    if(lm == null) {
		Font f;
		if((f = (Font)attrs.get(TextAttribute.FONT)) != null) {
		} else {
		    f = new Font(attrs);
		}
		lm = f.getLineMetrics("", rs.frc);
	    }
	    return(lm);
	}
	
	public int height() {
	    return((int)lm().getHeight());
	}
	
	public int baseline() {
	    return((int)lm().getAscent());
	}
    }
    
    public static class TextPart extends Part {
	public AttributedString str;
	public int start, end;
	private TextMeasurer tm = null;
	private TextLayout tl = null;
	
	public TextPart(AttributedString str, int start, int end) {
	    this.str = str;
	    this.start = start;
	    this.end = end;
	}
	
	public TextPart(String str, Map<? extends Attribute, ?> attrs) {
	    this((str.length() == 0)?(new AttributedString(str)):(new AttributedString(str, attrs)), 0, str.length());
	}
	
	public TextPart(String str) {
	    this(new AttributedString(str), 0, str.length());
	}
	
	public AttributedCharacterIterator ti() {
	    return(str.getIterator(null, start, end));
	}

	public void append(Part p) {
	    if(next == null) {
		if(p instanceof TextPart) {
		    TextPart tp = (TextPart)p;
		    str = AttributedStringBuffer.concat(ti(), tp.ti());
		    end = (end - start) + (tp.end - tp.start);
		    start = 0;
		    next = p.next;
		} else {
		    next = p;
		}
	    } else {
		next.append(p);
	    }
	}
	
	public TextMeasurer tm() {
	    if(tm == null)
		tm = new TextMeasurer(str.getIterator(), rs.frc);
	    return(tm);
	}

	public TextLayout tl() {
	    if(tl == null)
		tl = tm().getLayout(start, end);
	    return(tl);
	}

	public float advance(int from, int to) {
	    if(from == to) return(0);
	    return(tm().getAdvanceBetween(start + from, start + to));
	}

	public int width() {
	    if(start == end) return(0);
	    return((int)tm().getAdvanceBetween(start, end));
	}
	
	public int height() {
	    if(start == end) return(0);
	    return((int)(tl().getAscent() + tl().getDescent() + tl().getLeading()));
	}
	
	public int baseline() {
	    if(start == end) return(0);
	    return((int)tl().getAscent());
	}
	
	private Part split2(int e1, int s2) {
	    TextPart p1 = new TextPart(str, start, e1);
	    TextPart p2 = new TextPart(str, s2, end);
	    p1.next = p2;
	    p2.next = next;
	    p1.rs = p2.rs = rs;
	    return(p1);
	}

	public Part split(int w) {
	    int l = start, r = end;
	    while(true) {
		int t = l + ((r - l) / 2);
		int tw;
		if(t == l)
		    tw = 0;
		else
		    tw = (int)tm().getAdvanceBetween(start, t);
		if(tw > w) {
		    r = t;
		} else {
		    l = t;
		}
		if(l >= r - 1)
		    break;
	    }
	    CharacterIterator it = str.getIterator();
	    for(int i = l; i >= start; i--) {
		if(Character.isWhitespace(it.setIndex(i))) {
		    return(split2(i, i + 1));
		}
	    }
	    return(split2(l, l));
	}
	
	public void render(Graphics2D g) {
	    if(start == end) return;
	    tl().draw(g, x, y + tl().getAscent());
	}
	
	public TextHitInfo charat(float x, float y) {
	    return(tl().hitTestChar(x, y));
	}

	public TextHitInfo charat(Coord c) {
	    return(charat(c.x - x, c.y - y));
	}
    }

    public Part partat(Coord c) {
	for(Part p = parts; p != null; p = p.next) {
	    if((c.x >= p.x) && (c.y >= p.y) &&
	       (c.x < p.x + p.width()) && (c.y < p.y + p.height())) {
		return(p);
	    }
	}
	return(null);
    }
    
    public AttributedCharacterIterator attrat(Coord c) {
	Part p = partat(c);
	if((p == null) || !(p instanceof TextPart))
	    return(null);
	TextPart tp = (TextPart)p;
	AttributedCharacterIterator attr = tp.ti();
	attr.setIndex(tp.charat(c).getCharIndex());
	return(attr);
    }

    public Object attrat(Coord c, Attribute attr) {
	AttributedCharacterIterator ai = attrat(c);
	if(ai == null)
	    return(null);
	return(ai.getAttribute(attr));
    }
    
    public static Map<? extends Attribute, ?> fillattrs2(Map<? extends Attribute, ?> def, Object... attrs) {
	Map<Attribute, Object> a;
	if(def == null)
	    a = new HashMap<Attribute, Object>();
	else
	    a = new HashMap<Attribute, Object>(def);
	for(int i = 0; i < attrs.length; i += 2)
	    a.put((Attribute)attrs[i], attrs[i + 1]);
	return(a);
    }
    
    public static Map<? extends Attribute, ?> fillattrs(Object... attrs) {
	return(fillattrs2(null, attrs));
    }

    /*
     * This fix exists for Java 1.5. Apparently, before Java 1.6,
     * TextAttribute.SIZE had to be specified with a Float, and not a
     * general Number; however, Java 1.6 fails to mention that in the
     * documentation (which rather explicitly says that any
     * java.lang.Number is perfectly OK and accepted
     * practice). However, specifying ints for font size looks nicer
     * in the rest of the code, so this function gets to collect all
     * the ugliness of conversion in itself.
     */
    private static Map<? extends Attribute, ?> fixattrs(Map<? extends Attribute, ?> attrs) {
	Map<Attribute, Object> ret = new HashMap<Attribute, Object>();
	for(Map.Entry<? extends Attribute, ?> e : attrs.entrySet()) {
	    if(e.getKey() == TextAttribute.SIZE) {
		ret.put(e.getKey(), ((Number)e.getValue()).floatValue());
	    } else {
		ret.put(e.getKey(), e.getValue());
	    }
	}
	return(ret);
    }

    public static class Parser {
	private final Map<? extends Attribute, ?> defattrs;
	private final ImageSource isrc;
	
	public Parser(ImageSource isrc, Map<? extends Attribute, ?> defattrs) {
	    this.isrc = isrc;
	    this.defattrs = fixattrs(defattrs);
	}
	
	public Parser(Map<? extends Attribute, ?> defattrs) {
	    this(ImageSource.res(Resource.remote()), defattrs);
	}
	
	public Parser(ImageSource isrc, Object... attrs) {
	    this(isrc, fillattrs2(std.defattrs, attrs));
	}
	
	public Parser(Object... attrs) {
	    this(ImageSource.res(Resource.remote()), attrs);
	}
	
	public static class PState {
	    PeekReader in;
	
	    PState(PeekReader in) {
		this.in = in;
	    }
	}
    
	private static boolean namechar(char c) {
	    return((c == ':') || (c == '_') || (c == '$') || (c == '.') || (c == '-') || ((c >= '0') && (c <= '9')) || ((c >= 'A') && (c <= 'Z')) || ((c >= 'a') && (c <= 'z')));
	}

	protected String name(PeekReader in) throws IOException {
	    StringBuilder buf = new StringBuilder();
	    while(true) {
		int c = in.peek();
		if(c < 0) {
		    break;
		} else if(namechar((char)c)) {
		    buf.append((char)in.read());
		} else {
		    break;
		}
	    }
	    if(buf.length() == 0)
		throw(new FormatException("Expected name, got `" + (char)in.peek() + "'"));
	    return(buf.toString());
	}
    
	protected Color a2col(String[] args) {
	    int r = Integer.parseInt(args[0]);
	    int g = Integer.parseInt(args[1]);
	    int b = Integer.parseInt(args[2]);
	    int a = 255;
	    if(args.length > 3)
		a = Integer.parseInt(args[3]);
	    return(new Color(r, g, b, a));
	}

	protected Part tag(PState s, String tn, String[] args, Map<? extends Attribute, ?> attrs) throws IOException {
	    if(tn == "img") {
		int[] a = {0};
		Image img = isrc.get(args, a);
		img.attrs = attrs;
		for(; a[0] < args.length; a[0]++) {
		    if("c".equals(args[a[0]])) {
			img.center = true;
			continue;
		    }
		    int p = args[a[0]].indexOf('=');
		    if(p < 0)
			continue;
		    String k = args[a[0]].substring(0, p), v = args[a[0]].substring(p + 1);
		    switch(k) {
		    case "h": {
			if(v.endsWith("ln")) {
			    img.lh = Double.parseDouble(v.substring(0, v.length() - 2));
			} else if(v.endsWith("bl")) {
			    img.bh = Double.parseDouble(v.substring(0, v.length() - 2));
			} else {
			    img.h = (int)Math.round(UI.scale(Double.parseDouble(v)));
			}
			break;
		    }
		    }
		}
		return(img);
	    } else {
		Map<Attribute, Object> na = new HashMap<Attribute, Object>(attrs);
		if(tn == "font") {
		    na.put(TextAttribute.FAMILY, args[0]);
		    if(args.length > 1)
			na.put(TextAttribute.SIZE, UI.scale(Float.parseFloat(args[1])));
		} else if(tn == "size") {
		    na.put(TextAttribute.SIZE, UI.scale(Float.parseFloat(args[0])));
		} else if(tn == "b") {
		    na.put(TextAttribute.WEIGHT, TextAttribute.WEIGHT_BOLD);
		} else if(tn == "i") {
		    na.put(TextAttribute.POSTURE, TextAttribute.POSTURE_OBLIQUE);
		} else if(tn == "u") {
		    na.put(TextAttribute.UNDERLINE, TextAttribute.UNDERLINE_ON);
		} else if(tn == "col") {
		    na.put(TextAttribute.FOREGROUND, a2col(args));
		} else if(tn == "bg") {
		    na.put(TextAttribute.BACKGROUND, a2col(args));
		}
		if(s.in.peek(true) != '{')
		    throw(new FormatException("Expected `{', got `" + (char)s.in.peek() + "'"));
		s.in.read();
		return(text(s, na));
	    }
	}

	protected Part tag(PState s, Map<? extends Attribute, ?> attrs) throws IOException {
	    s.in.peek(true);
	    String tn = name(s.in).intern();
	    String[] args;
	    if(s.in.peek(true) == '[') {
		s.in.read();
		StringBuilder buf = new StringBuilder();
		while(true) {
		    int c = s.in.peek();
		    if(c < 0) {
			throw(new FormatException("Unexpected end-of-input when reading tag arguments"));
		    } else if(c == ']') {
			s.in.read();
			break;
		    } else {
			buf.append((char)s.in.read());
		    }
		}
		args = buf.toString().split(",");
	    } else {
		args = new String[0];
	    }
	    return(tag(s, tn, args, attrs));
	}
	
	protected Part text(PState s, String text, Map<? extends Attribute, ?> attrs) throws IOException {
	    return(new TextPart(text, attrs));
	}

	protected Part text(PState s, Map<? extends Attribute, ?> attrs) throws IOException {
	    Part buf = new TextPart("");
	    StringBuilder tbuf = new StringBuilder();
	    while(true) {
		int c = s.in.read();
		if(c < 0) {
		    buf.append(text(s, tbuf.toString(), attrs));
		    break;
		} else if(c == '\n') {
		    buf.append(text(s, tbuf.toString(), attrs));
		    tbuf = new StringBuilder();
		    buf.append(new Newline(attrs));
		} else if(c == '}') {
		    buf.append(text(s, tbuf.toString(), attrs));
		    break;
		} else if(c == '$') {
		    c = s.in.peek();
		    if((c == '$') || (c == '{') || (c == '}')) {
			s.in.read();
			tbuf.append((char)c);
		    } else {
			buf.append(text(s, tbuf.toString(), attrs));
			tbuf = new StringBuilder();
			buf.append(tag(s, attrs));
		    }
		} else {
		    tbuf.append((char)c);
		}
	    }
	    return(buf);
	}

	protected Part parse(PState s, Map<? extends Attribute, ?> attrs) throws IOException {
	    Part res = text(s, attrs);
	    if(s.in.peek() >= 0)
		throw(new FormatException("Junk left after the end of input: " + (char)s.in.peek()));
	    return(res);
	}
	
	public Part parse(Reader in, Map<? extends Attribute, ?> extra) throws IOException {
	    PState s = new PState(new PeekReader(in));
	    if(extra != null) {
		Map<Attribute, Object> attrs = new HashMap<Attribute, Object>();
		attrs.putAll(defattrs);
		attrs.putAll(extra);
		return(parse(s, attrs));
	    } else {
		return(parse(s, defattrs));
	    }
	}

	public Part parse(Reader in) throws IOException {
	    return(parse(in, null));
	}
	
	public Part parse(String text, Map<? extends Attribute, ?> extra) {
	    try {
		return(parse(new StringReader(text), extra));
	    } catch(IOException e) {
		throw(new Error(e));
	    }
	}

	public Part parse(String text) {
	    return(parse(text, null));
	}
	
	public static String quote(String in) {
	    StringBuilder buf = new StringBuilder();
	    for(int i = 0; i < in.length(); i++) {
		char c = in.charAt(i);
		if((c == '$') || (c == '{') || (c == '}')) {
		    buf.append('$');
		    buf.append(c);
		} else {
		    buf.append(c);
		}
	    }
	    return(buf.toString());
	}

	public static String col2a(Color col) {
	    StringBuilder buf = new StringBuilder();
	    buf.append("$col[");
	    buf.append(col.getRed());
	    buf.append(",");
	    buf.append(col.getGreen());
	    buf.append(",");
	    buf.append(col.getBlue());
	    if(col.getAlpha() != 255) {
		buf.append(",");
		buf.append(col.getAlpha());
	    }
	    buf.append("]");
	    return(buf.toString());
	}
    }
    
    public static class Foundry {
	private Parser parser;
	private RState rs;
	public boolean aa = false;
	
	public Foundry(Parser parser) {
	    this.parser = parser;
	    BufferedImage junk = TexI.mkbuf(new Coord(10, 10));
	    Graphics2D g = junk.createGraphics();
	    rs = new RState(g.getFontRenderContext());
	}

	public Foundry(ImageSource isrc, Map<? extends Attribute, ?> defattrs) {
	    this(new Parser(isrc, defattrs));
	}
	
	public Foundry(Map<? extends Attribute, ?> defattrs) {
	    this(new Parser(defattrs));
	}
	
	public Foundry(ImageSource isrc, Object... attrs) {
	    this(new Parser(isrc, attrs));
	}
	
	public Foundry(Object... attrs) {
	    this(new Parser(attrs));
	}
	
	private static Map<? extends Attribute, ?> xlate(Font f, Color defcol) {
	    Map<Attribute, Object> attrs = new HashMap<Attribute, Object>();
	    attrs.put(TextAttribute.FONT, f);
	    attrs.put(TextAttribute.FOREGROUND, defcol);
	    return(attrs);
	}

	public Foundry(Font f, Color defcol) {
	    this(xlate(f, defcol));
	}

	public Foundry aa(boolean aa) {
	    this.aa = aa;
	    return(this);
	}

	private static void aline/* Hurrhurr, pun intended*/(List<Part> line, int y) {
	    int mb = 0;
	    for(Part p : line) {
		int cb = p.baseline();
		if(cb > mb) mb = cb;
	    }
	    for(Part p : line) {
		p.y = y + mb - p.baseline();
	    }
	}

	private static Part layout(Part fp, int w) {
	    List<Part> line = new LinkedList<Part>();
	    int x = 0, y = 0;
	    int mw = 0, lh = 0;
	    Part lp = null;
	    for(Part p = fp; p != null; p = p.next) {
		boolean lb = p instanceof Newline;
		int pw, ph;
		while(true) {
		    p.x = x;
		    pw = p.width();
		    ph = p.height();
		    if(w > 0) {
			if(p.x + pw > w) {
			    p = p.split(w - x);
			    if(lp == null)
				fp = p;
			    else
				lp.next = p;
			    lb = true;
			    continue;
			}
		    }
		    break;
		}
		lp = p;
		line.add(p);
		if(ph > lh) lh = ph;
		x += pw;
		if(x > mw) mw = x;
		if(lb) {
		    aline(line, y);
		    x = 0;
		    y += lh;
		    lh = 0;
		    line = new LinkedList<Part>();
		}
	    }
	    aline(line, y);
	    return(fp);
	}

	private static Coord bounds(Part fp) {
	    Coord sz = new Coord(0, 0);
	    for(Part p = fp; p != null; p = p.next) {
		int x = p.x + p.width();
		int y = p.y + p.height();
		if(x > sz.x) sz.x = x;
		if(y > sz.y) sz.y = y;
	    }
	    return(sz);
	}

	public RichText render(String text, int width, Object... extra) {
	    Map<? extends Attribute, ?> extram = null;
	    if(extra.length > 0) {
		extram = fillattrs(extra);
	    }
	    Part fp = parser.parse(text, extram);
	    fp.prepare(rs);
	    fp = layout(fp, width);
	    Coord sz = bounds(fp);
	    if(sz.x < 1) sz = sz.add(1, 0);
	    if(sz.y < 1) sz = sz.add(0, 1);
	    BufferedImage img = TexI.mkbuf(sz);
	    Graphics2D g = img.createGraphics();
	    if(aa)
		Utils.AA(g);
	    for(Part p = fp; p != null; p = p.next)
		p.render(g);
	    return(new RichText(text, img, fp));
	}
	
	public RichText render(String text) {
	    return(render(text, 0));
	}
    }
    
    public static RichText render(String text, int width, Object... extra) {
	return(stdf.render(text, width, extra));
    }
    
    public static void main(String[] args) throws Exception {
	String cmd = args[0].intern();
	if(cmd == "render") {
	    Map<Attribute, Object> a = new HashMap<Attribute, Object>(std.defattrs);
	    PosixArgs opt = PosixArgs.getopt(args, 1, "aw:f:s:");
	    boolean aa = false;
	    int width = 0;
	    for(char c : opt.parsed()) {
		if(c == 'a') {
		    aa = true;
		} else if(c == 'f') {
		    a.put(TextAttribute.FAMILY, opt.arg);
		} else if(c == 'w') {
		    width = Integer.parseInt(opt.arg);
		} else if(c == 's') {
		    a.put(TextAttribute.SIZE, UI.scale(Float.parseFloat(opt.arg)));
		}
	    }
	    Foundry fnd = new Foundry(a);
	    fnd.aa = aa;
	    RichText t = fnd.render(opt.rest[0], width);
	    try(OutputStream out = Files.newOutputStream(Utils.path(opt.rest[1]))) {
		javax.imageio.ImageIO.write(t.img, "PNG", out);
	    }
	} else if(cmd == "pagina") {
	    PosixArgs opt = PosixArgs.getopt(args, 1, "aw:");
	    boolean aa = false;
	    int width = 0;
	    for(char c : opt.parsed()) {
		if(c == 'a') {
		    aa = true;
		} else if(c == 'w') {
		    width = Integer.parseInt(opt.arg);
		}
	    }
	    Foundry fnd = new Foundry();
	    fnd.aa = aa;
	    Resource res = Resource.local().loadwait(opt.rest[0]);
	    Resource.Pagina p = res.layer(Resource.pagina);
	    if(p == null)
		throw(new Exception("No pagina in " + res + ", loaded from " + res.source));
	    RichText t = fnd.render(p.text, width);
	    try(OutputStream out = Files.newOutputStream(Utils.path(opt.rest[1]))) {
		javax.imageio.ImageIO.write(t.img, "PNG", out);
	    }
	}
    }
    
    public static String color(String text, Color c) {
	if(text == null) {return null;}
	return String.format("$col[%d,%d,%d]{%s}", c.getRed(), c.getGreen(), c.getBlue(), text);
    }
    
    public static String bgcolor(String text, Color c) {
	if(text == null) {return null;}
	return String.format("$bg[%d,%d,%d]{%s}", c.getRed(), c.getGreen(), c.getBlue(), text);
    }
}
