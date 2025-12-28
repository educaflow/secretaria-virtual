package com.educaflow.apps.expedientes.db.repo;

import com.axelor.db.JPA;
import com.educaflow.apps.expedientes.db.TipoNumerador;

import jakarta.persistence.Query;

public class NumeradorRepository {
    public long getSiguienteNumeroExpediente(String centro, String anyo) {
        TipoNumerador tipoNumerador = TipoNumerador.Expediente;

        return getSiguienteNumero(tipoNumerador, centro, anyo);
    }





    private long getSiguienteNumero(TipoNumerador tipoNumerador, String centro, String anyo) {
        String sql = "INSERT INTO expedientes_numerador(id,tipo_numerador,centro, anyo, ultimo_numero)\n" +
                "        VALUES (nextval('expedientes_numerador_seq'),:tipoNumerador,:centro, :anyo, 1)\n" +
                "        ON CONFLICT (tipo_numerador,centro, anyo)\n" +
                "        DO UPDATE SET ultimo_numero = expedientes_numerador.ultimo_numero + 1\n" +
                "        RETURNING ultimo_numero";

        Query query = JPA.em().createNativeQuery(sql);
        query.setParameter("tipoNumerador", tipoNumerador.getValue());
        query.setParameter("centro", centro);
        query.setParameter("anyo", anyo);

        Object result = query.getSingleResult();
        if (result == null) {
            throw new IllegalStateException("No se puedo obtener el valor del numerador. TipoNumerador=" + tipoNumerador + " centro=" + centro + " Año=" + anyo);
        }
        long nextNumber = ((Number) result).longValue();

        return nextNumber;
    }




}
