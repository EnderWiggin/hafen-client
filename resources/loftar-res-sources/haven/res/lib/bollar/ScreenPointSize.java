/* Preprocessed source code */
package haven.res.lib.bollar;

import haven.*;
import haven.render.*;
import haven.render.sl.*;
import java.util.*;
import java.nio.*;
import static haven.render.sl.Type.*;
import static haven.render.sl.Cons.*;

@haven.FromResource(name = "lib/bollar", version = 3)
public abstract class ScreenPointSize extends State {
    private static final Uniform size = new Uniform(FLOAT, p -> ((ScreenPointSize)p.get(PointSize.slot)).sz(), PointSize.slot);
    private static final ShaderMacro prog = new ShaderMacro() {
	    final Function pdiv = new Function.Def(FLOAT) {{
		Expression vec = param(PDir.IN, VEC4).ref();
		code.add(new Return(div(pick(vec, "x"), pick(vec, "w"))));
	    }};

	    public void modify(ProgramContext prog) {
		Homo3D homo = Homo3D.get(prog);
		prog.vctx.ptsz.mod(in -> mul(sub(pdiv.call(homo.pprjxf(add(homo.eyev.depref(), vec4(size.ref(), l(0.0), l(0.0), l(0.0))))),
						 pdiv.call(prog.vctx.posv.depref())),
					     pick(FrameConfig.u_screensize.ref(), "x")),
				   0);
		prog.vctx.ptsz.force();
		Tex2D.get(prog).texcoord().mod(in -> FragmentContext.ptc, 0);
	    }
	};

    public abstract float sz();

    public ShaderMacro shader() {
	return(prog);
    }

    public void apply(Pipe buf) {
	buf.put(PointSize.slot, this);
    }

    public static ScreenPointSize fixed(float sz) {
	return(new ScreenPointSize() {
		public float sz() {return(sz);}
	    });
    }
}
