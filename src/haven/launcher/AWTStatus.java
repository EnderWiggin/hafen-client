/*
 *  This file is part of the Haven Java Launcher.
 *  Copyright (C) 2019 Fredrik Tolf <fredrik@dolda2000.com>, and
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

package haven.launcher;

import java.util.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.event.*;
import java.io.*;
import java.nio.file.*;
import java.net.*;
import javax.imageio.ImageIO;
import static haven.launcher.Config.expand;

public class AWTStatus implements Status {
    private final JFrame frame;
    private boolean subsumed;
    private JPanel imgcont, progcont;
    private JLabel message;
    private Component image;
    private JProgressBar prog;

    {
	try {
	    UIManager.setLookAndFeel(javax.swing.UIManager.getSystemLookAndFeelClassName());
	} catch(Exception e) {}
    }
	
    public AWTStatus() {
	frame = new JFrame("Launcher");
	frame.setResizable(false);
	JPanel cont = new JPanel();
	cont.setLayout(new BoxLayout(cont, BoxLayout.PAGE_AXIS));
	imgcont = new JPanel();
	imgcont.setLayout(new BoxLayout(imgcont, BoxLayout.LINE_AXIS));
	imgcont.add(image = Box.createHorizontalStrut(450));
	cont.add(imgcont);
	progcont = new JPanel();
	progcont.setLayout(new BoxLayout(progcont, BoxLayout.LINE_AXIS));
	progcont.add(message = new JLabel("Initializing..."));
	progcont.add(Box.createGlue());
	cont.add(progcont);
	frame.add(cont);
	frame.pack();
	SwingUtilities.invokeLater(() -> {
		frame.setVisible(true);
		Dimension ssz = Toolkit.getDefaultToolkit().getScreenSize();
		Dimension fsz = frame.getSize();
		frame.setLocation((ssz.width - fsz.width) / 2, (ssz.height - fsz.height) / 4);
	    });
    }

    public JFrame subsume() {
	subsumed = true;
	return(frame);
    }

    public void dispose() {
	if(!subsumed)
	    frame.dispose();
    }

    private void setimage(Path imgpath) throws IOException {
	Image img;
	try(InputStream fp = Files.newInputStream(imgpath)) {
	    img = ImageIO.read(fp);
	}
	SwingUtilities.invokeLater(() -> {
		JLabel nimage = new JLabel(new ImageIcon(img));
		imgcont.remove(image);
		imgcont.add(image = nimage);
		nimage.setAlignmentX(0);
		frame.pack();
	    });
    }

    private URI splash = null, icon = null;
    public boolean command(String[] argv, Config cfg, Config.Environment env) {
	switch(argv[0]) {
	case "splash-image": {
	    if(argv.length < 2)
		throw(new RuntimeException("usage: splash-image URL"));
	    Resource res;
	    try {
		res = new Resource(env.rel.resolve(new URI(expand(argv[1], env))), env.val).referrer(env.src);
	    } catch(URISyntaxException e) {
		throw(new RuntimeException("usage: splash-image URL", e));
	    }
	    if(!Objects.equals(res.uri, splash)) {
		try {
		    setimage(res.update());
		    splash = res.uri;
		} catch(IOException e) { /* Just ignore. */ }
	    }
	    return(true);
	}
	case "icon": {
	    if(argv.length < 2)
		throw(new RuntimeException("usage: icon URL"));
	    Resource res;
	    try {
		res = new Resource(env.rel.resolve(new URI(expand(argv[1], env))), env.val).referrer(env.src);
	    } catch(URISyntaxException e) {
		throw(new RuntimeException("usage: icon URL", e));
	    }
	    if(!Objects.equals(res.uri, icon)) {
		try(InputStream fp = Files.newInputStream(res.update())) {
		    Image img = ImageIO.read(fp);
		    SwingUtilities.invokeLater(() -> frame.setIconImage(img));
		    icon = res.uri;
		} catch(IOException e) { /* Just ignore. */ }
	    }
	    return(true);
	}
	case "title": {
	    if(argv.length < 2)
		throw(new RuntimeException("usage: title TITLE"));
	    String title = expand(argv[1], env);
	    SwingUtilities.invokeLater(() -> frame.setTitle(title));
	    return(true);
	}
	}
	return(false);
    }

    public void message(String text) {
	SwingUtilities.invokeLater(() -> {
		message.setText(text);
		if(prog != null) {
		    progcont.remove(prog);
		    prog = null;
		}
	    });
    }

    private static final String[] units = {"B", "kB", "MB", "GB", "TB", "PB"};
    private static String fmtbytes(long amount) {
	int ui = 0;
	long sz = 1;
	while((ui < units.length - 1) && ((amount / sz) >= 1000)) {
	    ui++;
	    sz *= 1000;
	}
	return(String.format("%d %s", (amount + (sz / 2)) / sz, units[ui]));
    }

    private long lastsize = -1, lastpos = -1, lastupd = 0;
    public void transfer(long size, long pos) {
	SwingUtilities.invokeLater(() -> {
		long now = System.currentTimeMillis();
		if(size < 0) {
		    if(prog != null) {
			progcont.remove(prog);
			prog = null;
		    }
		} else {
		    if(prog == null) {
			lastsize = lastpos = -1;
			lastupd = 0;
			progcont.add(prog = new JProgressBar());
			prog.setMinimumSize(new Dimension(100, 0));
			prog.setStringPainted(true);
		    }
		    if((now - lastupd > 100) || (pos >= size)) {
			prog.setMaximum((int)size);
			prog.setValue((int)pos);
			prog.setString(String.format("%s / %s", fmtbytes(pos), fmtbytes(size)));
			lastupd = now;
		    }
		}
		lastsize = size;
		lastpos = pos;
	    });
    }

    public void progress() {
    }

    private boolean errdone = false;
    public void error(Throwable exc) {
	StringWriter buf = new StringWriter();
	exc.printStackTrace(new PrintWriter(buf));
	String trace = buf.toString();
	try {
	    SwingUtilities.invokeAndWait(() -> {
		    JDialog errwnd = new JDialog(frame, "Launcher error!", true);
		    errwnd.setResizable(false);
		    JPanel cont = new JPanel();
		    cont.setLayout(new BoxLayout(cont, BoxLayout.PAGE_AXIS));
		    String message = ErrorMessage.getmessage(exc);
		    cont.add(new JLabel((message != null) ? message : "An error has occurred!"));
		    cont.add(new JLabel("If you want to report this, please including the following information:"));
		    JTextArea body = new JTextArea(15, 80);
		    body.setEditable(false);
		    body.setText(trace);
		    cont.add(new JScrollPane(body));
		    errwnd.add(cont);
		    errwnd.pack();
		    errwnd.addWindowListener(new WindowAdapter() {
			    public void windowClosing(WindowEvent ev) {
				synchronized(AWTStatus.this) {
				    errdone = true;
				    AWTStatus.this.notifyAll();
				}
			    }
			});
		    errwnd.setVisible(true);
		});
	    synchronized(AWTStatus.this) {
		while(!errdone)
		    AWTStatus.this.wait();
	    }
	} catch(InterruptedException e) {
	} catch(java.lang.reflect.InvocationTargetException e) {
	    throw(new RuntimeException(e));
	}
    }
}
