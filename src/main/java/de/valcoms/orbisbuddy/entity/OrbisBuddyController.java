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
            // TODO: AI off, movement off, combat off, offline anim, prevent true death
            return;
        }

        FollowMode followMode = data.getSettings().getFollowMode();
        CombatMode combatMode = data.getSettings().getCombatMode();

        // TODO: follow controller enable/disable
        // TODO: combat controller enable/disable
    }

    public boolean interceptDeathOrDowned(int hp) {
        return hp <= 0;
    }
}
