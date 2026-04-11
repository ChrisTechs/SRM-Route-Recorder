package io.github.christechs.routerec.commands;

import com.google.gson.*;
import io.github.christechs.routerec.Rotations;
import io.github.christechs.routerec.manager.RouteManager;
import io.github.christechs.routerec.manager.SecretManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.minestom.server.command.builder.Command;
import net.minestom.server.command.builder.arguments.ArgumentType;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.entity.Player;

import java.io.FileWriter;

public class FixerCommand extends Command {

    public FixerCommand() {
        super("fixer");

        var actionArg = ArgumentType.Word("action").from("offset", "flip", "anchor", "apply", "save");
        var xArg = ArgumentType.Double("x").setDefaultValue(0.0);
        var yArg = ArgumentType.Double("y").setDefaultValue(0.0);
        var zArg = ArgumentType.Double("z").setDefaultValue(0.0);

        addSyntax((sender, context) -> {
            if (!(sender instanceof Player player)) return;
            String action = context.get(actionArg).toLowerCase();

            switch (action) {
                case "offset" -> {
                    RouteManager.offsetX = context.get(xArg);
                    RouteManager.offsetY = context.get(yArg);
                    RouteManager.offsetZ = context.get(zArg);
                    player.sendMessage(Component.text("Offset updated. Re-rendering...", NamedTextColor.YELLOW));
                    RouteManager.visualize(RouteManager.currentRoomName, player.getInstance());
                }
                case "flip" -> {
                    RouteManager.flipState = context.get(xArg).intValue();
                    player.sendMessage(Component.text("Flip state updated. Re-rendering...", NamedTextColor.YELLOW));
                    RouteManager.visualize(RouteManager.currentRoomName, player.getInstance());
                }
                case "anchor" -> {
                    RouteManager.anchorMode = context.get(xArg).intValue();
                    player.sendMessage(Component.text("Anchor mode updated. Re-rendering...", NamedTextColor.YELLOW));
                    RouteManager.visualize(RouteManager.currentRoomName, player.getInstance());
                }
                case "apply" -> applyTransformations(player);
                case "save" -> saveFixes(player);
            }
        }, actionArg, xArg, yArg, zArg);
    }

    private void applyTransformations(Player player) {
        String roomName = RouteManager.currentRoomName;
        if (roomName == null) {
            player.sendMessage(Component.text("No room loaded!", NamedTextColor.RED));
            return;
        }

        String routeKey = RouteManager.getMatchedKey(roomName);
        String secretKey = SecretManager.getMatchedKey(roomName);

        Vec offsetVec = new Vec(RouteManager.offsetX, RouteManager.offsetY, RouteManager.offsetZ);
        Rotations oldRot = RouteManager.appliedRotation;
        Vec oldAnchor = RouteManager.activeAnchor;

        Rotations newRot = RouteManager.baseRotation;
        Vec newAnchor = RouteManager.clayPos;

        if (routeKey != null) {
            JsonArray routes = RouteManager.getRouteDataCache().getAsJsonArray(routeKey);
            for (JsonElement el : routes) {
                JsonObject route = el.getAsJsonObject();
                transformArrayOfArrays(route, "locations", oldRot, oldAnchor, offsetVec, newRot, newAnchor);
                transformArrayOfArrays(route, "etherwarps", oldRot, oldAnchor, offsetVec, newRot, newAnchor);
                transformArrayOfArrays(route, "mines", oldRot, oldAnchor, offsetVec, newRot, newAnchor);
                transformArrayOfArrays(route, "interacts", oldRot, oldAnchor, offsetVec, newRot, newAnchor);
                transformArrayOfArrays(route, "tnts", oldRot, oldAnchor, offsetVec, newRot, newAnchor);
                transformArrayOfArrays(route, "enderpearls", oldRot, oldAnchor, offsetVec, newRot, newAnchor);

                if (route.has("enderpearlangles")) {
                    JsonArray angles = route.getAsJsonArray("enderpearlangles");
                    float oldYawOffset = getYawOffset(oldRot);
                    float newYawOffset = getYawOffset(newRot);
                    for (JsonElement angEl : angles) {
                        JsonArray ang = angEl.getAsJsonArray();
                        double oldRelYaw = ang.get(1).getAsDouble();
                        double newRelYaw = oldRelYaw + oldYawOffset - newYawOffset;
                        ang.set(1, new JsonPrimitive(cleanNumber(newRelYaw)));
                    }
                }

                if (route.has("secret") && route.getAsJsonObject("secret").has("location")) {
                    JsonArray loc = route.getAsJsonObject("secret").getAsJsonArray("location");
                    Vec oldWorld = relativeToActualMath(loc.get(0).getAsDouble(), loc.get(1).getAsDouble(), loc.get(2).getAsDouble(), oldRot, oldAnchor, offsetVec);
                    Vec newRel = actualToRelativeMath(oldWorld, newRot, newAnchor);
                    loc.set(0, new JsonPrimitive(cleanNumber(newRel.x())));
                    loc.set(1, new JsonPrimitive(cleanNumber(newRel.y())));
                    loc.set(2, new JsonPrimitive(cleanNumber(newRel.z())));
                }
            }
        }

        if (secretKey != null) {
            JsonArray secrets = SecretManager.getSecretDataCache().getAsJsonArray(secretKey);
            for (JsonElement el : secrets) {
                JsonObject secret = el.getAsJsonObject();
                if (secret.has("x") && secret.has("y") && secret.has("z")) {
                    Vec oldWorld = relativeToActualMath(secret.get("x").getAsDouble(), secret.get("y").getAsDouble(), secret.get("z").getAsDouble(), oldRot, oldAnchor, offsetVec);
                    Vec newRel = actualToRelativeMath(oldWorld, newRot, newAnchor);
                    secret.addProperty("x", cleanNumber(newRel.x()));
                    secret.addProperty("y", cleanNumber(newRel.y()));
                    secret.addProperty("z", cleanNumber(newRel.z()));
                }
            }
        }

        RouteManager.offsetX = 0;
        RouteManager.offsetY = 0;
        RouteManager.offsetZ = 0;
        RouteManager.flipState = 0;
        RouteManager.anchorMode = 0;
        RouteManager.appliedRotation = RouteManager.baseRotation;
        RouteManager.activeAnchor = RouteManager.clayPos;

        player.sendMessage(Component.text("Transformations applied to memory!", NamedTextColor.GREEN));

        RouteManager.visualize(roomName, player.getInstance());
    }

    private void saveFixes(Player player) {
        saveCompactJson("routes/pearlroutes-fixed.json", RouteManager.getRouteDataCache());
        saveCompactJson("routes/secretlocations-fixed.json", SecretManager.getSecretDataCache());
        player.sendMessage(Component.text("Successfully saved aligned JSONs!", NamedTextColor.GREEN));
    }

    private void saveCompactJson(String path, JsonObject cache) {
        try (FileWriter writer = new FileWriter(path)) {
            Gson gson = new Gson();
            StringBuilder sb = new StringBuilder("{\n");
            boolean first = true;
            for (String key : cache.keySet()) {
                if (!first) sb.append(",\n");
                sb.append("  \"").append(key).append("\":");
                sb.append(gson.toJson(cache.get(key)));
                first = false;
            }
            sb.append("\n}");
            writer.write(sb.toString());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void transformArrayOfArrays(JsonObject obj, String key, Rotations oldRot, Vec oldAnchor, Vec offset, Rotations newRot, Vec newAnchor) {
        if (!obj.has(key)) return;
        JsonArray arr = obj.getAsJsonArray(key);
        for (JsonElement el : arr) {
            JsonArray coord = el.getAsJsonArray();
            Vec oldWorld = relativeToActualMath(coord.get(0).getAsDouble(), coord.get(1).getAsDouble(), coord.get(2).getAsDouble(), oldRot, oldAnchor, offset);
            Vec newRel = actualToRelativeMath(oldWorld, newRot, newAnchor);
            coord.set(0, new JsonPrimitive(cleanNumber(newRel.x())));
            coord.set(1, new JsonPrimitive(cleanNumber(newRel.y())));
            coord.set(2, new JsonPrimitive(cleanNumber(newRel.z())));
        }
    }

    private Vec relativeToActualMath(double rx, double ry, double rz, Rotations rotation, Vec corner, Vec offset) {
        double cx = corner.x();
        double cz = corner.z();
        Vec pos = switch (rotation) {
            case WEST -> new Vec(cx - rz, ry, cz + rx);
            case NORTH -> new Vec(cx - rx, ry, cz - rz);
            case EAST -> new Vec(cx + rz, ry, cz - rx);
            default -> new Vec(cx + rx, ry, cz + rz);
        };
        return pos.add(offset);
    }

    private Vec actualToRelativeMath(Vec actual, Rotations rotation, Vec corner) {
        double ax = actual.x();
        double ay = actual.y();
        double az = actual.z();
        double cx = corner.x();
        double cz = corner.z();

        return switch (rotation) {
            case WEST -> new Vec(az - cz, ay, cx - ax);
            case NORTH -> new Vec(cx - ax, ay, cz - az);
            case EAST -> new Vec(cz - az, ay, ax - cx);
            default -> new Vec(ax - cx, ay, az - cz);
        };
    }

    private float getYawOffset(Rotations rot) {
        return switch (rot) {
            case WEST -> 90f;
            case NORTH -> 180f;
            case EAST -> 270f;
            default -> 0f;
        };
    }

    private Number cleanNumber(double val) {
        double rounded = Math.round(val * 1000.0) / 1000.0;
        if (rounded == Math.floor(rounded)) return (int) rounded;
        return rounded;
    }
}