package haven;

import java.awt.*;

import static haven.Cal.*;

public class TimeWdg extends Widget {
    public static final double SERVER_TIME_RATIO = 3.29d;
    private final double updateinterval = 5;
    private double lastupdate = updateinterval + 0.01;
    private static final long dewyladysmantletimemin = 4 * 60 + 45;
    private static final long dewyladysmantletimemax = 7 * 60 + 15;

    public Tex timetex;
    public Tex seasontex;
    public Tex moontex;
    public Tex dewytex;

    public TimeWdg() {
	super(UI.scale(200, 20));
    }

    @Override
    public void tick(double dt) {
	super.tick(dt);

	if(!CFG.SHOW_TIME.get()) return;

	lastupdate += dt;
	if (lastupdate > updateinterval) {
	    updateTime();
	    lastupdate -= updateinterval;
	}
    }

    public void updateTime() {
	Astronomy a = ui.sess.glob.ast;
	int mp = (int)Math.round(a.mp * (double)moon.f.length) % moon.f.length;
	double nextseason = (1 - a.sp) * a.season().length / SERVER_TIME_RATIO;
	int curtimeM = a.hh * 60 + a.mm;
	timetex = RichText.renderstroked(String.format("%02d:%02d %s, day %d of %d", a.hh, a.mm, a.season(), a.scday + 1, a.season().length), Color.WHITE, Color.BLACK).tex();
	int d = (int) Math.floor(nextseason);
	int h = (int) (Math.floor(nextseason * 24) % 24);
	int m = (int) (Math.ceil(nextseason * 24 * 60) % 60);
	if (m == 60) { m = 0; h++; }
	if (h == 24) { h = 0; d++; }
	StringBuilder nextseasont = new StringBuilder();
	if (d > 0) {
	    nextseasont.append(d).append(" day");
	    if (d > 1) nextseasont.append("s");
	}
	nextseasont.append(String.format(" %02d:%02d RL left", h, m));
	seasontex = RichText.renderstroked(nextseasont.toString(), Color.WHITE, Color.BLACK).tex();
	moontex = RichText.renderstroked(String.format("Moon: %s", Astronomy.phase[mp]), Color.WHITE, Color.BLACK).tex();
	if (curtimeM >= dewyladysmantletimemin && curtimeM <= dewyladysmantletimemax) {
	    int dwh = (int) Math.floor((dewyladysmantletimemax - curtimeM) / 60.0 / SERVER_TIME_RATIO);
	    int dwm = (int) (((dewyladysmantletimemax - curtimeM) / SERVER_TIME_RATIO) % 60);
	    dewytex = RichText.renderstroked(String.format("Dewy Lady's Mantle (%02d:%02d RL left)", dwh, dwm), new Color(0, 200, 255), Color.BLACK).tex();
	} else {
	    if (CFG.ALWAYS_SHOW_DEWY_TIME.get()) {
		if (curtimeM > dewyladysmantletimemax) { curtimeM -=  24 * 60; }
		int dwh = (int) Math.floor((dewyladysmantletimemin - curtimeM) / 60.0 / SERVER_TIME_RATIO);
		int dwm = (int) (((dewyladysmantletimemin - curtimeM) / SERVER_TIME_RATIO) % 60);
		dewytex = RichText.renderstroked(String.format("Dewy Lady's Mantle in %02d:%02d RL", dwh, dwm), Color.WHITE, Color.BLACK).tex();
	    } else {
		dewytex = null;
	    }
	}
    }

    @Override
    public void draw(GOut g) {
	if (!CFG.SHOW_TIME.get()) return;
	int y = 0;
	if (timetex != null) {
	    g.image(timetex, new Coord(sz.x - timetex.sz().x, 0));
	    y += timetex.sz().y;
	}
	if (seasontex != null) {
	    g.image(seasontex, new Coord(sz.x - seasontex.sz().x, y));
	    y += seasontex.sz().y;
	}
	if (moontex != null) {
	    g.image(moontex, new Coord(sz.x - moontex.sz().x, y));
	    y += moontex.sz().y;
	}
	if (dewytex != null) {
	    g.image(dewytex, new Coord(sz.x - dewytex.sz().x, y));
	    y += dewytex.sz().y;
	}
	sz.y = y;
    }
}