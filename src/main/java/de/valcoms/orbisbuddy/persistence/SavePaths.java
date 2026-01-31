package de.valcoms.orbisbuddy.persistence;

import java.nio.file.Path;

public final class SavePaths {
    private SavePaths() {}

    public static Path golemDataFile(Path dataDir) {
        return dataDir.resolve("golems.json");
    }
}
