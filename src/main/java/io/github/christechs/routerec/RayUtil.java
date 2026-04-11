package io.github.christechs.routerec;

import net.minestom.server.coordinate.Vec;

public class RayUtil {

    private static final double epsilon = 1e-6;

    public static void voxelTraversal(Vec start, Vec end, VoxelVisitor visitor) {
        int currentX = (int) Math.floor(start.x());
        int currentY = (int) Math.floor(start.y());
        int currentZ = (int) Math.floor(start.z());

        int lastX = (int) Math.floor(end.x());
        int lastY = (int) Math.floor(end.y());
        int lastZ = (int) Math.floor(end.z());

        double rayX = end.x() - start.x();
        double rayY = end.y() - start.y();
        double rayZ = end.z() - start.z();

        int stepX = rayX >= 0 ? 1 : -1;
        int stepY = rayY >= 0 ? 1 : -1;
        int stepZ = rayZ >= 0 ? 1 : -1;

        double nextBoundaryX = stepX > 0 ? currentX + 1.0 : currentX;
        double nextBoundaryY = stepY > 0 ? currentY + 1.0 : currentY;
        double nextBoundaryZ = stepZ > 0 ? currentZ + 1.0 : currentZ;

        double tMaxX = rayX != 0.0 ? (nextBoundaryX - start.x()) / rayX : Double.MAX_VALUE;
        double tMaxY = rayY != 0.0 ? (nextBoundaryY - start.y()) / rayY : Double.MAX_VALUE;
        double tMaxZ = rayZ != 0.0 ? (nextBoundaryZ - start.z()) / rayZ : Double.MAX_VALUE;

        double tDeltaX = rayX != 0.0 ? Math.abs(1.0 / rayX) : Double.MAX_VALUE;
        double tDeltaY = rayY != 0.0 ? Math.abs(1.0 / rayY) : Double.MAX_VALUE;
        double tDeltaZ = rayZ != 0.0 ? Math.abs(1.0 / rayZ) : Double.MAX_VALUE;

        if (visitor.visit(currentX, currentY, currentZ, 0, 0, 0)) return;

        while (currentX != lastX || currentY != lastY || currentZ != lastZ) {
            int normalX = 0, normalY = 0, normalZ = 0;

            if (tMaxX < tMaxY) {
                if (tMaxX < tMaxZ) {
                    currentX += stepX;
                    tMaxX += tDeltaX;
                    normalX = -stepX;
                } else {
                    currentZ += stepZ;
                    tMaxZ += tDeltaZ;
                    normalZ = -stepZ;
                }
            } else if (tMaxY < tMaxZ) {
                currentY += stepY;
                tMaxY += tDeltaY;
                normalY = -stepY;
            } else {
                currentZ += stepZ;
                tMaxZ += tDeltaZ;
                normalZ = -stepZ;
            }

            if (visitor.visit(currentX, currentY, currentZ, normalX, normalY, normalZ)) return;
        }
    }

    public static double intersectPlane(double start, double end, double plane) {
        double diff = end - start;
        return diff == 0 ? 1.0 : (plane - start) / diff;
    }

    public static boolean rayIntersectsAABB(Vec rayStart, Vec rayDir, double minX, double minY, double minZ, double maxX, double maxY, double maxZ) {
        double dirX = rayDir.x(), dirY = rayDir.y(), dirZ = rayDir.z();

        double t1 = (minX - rayStart.x()) / (dirX == 0 ? epsilon : dirX);
        double t2 = (maxX - rayStart.x()) / (dirX == 0 ? epsilon : dirX);
        double tmin = Math.min(t1, t2);
        double tmax = Math.max(t1, t2);

        t1 = (minY - rayStart.y()) / (dirY == 0 ? epsilon : dirY);
        t2 = (maxY - rayStart.y()) / (dirY == 0 ? epsilon : dirY);
        tmin = Math.max(tmin, Math.min(t1, t2));
        tmax = Math.min(tmax, Math.max(t1, t2));

        t1 = (minZ - rayStart.z()) / (dirZ == 0 ? epsilon : dirZ);
        t2 = (maxZ - rayStart.z()) / (dirZ == 0 ? epsilon : dirZ);
        tmin = Math.max(tmin, Math.min(t1, t2));
        tmax = Math.min(tmax, Math.max(t1, t2));

        return tmax >= Math.max(0.0, tmin) && tmin <= 1.0;
    }

    public interface VoxelVisitor {
        boolean visit(int x, int y, int z, int normalX, int normalY, int normalZ);
    }

}
