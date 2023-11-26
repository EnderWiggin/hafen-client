package haven;

import java.awt.image.BufferedImage;
import java.util.*;
import java.util.function.Function;

public class CharacterInfo {

    public final Constipation constipation = new Constipation();

    public static class Constipation {
	public final List<Data> els = new ArrayList<Data>();
	private Integer[] order = {};
    
	public void update(ResData t, double a) {
	    prev: {
		for(Iterator<Data> i = els.iterator(); i.hasNext();) {
		    Data el = i.next();
		    if(!Utils.eq(el.rd, t))
			continue;
		    if(a == 1.0)
			i.remove();
		    else
			el.update(a);
		    break prev;
		}
		els.add(new Data(t, a));
	    }
	    order();
	}
    
	private void order() {
	    int n = els.size();
	    order = new Integer[n];
	    for(int i = 0; i < n; i++)
		order[i] = i;
	    Arrays.sort(order, (a, b) -> (ecmp.compare(els.get(a), els.get(b))));
	}
    
	private static final Comparator<Data> ecmp = (a, b) -> {
	    if(a.value < b.value)
		return(-1);
	    else if(a.value > b.value)
		return(1);
	    return(0);
	};

	public Data get(int i) {
	    return els.size() > i ? els.get(i) : null;
	}

	public static class Data {
	    private final Map<Class, BufferedImage> renders = new HashMap<>();
	    public final Indir<Resource> res;
	    private ResData rd;
	    public double value;

	    public Data(ResData rd, double value) {
		this.rd = rd;
		this.res = rd.res;
		this.value = value;
	    }

	    public void update(double a) {
		value = a;
		renders.clear();
	    }

	    private BufferedImage render(Class type, Function<Data, BufferedImage> renderer) {
		if(!renders.containsKey(type)) {
		    renders.put(type, renderer.apply(this));
		}
		return renders.get(type);
	    }
	}

	private final Map<Class, Function<Data, BufferedImage>> renderers = new HashMap<>();

	public void addRenderer(Class type, Function<Data, BufferedImage> renderer) {
	    renderers.put(type, renderer);
	}

	public boolean hasRenderer(Class type) {
	    return renderers.containsKey(type);
	}

	public BufferedImage render(Class type, Data data) {
	    try {
		return renderers.containsKey(type) ? data.render(type, renderers.get(type)) : null;
	    } catch (Loading ignored) {}
	    return null;
	}
    }
}
