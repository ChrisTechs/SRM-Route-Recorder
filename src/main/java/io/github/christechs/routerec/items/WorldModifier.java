package io.github.christechs.routerec.items;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.Player;
import net.minestom.server.event.EventNode;
import net.minestom.server.event.player.PlayerStartDiggingEvent;
import net.minestom.server.event.player.PlayerUseItemEvent;
import net.minestom.server.event.trait.PlayerEvent;
import net.minestom.server.instance.Instance;
import net.minestom.server.instance.block.Block;
import net.minestom.server.item.ItemStack;
import net.minestom.server.item.Material;
import net.minestom.server.tag.Tag;

import java.util.Map;
import java.util.Stack;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class WorldModifier implements CustomItem {
    private static final Tag<Boolean> MODIFIER_TAG = Tag.Boolean("world_modifier");

    private static final Map<UUID, Stack<BlockRecord>> undoStack = new ConcurrentHashMap<>();

    public static void clearMemory() {
        undoStack.clear();
    }

    @Override
    public ItemStack getItem() {
        return ItemStack.builder(Material.GOLDEN_HOE)
                .set(MODIFIER_TAG, true)
                .customName(Component.text("Hoer", NamedTextColor.YELLOW).decoration(TextDecoration.ITALIC, false))
                .build();
    }

    @Override
    public void registerEvents(EventNode<PlayerEvent> node) {

        node.addListener(PlayerStartDiggingEvent.class, event -> {
            Player player = event.getPlayer();
            if (!player.getItemInMainHand().hasTag(MODIFIER_TAG)) return;

            event.setCancelled(true);
            Instance instance = player.getInstance();
            if (instance == null) return;

            Pos pos = new Pos(event.getBlockPosition());
            Block block = instance.getBlock(pos);

            if (block.isAir() || block.isLiquid()) return;

            undoStack.computeIfAbsent(player.getUuid(), k -> new Stack<>()).push(new BlockRecord(instance, pos, block));
            instance.setBlock(pos, Block.AIR);
        });

        node.addListener(PlayerUseItemEvent.class, event -> {
            Player player = event.getPlayer();
            if (!player.getItemInMainHand().hasTag(MODIFIER_TAG)) return;
            event.setCancelled(true);

            Stack<BlockRecord> stack = undoStack.get(player.getUuid());
            if (stack != null && !stack.isEmpty()) {
                BlockRecord record = stack.pop();

                if (record.instance() != null && record.instance().isRegistered()) {
                    record.instance().setBlock(record.pos(), record.block());
                } else {
                    player.sendMessage(Component.text("Instance unloaded, cannot undo further.", NamedTextColor.RED));
                    stack.clear();
                }
            }
        });
    }

    public record BlockRecord(Instance instance, Pos pos, Block block) {
    }
}