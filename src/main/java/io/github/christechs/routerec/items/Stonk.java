package io.github.christechs.routerec.items;

import io.github.christechs.routerec.manager.RecorderManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.minestom.server.MinecraftServer;
import net.minestom.server.coordinate.Point;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.Player;
import net.minestom.server.event.EventNode;
import net.minestom.server.event.player.PlayerBlockBreakEvent;
import net.minestom.server.event.player.PlayerStartDiggingEvent;
import net.minestom.server.event.trait.PlayerEvent;
import net.minestom.server.instance.Instance;
import net.minestom.server.instance.block.Block;
import net.minestom.server.item.ItemStack;
import net.minestom.server.item.Material;
import net.minestom.server.tag.Tag;
import net.minestom.server.timer.TaskSchedule;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class Stonk implements CustomItem {
    private static final Tag<Boolean> STONK_TAG = Tag.Boolean("stonk");
    private final Map<UUID, Integer> chargesMap = new ConcurrentHashMap<>();

    @Override
    public ItemStack getItem() {
        return ItemStack.builder(Material.DIAMOND_PICKAXE)
                .set(STONK_TAG, true)
                .customName(Component.text("Dungeonbreaker", NamedTextColor.GOLD).decoration(TextDecoration.ITALIC, false))
                .build();
    }

    @Override
    public void registerEvents(EventNode<PlayerEvent> node) {
        MinecraftServer.getSchedulerManager().buildTask(() -> {
            chargesMap.replaceAll((uuid, current) -> Math.min(20, current + 1));
        }).repeat(TaskSchedule.tick(10)).schedule();

        node.addListener(PlayerStartDiggingEvent.class, event -> {
            Player player = event.getPlayer();
            if (player.getItemInMainHand().hasTag(STONK_TAG)) {
                event.setCancelled(true);
                handleBreakerMining(player, event.getBlockPosition());
            }
        });

        node.addListener(PlayerBlockBreakEvent.class, event -> {
            if (event.getPlayer().getItemInMainHand().hasTag(STONK_TAG)) {
                event.setCancelled(true);
            }
        });
    }

    private void handleBreakerMining(Player player, Point pos) {
        Instance instance = player.getInstance();
        if (instance == null) return;

        Block block = instance.getBlock(pos);
        if (block.isAir() || block.isLiquid()) return;

        int charges = chargesMap.getOrDefault(player.getUuid(), 20);
        if (charges > 0) {
            chargesMap.put(player.getUuid(), charges - 1);
            player.sendActionBar(Component.text("Dungeonbreaker Charges: " + (charges - 1) + "/20", NamedTextColor.GOLD));

            RecorderManager.recordMine(Pos.fromPoint(pos));
            instance.setBlock(pos, Block.AIR);

            MinecraftServer.getSchedulerManager().buildTask(() -> {
                if (instance.getBlock(pos).isAir()) instance.setBlock(pos, block);
            }).delay(TaskSchedule.seconds(10)).schedule();
        }
    }
}