package de.valcoms.orbisbuddy.service;

import de.valcoms.orbisbuddy.entity.OrbisBuddyController;
import de.valcoms.orbisbuddy.model.GolemData;

public class DefaultGolemRuntimeAdapter implements GolemRuntimeAdapter {

    private final GolemInstanceStore store;

    public DefaultGolemRuntimeAdapter(GolemInstanceStore store) {
        this.store = store;
    }

    @Override
    public void applyState(String ownerId, GolemData data) {
        OrbisBuddyController controller = store.getController(ownerId);
        if (controller == null) {
            //TODO (engine): log missing controller/entity for ownerId
            return;
        }

        controller.applyState(data);
    }
}
