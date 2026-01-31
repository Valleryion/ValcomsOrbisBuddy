package de.valcoms.orbisbuddy.service;

import de.valcoms.orbisbuddy.entity.OrbisBuddyController;
import de.valcoms.orbisbuddy.model.GolemData;

import java.util.concurrent.ConcurrentHashMap;

public class GolemInstanceStore {

    private final ConcurrentHashMap<String, Object> ownerToEntity = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, OrbisBuddyController> ownerToController = new ConcurrentHashMap<>();

    public Object getEntity(String ownerId) {
        return ownerToEntity.get(ownerId);
    }

    public void setEntity(String ownerId, Object entityRef) {
        ownerToEntity.put(ownerId, entityRef);
    }

    public OrbisBuddyController getController(String ownerId) {
        return ownerToController.get(ownerId);
    }

    public void setController(String ownerId, OrbisBuddyController controller) {
        ownerToController.put(ownerId, controller);
    }

    public void clear(String ownerId) {
        ownerToEntity.remove(ownerId);
        ownerToController.remove(ownerId);
    }
}
