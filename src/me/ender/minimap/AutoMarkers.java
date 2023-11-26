package me.ender.minimap;

import com.google.gson.Gson;
import haven.*;
import me.ender.IDPool;

import java.awt.*;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import static haven.MapFile.*;

public class AutoMarkers {
    private static final Map<String, Mark> automark = new HashMap<>();
    
    static {init();}
    
    private static class VO {
	Map<String, Mark> basic;
    }
    
    public static class Mark {
	public String name;
	public String res;
    }
    
    public static void init() {
	final Gson gson = new Gson();
	final VO markers = gson.fromJson(Config.loadJarFile("automark.json5"), VO.class);
	for (final String marker : markers.basic.keySet()) {
	    automark.put(marker, markers.basic.get(marker));
	}
    }
    
    public static Optional<Mark> marker(final String name) {
	return Optional.ofNullable(automark.get(name));
    }
    
    public static Collection<Marker> defaultMarkers(Collection<Marker> markers) {
	return markers.stream()
	    .filter(marker -> !(marker instanceof CustomMarker))
	    .collect(Collectors.toList());
    }
    
    private static Collection<Marker> customMarkers(Collection<Marker> markers) {
	return markers.stream()
	    .filter(marker -> marker instanceof CustomMarker)
	    .collect(Collectors.toList());
    }
    
    public static void loadCustomMarkers(MapFile file) {
	InputStream fp;
	try {
	    fp = file.sfetch("index-ender");
	} catch (IOException e) {
	    return;
	}
	try (StreamMessage data = new StreamMessage(fp)) {
	    int ver = data.uint8();
	    if(ver == 1) {
		file.markerids = new IDPool(data);
		for (int i = 0, no = data.int32(); i < no; i++) {
		    Marker mark = loadcmarker(data);
		    file.markers.add(mark);
		}
	    } else {
		warn("unknown mapfile index-ender version: %d", ver);
	    }
	} catch (Message.BinError e) {
	    warn(e, "error when loading index-ender: %s", e);
	}
    }
    
    public static void saveCustomMarkers(MapFile file) {
	OutputStream fp;
	try {
	    fp = file.sstore("index-ender");
	} catch (IOException e) {
	    throw (new StreamMessage.IOError(e));
	}
	Collection<Marker> markers = customMarkers(file.markers);
	try (StreamMessage out = new StreamMessage(fp)) {
	    out.adduint8(1);
	    file.markerids.save(out);
	    out.addint32(markers.size());
	    for (Marker mark : markers)
		savecmarker(out, mark);
	}
    }
    
    public static Marker loadcmarker(Message fp) {
	int ver = fp.uint8();
	long seg = fp.int64();
	Coord tc = fp.coord();
	String nm = fp.string();
	char type = (char) fp.uint8();
	switch (type) {
	case 'r':
	    if(fp.uint8() == 1) {
		Color c = fp.color();
		Resource.Spec r = new Resource.Spec(Resource.remote(), fp.string(), fp.int16());
		return new CustomMarker(seg, tc, nm, c, r);
	    } else {
		throw (new Message.FormatError("Unknown custom marker version: " + ver));
	    }
	default:
	    throw (new Message.FormatError("Unknown marker type: " + (int) type));
	}
    }
    
    public static void savecmarker(Message fp, Marker mark) {
	if(mark instanceof CustomMarker) {
	    CustomMarker rm = (CustomMarker) mark;
	    fp.adduint8(1);
	    fp.addint64(mark.seg);
	    fp.addcoord(mark.tc);
	    fp.addstring(mark.nm);
	    fp.adduint8(rm.identifier());
	    fp.adduint8(rm.version());
	    fp.addcolor(rm.color);
	    fp.addstring(rm.res.name);
	    fp.addint16((short) rm.res.ver);
	} else {
	    throw (new ClassCastException("Unknown marker type " + mark.getClass().getCanonicalName()));
	}
    }
}
