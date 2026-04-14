package io.github.christechs.routerec.items;

import io.github.christechs.routerec.manager.RecorderManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.Player;
import net.minestom.server.event.EventNode;
import net.minestom.server.event.player.PlayerBlockBreakEvent;
import net.minestom.server.event.player.PlayerBlockPlaceEvent;
import net.minestom.server.event.trait.PlayerEvent;
import net.minestom.server.instance.Instance;
import net.minestom.server.instance.block.Block;
import net.minestom.server.item.ItemStack;
import net.minestom.server.item.Material;
import net.minestom.server.tag.Tag;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;

public class Superboom implements CustomItem {
    private static final Tag<Boolean> SUPERBOOM_TAG = Tag.Boolean("superboom");
    private static final int[][] NEIGHBOR_OFFSETS = {{1, 0, 0}, {-1, 0, 0}, {0, 1, 0}, {0, -1, 0}, {0, 0, 1}, {0, 0, -1}};

    private static final Map<Pos, Map<Pos, Block>> superboomMemory = new ConcurrentHashMap<>();

    public static void clearMemory() {
        superboomMemory.clear();
    }

    @Override
    public ItemStack getItem() {
        return ItemStack.builder(Material.TNT)
                .set(SUPERBOOM_TAG, true)
                .customName(Component.text("Superboom TNT", NamedTextColor.RED).decoration(TextDecoration.ITALIC, false))
                .build();
    }

    @Override
    public void registerEvents(EventNode<PlayerEvent> node) {

        node.addListener(PlayerBlockBreakEvent.class, event -> {
            Pos pos = new Pos(event.getBlockPosition());
            if (superboomMemory.containsKey(pos)) {
                Map<Pos, Block> restored = superboomMemory.remove(pos);
                Instance instance = event.getPlayer().getInstance();
                if (instance != null && instance.isRegistered()) {
                    restored.forEach(instance::setBlock);
                }
            }
        });

        node.addListener(PlayerBlockPlaceEvent.class, event -> {
            Player player = event.getPlayer();
            if (!player.getItemInMainHand().hasTag(SUPERBOOM_TAG)) return;

            Pos placedPos = new Pos(event.getBlockPosition());
            Instance instance = player.getInstance();

            Map<Pos, Block> brokenBlocks = new HashMap<>();
            Queue<Pos> queue = new LinkedList<>();

            for (int[] offset : NEIGHBOR_OFFSETS) {
                Pos check = placedPos.add(offset[0], offset[1], offset[2]);
                if (isCrackedBrick(instance, check) || isCrypt(instance, check)) {
                    queue.add(check);
                }
            }

            if (queue.isEmpty()) {
                event.setCancelled(true);
                return;
            }

            int limit = 100;
            while (!queue.isEmpty() && limit > 0) {
                Pos current = queue.poll();
                if (brokenBlocks.containsKey(current)) continue;

                Block currentBlock = instance.getBlock(current);
                brokenBlocks.put(current, currentBlock);
                limit--;

                if (isCryptBottom(currentBlock)) {
                    Pos topPos = current.add(0, 1, 0);
                    if (isCryptTop(instance.getBlock(topPos)) && !brokenBlocks.containsKey(topPos)) {
                        queue.add(topPos);
                    }
                } else if (isCryptTop(currentBlock)) {
                    Pos bottomPos = current.add(0, -1, 0);
                    if (isCryptBottom(instance.getBlock(bottomPos)) && !brokenBlocks.containsKey(bottomPos)) {
                        queue.add(bottomPos);
                    }
                }

                for (int[] offset : NEIGHBOR_OFFSETS) {
                    Pos neighbor = current.add(offset[0], offset[1], offset[2]);
                    if (!brokenBlocks.containsKey(neighbor)) {
                        Block neighborBlock = instance.getBlock(neighbor);

                        if (isCrackedBrick(instance, neighbor) || isCrypt(instance, neighbor)) {
                            queue.add(neighbor);
                        }
                        else if (isCryptTop(currentBlock) && isCryptTop(neighborBlock)) {
                            queue.add(neighbor);
                        }
                    }
                }
            }

            brokenBlocks.forEach((p, b) -> instance.setBlock(p, Block.AIR));
            if (!brokenBlocks.isEmpty())
                RecorderManager.recordTnt(placedPos);
            superboomMemory.put(placedPos, brokenBlocks);

            event.consumeBlock(false);
        });
    }

    private boolean isCrackedBrick(Instance instance, Pos pos) {
        return instance.getBlock(pos).compare(Block.CRACKED_STONE_BRICKS);
    }

    private boolean isCryptBottom(Block b) {
        if (b.isAir()) return false;
        String name = b.name().toLowerCase();
        return name.contains("stone_brick") || name.equals("minecraft:stone");
    }

    private boolean isCryptTop(Block b) {
        if (b.isAir()) return false;
        String name = b.name().toLowerCase();
        return name.contains("smooth_stone") || name.contains("stone_slab") || name.contains("stone_stairs");
    }

    private boolean isCrypt(Instance instance, Pos pos) {
        Block b = instance.getBlock(pos);
        if (isCryptBottom(b)) {
            return isCryptTop(instance.getBlock(pos.add(0, 1, 0)));
        } else if (isCryptTop(b)) {
            return isCryptBottom(instance.getBlock(pos.add(0, -1, 0)));
        }
        return false;
    }
}