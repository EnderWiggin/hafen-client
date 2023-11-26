package haven;

public enum Theme {
    Pretty(DecoX.DecoThemeType.Big),
    Small(DecoX.DecoThemeType.Small);
    
    public final DecoX.DecoThemeType deco;
    
    Theme(DecoX.DecoThemeType deco) {
        this.deco = deco;
    }
}
