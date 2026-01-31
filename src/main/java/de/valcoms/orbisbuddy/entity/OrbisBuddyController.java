package de.valcoms.orbisbuddy.entity;

import de.valcoms.orbisbuddy.model.CombatMode;
import de.valcoms.orbisbuddy.model.FollowMode;
import de.valcoms.orbisbuddy.model.GolemData;
import de.valcoms.orbisbuddy.model.GolemState;

public class OrbisBuddyController {
    private final OrbisBuddyEntity entity;

    public OrbisBuddyController(OrbisBuddyEntity entity) {
        this.entity = entity;
    }

    public void applyState(GolemData data) {
        if (data.getState() == GolemState.OFFLINE) {
            setAiEnabled(false);
            setMovementEnabled(false);
            setCombatEnabled(false);
            // TODO(engine): set offline animation/state
            return;
        }

        setAiEnabled(true);
        setMovementEnabled(true);
        setCombatEnabled(true);

        FollowMode followMode = data.getSettings().getFollowMode();
        CombatMode combatMode = data.getSettings().getCombatMode();

        // TODO: follow controller enable/disable
        // TODO: combat controller enable/disable
    }

    private void setAiEnabled(boolean enabled) {
        // TODO(engine): toggle AI for entity
    }

    private void setMovementEnabled(boolean enabled) {
        // TODO(engine): toggle movement for entity
    }

    private void setCombatEnabled(boolean enabled) {
        // TODO(engine): toggle combat for entity
    }

    public boolean interceptDeathOrDowned(int hp) {
        return hp <= 0;
    }
}
