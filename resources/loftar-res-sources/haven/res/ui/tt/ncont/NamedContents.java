/* Preprocessed source code */
package haven.res.ui.tt.ncont;

import haven.*;
import java.util.*;
import java.awt.Graphics;
import java.awt.image.BufferedImage;

/* >tt: NamedContents */
@haven.FromResource(name = "ui/tt/ncont", version = 2)
public class NamedContents extends ItemInfo.Tip {
    public final List<ItemInfo> sub;
    public final Text.Line ch;

    public NamedContents(Owner owner, String name, List<ItemInfo> sub) {
	super(owner);
	ch = Text.render(name + ":");
	this.sub = sub;
    }

    public static ItemInfo mkinfo(Owner owner, Object... args) {
	String name = (String)args[1];
	List<ItemInfo> sub = buildinfo(owner, (Object[])args[2]);
	return(new NamedContents(owner, name, sub));
    }

    public BufferedImage tipimg() {
	BufferedImage stip = longtip(sub);
	BufferedImage img = TexI.mkbuf(Coord.of(stip.getWidth(), stip.getHeight()).add(UI.scale(10, 15)));
	Graphics g = img.getGraphics();
	g.drawImage(ch.img, 0, 0, null);
	g.drawImage(stip, UI.scale(10), UI.scale(15), null);
	g.dispose();
	return(img);
    }
}
