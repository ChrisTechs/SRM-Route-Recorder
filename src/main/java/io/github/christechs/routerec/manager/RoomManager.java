package io.github.christechs.routerec.manager;

import io.github.christechs.routerec.RouteRecorderServer;
import net.hollowcube.polar.PolarChunk;
import net.hollowcube.polar.PolarLoader;
import net.hollowcube.polar.PolarWorld;
import net.minestom.server.MinecraftServer;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.instance.InstanceContainer;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class RoomManager {
    public static final File POLAR_DIR = new File("polar_worlds");
    private static InstanceContainer currentLoadedInstance = null;
    private static InstanceContainer oldInstance = null;

    public static void init() {
        if (!POLAR_DIR.exists()) POLAR_DIR.mkdirs();
    }

    public static List<File> getAvailableWorlds() {
        File[] files = POLAR_DIR.listFiles((dir, name) -> name.endsWith(".polar"));
        if (files == null) return new ArrayList<>();
        List<File> fileList = new ArrayList<>(Arrays.asList(files));
        fileList.sort(Comparator.comparing(File::getName));
        return fileList;
    }

    public static CompletableFuture<LoadResult> loadRoom(String worldName) {
        File file = new File(POLAR_DIR, worldName + ".polar");
        if (!file.exists()) return CompletableFuture.completedFuture(new LoadResult(null, null, false));

        if (currentLoadedInstance != null) {
            oldInstance = currentLoadedInstance;
        }

        InstanceContainer instance = MinecraftServer.getInstanceManager().createInstanceContainer(RouteRecorderServer.FULLBRIGHT_DIMENSION_KEY);
        currentLoadedInstance = instance;

        try {
            PolarLoader loader = new PolarLoader(Path.of(file.getAbsolutePath()));
            instance.setChunkLoader(loader);
            PolarWorld polarWorld = loader.world();

            double sumX = 0, sumZ = 0;
            for (PolarChunk chunk : polarWorld.chunks()) {
                sumX += chunk.x() * 16;
                sumZ += chunk.z() * 16;
            }
            int count = polarWorld.chunks().size();
            Pos spawnPos = new Pos(count > 0 ? (sumX / count) + 8 : 0, 90, count > 0 ? (sumZ / count) + 8 : 0);

            return CompletableFuture.allOf(polarWorld.chunks().stream()
                            .map(chunk -> instance.loadChunk(chunk.x(), chunk.z()))
                            .toArray(CompletableFuture[]::new))
                    .thenApply(v -> new LoadResult(instance, spawnPos, true));

        } catch (Exception e) {
            e.printStackTrace();
            return CompletableFuture.completedFuture(new LoadResult(null, null, false));
        }
    }

    public static void cleanupOldInstance() {
        if (oldInstance != null) {
            MinecraftServer.getInstanceManager().unregisterInstance(oldInstance);
            oldInstance = null;
        }
    }

    public record LoadResult(InstanceContainer instance, Pos spawnPos, boolean success) {
    }
}