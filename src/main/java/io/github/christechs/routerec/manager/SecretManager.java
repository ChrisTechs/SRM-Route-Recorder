package io.github.christechs.routerec.manager;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.github.christechs.routerec.Rotations;
import io.github.christechs.routerec.render.RenderUtils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.entity.Player;
import net.minestom.server.event.EventNode;
import net.minestom.server.event.player.PlayerEntityInteractEvent;
import net.minestom.server.event.trait.PlayerEvent;
import net.minestom.server.instance.Instance;
import net.minestom.server.tag.Tag;

import java.io.FileReader;
import java.io.Reader;
import java.util.UUID;

public class SecretManager {
    public static final Tag<String> SECRET_TAG = Tag.String("secret_name");
    public static final Tag<String> SECRET_CATEGORY_TAG = Tag.String("secret_category");

    private static JsonObject secretDataCache;

    public static void init(String jsonFilePath) {
        try (Reader reader = new FileReader(jsonFilePath)) {
            secretDataCache = JsonParser.parseReader(reader).getAsJsonObject();
        } catch (Exception e) {
            secretDataCache = new JsonObject();
        }
    }

    public static void visualizeSecrets(String roomName, Instance instance, Player player, Rotations appliedRotation, Vec activeAnchor) {
        if (secretDataCache == null || secretDataCache.isEmpty()) return;

        JsonArray secrets = getSecretsIgnoreCase(roomName);
        if (secrets == null) return;

        for (JsonElement el : secrets) {
            JsonObject secret = el.getAsJsonObject();
            String category = secret.get("category").getAsString().toLowerCase();

            if (!category.equals("item") && !category.equals("chest") &&
                    !category.equals("wither") && !category.equals("lever") && !category.equals("bat")) {
                continue;
            }

            String secretName = secret.get("secretName").getAsString();
            double rx = secret.get("x").getAsDouble();
            double ry = secret.get("y").getAsDouble();
            double rz = secret.get("z").getAsDouble();

            Vec basePos = RouteManager.relativeToActual(rx, ry, rz, appliedRotation, activeAnchor);
            basePos = basePos.add(RouteManager.offsetX, RouteManager.offsetY, RouteManager.offsetZ);

            Pos spawnPos = new Pos(basePos.x() + 0.5, basePos.y() + 0.5, basePos.z() + 0.5);

            RenderUtils.spawnSecretEntity(player, "pearlroutes:secret_" + UUID.randomUUID(), spawnPos, category, secretName);
        }
    }

    public static JsonObject getSecretDataCache() {
        return secretDataCache;
    }

    public static String getMatchedKey(String roomName) {
        String cleanRoomName = roomName.replaceAll("_\\d+$", "");
        for (String key : secretDataCache.keySet()) {
            String cleanKey = key.replace("-0", "");
            if (cleanKey.equalsIgnoreCase(roomName) || cleanKey.equalsIgnoreCase(cleanRoomName)) {
                return key;
            }
        }
        return null;
    }

    private static JsonArray getSecretsIgnoreCase(String roomName) {
        String cleanRoomName = roomName.replaceAll("_\\d+$", "");
        for (String key : secretDataCache.keySet()) {
            String cleanKey = key.replace("-0", "");
            if (cleanKey.equalsIgnoreCase(roomName) || cleanKey.equalsIgnoreCase(cleanRoomName)) {
                return secretDataCache.getAsJsonArray(key);
            }
        }
        return null;
    }

    public static void registerEvents(EventNode<PlayerEvent> node) {
        node.addListener(PlayerEntityInteractEvent.class, event -> {
            if (event.getTarget().hasTag(SECRET_TAG)) {
                String secretName = event.getTarget().getTag(SECRET_TAG);
                String category = event.getTarget().hasTag(SECRET_CATEGORY_TAG) ? event.getTarget().getTag(SECRET_CATEGORY_TAG) : "interact";

                if (RecorderManager.isRecording && !RecorderManager.isPaused) {

                    String type = category;
                    if (category.equals("wither") || category.equals("lever") || category.equals("chest")) {
                        type = "interact";
                    }

                    Vec targetPos = event.getTarget().getPosition().asBlockVec().asVec();
                    RecorderManager.setSecret(type, targetPos);

                    event.getPlayer().sendMessage(
                            Component.text("Secret Recorded: ", NamedTextColor.GREEN)
                                    .append(Component.text(secretName, NamedTextColor.GOLD))
                                    .append(Component.text(" (" + type + ")", NamedTextColor.GRAY))
                    );
                } else {
                    event.getPlayer().sendMessage(
                            Component.text("Secret Clicked: ", NamedTextColor.AQUA)
                                    .append(Component.text(secretName, NamedTextColor.GOLD))
                    );
                }
            }
        });
    }
}