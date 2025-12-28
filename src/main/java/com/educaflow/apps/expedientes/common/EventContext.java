package com.educaflow.apps.expedientes.common;


import com.educaflow.apps.configuracioncentro.db.Centro;

public class EventContext<Profile extends Enum<Profile>> {

    final private Profile profile;
    final private Centro centro;

    public EventContext(Profile profile, Centro centro) {
        this.profile = profile;
        this.centro = centro;
    }

    public Profile getProfile() {
        return profile;
    }
    public Centro getCentro() { return centro; }

    public String toString() {
        return "EventContext [profile=" + profile + ", centro=" + centro + "]";
    }
}
