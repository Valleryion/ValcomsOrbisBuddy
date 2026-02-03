package de.valcoms.orbisbuddy.model;

public class GolemData {
    private String ownerId;
    private GolemState state = GolemState.OFFLINE;
    private GolemSettings settings = new GolemSettings();

    private double x;
    private double y;
    private double z;
    private float health = -1.0f;

    public GolemData() {
    }

    public GolemData(String ownerId) {
        this.ownerId = ownerId;
    }

    public String getOwnerId() {
        return ownerId;
    }

    public void setOwnerId(String ownerId) {
        this.ownerId = ownerId;
    }

    public GolemState getState() {
        return state;
    }

    public void setState(GolemState state) {
        this.state = state;
    }

    public GolemSettings getSettings() {
        return settings;
    }

    public void setSettings(GolemSettings settings) {
        this.settings = settings;
    }

    public void setPos(double x, double y, double z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public double getX() {
        return x;
    }

    public double getY() {
        return y;
    }

    public double getZ() {
        return z;
    }

    public float getHealth() {
        return health;
    }

    public void setHealth(float health) {
        this.health = health;
    }
}
