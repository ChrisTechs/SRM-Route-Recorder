package io.github.christechs.routerec.manager;

import io.github.christechs.routerec.RouteRecorderServer;
import net.hollowcube.polar.PolarChunk;
import net.hollowcube.polar.PolarLoader;
import net.hollowcube.polar.PolarWorld;
import net.minestom.server.MinecraftServer;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.instance.Chunk;
import net.minestom.server.instance.InstanceContainer;
import net.minestom.server.instance.batch.AbsoluteBlockBatch;
import net.minestom.server.instance.block.Block;

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

        InstanceContainer tempInstance = MinecraftServer.getInstanceManager().createInstanceContainer();

        InstanceContainer shiftedInstance = MinecraftServer.getInstanceManager().createInstanceContainer(RouteRecorderServer.FULLBRIGHT_DIMENSION_KEY);
        currentLoadedInstance = shiftedInstance;

        try {
            PolarLoader loader = new PolarLoader(Path.of(file.getAbsolutePath()));
            tempInstance.setChunkLoader(loader);
            PolarWorld polarWorld = loader.world();

            return CompletableFuture.allOf(polarWorld.chunks().stream()
                            .map(chunk -> tempInstance.loadChunk(chunk.x(), chunk.z()))
                            .toArray(CompletableFuture[]::new))
                    .thenCompose(v -> {
                        List<CompletableFuture<Chunk>> chunkFutures = new ArrayList<>();
                        for (PolarChunk pChunk : polarWorld.chunks()) {
                            int cx = pChunk.x();
                            int cz = pChunk.z();
                            chunkFutures.add(shiftedInstance.loadChunk(cx, cz));
                            chunkFutures.add(shiftedInstance.loadChunk(cx - 1, cz));
                            chunkFutures.add(shiftedInstance.loadChunk(cx, cz - 1));
                            chunkFutures.add(shiftedInstance.loadChunk(cx - 1, cz - 1));
                        }

                        return CompletableFuture.allOf(chunkFutures.toArray(new CompletableFuture[0]));
                    })
                    .thenCompose(v2 -> {
                        CompletableFuture<LoadResult> future = new CompletableFuture<>();
                        AbsoluteBlockBatch batch = new AbsoluteBlockBatch();
                        double sumX = 0, sumZ = 0;
                        int blockCount = 0;

                        for (PolarChunk pChunk : polarWorld.chunks()) {
                            int chunkX = pChunk.x();
                            int chunkZ = pChunk.z();
                            Chunk mChunk = tempInstance.getChunk(chunkX, chunkZ);

                            if (mChunk == null) continue;

                            int minY = mChunk.getMinSection() * 16;
                            int maxY = mChunk.getMaxSection() * 16;

                            for (int x = 0; x < 16; x++) {
                                for (int y = minY; y < maxY; y++) {
                                    for (int z = 0; z < 16; z++) {
                                        Block block = mChunk.getBlock(x, y, z);
                                        if (!block.isAir()) {
                                            int globalX = (chunkX * 16) + x;
                                            int globalZ = (chunkZ * 16) + z;

                                            int shiftedX = globalX - 8;
                                            int shiftedZ = globalZ - 8;

                                            batch.setBlock(shiftedX, y, shiftedZ, block);

                                            sumX += shiftedX;
                                            sumZ += shiftedZ;
                                            blockCount++;
                                        }
                                    }
                                }
                            }
                        }

                        Pos spawnPos = new Pos(blockCount > 0 ? (sumX / blockCount) : 0, 90, blockCount > 0 ? (sumZ / blockCount) : 0);

                        batch.apply(shiftedInstance, b -> {
                            MinecraftServer.getInstanceManager().unregisterInstance(tempInstance);
                            future.complete(new LoadResult(shiftedInstance, spawnPos, true));
                        });

                        return future;
                    });

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