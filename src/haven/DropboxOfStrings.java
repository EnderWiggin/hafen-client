package haven;

import rx.functions.Action2;

import java.util.List;

public class DropboxOfStrings extends Dropbox<String> {
    List<String> data;
    private Action2<Integer, String> callback;

    public DropboxOfStrings(int w, int listh, int itemh) {
	super(w, listh, itemh);
    }

    public DropboxOfStrings setData(List<String> data) {
	this.data = data;
	return this;
    }

    public DropboxOfStrings setChangedCallback(Action2<Integer, String> callback) {
	this.callback = callback;
	return this;
    }

    @Override
    protected String listitem(int i) {
	return (data != null && data.size() > i && i >= 0) ? data.get(i) : null;
    }

    @Override
    protected int listitems() {
	return data != null ? data.size() : 0;
    }

    @Override
    protected void drawitem(GOut g, String item, int i) {
	g.text(item, Coord.z);
    }

    @Override
    public void change(String item) {
	super.change(item);
	changed(find(item), item);
    }

    @Override
    public void change(int index) {
	super.change(index);
	changed(index, index >= 0 ? listitem(index) : null);
    }

    private void changed(int index, String item) {
	if(callback != null) {callback.call(index, item);}
    }
}
