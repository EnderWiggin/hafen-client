/* Preprocessed source code */
package haven.res.ui.pag.toggle;

import haven.*;
import haven.MenuGrid.*;

@haven.FromResource(name = "ui/pag/toggle", version = 1)
public class Fac implements PagButton.Factory {
    public static Message sdt(Pagina pag) {
	try {
	    return(pag.data());
	} catch(NoSuchMethodError e) {
	    return(Message.nil);
	}
    }

    public PagButton make(Pagina info) {
	Message sdt = sdt(info);
	return(new Toggle(info, sdt.eom() || (sdt.uint8() != 0)));
    }
}
