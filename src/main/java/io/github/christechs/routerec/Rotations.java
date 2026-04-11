package io.github.christechs.routerec;

public enum Rotations {
    NORTH(15, 15),
    SOUTH(-15, -15),
    WEST(15, -15),
    EAST(-15, 15),
    NONE(0, 0);

    public final int x;
    public final int z;

    Rotations(int x, int z) {
        this.x = x;
        this.z = z;
    }
}
