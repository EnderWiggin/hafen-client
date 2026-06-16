/*
 *  This file is part of the Haven & Hearth game client.
 *  Copyright (C) 2009 Fredrik Tolf <fredrik@dolda2000.com>, and
 *                     Björn Johannessen <johannessen.bjorn@gmail.com>
 *
 *  Redistribution and/or modification of this file is subject to the
 *  terms of the GNU Lesser General Public License, version 3, as
 *  published by the Free Software Foundation.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  Other parts of this source tree adhere to other copying
 *  rights. Please see the file `COPYING' in the root directory of the
 *  source tree for details.
 *
 *  A copy the GNU Lesser General Public License is distributed along
 *  with the source tree of which this file is a part in the file
 *  `doc/LPGL-3'. If it is missing for any reason, please see the Free
 *  Software Foundation's website at <http://www.fsf.org/>, or write
 *  to the Free Software Foundation, Inc., 59 Temple Place, Suite 330,
 *  Boston, MA 02111-1307 USA
 */

package haven.ffi.dbus;

import java.util.*;
import java.io.*;
import javax.xml.*;
import javax.xml.parsers.*;
import org.xml.sax.*;
import org.w3c.dom.*;

public class Introspec {
    public final Map<String, Interface> ifaces = new HashMap<>();

    public Introspec(String xml) {
	Element doc;
	try {
	    DocumentBuilderFactory conf = DocumentBuilderFactory.newInstance();
	    conf.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
	    conf.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
	    conf.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
	    conf.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
	    doc = conf.newDocumentBuilder().parse(new InputSource(new StringReader(xml))).getDocumentElement();
	} catch(ParserConfigurationException | IOException e) {
	    throw(new RuntimeException(e));
	} catch(SAXException e) {
	    throw(new RuntimeException("introspection syntax error", e));
	}
	for(Element idesc : els(doc, "interface")) {
	    Interface iface = new Interface(idesc.getAttribute("name"));
	    ifaces.put(iface.name, iface);
	}
    }

    public static Iterable<Element> els(Element el, String tag) {
	NodeList l = el.getElementsByTagName(tag);
	return(() -> new Iterator<Element>() {
		int i = 0;
		public boolean hasNext() {return(i < l.getLength());}
		public Element next() {return((Element)l.item(i++));}
	    });
    }

    public static class Interface {
	public final String name;

	public Interface(String name){
	    this.name = name;
	}
    }
}
