/* Preprocessed source code */
package haven.res.ui.alchbook;

import java.util.*;
import haven.*;
import haven.MenuGrid.*;
import java.awt.Color;
import java.awt.image.BufferedImage;
import static haven.PType.*;

@FromResource(name = "ui/alchbook", version = 1)
public class Formula extends Widget {
    public static final Text.Foundry fnd = new Text.Foundry(Text.Foundry.fontpxsz(Text.sans, Book.HEIGHT), Color.WHITE).aa(true);
    public static final Text plus = fnd.render("+");
    public static final Text lp = fnd.render("(");
    public static final Text rp = fnd.render(")");

    public Formula(List<Input> inputs) {
	Coord pos = Coord.z;
	boolean f = true;
	for(Input inp : inputs) {
	    if(!f)
		pos = add(new Img(plus.tex()), pos).pos("ur");
	    pos = add(new Icon(Book.HEIGHT, inp.type), pos).pos("ur");
	    if(!inp.sub.isEmpty()) {
		pos = add(new Img(lp.tex()), pos).pos("ur");
		pos = add(new Formula(inp.sub), pos).pos("ur");
		pos = add(new Img(rp.tex()), pos).pos("ur");
	    }
	    f = false;
	}
	pack();
    }
}
