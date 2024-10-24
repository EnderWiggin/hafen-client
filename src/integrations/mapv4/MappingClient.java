package integrations.mapv4;

import haven.*;
import haven.MCache.LoadingMap;
import me.ender.minimap.*;
import org.json.JSONArray;
import org.json.JSONObject;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.*;
import java.lang.ref.WeakReference;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;


/**
 * @author Vendan
 */
public class MappingClient {
    
    private ExecutorService gridsUploader = Executors.newSingleThreadExecutor();
    private ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(3);
    
    private static volatile MappingClient INSTANCE = null;
    
    private int spamPreventionVal = 3;
    private int spamCount = 0;
    private Glob glob;
    
    private boolean checkWisp = true;
    private boolean isWisp = false;
    public static void init(Glob glob) {
	synchronized (MappingClient.class) {
	    if(INSTANCE == null) {
		INSTANCE = new MappingClient(glob);
	    } else {
		throw new IllegalStateException("MappingClient can only be initialized once!");
	    }
	}
    }
    
    public static void destroy() {
	synchronized (MappingClient.class) {
	    if(INSTANCE != null) {
	        INSTANCE.gridsUploader.shutdown();
	        INSTANCE.scheduler.shutdown();
		INSTANCE = null;
	    }
	}
    }
    
    public static boolean initialized() {return INSTANCE != null;}
    
    public static MappingClient getInstance() {
	synchronized (MappingClient.class) {
	    if(INSTANCE == null) {
		throw new IllegalStateException("MappingClient should be initialized first!");
	    }
	    return INSTANCE;
	}
    }
    
    private boolean trackingEnabled;
    
    /***
     * Enable tracking for this execution.  Must be called each time the client is started.
     * @param enabled
     */
    public void EnableTracking(boolean enabled) {
	trackingEnabled = enabled;
    }
    
    private boolean gridEnabled;
    
    /***
     * Enable grid data/image upload for this execution.  Must be called each time the client is started.
     * @param enabled
     */
    public void EnableGridUploads(boolean enabled) {
	gridEnabled = enabled;
    }
    
    private PositionUpdates pu = new PositionUpdates();
    
    private MappingClient(Glob glob) {
	this.glob = glob;
	scheduler.scheduleAtFixedRate(pu, 2L, 2L, TimeUnit.SECONDS);
    }
    
    private String endpoint;
    
    /***
     * Set mapping server endpoint.  Must be called each time the client is started.  Takes effect immediately.
     * @param endpoint
     */
    public void SetEndpoint(String endpoint) {
	this.endpoint = endpoint;
    }
    
    private String playerName;
    
    /***
     * Set the player name.  Typically called from Charlist.wdgmsg
     * @param name
     */
    public void SetPlayerName(String name) {
	playerName = name;
    }
    
    /***
     * Checks that the endpoint is functional and matches the version of this mapping client.
     * @return
     */
    public boolean CheckEndpoint() {
	try {
	    HttpURLConnection connection =
		(HttpURLConnection) new URL(endpoint + "/checkVersion?version=4").openConnection();
	    connection.setRequestMethod("GET");
	    return connection.getResponseCode() == 200;
	} catch (Exception ex) {
	    return false;
	}
    }
    
    /***
     * Track a gob at a location.  Typically called in Gob.move
     * @param id
     * @param coordinates
     */
    public void Track(long id, Coord2d coordinates) {
	try {
	    MCache.Grid g = glob.map.getgrid(toGC(coordinates));
	    pu.Track(id, coordinates, g.id);
	} catch (Exception ex) {}
    }
    
    private Coord lastGC = null;
    
    /***
     * Called when entering a new grid
     * @param gc Grid coordinates
     */
    public void EnterGrid(Coord gc) {
	lastGC = gc;
	scheduler.execute(new GenerateGridUpdateTask(gc));
    }
    
    /***
     * Called as you move around, automatically calculates if you have entered a new grid and calls EnterGrid accordingly.
     * @param gob Player gob, c Normal coordinates
     */
    public void CheckGridCoord(Gob gob, Coord2d c) {
	Coord gc = toGC(c);
	if(lastGC == null || !gc.equals(lastGC)) {
	    if (checkWisp) {
		checkWisp = false;
		try {
		    String name = gob.getres().name;
		    isWisp = name.equals("gfx/borka/wisp");
		} catch (Exception e) {
		    checkWisp = true;
		    lastGC = gc;
		    return;
		}
	    }
	    if (isWisp) {
		if (lastGC != null && lastGC.dist(gc) > 5) {
		    isWisp = false;
		} else {
		    lastGC = gc;
		    return;
		}
	    }
	    EnterGrid(gc);
	}
    }
    
    private final Map<Long, MapRef> cache = new HashMap<Long, MapRef>();
    
    /***
     * Gets a MapRef (mapid, coordinate pair) for the players current location
     * @return Current grid MapRef
     */
    public MapRef GetMapRef() {
	try {
	    Gob player = glob.sess.ui.gui.map.player();
	    Coord gc = toGC(player.rc);
	    synchronized (cache) {
		long id = glob.map.getgrid(gc).id;
		MapRef mapRef = cache.get(id);
		if(mapRef == null) {
		    scheduler.execute(new Locate(id));
		}
		return mapRef;
	    }
	} catch (Exception e) {}
	return null;
    }
    
    /***
     * Given a mapref, opens the map to the corresponding location
     * @param mapRef
     */
    public void OpenMap(MapRef mapRef) {
	try {
	    if(mapRef == null) {return;}
	    WebBrowser.self.show(new URL(
		String.format(endpoint + "/#/grid/%d/%d/%d/6", mapRef.mapID, mapRef.gc.x, mapRef.gc.y)));
	} catch (Exception ex) {}
    }
    
    private class Locate implements Runnable {
	long gridID;
	
	Locate(long gridID) {
	    this.gridID = gridID;
	}
	
	@Override
	public void run() {
	    try {
		final HttpURLConnection connection =
		    (HttpURLConnection) new URL(endpoint + "/locate?gridID=" + gridID).openConnection();
		try (BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
		    String resp = reader.lines().collect(Collectors.joining());
		    String[] parts = resp.split(";");
		    if(parts.length == 3) {
			MapRef mr = new MapRef(Integer.valueOf(parts[0]), new Coord(Integer.valueOf(parts[1]), Integer.valueOf(parts[2])));
			synchronized (cache) {
			    cache.put(gridID, mr);
			}
		    }
		    
		} finally {
		    connection.disconnect();
		}
		
	    } catch (final Exception ex) { }
	}
    }
    
    /***
     * Process a mapfile to extract markers to upload
     * @param mapfile
     * @param uploadCheck
     */
    public void ProcessMap(MapFile mapfile, Predicate<Marker> uploadCheck) {
	scheduler.schedule(new ExtractMapper(mapfile, uploadCheck), 5, TimeUnit.SECONDS);
	
    }
    
    private class ExtractMapper implements Runnable {
	MapFile mapfile;
	Predicate<Marker> uploadCheck;
	int retries = 5;
	
	ExtractMapper(MapFile mapfile, Predicate<Marker> uploadCheck) {
	    this.mapfile = mapfile;
	    this.uploadCheck = uploadCheck;
	}
	
	@Override
	public void run() {
	    if(mapfile.lock.readLock().tryLock()) {
		List<MarkerData> markers = mapfile.markers.stream().filter(uploadCheck).map(m -> {
		    Coord mgc = new Coord(Math.floorDiv(m.tc.x, 100), Math.floorDiv(m.tc.y, 100));
		    Indir<MapFile.Grid> indirGrid = mapfile.segments.get(m.seg).grid(mgc);
		    return new MarkerData(m, indirGrid);
		}).collect(Collectors.toList());
		System.out.println("collected " + markers.size() + " markers");
		mapfile.lock.readLock().unlock();
		scheduler.execute(new ProcessMapper(mapfile, markers));
	    } else {
		if(retries-- > 0) {
		    System.out.println("rescheduling upload");
		    scheduler.schedule(this, 5, TimeUnit.SECONDS);
		}
	    }
	}
    }
    
    private class MarkerData {
	Marker m;
	Indir<MapFile.Grid> indirGrid;
	
	MarkerData(Marker m, Indir<MapFile.Grid> indirGrid) {
	    this.m = m;
	    this.indirGrid = indirGrid;
	}
    }
    
    private class ProcessMapper implements Runnable {
	MapFile mapfile;
	List<MarkerData> markers;
	
	ProcessMapper(MapFile mapfile, List<MarkerData> markers) {
	    this.mapfile = mapfile;
	    this.markers = markers;
	}
	
	@Override
	public void run() {
	    ArrayList<JSONObject> loadedMarkers = new ArrayList<>();
	    while (!markers.isEmpty()) {
		System.out.println("processing " + markers.size() + " markers");
		Iterator<MarkerData> iterator = markers.iterator();
		while (iterator.hasNext()) {
		    MarkerData md = iterator.next();
		    try {
			Coord mgc = new Coord(Math.floorDiv(md.m.tc.x, 100), Math.floorDiv(md.m.tc.y, 100));
			long gridId = md.indirGrid.get().id;
			JSONObject o = new JSONObject();
			o.put("name", md.m.nm);
			o.put("gridID", String.valueOf(gridId));
			Coord gridOffset = md.m.tc.sub(mgc.mul(100));
			o.put("x", gridOffset.x);
			o.put("y", gridOffset.y);
			
			if(md.m instanceof SMarker) {
			    o.put("type", "shared");
			    o.put("id", ((SMarker) md.m).oid);
			    o.put("image", ((SMarker) md.m).res.name);
			} else if(md.m instanceof PMarker) {
			    o.put("type", "player");
			    o.put("color", ((PMarker) md.m).color);
			}
			loadedMarkers.add(o);
			iterator.remove();
		    } catch (Loading ex) {
		    }
		}
		try {
		    Thread.sleep(50);
		} catch (InterruptedException ex) { }
	    }
	    System.out.println("scheduling marker upload");
	    try {
		scheduler.execute(new MarkerUpdate(new JSONArray(loadedMarkers.toArray())));
	    } catch (Exception ex) {
		System.out.println(ex);
	    }
	}
    }
    
    private class MarkerUpdate implements Runnable {
	JSONArray data;
	
	MarkerUpdate(JSONArray data) {
	    this.data = data;
	}
	
	@Override
	public void run() {
	    try {
		HttpURLConnection connection =
		    (HttpURLConnection) new URL(endpoint + "/markerUpdate").openConnection();
		connection.setRequestMethod("POST");
		connection.setRequestProperty("Content-Type", "application/json;charset=UTF-8");
		connection.setDoOutput(true);
		try (DataOutputStream out = new DataOutputStream(connection.getOutputStream())) {
		    final String json = data.toString();
		    out.write(json.getBytes(StandardCharsets.UTF_8));
		}
		int code = connection.getResponseCode();
		connection.disconnect();
	    } catch (Exception ex) {
		System.out.println(ex);
	    }
	}
    }
    
    private class PositionUpdates implements Runnable {
	private class Tracking {
	    public String name;
	    public String type;
	    public long gridId;
	    public Coord2d coords;
	    
	    public JSONObject getJSON() {
		JSONObject j = new JSONObject();
		j.put("name", name);
		j.put("type", type);
		j.put("gridID", String.valueOf(gridId));
		JSONObject c = new JSONObject();
		c.put("x", (int) (coords.x / 11));
		c.put("y", (int) (coords.y / 11));
		j.put("coords", c);
		return j;
	    }
	}
	
	private Map<Long, Tracking> tracking = new ConcurrentHashMap<Long, Tracking>();
	
	private PositionUpdates() {
	}
	
	private void Track(long id, Coord2d coordinates, long gridId) {
	    Tracking t = tracking.get(id);
	    if(t == null) {
		t = new Tracking();
		tracking.put(id, t);
		
		if(id == glob.sess.ui.gui.map.plgob) {
		    t.name = playerName;
		    t.type = "player";
		} else {
		    Glob g = glob;
		    Gob gob = g.oc.getgob(id);
		    t.name = "???";
		    t.type = "white";
		    //TODO: remove or migrate to new Buddy class
//		    if(gob != null) {
//			KinInfo ki = gob.getattr(KinInfo.class);
//			if(ki != null) {
//			    t.name = ki.name;
//			    t.type = Integer.toHexString(BuddyWnd.gc[ki.group].getRGB());
//			}
//		    }
		}
	    }
	    t.gridId = gridId;
	    t.coords = gridOffset(coordinates);
	}
	
	@Override
	public void run() {
	    if(spamCount == spamPreventionVal) {
		spamCount = 0;
		if(trackingEnabled) {
		    Glob g = glob;
		    Iterator<Map.Entry<Long, Tracking>> i = tracking.entrySet().iterator();
		    JSONObject upload = new JSONObject();
		    while (i.hasNext()) {
			Map.Entry<Long, Tracking> e = i.next();
			if(g.oc.getgob(e.getKey()) == null) {
			    i.remove();
			} else {
			    upload.put(String.valueOf(e.getKey()), e.getValue().getJSON());
			}
		    }
		    
		    try {
			final HttpURLConnection connection =
			    (HttpURLConnection) new URL(endpoint + "/positionUpdate").openConnection();
			connection.setRequestMethod("POST");
			connection.setRequestProperty("Content-Type", "application/json;charset=UTF-8");
			connection.setDoOutput(true);
			try (DataOutputStream out = new DataOutputStream(connection.getOutputStream())) {
			    final String json = upload.toString();
			    out.write(json.getBytes(StandardCharsets.UTF_8));
			} catch (Exception e) {
			}
			connection.getResponseCode();
		    } catch (final Exception ex) {
		    }
		}
	    } else {
		spamCount++;
	    }
	}
    }
    
    private static class GridUpdate {
	String[][] grids;
	Map<String, WeakReference<MCache.Grid>> gridRefs;
	
	GridUpdate(final String[][] grids, Map<String, WeakReference<MCache.Grid>> gridRefs) {
	    this.grids = grids;
	    this.gridRefs = gridRefs;
	}
	
	@Override
	public String toString() {
	    return String.format("GridUpdate (%s)", grids[1][1]);
	}
    }
    
    private class GenerateGridUpdateTask implements Runnable {
	Coord coord;
	int retries = 3;
	
	GenerateGridUpdateTask(Coord c) {
	    this.coord = c;
	}
	
	@Override
	public void run() {
	    if(gridEnabled) {
		final String[][] gridMap = new String[3][3];
		Map<String, WeakReference<MCache.Grid>> gridRefs = new HashMap<String, WeakReference<MCache.Grid>>();
		try {
		    for (int x = -1; x <= 1; x++) {
			for (int y = -1; y <= 1; y++) {
			    final MCache.Grid subg = glob.map.getgrid(coord.add(x, y));
			    gridMap[x + 1][y + 1] = String.valueOf(subg.id);
			    gridRefs.put(String.valueOf(subg.id), new WeakReference<MCache.Grid>(subg));
			}
		    }
		    scheduler.execute(new UploadGridUpdateTask(new GridUpdate(gridMap, gridRefs)));
		} catch (LoadingMap lm) {
		    retries--;
		    if(retries >= 0) {
			scheduler.schedule(this, 1L, TimeUnit.SECONDS);
		    }
		} catch (Exception e) {
		    System.out.println(e);
		}
		;
	    }
	}
    }
    
    private class UploadGridUpdateTask implements Runnable {
	private final GridUpdate gridUpdate;
	
	UploadGridUpdateTask(final GridUpdate gridUpdate) {
	    this.gridUpdate = gridUpdate;
	}
	
	@Override
	public void run() {
	    if(gridEnabled) {
		HashMap<String, Object> dataToSend = new HashMap<>();
		
		dataToSend.put("grids", this.gridUpdate.grids);
		try {
		    HttpURLConnection connection =
			(HttpURLConnection) new URL(endpoint + "/gridUpdate").openConnection();
		    connection.setRequestMethod("POST");
		    connection.setRequestProperty("Content-Type", "application/json;charset=UTF-8");
		    connection.setDoOutput(true);
		    try (DataOutputStream out = new DataOutputStream(connection.getOutputStream())) {
			String json = new JSONObject(dataToSend).toString();
			out.write(json.getBytes(StandardCharsets.UTF_8));
		    }
		    if(connection.getResponseCode() == 200) {
			DataInputStream dio = new DataInputStream(connection.getInputStream());
			int nRead;
			byte[] data = new byte[1024];
			ByteArrayOutputStream buffer = new ByteArrayOutputStream();
			while ((nRead = dio.read(data, 0, data.length)) != -1) {
			    buffer.write(data, 0, nRead);
			}
			buffer.flush();
			String response = buffer.toString(StandardCharsets.UTF_8.name());
			JSONObject jo = new JSONObject(response);
			JSONArray reqs = jo.optJSONArray("gridRequests");
			synchronized (cache) {
			    cache.put(Long.valueOf(gridUpdate.grids[1][1]), new MapRef(jo.getLong("map"), new Coord(jo.getJSONObject("coords").getInt("x"), jo.getJSONObject("coords").getInt("y"))));
			}
			for (int i = 0; reqs != null && i < reqs.length(); i++) {
			    gridsUploader.execute(new GridUploadTask(reqs.getString(i), gridUpdate.gridRefs.get(reqs.getString(i))));
			}
		    }
		    
		} catch (Exception ex) {
		    System.out.println(ex);
		}
	    }
	}
    }
    
    private class GridUploadTask implements Runnable {
	private final String gridID;
	private final WeakReference<MCache.Grid> grid;
	
	GridUploadTask(String gridID, WeakReference<MCache.Grid> grid) {
	    this.gridID = gridID;
	    this.grid = grid;
	}
	
	@Override
	public void run() {
	    try {
		MCache.Grid g = grid.get();
		if(g != null && glob != null && glob.map != null) {
		    BufferedImage image = MinimapImageGenerator.drawmap(glob.map, g);
		    if(image == null) {
			throw new Loading();
		    }
		    try {
			JSONObject extraData = new JSONObject();
			ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
			ImageIO.write(image, "png", outputStream);
			ByteArrayInputStream inputStream = new ByteArrayInputStream(outputStream.toByteArray());
			MultipartUtility multipart = new MultipartUtility(endpoint + "/gridUpload", "utf-8");
			multipart.addFormField("id", this.gridID);
			multipart.addFilePart("file", inputStream, "minimap.png");
			extraData.put("season", glob.ast.is);
			multipart.addFormField("extraData", extraData.toString());
			MultipartUtility.Response response = multipart.finish();
			if(response.statusCode != 200) {
			    System.out.println("Upload Error: Code" + response.statusCode + " - " + response.response);
			} else {
			
			}
		    } catch (IOException e) {
			System.out.println("Cannot upload " + gridID + ": " + e.getMessage());
		    }
		}
	    } catch (Loading ex) {
		// Retry on Loading
		gridsUploader.submit(this);
	    }
	    
	}
    }
    
    private static Coord toGC(Coord2d c) {
	return new Coord(Math.floorDiv((int) c.x, 1100), Math.floorDiv((int) c.y, 1100));
    }
    
    private static Coord toGridUnit(Coord2d c) {
	return new Coord(Math.floorDiv((int) c.x, 1100) * 1100, Math.floorDiv((int) c.y, 1100) * 1100);
    }
    
    private static Coord2d gridOffset(Coord2d c) {
	Coord gridUnit = toGridUnit(c);
	return new Coord2d(c.x - gridUnit.x, c.y - gridUnit.y);
    }
    
    public class MapRef {
	public Coord gc;
	public long mapID;
	
	private MapRef(long mapID, Coord gc) {
	    this.gc = gc;
	    this.mapID = mapID;
	}
	
	public String toString() {
	    return (gc.toString() + " in map space " + mapID);
	}
    }
}
