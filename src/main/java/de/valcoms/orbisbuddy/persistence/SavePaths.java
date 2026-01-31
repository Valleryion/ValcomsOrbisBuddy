package de.valcoms.orbisbuddy.persistence;

import java.nio.file.Path;

public class SavePaths {
    private SavePaths() {};

    public static Path golemDataFile(Path worldRoot) {
        return worldRoot.resolve("orbisbuddy").resolve("golem_state.json");
    }
}
