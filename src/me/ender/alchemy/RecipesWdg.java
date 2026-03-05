package me.ender.alchemy;

import haven.*;

import java.awt.*;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

import static me.ender.alchemy.AlchemyWnd.*;

class RecipesWdg extends AlchemyWdg {
    private final ElixirWdg elixir;


    RecipesWdg() {
	ElixirList list = new ElixirList(this::onSelectionChanged);
	Coord p = add(list, PAD, PAD).pos("ur");

	elixir = add(new ElixirWdg(CONTENT_W, list.sz.y), p.addx(GAP));

	pack();
    }

    private void onSelectionChanged(Elixir elixir) {
	this.elixir.update(elixir);
    }

    private static class ElixirList extends FilteredListBox<Elixir> {
	private final Map<String, RichText> names = new HashMap<>();
	private final Consumer<Elixir> onChanged;
	private boolean dirty = true;

	public ElixirList(Consumer<Elixir> onChanged) {
	    super(LIST_W, ITEMS, ITEM_H);
	    bgcolor = BGCOLOR;
	    this.onChanged = onChanged;
	    listen(AlchemyData.ELIXIRS_UPDATED, this::onElixirsUpdated);
	}

	@Override
	public boolean mousedown(MouseDownEvent ev) {
	    parent.setfocus(this);
	    return super.mousedown(ev);
	}

	private void onElixirsUpdated() {
	    Elixir was = sel;
	    update();
	    change(was);
	}

	@Override
	public void changed(Elixir item, int index) {
	    onChanged.accept(item);
	}

	private void update() {
	    if(tvisible()) {
		setItems(AlchemyData.elixirs(ui.gui.genus));
		dirty = false;
	    } else {
		dirty = true;
	    }
	}

	@Override
	public void draw(GOut g, boolean strict) {
	    if(dirty) {update();}
	    super.draw(g, strict);
	}

	private Text text(Elixir elixir) {
	    String name = elixir.name();
	    RichText text = names.getOrDefault(name, null);
	    if(text != null) {return text;}

	    try {
		text= RichText.render(String.format("$img[%s,h=16,c] %s", elixir.recipe.res, name), CONTENT_W);
	    } catch (Loading e) {
		return Text.renderf(Color.WHITE, name);
	    }
	    names.put(name, text);
	    return text;
	}

	@Override
	public void dispose() {
	    names.values().forEach(Text::dispose);
	    names.clear();
	    super.dispose();
	}

	@Override
	protected boolean match(Elixir item, String text) {
	    if(text == null || text.isEmpty()) {return true;}

	    final String filter = text.toLowerCase();
	    return item.name().toLowerCase().contains(filter)
		|| item.effects.stream().anyMatch(e -> e.matches(filter))
		|| item.recipe.matches(filter);
	}

	@Override
	public void draw(GOut g) {
	    super.draw(g);
	}

	@Override
	protected void drawitem(GOut g, Elixir item, int i) {
	    g.image(text(item).tex(), Coord.z);
	}
    }
}
