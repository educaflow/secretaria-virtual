package com.educaflow.system.ventanilla.service;

import com.axelor.auth.db.User;
import com.educaflow.subsystem.expedientes.db.Expediente;
import com.educaflow.subsystem.expedientes.db.Profile;

import java.util.List;

/**
 * Lo que las bandejas de expedientes de la ventanilla necesitan saber de un usuario: qué expedientes
 * le tocan y con qué perfiles los ve.
 *
 * <p>No es un {@code ModelService}: no gestiona el ciclo de vida de ninguna entidad, solo consulta.
 * Su binding está en {@code VentanillaModule}.
 */
public interface BandejaService {

    /**
     * Los ids de los expedientes abiertos de los centros del usuario que están esperando a que actúe
     * él <b>como tramitador</b>: el perfil que declara su estado actual no es {@code CREADOR} y el
     * usuario lo ostenta sobre ese expediente. Vacía si no hay ninguno.
     */
    List<Long> idsPendientesDeMi(User user);

    /**
     * El perfil con el que el usuario abre el expediente: el que declara su estado actual si lo
     * ostenta y, si no, el de mayor prioridad de los suyos. {@code UnauthorizedException} si no tiene
     * ninguno.
     */
    Profile perfilConElQueAbrir(Expediente expediente, User user);
}
