package com.educaflow.subsystem.firmas.service;

import com.educaflow.subsystem.firma.db.Firma;

public interface FirmaNotifier {
    void notify(Firma firma, Object callBackData);
}
