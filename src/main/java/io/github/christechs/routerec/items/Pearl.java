package io.github.christechs.routerec.items;

import io.github.christechs.routerec.RayUtil;
import io.github.christechs.routerec.manager.RecorderManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.minestom.server.MinecraftServer;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.entity.Entity;
import net.minestom.server.entity.EntityType;
import net.minestom.server.entity.Player;
import net.minestom.server.event.EventFilter;
import net.minestom.server.event.EventNode;
import net.minestom.server.event.player.PlayerUseItemEvent;
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
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

public class Pearl implements CustomItem {
    public static final Tag<Boolean> PEARL_TAG = Tag.Boolean("pearl");

    private static final Map<UUID, Long> cooldown = new ConcurrentHashMap<>();

    @Override
    public ItemStack getItem() {
        return ItemStack.builder(Material.ENDER_PEARL)
                .set(PEARL_TAG, true)
                .customName(Component.text("Ender Pearl", NamedTextColor.GREEN).decoration(TextDecoration.ITALIC, false))
                .build();
    }

    @Override
    public void registerEvents(EventNode<PlayerEvent> parentNode) {
        EventNode<PlayerEvent> node = EventNode.type("pearl", EventFilter.PLAYER);

        node.addListener(PlayerUseItemEvent.class, event -> {
            Player player = event.getPlayer();
            if (!player.getItemInMainHand().hasTag(PEARL_TAG)) return;

            event.setCancelled(true);

            long now = System.currentTimeMillis();
            cooldown.put(player.getUuid(), now);

            Instance instance = player.getInstance();
            if (instance == null) return;

            Entity pearl = new Entity(EntityType.ENDER_PEARL);
            pearl.setNoGravity(true);

            Pos startPos = player.getPosition().add(0, 1.62, 0);
            RecorderManager.recordPearl(startPos, player.getPosition().pitch(), player.getPosition().yaw());
            pearl.setInstance(instance, startPos);

            AtomicReference<Vec> currentVelocity = new AtomicReference<>(player.getPosition().direction().mul(1.5));

            MinecraftServer.getSchedulerManager().submitTask(() -> {
                if (pearl.isRemoved() || pearl.getInstance() == null) return TaskSchedule.stop();

                Vec start = pearl.getPosition().asVec();
                Vec nextPos = start.add(currentVelocity.get());
                AtomicBoolean hit = new AtomicBoolean(false);

                RayUtil.voxelTraversal(start, nextPos, (x, y, z, nx, ny, nz) -> {
                    Block block = instance.getBlock(x, y, z);

                    if (block.isSolid()) {

                        double t = 1.0;
                        if (nx != 0) {
                            t = ((nx < 0 ? x : x + 1.0) - start.x()) / (nextPos.x() - start.x());
                        } else if (ny != 0) {
                            t = ((ny < 0 ? y : y + 1.0) - start.y()) / (nextPos.y() - start.y());
                        } else if (nz != 0) {
                            t = ((nz < 0 ? z : z + 1.0) - start.z()) / (nextPos.z() - start.z());
                        }

                        Vec exactHit = start.add(nextPos.sub(start).mul(t));

                        double newX = x + nx + 0.5;
                        double newZ = z + nz + 0.5;
                        double newY;

                        if (ny == 1) {
                            newY = y + 1.0;
                        } else {
                            newY = exactHit.y();
                        }

                        player.teleport(new Pos(newX, newY, newZ, player.getPosition().yaw(), player.getPosition().pitch()));
                        hit.set(true);
                        return true;
                    }
                    return false;
                });

                if (hit.get()) {
                    pearl.remove();
                    return TaskSchedule.stop();
                }

                pearl.teleport(new Pos(nextPos.x(), nextPos.y(), nextPos.z()));

                Vec velocity = currentVelocity.get();
                currentVelocity.set(velocity.mul(0.99).sub(0, 0.03, 0));

                return TaskSchedule.tick(1);
            });
        });

        parentNode.addChild(node);
    }
}