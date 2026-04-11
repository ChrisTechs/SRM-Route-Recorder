package io.github.christechs.routerec.render;

import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.minestom.server.instance.block.Block;

public enum RouteColor {
    ETHERWARP(0x44B24CD8, 0xFFB24CD8, Block.PURPLE_STAINED_GLASS, NamedTextColor.LIGHT_PURPLE),
    MINE(0x15FFD700, 0x00000000, Block.YELLOW_STAINED_GLASS, NamedTextColor.GOLD),
    TNT(0x44FF0000, 0xFFFF0000, Block.RED_STAINED_GLASS, NamedTextColor.RED),
    INTERACT(0x440000FF, 0xFF0000FF, Block.BLUE_STAINED_GLASS, NamedTextColor.BLUE),
    SECRET(0x4400FF00, 0xFF00FF00, Block.LIME_STAINED_GLASS, NamedTextColor.GREEN),
    PATH_LINE(0x00000000, 0xFFAAFFAA, Block.LIME_STAINED_GLASS, NamedTextColor.GREEN),
    AOTV_TARGET(0x44AA00AA, 0xFFAA00AA, Block.MAGENTA_STAINED_GLASS, NamedTextColor.LIGHT_PURPLE),

    BAT(0x448B4513, 0xFF8B4513, Block.BROWN_STAINED_GLASS, NamedTextColor.GOLD),
    EXIT(0x44FFFFFF, 0xFFFFFFFF, Block.WHITE_STAINED_GLASS, NamedTextColor.WHITE),
    ENDERPEARL(0x4400AAAA, 0xFF00AAAA, Block.CYAN_STAINED_GLASS, NamedTextColor.DARK_AQUA),

    ITEM(0x44FFAA00, 0xFFFFAA00, Block.ORANGE_STAINED_GLASS, NamedTextColor.GOLD);

    public final int faceColor;
    public final int edgeColor;
    public final Block fallbackBlock;
    public final TextColor textColor;

    RouteColor(int faceColor, int edgeColor, Block fallbackBlock, TextColor textColor) {
        this.faceColor = faceColor;
        this.edgeColor = edgeColor;
        this.fallbackBlock = fallbackBlock;
        this.textColor = textColor;
    }
}