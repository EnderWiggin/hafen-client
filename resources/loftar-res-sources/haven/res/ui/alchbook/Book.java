/* Preprocessed source code */
package haven.res.ui.alchbook;

import java.util.*;
import java.util.function.*;
import haven.*;
import haven.MenuGrid.*;
import java.awt.Color;
import java.awt.image.BufferedImage;
import static haven.PType.*;

@haven.FromResource(name = "ui/alchbook", version = 4)
public class Book extends Widget {
    public static final int HEIGHT = UI.scale(24);
    public final RecipeList rl;
    public final EffectList el;
    public final Widget rlc, elc;

    public Book() {
	(rlc = add(filterdsp(rl = new RecipeList()), Coord.z)).show(true);
	(elc = add(filterdsp(el = new EffectList()), Coord.z)).show(false);
	prev = add(new TabButton(0, 1).action(() -> {rlc.show(); elc.hide();}).settip("Elixirs"),  rlc.pos("bl").adds(0, 10));
	prev = add(new TabButton(2, 3).action(() -> {rlc.hide(); elc.show();}).settip("Ingredients"), prev.pos("ur").adds(10, 0));
	pack();
    }

    public static <T> Widget filterdsp(FilterDisplay.Filtered<T> fc) {
	Widget wdg = (Widget)fc;
	Widget cnt = new Widget();
	cnt.add(wdg);
	cnt.add(new FilterDisplay<T>(fc), wdg.pos("bl"));
	cnt.pack();
	return(cnt);
    }

    /*
    public static Book mkwidget(UI ui, Object... args) {
	return(new Book());
    }
    */

    public void uimsg(String name, Object... args) {
	if(name == "add") {
	    Recipe rcp = Recipe.parse(this, args);
	    rl.add(rcp);
	} else if(name == "inp") {
	    KnownEffects ik = KnownEffects.parse(this, args);
	    el.add(ik);
	} else if(name == "addto") {
	    MenuGrid menu = (MenuGrid)ui.getwidget(Utils.iv(args[0]));
	    Pagina pag = menu.paginafor(args[1], null);
	    BookButton btn = (BookButton)pag.button();
	    btn.register(this);
	} else {
	    super.uimsg(name, args);
	}
    }

    public static BufferedImage rowscaleh(BufferedImage img, int h) {
	return(PUtils.uiscale(img, Coord.of((img.getWidth() * h) / img.getHeight(), h)));
    }
    public static BufferedImage rowscaleh(BufferedImage img) {
	return(rowscaleh(img, HEIGHT));
    }

    public static BufferedImage rowscalea(BufferedImage img, int sz) {
	Coord is = PUtils.imgsz(img);
	return(PUtils.uiscale(img, is.mul(sz).div(is.max())));
    }
    public static BufferedImage rowscalea(BufferedImage img) {
	return(rowscalea(img, HEIGHT));
    }
}
