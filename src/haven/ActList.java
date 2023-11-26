package haven;

import java.util.*;

public class ActList extends Listbox<ActList.ActItem> {
    private static final Text.Foundry font = new Text.Foundry(Text.sans, 15).aa(true);
    private final List<ActItem> items = new ArrayList<ActItem>();
    private final Map<MenuGrid.Pagina, ActItem> map = new HashMap<MenuGrid.Pagina, ActItem>();

    public ActList(int w, int h) {
	super(w, h, font.height() + 2);
    }

    @Override
    protected void itemactivate(ActItem item) {}

    public void add(MenuGrid.Pagina pagina) {
	ActItem item = new ActItem(pagina);
	map.put(pagina, item);
	items.add(item);
    }

    public void remove(MenuGrid.Pagina pagina) {
	ActItem item = map.remove(pagina);
	if(item != null)
	    items.remove(item);
    }

    public void clear() {
	map.clear();
	items.clear();
    }

    public void sort(Comparator<ActItem> comparator) {
	Collections.sort(items, comparator);
    }

    @Override
    protected ActItem listitem(int i) {
	return items.get(i);
    }

    @Override
    protected int listitems() {
	return items.size();
    }

    @Override
    protected void drawitem(GOut g, ActItem item, int i) {
	g.image(item.icon, Coord.z);
	g.aimage(item.name.tex(), new Coord(itemh + 5, itemh / 2), 0, 0.5);
    }

    public class ActItem {
	public final MenuGrid.Pagina pagina;
	public final Text name;
	public final Tex icon;

	public ActItem(MenuGrid.Pagina pagina) {
	    this.pagina = pagina;
	    this.name = font.render(this.pagina.act().name);
	    this.icon = new TexI(PUtils.convolvedown(pagina.res.get().layer(Resource.imgc).img, new Coord(itemh, itemh), CharWnd.iconfilter));
	}
    }
}