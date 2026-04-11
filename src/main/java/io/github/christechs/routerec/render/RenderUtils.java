package io.github.christechs.routerec.render;

import io.github.christechs.routerec.debug.BoxShape;
import io.github.christechs.routerec.debug.DebugShapes;
import io.github.christechs.routerec.debug.LineShape;
import io.github.christechs.routerec.manager.SecretManager;
import net.kyori.adventure.text.Component;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.entity.Entity;
import net.minestom.server.entity.EntityType;
import net.minestom.server.entity.Player;
import net.minestom.server.entity.metadata.display.AbstractDisplayMeta;
import net.minestom.server.entity.metadata.display.BlockDisplayMeta;
import net.minestom.server.entity.metadata.display.ItemDisplayMeta;
import net.minestom.server.entity.metadata.display.TextDisplayMeta;
import net.minestom.server.entity.metadata.other.InteractionMeta;
import net.minestom.server.instance.Instance;
import net.minestom.server.instance.block.Block;
import net.minestom.server.item.ItemStack;
import net.minestom.server.item.Material;
import net.minestom.server.network.packet.server.common.PluginMessagePacket;
import net.minestom.server.tag.Tag;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class RenderUtils {
    private static final Tag<Boolean> DEBUG_MOD_TAG = Tag.Boolean("has_debug_mod");
    private static final Map<Player, Map<String, List<Entity>>> fallbackEntities = new ConcurrentHashMap<>();

    public static void markHasMod(Player player) {
        player.setTag(DEBUG_MOD_TAG, true);
    }

    public static boolean hasMod(Player player) {
        return Boolean.TRUE.equals(player.getTag(DEBUG_MOD_TAG));
    }

    public static void clearShape(Player player, String namespace) {
        if (hasMod(player)) {
            byte[] payload = DebugShapes.createPayload(List.of(new DebugShapes.Remove(namespace)));
            player.sendPacket(new PluginMessagePacket(DebugShapes.CHANNEL, payload));
        }

        Map<String, List<Entity>> playerFallbacks = fallbackEntities.get(player);
        if (playerFallbacks != null && playerFallbacks.containsKey(namespace)) {
            playerFallbacks.get(namespace).forEach(Entity::remove);
            playerFallbacks.remove(namespace);
        }
    }

    public static void clearAll(Player player) {
        if (hasMod(player)) {
            byte[] payload = DebugShapes.createPayload(List.of(new DebugShapes.Clear()));
            player.sendPacket(new PluginMessagePacket(DebugShapes.CHANNEL, payload));
        }

        Map<String, List<Entity>> playerFallbacks = fallbackEntities.remove(player);
        if (playerFallbacks != null) {
            playerFallbacks.values().forEach(list -> list.forEach(Entity::remove));
        }
    }

    public static void spawnSecretEntity(Player player, String namespace, Pos pos, String category, String secretName) {
        Instance instance = player.getInstance();
        if (instance == null) return;

        Pos blockPos = new Pos(Math.floor(pos.x()), Math.floor(pos.y()), Math.floor(pos.z()));
        if (!instance.getBlock(blockPos).isAir()) {
            instance.setBlock(blockPos, Block.AIR);
        }

        Entity entity;

        if (category.equalsIgnoreCase("bat")) {
            entity = new Entity(EntityType.BAT);
            entity.setNoGravity(true);
            entity.getEntityMeta().setSilent(true);
            pos = pos.sub(0, 0.25, 0);

            entity.setInstance(instance, pos);
            entity.setGlowing(true);

            entity.setTag(SecretManager.SECRET_TAG, secretName);
            entity.setTag(SecretManager.SECRET_CATEGORY_TAG, category);
            trackFallback(player, namespace, entity);
            return;

        } else if (category.equalsIgnoreCase("item")) {
            entity = new Entity(EntityType.ITEM_DISPLAY);
            entity.setNoGravity(true);

            ItemDisplayMeta meta =
                    (ItemDisplayMeta) entity.getEntityMeta();
            meta.setItemStack(ItemStack.of(Material.ITEM_FRAME));
            meta.setDisplayContext(ItemDisplayMeta.DisplayContext.FIXED);
            meta.setScale(new Vec(0.75, 0.75, 0.75));

        } else {
            entity = new Entity(EntityType.BLOCK_DISPLAY);
            entity.setNoGravity(true);

            BlockDisplayMeta meta =
                    (BlockDisplayMeta) entity.getEntityMeta();

            if (category.equalsIgnoreCase("chest")) {
                meta.setBlockState(Block.CHEST);
            } else if (category.equalsIgnoreCase("lever")) {
                meta.setBlockState(Block.LEVER);
            } else if (category.equalsIgnoreCase("wither")) {
                meta.setBlockState(Block.WITHER_SKELETON_SKULL);
            } else {
                return;
            }
            meta.setTranslation(new Vec(-0.5, -0.5, -0.5));
        }

        entity.setInstance(instance, pos);
        entity.setGlowing(true);
        trackFallback(player, namespace, entity);

        Entity interact = new Entity(EntityType.INTERACTION);
        interact.setNoGravity(true);

        InteractionMeta interactMeta =
                (InteractionMeta) interact.getEntityMeta();

        interactMeta.setWidth(1.0f);
        interactMeta.setHeight(1.0f);

        interact.setInstance(instance, pos.sub(0, 0.5, 0));
        interact.setTag(SecretManager.SECRET_TAG, secretName);
        interact.setTag(SecretManager.SECRET_CATEGORY_TAG, category);

        trackFallback(player, namespace + "_interact", interact);
    }

    public static void drawBox(Player player, String namespace, Vec min, Vec max, RouteColor color, DrawMode mode) {
        drawBox(player, namespace, min, max, color, mode, false);
    }

    public static void drawBox(Player player, String namespace, Vec min, Vec max, RouteColor color, DrawMode mode, boolean forceDisplayEntities) {
        if (hasMod(player) && !forceDisplayEntities) {
            int face = mode.fill ? color.faceColor : 0x00000000;
            int edge = mode.border ? color.edgeColor : 0x00000000;
            BoxShape box = new BoxShape(min.x(), min.y(), min.z(), max.x(), max.y(), max.z(), face, 1, edge, 1, 2.0f);
            byte[] payload = DebugShapes.createPayload(List.of(new DebugShapes.Set(namespace, box)));
            player.sendPacket(new PluginMessagePacket(DebugShapes.CHANNEL, payload));
        } else {
            Entity display = new Entity(EntityType.BLOCK_DISPLAY);
            BlockDisplayMeta meta = (BlockDisplayMeta) display.getEntityMeta();

            Block block = (mode == DrawMode.BORDER_ONLY) ? Block.GLASS : color.fallbackBlock;
            meta.setBlockState(block);
            meta.setScale(new Vec(max.x() - min.x(), max.y() - min.y(), max.z() - min.z()));

            display.setGlowing(true);
            display.setNoGravity(true);
            display.setInstance(player.getInstance(), new Pos(min.x(), min.y(), min.z()));

            trackFallback(player, namespace, display);
        }
    }

    public static void drawLine(Player player, String namespace, List<Vec> points, RouteColor color, float width, boolean forceDisplayEntities) {
        if (hasMod(player) && !forceDisplayEntities) {
            LineShape line = new LineShape(1, points, color.edgeColor, 1, width);
            byte[] payload = DebugShapes.createPayload(List.of(new DebugShapes.Set(namespace, line)));
            player.sendPacket(new PluginMessagePacket(DebugShapes.CHANNEL, payload));
        } else {
            for (int i = 0; i < points.size() - 1; i++) {
                Vec start = points.get(i);
                Vec end = points.get(i + 1);
                double distance = start.distance(end);

                Entity display = new Entity(EntityType.BLOCK_DISPLAY);
                BlockDisplayMeta meta = (BlockDisplayMeta) display.getEntityMeta();
                meta.setBlockState(color.fallbackBlock);

                double thickness = Math.max(0.02, width / 100.0);
                meta.setScale(new Vec(thickness, thickness, distance));
                meta.setTranslation(new Vec(-thickness / 2, -thickness / 2, 0));

                Pos pos = Pos.fromPoint(start).withDirection(end.sub(start));
                display.setInstance(player.getInstance(), pos);
                display.setGlowing(true);
                display.setNoGravity(true);

                trackFallback(player, namespace, display);
            }
        }
    }

    public static void drawText(Player player, String namespace, Pos pos, Component text) {
        Entity display = new Entity(EntityType.TEXT_DISPLAY);
        TextDisplayMeta meta = (TextDisplayMeta) display.getEntityMeta();

        meta.setText(text);
        meta.setBillboardRenderConstraints(AbstractDisplayMeta.BillboardConstraints.CENTER);
        meta.setSeeThrough(true);
        meta.setUseDefaultBackground(true);

        display.setInstance(player.getInstance(), pos);
        display.setNoGravity(true);

        trackFallback(player, namespace, display);
    }

    private static void trackFallback(Player player, String namespace, Entity entity) {
        fallbackEntities.computeIfAbsent(player, k -> new ConcurrentHashMap<>())
                .computeIfAbsent(namespace, k -> new ArrayList<>())
                .add(entity);
    }
}