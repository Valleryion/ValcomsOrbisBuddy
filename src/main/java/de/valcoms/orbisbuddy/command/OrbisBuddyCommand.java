package de.valcoms.orbisbuddy.command;

import de.valcoms.orbisbuddy.model.CombatMode;
import de.valcoms.orbisbuddy.model.FollowMode;
import de.valcoms.orbisbuddy.service.GolemService;

public class OrbisBuddyCommand {

    private final GolemService service;


    public OrbisBuddyCommand(GolemService service) {
        this.service = service;
    }

    public void handle(Object playerRef, String ownerId, String[] args) {
        if (args.length == 0) {
            return;
        }

        String sub = args[0].toLowerCase();

        switch (sub) {
            case "status" -> {
                //TODO: send status message to player
            }
            case "follow" -> service.setFollowMode(ownerId, FollowMode.FOLLOW);
            case "stay" -> service.setFollowMode(ownerId, FollowMode.STAY);
            case "assist" -> service.setCombatMode(ownerId, CombatMode.ASSIST);
            case "passive" -> service.setCombatMode(ownerId, CombatMode.PASSIVE);
            case "activate" -> {
                boolean initial = service.isOffline(ownerId);
                service.tryActivate(playerRef, ownerId, initial);
            }
            default -> {
                //TODO: send usage message
            }
        }

    }

}
