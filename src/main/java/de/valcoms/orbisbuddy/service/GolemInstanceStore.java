package de.valcoms.orbisbuddy.service;

import de.valcoms.orbisbuddy.model.GolemData;

import java.util.concurrent.ConcurrentHashMap;

public class GolemInstanceStore {

    private final ConcurrentHashMap<String, Object> ownerToEntity = new ConcurrentHashMap<>();

    public Object getEntity(String ownerId) {
        return ownerToEntity.get(ownerId);
    }

    public void setEntity(String ownerId, Object entityRef) {
        ownerToEntity.put(ownerId, entityRef);
    }

    public void clear(String ownerId) {
        ownerToEntity.remove(ownerId);
    }
}
