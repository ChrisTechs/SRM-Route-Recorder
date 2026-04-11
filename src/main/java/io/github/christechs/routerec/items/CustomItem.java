package io.github.christechs.routerec.items;

import net.minestom.server.event.EventNode;
import net.minestom.server.event.trait.PlayerEvent;
import net.minestom.server.item.ItemStack;

public interface CustomItem {
    ItemStack getItem();

    void registerEvents(EventNode<PlayerEvent> node);
}