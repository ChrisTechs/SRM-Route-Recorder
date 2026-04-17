package io.github.christechs.routerec.items;

import io.github.christechs.routerec.manager.RecorderManager;
import io.github.christechs.routerec.manager.RouteManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.minestom.server.event.EventNode;
import net.minestom.server.event.player.PlayerUseItemEvent;
import net.minestom.server.event.trait.PlayerEvent;
import net.minestom.server.item.ItemStack;
import net.minestom.server.item.Material;
import net.minestom.server.tag.Tag;

public class StepBackward implements CustomItem {
    private static final Tag<Boolean> TAG = Tag.Boolean("step_backward");

    @Override
    public ItemStack getItem() {
        return ItemStack.builder(Material.RED_DYE)
                .set(TAG, true)
                .customName(Component.text("⬅ Previous Step", NamedTextColor.RED).decoration(TextDecoration.ITALIC, false))
                .build();
    }

    @Override
    public void registerEvents(EventNode<PlayerEvent> node) {
        node.addListener(PlayerUseItemEvent.class, event -> {
            if (!event.getPlayer().getItemInMainHand().hasTag(TAG)) return;
            event.setCancelled(true);

            if (RouteManager.currentRoomName == null) return;

            if (RecorderManager.isRecording) {
                if (RecorderManager.currentStep > 0) {
                    RecorderManager.currentStep--;

                    RouteManager.renderStepIndex = RecorderManager.currentStep;

                    event.getPlayer().sendMessage(Component.text("Moved to Recording Step: " + RecorderManager.currentStep, NamedTextColor.AQUA));
                    RecorderManager.updateSidebar();
                    RouteManager.visualize(RouteManager.currentRoomName, event.getPlayer().getInstance());
                } else {
                    event.getPlayer().sendMessage(Component.text("Already at the first recording step.", NamedTextColor.RED));
                }
            } else {
                if (RouteManager.renderStepIndex > 0) {
                    RouteManager.renderStepIndex--;
                    event.getPlayer().sendMessage(Component.text("Showing Step: " + RouteManager.renderStepIndex, NamedTextColor.RED));
                    RouteManager.visualize(RouteManager.currentRoomName, event.getPlayer().getInstance());
                } else {
                    event.getPlayer().sendMessage(Component.text("Already at the first step.", NamedTextColor.RED));
                }
            }
        });
    }
}