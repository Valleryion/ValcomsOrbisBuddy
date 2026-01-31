package de.valcoms.orbisbuddy.model;

public class GolemSettings {
    private FollowMode followMode = FollowMode.FOLLOW;
    private CombatMode combatMode = CombatMode.ASSIST;

    private String customName;
    private Integer tintVariant;

    public FollowMode getFollowMode() {
        return followMode;
    }

    public void setFollowMode(FollowMode followMode) {
        this.followMode = followMode;
    }

    public CombatMode getCombatMode() {
        return combatMode;
    }

    public void setCombatMode(CombatMode combatMode) {
        this.combatMode = combatMode;
    }

    public String getCustomName() {
        return customName;
    }

    public void setCustomName(String customName) {
        this.customName = customName;
    }

    public Integer getTintVariant() {
        return tintVariant;
    }

    public void setTintVariant(Integer tintVariant) {
        this.tintVariant = tintVariant;
    }
}
