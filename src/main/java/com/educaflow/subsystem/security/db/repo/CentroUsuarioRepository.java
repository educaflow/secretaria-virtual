package com.educaflow.subsystem.security.db.repo;

import com.axelor.db.JpaRepository;
import com.axelor.db.Repository;
import com.educaflow.subsystem.common.db.Centro;
import com.educaflow.subsystem.security.db.CentroUsuario;
import com.educaflow.subsystem.security.db.TipoUsuario;

import java.util.List;

public class CentroUsuarioRepository extends AbstractCentroUsuarioRepository {

    public List<CentroUsuario> getCargosByCentro(Long centroId, String tipoCode){
        return all()
                .filter("self.centro.id = :centroId AND self.id IN " +
                        "(SELECT t.centroUsuario.id FROM CentroUsuarioTipoUsuario t " +
                        " WHERE t.centroUsuario.centro.id = :centroId AND t.tipoUsuario.code = :tipoCode)")
                .bind("centroId", centroId)
                .bind("tipoCode", tipoCode)
                .fetch();
    }

    /** Un único usuario del centro con ese tipo (para cargos únicos: director, secretario…) */
    public CentroUsuario findOneByCentroAndTipoUsuario(Long centroId, TipoUsuario tipoUsuario) {
        return all()
                .filter("self.centro.id = :centroId AND EXISTS " +
                        "(SELECT t FROM CentroUsuarioTipoUsuario t " +
                        " WHERE t.centroUsuario = self AND t.tipoUsuario = :tipoUsuario)")
                .bind("centroId", centroId)
                .bind("tipoUsuario", tipoUsuario)
                .fetchOne();
    }

    public CentroUsuario findOneByCentroAndTipoUsuario(Long centroId, String codigoTipo) {
        return all()
                .filter("self.centro.id = :centroId AND EXISTS " +
                        "(SELECT t FROM CentroUsuarioTipoUsuario t " +
                        " WHERE t.centroUsuario = self AND t.tipoUsuario.code = :codigoTipo)")
                .bind("centroId", centroId)
                .bind("codigoTipo", codigoTipo)
                .fetchOne();
    }

    /** Todos los usuarios del centro con ese tipo (para cargos múltiples: jefes de estudio…) */
    public List<CentroUsuario> findByCentroAndTipoUsuario(Long centroId, TipoUsuario tipoUsuario) {
        return all()
                .filter("self.centro.id = :centroId AND EXISTS " +
                        "(SELECT t FROM CentroUsuarioTipoUsuario t " +
                        " WHERE t.centroUsuario = self AND t.tipoUsuario = :tipoUsuario)")
                .bind("centroId", centroId)
                .bind("tipoUsuario", tipoUsuario)
                .fetch();
    }

    public List<CentroUsuario> findByCentroAndTipoUsuario(Long centroId, String codigoTipo) {
        return all()
                .filter("self.centro.id = :centroId AND EXISTS " +
                        "(SELECT t FROM CentroUsuarioTipoUsuario t " +
                        " WHERE t.centroUsuario = self AND t.tipoUsuario.code = :codigoTipo)")
                .bind("centroId", centroId)
                .bind("codigoTipo", codigoTipo)
                .fetch();
    }

    /** Versión por código de tipo (equivalente al extra-code del dominio) */
    public List<CentroUsuario> findByCentroAndCodigoTipo(Centro centro, String codigoTipo) {
        return all()
                .filter("self.centro = :centro AND EXISTS " +
                        "(SELECT t FROM CentroUsuarioTipoUsuario t " +
                        " WHERE t.centroUsuario = self AND t.tipoUsuario.code = :codigoTipo)")
                .bind("centro", centro)
                .bind("codigoTipo", codigoTipo)
                .fetch();
    }
}