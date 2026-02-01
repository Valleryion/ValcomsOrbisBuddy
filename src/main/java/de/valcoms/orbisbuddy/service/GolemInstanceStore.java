package de.valcoms.orbisbuddy.service;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import de.valcoms.orbisbuddy.entity.OrbisBuddyController;

import java.util.concurrent.ConcurrentHashMap;

public class GolemInstanceStore {
    private final ConcurrentHashMap<String, Object> ownerToEntity = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, OrbisBuddyController> ownerToController = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Object, String> entityToOwner = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Ref<EntityStore>> ownerToEntityRef = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Ref<EntityStore>> ownerToPlayerRef = new ConcurrentHashMap<>();

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

    public Ref<EntityStore> getEntityRef(String ownerId) {
        return ownerToEntityRef.get(ownerId);
    }

    public void setEntityRef(String ownerId, Ref<EntityStore> ref) {
        if (ref == null) {
            ownerToEntityRef.remove(ownerId);
            return;
        }
        ownerToEntityRef.put(ownerId, ref);
    }

    public Ref<EntityStore> getPlayerRef(String ownerId) {
        return ownerToPlayerRef.get(ownerId);
    }

    public void setPlayerRef(String ownerId, Ref<EntityStore> ref) {
        if (ownerId == null) {
            return;
        }
        if (ref == null) {
            ownerToPlayerRef.remove(ownerId);
        } else {
            ownerToPlayerRef.put(ownerId, ref);
        }
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
        ownerToEntityRef.remove(ownerId);
        ownerToPlayerRef.remove(ownerId);
    }

    public void removeByEntity(Object entityRef) {
        if (entityRef == null) {
            return;
        }
        String ownerId = entityToOwner.remove(entityRef);
        if (ownerId != null) {
            ownerToEntity.remove(ownerId, entityRef);
            ownerToController.remove(ownerId);
            ownerToEntityRef.remove(ownerId);
            ownerToPlayerRef.remove(ownerId);
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


    /** Convenience to avoid partial binding during spawn/rebind. */
    public void bind(String ownerId, Ref<EntityStore> ref, Object entity, OrbisBuddyController controller) {
        setEntity(ownerId, entity);
        setController(ownerId, controller);
        setEntityRef(ownerId, ref);
    }
}
