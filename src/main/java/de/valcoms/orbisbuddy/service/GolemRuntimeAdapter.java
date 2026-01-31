package de.valcoms.orbisbuddy.service;

import de.valcoms.orbisbuddy.model.GolemData;

public interface GolemRuntimeAdapter {
    void applyState(String ownerId, GolemData data);
}
