package haven;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.*;

public class AccountList extends Widget {
    public static final String ACCOUNTS_JSON = "accounts.json";
    public static final Map<String, String> accountmap = new HashMap<>();
    private static final Coord SZ = UI.scale(230, 30);
    private static final Comparator<Account> accountComparator = Comparator.comparing(o -> o.name);
    
    static {
	AccountList.loadAccounts();
    }
    
    public int height, y;
    public final List<Account> accounts = new ArrayList<>();
    
    static void loadAccounts() {
	String json = Config.loadFile(ACCOUNTS_JSON);
	if(json != null) {
	    try {
		Gson gson = (new GsonBuilder()).create();
		Type collectionType = new TypeToken<HashMap<String, String>>() {
		}.getType();
		Map<String, String> tmp = gson.fromJson(json, collectionType);
		accountmap.putAll(tmp);
	    } catch (Exception ignored) {
	    }
	}
    }

    public static void storeAccount(String name, String token) {
	synchronized(accountmap) {
	    accountmap.put(name, token);
	}
	saveAccounts();
    }

    public static void removeAccount(String name) {
	synchronized(accountmap) {
	    accountmap.remove(name);
	}
	saveAccounts();
    }

    public static void saveAccounts() {
	synchronized(accountmap) {
	    Gson gson = (new GsonBuilder()).setPrettyPrinting().create();
	    Config.saveFile(ACCOUNTS_JSON, gson.toJson(accountmap));
	}
    }
    
    //TODO: find how to incorporate hostname without breaking stuff
    public static byte[] getToken(String user, String hostname) {
	synchronized (accountmap) {
	    String token = accountmap.get(user);
	    if(token == null){
		return null;
	    } else {
		return Utils.hex2byte(token);
	    }
	}
    }
    
    public static void setToken(String user, String hostname, byte[] token) {
	if(token == null) {
	    removeToken(user, hostname);
	} else {
	    storeAccount(user, Utils.byte2hex(token));
	}
    }
    
    public static void removeToken(String user, String hostname) {
	removeAccount(user);
    }
    
    public static class Account {
	public String name, token;
	Button plb, del;

	public Account(String name, String token) {
	    this.name = name;
	    this.token = token;
	}
    }

    public AccountList(int height) {
	super();
	this.height = height;
	this.sz = new Coord(SZ.x, SZ.y * height);
	y = 0;
    
	for (Map.Entry<String, String> entry : accountmap.entrySet()) {
	    add(entry.getKey(), entry.getValue());
	}
	accounts.sort(accountComparator);
    }

    public void scroll(int amount) {
	y += amount;
	synchronized(accounts) {
	    if(y > accounts.size() - height)
		y = accounts.size() - height;
	}
	if(y < 0)
	    y = 0;
    }
    
    public void draw(GOut g) {
	Coord step = UI.scale(5, 5);
	Coord cc = UI.scale(5, 5);
	synchronized (accounts) {
	    for (Account account : accounts) {
		account.plb.hide();
		account.del.hide();
	    }
	    for (int i = 0; (i < height) && (i + this.y < accounts.size()); i++) {
		Account account = accounts.get(i + this.y);
		account.plb.show();
		account.plb.c = cc;
		account.del.show();
		account.del.c = cc.add(account.plb.sz.x + step.x, step.y);
		cc = cc.add(0, SZ.y);
	    }
	}
	super.draw(g);
    }

    public boolean mousewheel(Coord c, int amount) {
	scroll(amount);
	return (true);
    }

    public void wdgmsg(Widget sender, String msg, Object... args) {
	if(sender instanceof Button) {
	    synchronized(accounts) {
		for(Account account : accounts) {
		    if(sender == account.plb) {
			super.wdgmsg("account", account.name, account.token);
			break;
		    } else if(sender == account.del) {
			remove(account);
			break;
		    }
		}
	    }
	} else {
	    super.wdgmsg(sender, msg, args);
	}
    }

    public void add(String name, String token) {
	Account c = new Account(name, token);
	c.plb = add(new Button(UI.scale(200), name) {
	    @Override
	    protected boolean i10n() { return false; }
	});
	c.plb.hide();
	c.del = add(new Button(UI.scale(20), "X") {
	    @Override
	    protected boolean i10n() { return false; }
	});
	c.del.hide();
	synchronized (accounts) {
	    accounts.add(c);
	}
    }

    public void remove(Account account) {
	synchronized(accounts) {
	    accounts.remove(account);
	}
	scroll(0);
	removeAccount(account.name);
	ui.destroy(account.plb);
	ui.destroy(account.del);
    }
}