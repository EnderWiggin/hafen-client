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

package haven;

import java.awt.image.BufferedImage;
import java.awt.Graphics;
import java.util.*;
import java.util.function.*;
import java.lang.reflect.Constructor;
import haven.render.*;

public abstract class Sprite implements RenderTree.Node, PView.Render2D {
    public final Resource res;
    public final Owner owner;
    
    protected Tex tex2d;
    private final Coord3f pos2d = new Coord3f(0, 0, 1);
    protected final Object texLock = new Object();
    protected Pair<Double, Double> tex2dAlign = new Pair<>(0.5, 0.5);
    public static List<Factory> factories = new LinkedList<Factory>();
    static {
	factories.add(SpriteLink.sfact);
	factories.add(SkelSprite.fact);
	factories.add(AnimSprite.fact);
	factories.add(StaticSprite.fact);
	factories.add(AudioSprite.fact);
    }

    public interface Owner extends OwnerContext {
	public Random mkrandoom();
	public Resource getres();
    }

    public class RecOwner implements Owner, Skeleton.HasPose {
	public Random mkrandoom() {return(owner.mkrandoom());}
	public <T> T context(Class<T> cl) {return(owner.context(cl));}

	public Resource getres() {return(res);}

	public Skeleton.Pose getpose() {return(Skeleton.getpose(Sprite.this));}
    }

    public static interface CDel {
	public void delete();
    }

    public static interface CUpd {
	public void update(Message sdt);
    }

    public static class FactMaker extends Resource.PublishedCode.Instancer.Chain<Factory> {
	public FactMaker() {super(Factory.class);}
	{
	    add(new Direct<>(Factory.class));
	    add(new StaticCall<>(Factory.class, "mksprite", Sprite.class, new Class<?>[] {Owner.class, Resource.class, Message.class},
				 (make) -> (owner, res, sdt) -> make.apply(new Object[] {owner, res, sdt})));
	    add(new Construct<>(Factory.class, Sprite.class, new Class<?>[] {Owner.class, Resource.class},
				(cons) -> (owner, res, sdt) -> cons.apply(new Object[] {owner, res})));
	    add(new Construct<>(Factory.class, Sprite.class, new Class<?>[] {Owner.class, Resource.class, Message.class},
				(cons) -> (owner, res, sdt) -> cons.apply(new Object[] {owner, res, sdt})));
	}}

    @Resource.PublishedCode(name = "spr", instancer = FactMaker.class)
    public interface Factory {
	public Sprite create(Owner owner, Resource res, Message sdt);
    }

    public static class ResourceException extends RuntimeException {
	public Resource res;

	public ResourceException(String msg, Resource res) {
	    super(msg + " (" + res + ", from " + res.source + ")");
	    this.res = res;
	}

	public ResourceException(String msg, Throwable cause, Resource res) {
	    super(msg + " (" + res + ", from " + res.source + ")", cause);
	    this.res = res;
	}
    }

    protected Sprite(Owner owner, Resource res) {
	this.res = res;
	this.owner = owner;
    }

    public static int decnum(Message sdt) {
	if(sdt == null)
	    return(0);
	int ret = 0, off = 0;
	while(!sdt.eom()) {
	    ret |= sdt.uint8() << off;
	    off += 8;
	}
	return(ret);
    }

    public static Sprite create(Owner owner, Resource res, Message sdt) {
	{
	    Factory f = res.getcode(Factory.class, false);
	    if(f != null)
		return(f.create(owner, res, sdt));
	}
	for(Factory f : factories) {
	    Sprite ret = f.create(owner, res, sdt);
	    if(ret != null)
		return(ret);
	}
	/* XXXRENDER
	throw(new ResourceException("Does not know how to draw resource " + res.name, res));
	*/
	return(new Sprite(owner, res) {});
    }

    public void draw(GOut g) {}
    
    public void setTex2d(Tex t) {
	synchronized (texLock) {
	    tex2d = t;
	}
    }
    
    protected void up2d(int up) {pos2d.z = up;}
    
    @Override
    public void draw(GOut g, Pipe state) {
	if(tex2d == null) {return;} //quick check, since most sprites don't have 2d textures
	synchronized (texLock) {
	    if(tex2d == null) {return;}
	    if(owner instanceof Gob && !((Gob) owner).info.enabled()) {return;}
	    Coord3f c3d = Homo3D.obj2view2(pos2d, state, Area.sized(g.sz()));
	    if(c3d == null) {return;}
	    Coord sc = c3d.round2();
	    if(sc.isect(Coord.z, g.sz())) {
		g.aimage(tex2d, sc, tex2dAlign.a, tex2dAlign.b);
	    }
	}
    }

    public boolean tick(double dt) {
	return(false);
    }

    public void gtick(Render g) {
    }

    public void dispose() {
    }
}
