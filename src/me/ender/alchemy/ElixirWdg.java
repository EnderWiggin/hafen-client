package me.ender.alchemy;

import haven.*;

import java.io.IOException;
import java.util.List;

public class ElixirWdg extends Widget {
    private Elixir elixir;
    private Tex image = null;
    private final Coord TOOLTIP_C;
    private final TextEntry name;
    private final Button open, delete;

    public ElixirWdg(int w, int h) {
	super();

	name = add(new TextEntry(w, "") {
	    @Override
	    public void activate(String text) {
		AlchemyData.rename(elixir, text, ui.gui.genus);
		name.settext(elixir.name());
		//TODO: find a way to remove focus from text field
	    }
	});
	Coord p = name.pos("bl");

	open = add(new Button(UI.scale(55), "Open", false, this::open), p.addy(AlchemyWnd.PAD));
	open.settip("Open in Yoda's Alchemy Graph site");
	//TODO: add remove button

	p = open.pos("ur");
	delete = add(new Button(UI.scale(55), "Delete", false, this::delete), p.addx(AlchemyWnd.PAD));
	p = open.pos("bl");

	TOOLTIP_C = p.add(AlchemyWnd.PAD, AlchemyWnd.PAD);
	sz = Coord.of(w, h);

	update(null);
    }

    private void open() {
	if(elixir != null) {
	    try {
		ui.wnd.toolkit().browse(Utils.uri(elixir.toAlchemyUrl()));
	    } catch (IOException ignored) {}
	}
    }

    private void delete() {
	//TODO: add confirmation popup?
	AlchemyData.remove(elixir, ui.gui.genus);
    }

    public void update(Elixir elixir) {
	this.elixir = elixir;
	if(image != null) {image.dispose();}
	image = null;

	if(elixir == null) {
	    name.hide();
	    name.settext("");
	    open.hide();
	    delete.hide();
	} else {
	    name.show();
	    name.settext(elixir.name());
	    open.show();
	    delete.show();
	}
    }

    private Tex image() {
	if(elixir == null) {return null;}

	try {
	    List<ItemInfo> info = Effect.elixirInfo(elixir.effects);
	    info.addAll(elixir.recipe.info(ui));

	    if(info.isEmpty()) {return null;}
	    return new TexI(ItemInfo.longtip(info));
	} catch (Loading ignore) {}
	return null;
    }

    @Override
    public void draw(GOut g) {
	g.chcolor(AlchemyWnd.BGCOLOR);
	g.frect(Coord.z, sz);
	g.chcolor();

	if(elixir != null && image == null) {
	    image = image();
	}

	if(image != null) {
	    g.image(image, TOOLTIP_C);
	}

	super.draw(g);
    }
}
