package me.ender.ui;

import auto.InvHelper;
import haven.*;

import java.awt.*;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class DrinkMeter extends Widget {
    private static final Tex FRAME = Resource.loadtex("gfx/hud/meter/custom/drinks");
    private static final Color BG = new Color(40, 51, 46);
    private static final Color TEA = new Color(147, 110, 68);
    private static final Color WATER = new Color(82, 116, 188);

    private float water = 17.5f;
    private float tea = 5;
    private float max = 30;

    public DrinkMeter() {
	super(IMeter.fsz);
    }

    @Override
    public void draw(GOut g) {
	Coord isz = IMeter.msz;
	Coord off = IMeter.off;
	g.chcolor(BG);
	g.frect(off, isz);
	float a;
	if(max > 0) {
	    if(tea > 0) {
		g.chcolor(TEA);
		a = (tea + water) / max;
		g.frect(off, new Coord(Math.round(isz.x * a), isz.y));
	    }
	    if(water > 0) {
		g.chcolor(WATER);
		a = water / max;
		g.frect(off, new Coord(Math.round(isz.x * a), isz.y));
	    }
	}
	g.chcolor();
	g.image(FRAME, Coord.z);
    }

    private String stip = null;
    private Tex tip = null;

    @Override
    public Object tooltip(Coord c, Widget prev) {
	if(max <= 0) {return null;}

	String tt = String.format("Capacity: %.2f litres", max);
	if(water > 0) {
	    tt = String.format("%.2f l of Water\n", water) + tt;
	}
	if(tea > 0) {
	    tt = String.format("%.2f l of Tea\n", tea) + tt;
	}

	if(!Objects.equals(tt, stip)) {
	    stip = tt;
	    if(tip != null) {tip.dispose();}
	    tip = RichText.render(tt, 0).tex();
	}

	return tip;
    }

    double wait = 0;

    @Override
    public void tick(double dt) {
	super.tick(dt);
	wait -= dt;
	if(wait <= 0) {
	    wait = process() ? 0.25 : 1;
	}
    }

    private boolean process() {
	if(ui == null || ui.gui == null) {return false;}
	GameUI gui = ui.gui;

	max = water = tea = 0f;

	List<WItem> items = Stream.of(
		InvHelper.HANDS(gui).get().stream().filter(InvHelper::isBucket),
		InvHelper.POUCHES(gui).get().stream().filter(InvHelper::isDrinkContainer),
		InvHelper.INVENTORY(gui).get().stream().filter(InvHelper::isDrinkContainer),
		InvHelper.BELT(gui).get().stream().filter(InvHelper::isDrinkContainer)
	    ).flatMap(x -> x)
	    .collect(Collectors.toList());


	for (WItem item : items) {
	    ItemData.Content content = item.contains.get();
	    if(content.is(ItemData.WATER)) {
		water += content.count;
	    } else if(content.is(ItemData.TEA)) {
		tea += content.count;
	    } else if(!content.empty()) {
		continue;
	    }
	    //drinks are 100 units per litre
	    max += ItemData.getMaxCapacity(item) / 100f;
	}

	return true;
    }

    public static void add(UI ui) {
	if(ui.gui == null) {return;}
	ui.gui.addcmeter(new DrinkMeter());
    }

    public static void rem(UI ui) {
	if(ui.gui == null) {return;}
	ui.gui.delcmeter(DrinkMeter.class);
    }
}
