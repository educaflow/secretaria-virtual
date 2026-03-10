package com.educaflow.shared.registroentradasalida.db.repo;

import com.educaflow.shared.registroentradasalida.db.RegistroEntrada;
import jakarta.persistence.PostPersist;
import jakarta.persistence.PostRemove;
import jakarta.persistence.PostUpdate;

public class RegistroEntradaListener {

    @PostPersist
    private void onPostPersist(RegistroEntrada registroEntrada) {

    }


    @PostUpdate
    private void onPostUpdate(RegistroEntrada registroEntrada) {

    }

    @PostRemove
    private void onPostRemove(RegistroEntrada registroEntrada) {

    }


}
