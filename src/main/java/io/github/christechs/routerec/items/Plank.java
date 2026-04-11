package io.github.christechs.routerec.items;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.minestom.server.event.EventNode;
import net.minestom.server.event.player.PlayerBlockPlaceEvent;
import net.minestom.server.event.trait.PlayerEvent;
import net.minestom.server.item.ItemStack;
import net.minestom.server.item.Material;
import net.minestom.server.tag.Tag;

public class Plank implements CustomItem {
    private static final Tag<Boolean> INFINITE_TAG = Tag.Boolean("infinite_block");

    @Override
    public ItemStack getItem() {
        return ItemStack.builder(Material.OAK_PLANKS)
                .set(INFINITE_TAG, true)
                .customName(Component.text("Plank", NamedTextColor.GOLD).decoration(TextDecoration.ITALIC, false))
                .build();
    }

    @Override
    public void registerEvents(EventNode<PlayerEvent> node) {
        node.addListener(PlayerBlockPlaceEvent.class, event -> {
            if (event.getPlayer().getItemInHand(event.getHand()).hasTag(INFINITE_TAG)) {

                event.consumeBlock(false);
            }
        });
    }
}