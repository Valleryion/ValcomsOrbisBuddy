package de.valcoms.orbisbuddy.persistence;

import de.valcoms.orbisbuddy.model.GolemData;

public interface GolemSaveRepository {
    GolemData load(Object worldRef, String ownerId);

    void save(Object worldRef, String ownerId, GolemData golemData);
}
