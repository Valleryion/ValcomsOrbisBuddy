package de.valcoms.orbisbuddy.service;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.server.core.entity.Entity;
import com.hypixel.hytale.server.core.entity.EntityUtils;
import com.hypixel.hytale.server.core.entity.LivingEntity;
import com.hypixel.hytale.server.core.inventory.Inventory;
import com.hypixel.hytale.server.core.inventory.ResourceQuantity;
import com.hypixel.hytale.server.core.inventory.container.ItemContainer;
import com.hypixel.hytale.server.core.inventory.transaction.ResourceTransaction;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import de.valcoms.orbisbuddy.ids.OrbisBuddyIds;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

public class InventoryService {
    public boolean hasItem(Object playerRef, String itemId, int count) {
        return withInventory(playerRef, container -> {
            int total = countItems(container, itemId);
            return total >= count;
        }, false);
    }

    public boolean consumeItem(Object playerRef, String itemId, int count) {
        return withInventory(playerRef, container -> {
            ResourceQuantity resource = new ResourceQuantity(itemId, count);
            if (!container.canRemoveResource(resource)) {
                return false;
            }

            ResourceTransaction transaction = container.removeResource(resource);
            return transaction != null && transaction.getRemainder() == 0;
        }, false);
    }

    public boolean hasEnergyCore(Object playerRef) {
        return hasItem(playerRef, OrbisBuddyIds.ITEM_ENERGY_CORE, 1);
    }

    public boolean consumeEnergyCore(Object playerRef, int count) {
        return consumeItem(playerRef, OrbisBuddyIds.ITEM_ENERGY_CORE, count);
    }

    private <T> T withInventory(Object playerRef, java.util.function.Function<ItemContainer, T> action, T fallback) {
        if (!(playerRef instanceof Ref<?> ref)) {
            return fallback;
        }

        @SuppressWarnings("unchecked")
        Ref<EntityStore> entityRef = (Ref<EntityStore>) ref;
        EntityStore entityStore = entityRef.getStore().getExternalData();
        World world = entityStore.getWorld();
        if (world == null) {
            return fallback;
        }

        CompletableFuture<T> future = new CompletableFuture<>();
        world.execute(() -> {
            try {
                Entity entity = EntityUtils.getEntity(entityRef, entityStore.getStore());
                if (!(entity instanceof LivingEntity living)) {
                    future.complete(fallback);
                    return;
                }

                Inventory inventory = living.getInventory();
                if (inventory == null) {
                    future.complete(fallback);
                    return;
                }

                ItemContainer container = inventory.getCombinedEverything();
                if (container == null) {
                    future.complete(fallback);
                    return;
                }

                future.complete(action.apply(container));
            } catch (Throwable throwable) {
                future.completeExceptionally(throwable);
            }
        });

        try {
            return future.get();
        } catch (InterruptedException interruptedException) {
            Thread.currentThread().interrupt();
            return fallback;
        } catch (ExecutionException executionException) {
            return fallback;
        }
    }

    private int countItems(ItemContainer container, String itemId) {
        final int[] total = {0};
        container.forEach((slot, itemStack) -> {
            if (itemStack == null || itemStack.isEmpty()) {
                return;
            }
            if (itemId.equals(itemStack.getItemId())) {
                total[0] += itemStack.getQuantity();
            }
        });
        return total[0];
    }
}
