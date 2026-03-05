package me.ender;

import haven.CFG;
import haven.MCache;
import haven.Material;
import haven.render.BaseColor;
import haven.render.States;

import java.awt.*;
import java.util.Collection;
import java.util.Collections;

public class CFGOverlayId implements MCache.OverlayInfo {
    Material mat, omat;
    float alpha;
    float oalpha;
    private final Collection<String> tags;

    public CFGOverlayId(CFG<Color> cfg, String tag) {
	this(cfg, tag, -1f);
    }
    
    public CFGOverlayId(CFG<Color> cfg, String tag, float oalpha) {
	tags = Collections.singletonList(tag);
	this.oalpha = oalpha;
	cfg.observe(this::update);
	update(cfg);
    }
    
    @Override
    public Collection<String> tags() {return tags;}
    
    @Override
    public Material mat() {return (mat);}
    
    @Override
    public Material omat() {return omat;}
    
    private void update(CFG<Color> cfg) {
	Color c = cfg.get();
	mat = new Material(BaseColor.fromColorAndAlpha(c, c.getAlpha() / 256f), States.maskdepth);
	omat = oalpha > 0
	    ? new Material(BaseColor.fromColorAndAlpha(c, oalpha), States.maskdepth)
	    : null;
    }
}
