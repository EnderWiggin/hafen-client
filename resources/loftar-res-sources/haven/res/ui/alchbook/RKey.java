/* Preprocessed source code */
package haven.res.ui.alchbook;

import java.util.*;
import java.util.function.*;
import haven.*;
import haven.MenuGrid.*;
import java.awt.Color;
import java.awt.image.BufferedImage;
import static haven.PType.*;

@haven.FromResource(name = "ui/alchbook", version = 3)
public class RKey {
    public final Recipe rcp;

    public RKey(Recipe rcp) {
	this.rcp = rcp;
    }

    public boolean equals(RKey that) {
	return(this.rcp.rcp.res.equals(that.rcp.rcp.res) && this.rcp.inputs.equals(that.rcp.inputs));
    }
    public boolean equals(Object x) {
	return((x instanceof RKey) && equals((RKey)x));
    }
    public int hashCode() {
	return(Objects.hash(rcp.rcp.res, rcp.inputs));
    }
}
