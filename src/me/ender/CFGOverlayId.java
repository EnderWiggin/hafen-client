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
	this(cfg, tag, 0.25f, 0.75f);
    }
    
    public CFGOverlayId(CFG<Color> cfg, String tag, float alpha, float oalpha) {
	tags = Collections.singletonList(tag);
	this.alpha = alpha;
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
	mat = new Material(BaseColor.fromColorAndAlpha(cfg.get(), alpha), States.maskdepth);
	omat = oalpha > 0
	    ? new Material(BaseColor.fromColorAndAlpha(cfg.get(), oalpha), States.maskdepth)
	    : null;
    }
}
