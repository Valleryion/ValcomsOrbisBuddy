package de.valcoms.orbisbuddy.ids;

public final class OrbisBuddyIds {
    private OrbisBuddyIds() {}

    /** Stable plugin namespace (resource IDs for items/entities you register yourself). */
    public static final String MOD_ID = "de.valcoms.ValcomsOrbisBuddy";

    /** Custom entity id (only works once you have a real entity registration + asset mapping). */
    public static final String ENTITY_GOLEM = MOD_ID + ":orbis_golem";

    /** Item IDs (resource IDs). */
    public static final String ITEM_ENERGY_CORE = MOD_ID + ":energy_core";
    public static final String ITEM_LOST_CORE   = MOD_ID + ":lost_energy_core";
    public static final String ITEM_GOLEM_CONTROLLER = MOD_ID + "Item/Items/Golem_Controller";
    public static final String ITEM_GOLEM_FOLLOW_CONTROLLER = MOD_ID + "Item/Items/Golem_Follow_Controller";

    /**
     * Command names as registered in the engine.
     * Your AbstractCommand constructors already use these plain names.
     */
    public static final String CMD_GOLEM = "golem";
    public static final String CMD_DEBUG = "bdebug";

    /**
     * NPC Role spawn keys for the TestDummy pack:
     * server/mods/Valcoms.valcoms_orbisbuddy_testdummy/Server/NPC/Roles/Undead/Skeleton/OrbisBuddy_TestDummy.json
     *
     * Depending on engine build, spawn key can be either plain asset id or folder-qualified.
     */
    public static final String NPCROLE_ORBISBUDDY_TESTDUMMY = "OrbisBuddy_TestDummy";
    public static final String NPCROLE_ORBISBUDDY_TESTDUMMY_QUALIFIED = "Undead/Skeleton/OrbisBuddy_TestDummy";

    public static final String NPCROLE_ORBISBUDDY_FOLLOW_ASSIST = "Valcoms_Orbis_Buddy_Role_Follow_Assist";
    public static final String NPCROLE_ORBISBUDDY_FOLLOW_PASSIVE = "Valcoms_Orbis_Buddy_Role_Follow_Passive";
    public static final String NPCROLE_ORBISBUDDY_STAY_ASSIST = "Valcoms_Orbis_Buddy_Role_Stay_Assist";
    public static final String NPCROLE_ORBISBUDDY_STAY_PASSIVE = "Valcoms_Orbis_Buddy_Role_Stay_Passive";
    public static final String NPCROLE_ORBISBUDDY_OFFLINE = "Valcoms_Orbis_Buddy_Role_Offline";

    public static final String NPCROLE_ORBISBUDDY_FOLLOW_ASSIST_QUALIFIED = "Undead/Skeleton/Skeleton/Valcoms_Orbis_Buddy_Role_Follow_Assist";
    public static final String NPCROLE_ORBISBUDDY_FOLLOW_PASSIVE_QUALIFIED = "Undead/Skeleton/Skeleton/Valcoms_Orbis_Buddy_Role_Follow_Passive";
    public static final String NPCROLE_ORBISBUDDY_STAY_ASSIST_QUALIFIED = "Undead/Skeleton/Skeleton/Valcoms_Orbis_Buddy_Role_Stay_Assist";
    public static final String NPCROLE_ORBISBUDDY_STAY_PASSIVE_QUALIFIED = "Undead/Skeleton/Skeleton/Valcoms_Orbis_Buddy_Role_Stay_Passive";
    public static final String NPCROLE_ORBISBUDDY_OFFLINE_QUALIFIED = "Undead/Skeleton/Skeleton/Valcoms_Orbis_Buddy_Role_Offline";
}
