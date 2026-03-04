/* Preprocessed source code */
package haven.res.ui.tt.slots;

import haven.*;
import static haven.PUtils.*;
import java.awt.image.*;
import java.awt.Graphics;
import java.awt.Font;
import java.awt.Color;
import java.util.*;

/* >tt: Fac */
@haven.FromResource(name = "ui/tt/slots", version = 34)
public class Fac implements ItemInfo.InfoFactory {
    public ItemInfo build(ItemInfo.Owner owner, ItemInfo.Raw rawi, Object... args) {
	Resource.Resolver rr = owner.context(Resource.Resolver.class);
	int a = 1;
	double pmin = Utils.dv(args[a++]);
	double pmax = Utils.dv(args[a++]);
	List<Resource> attrs = new LinkedList<Resource>();
	/* XXX? Make attrs a sublist? */
	while(args[a] != null)
	    attrs.add(rr.getresv(args[a++]).get());
	a++;
	int uses = Utils.iv(args[a++]);
	int used = Utils.iv(args[a++]);
	int left = uses - used;
	ISlots ret = new ISlots(owner, left, pmin, pmax, attrs.toArray(new Resource[0]));
	while(a < args.length) {
	    Indir<Resource> res = rr.getresv(args[a++]);
	    Message sdt = Message.nil;
	    if(args[a] instanceof byte[])
		sdt = new MessageBuf((byte[])args[a++]);
	    Object[] raw = (Object[])args[a++];
	    ret.s.add(ret.new SItem(new ResData(res, sdt), raw));
	}
	return(ret);
    }
}
