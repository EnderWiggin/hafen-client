import haven.ItemInfo;
import haven.L10N;

public class CustName
    implements ItemInfo.InfoFactory {
    @Override
    public ItemInfo build(ItemInfo.Owner owner, ItemInfo.Raw raw, Object... args) {
	String name = (String) args[1];
	return new ItemInfo.Name(owner, L10N.label(name), name);
    }
}