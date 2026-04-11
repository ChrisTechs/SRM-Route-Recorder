package io.github.christechs.routerec.render;

public enum DrawMode {
    SOLID_WITH_BORDER(true, true),
    BORDER_ONLY(false, true),
    SOLID_ONLY(true, false);

    public final boolean fill;
    public final boolean border;

    DrawMode(boolean fill, boolean border) {
        this.fill = fill;
        this.border = border;
    }
}