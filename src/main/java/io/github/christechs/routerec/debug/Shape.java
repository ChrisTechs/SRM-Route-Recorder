package io.github.christechs.routerec.debug;

import net.minestom.server.network.NetworkBuffer;

public interface Shape {
    int typeOrdinal();

    void write(NetworkBuffer writer);
}