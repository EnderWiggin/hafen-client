/* Preprocessed source code */
package haven.res.ui.alchbook;

import java.util.*;
import haven.*;
import haven.MenuGrid.*;
import java.awt.Color;
import java.awt.image.BufferedImage;
import static haven.PType.*;

@FromResource(name = "ui/alchbook", version = 1)
public class Input {
    public final ItemSpec type;
    public final List<Input> sub = new ArrayList<>();

    public Input(ItemSpec type) {
	this.type = type;
    }

    public boolean equals(Input that) {
	return(this.type.res.equals(that.type.res) && this.sub.equals(that.sub));
    }
    public boolean equals(Object x) {
	return((x instanceof Input) && equals((Input)x));
    }
    public int hashCode() {
	return(Objects.hash(sub, type.res));
    }

    public String toString() {
	if(sub.isEmpty())
	    return(type.res.toString());
	return(type.res.toString() + "(" + sub.stream().map(Object::toString).collect(java.util.stream.Collectors.joining(" + ")) + ")");
    }
}
