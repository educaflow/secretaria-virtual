package com.educaflow.subsystem.firmas.service;

import com.educaflow.subsystem.firmas.db.TareaFirma;

public interface FirmaNotifier {
    void notify(TareaFirma tareaFirma, Object callBackData);
}
