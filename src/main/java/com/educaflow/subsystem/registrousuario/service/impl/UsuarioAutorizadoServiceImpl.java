package com.educaflow.subsystem.registrousuario.service.impl;

import com.axelor.db.JpaRepository;
import com.axelor.db.Repository;
import com.axelor.db.modelservice.DefaultModelService;
import com.educaflow.subsystem.common.db.Centro;
import com.educaflow.subsystem.common.db.TipoUsuario;
import com.educaflow.subsystem.registrousuario.db.UsuarioAutorizado;
import com.educaflow.subsystem.registrousuario.db.repo.UsuarioAutorizadoRepository;
import com.educaflow.subsystem.registrousuario.service.UsuarioAutorizadoService;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

public class UsuarioAutorizadoServiceImpl extends DefaultModelService<UsuarioAutorizado> implements UsuarioAutorizadoService {

    private static final Map<String, String> EX_MAPPING = Map.of(
            "PROFESOR",  "EXPROFESOR",
            "ALUMNO",    "EXALUMNO",
            "FAMILIAR",  "EXFAMILIAR"
    );

    public UsuarioAutorizadoServiceImpl(Class<UsuarioAutorizado> model, Repository<UsuarioAutorizado> repository) {
        super(model, (UsuarioAutorizadoRepository) repository);
    }

    private UsuarioAutorizadoRepository getUsuarioAutorizadoRepository() {
        return (UsuarioAutorizadoRepository) repository;
    }

    @Override
    public List<UsuarioAutorizado> getByCentro(Centro centro) {
        Map<String, TipoUsuario> tiposPorCodigo = JpaRepository.of(TipoUsuario.class).all().fetch()
                .stream().collect(Collectors.toMap(TipoUsuario::getCodigo, t -> t, (a, b) -> a));

        List<UsuarioAutorizado> resultado = new ArrayList<>();
        for (UsuarioAutorizado ua : getUsuarioAutorizadoRepository().findByCentro(centro.getId())) {
            if (Boolean.TRUE.equals(ua.getActivo())) {
                resultado.add(ua);
            } else {
                UsuarioAutorizado uaEx = clonarComoEx(ua, tiposPorCodigo);
                if (uaEx != null) resultado.add(uaEx);
            }
        }
        return resultado;
    }

    @Override
    public void marcarTodosInactivos(Centro centro, TipoUsuario tipoUsuario) {
        getUsuarioAutorizadoRepository().marcarTodosInactivos(centro.getId(), tipoUsuario.getId());
    }

    @Override
    public Optional<UsuarioAutorizado> findByCentroAndDniAndTipoUsuario(Centro centro, String dni, TipoUsuario tipoUsuario) {
        return getUsuarioAutorizadoRepository().findByCentroAndDocumentoAndTipoUsuario(centro, dni, tipoUsuario);
    }

    @Override
    public List<UsuarioAutorizado> findByCentroAndCodigoTipoUsuario(Long centroId, String codigoTipoUsuario) {
        return getUsuarioAutorizadoRepository().findByCentroAndCodigoTipoUsuario(centroId, codigoTipoUsuario);
    }

    @Override
    public List<UsuarioAutorizado> findActivosByCentroAndCodigo(Long centroId, String codigoTipoUsuario) {
        return getUsuarioAutorizadoRepository().findActivosByCentroAndCodigo(centroId, codigoTipoUsuario);
    }

    private UsuarioAutorizado clonarComoEx(UsuarioAutorizado ua, Map<String, TipoUsuario> tiposPorCodigo) {
        String exCode = EX_MAPPING.get(ua.getTipoUsuario().getCodigo());
        if (exCode == null) return null;
        TipoUsuario tipoEx = tiposPorCodigo.get(exCode);
        if (tipoEx == null) return null;

        UsuarioAutorizado uaEx = new UsuarioAutorizado();
        uaEx.setCentro(ua.getCentro());
        uaEx.setDni(ua.getDni());
        uaEx.setTipoUsuario(tipoEx);
        return uaEx;
    }
}