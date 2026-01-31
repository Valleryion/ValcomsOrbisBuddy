package de.valcoms.orbisbuddy.service;

import de.valcoms.orbisbuddy.entity.OrbisBuddyController;

import java.util.concurrent.ConcurrentHashMap;

public class GolemInstanceStore {
    private final ConcurrentHashMap<String, Object> ownerToEntity = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, OrbisBuddyController> ownerToController = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Object, String> entityToOwner = new ConcurrentHashMap<>();

    public Object getEntity(String ownerId) {
        return ownerToEntity.get(ownerId);
    }

    public void setEntity(String ownerId, Object entityRef) {
        Object previous = ownerToEntity.put(ownerId, entityRef);
        if (previous != null) {
            entityToOwner.remove(previous);
        }
        if (entityRef != null) {
            entityToOwner.put(entityRef, ownerId);
        }
    }

    public OrbisBuddyController getController(String ownerId) {
        return ownerToController.get(ownerId);
    }

    public void setController(String ownerId, OrbisBuddyController controller) {
        ownerToController.put(ownerId, controller);
    }

    public void clear(String ownerId) {
        removeByOwner(ownerId);
    }

    public void removeByOwner(String ownerId) {
        if (ownerId == null) {
            return;
        }
        Object previous = ownerToEntity.remove(ownerId);
        if (previous != null) {
            entityToOwner.remove(previous);
        }
        ownerToController.remove(ownerId);
    }

    public void removeByEntity(Object entityRef) {
        if (entityRef == null) {
            return;
        }
        String ownerId = entityToOwner.remove(entityRef);
        if (ownerId != null) {
            ownerToEntity.remove(ownerId, entityRef);
            ownerToController.remove(ownerId);
        }
    }

    public String findOwnerIdByEntity(Object entityRef) {
        if (entityRef == null) {
            return null;
        }
        return entityToOwner.get(entityRef);
    }

    public boolean isBuddyEntity(Object entityRef) {
        return entityRef != null && entityToOwner.containsKey(entityRef);
    }
}
