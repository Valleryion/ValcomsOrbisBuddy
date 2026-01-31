package de.valcoms.orbisbuddy.persistence;

import de.valcoms.orbisbuddy.model.GolemData;

public class JsonGolemSaveRepository implements GolemSaveRepository {

    @Override
    public GolemData load(Object worldRef, String ownerId) {
        //TODO:
        // 1) worldRoot aus worldRef ableiten (engine-spezifisch)
        // 2) Datei lesen (oder owner map) und ownerId selektieren
        // 3) JSON -> GolemData
        return null;
    }

    @Override
    public void save(Object worldRef, String ownerId, GolemData golemData) {
        // TODO:
        // 1) worldRoot ableiten
        // 2) orbisbuddy/ erstellen
        // 3) JSON schreiben (ownerId -> data)
    }
}
