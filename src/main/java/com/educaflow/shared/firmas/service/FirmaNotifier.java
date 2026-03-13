package com.educaflow.shared.firmas.service;

import com.educaflow.shared.firma.db.Firma;

public interface FirmaNotifier {
    void notify(Firma firma, Object callBackData);
}
