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
import java.util.Objects;
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

    private UsuarioAutorizadoRepository repo() {
        return (UsuarioAutorizadoRepository) repository;
    }

    @Override
    public boolean isAuthorized(String dni) {
        return repo().isAuthorized(dni);
    }

    @Override
    public List<UsuarioAutorizado> getByCentroAndCurso(Centro centro, Integer curso) {
        Map<String, TipoUsuario> tiposPorCodigo = JpaRepository.of(TipoUsuario.class).all().fetch()
                .stream().collect(Collectors.toMap(TipoUsuario::getCode, t -> t, (a, b) -> a));

        List<UsuarioAutorizado> resultado = new ArrayList<>();
        for (UsuarioAutorizado ua : repo().findByCentroHastaCurso(centro.getId(), curso)) {
            if (Objects.equals(ua.getCurso(), curso)) {
                resultado.add(ua);
            } else {
                UsuarioAutorizado uaEx = clonarComoEx(ua, tiposPorCodigo);
                if (uaEx != null) resultado.add(uaEx);
            }
        }
        return resultado;
    }

    private UsuarioAutorizado clonarComoEx(UsuarioAutorizado ua, Map<String, TipoUsuario> tiposPorCodigo) {
        String exCode = EX_MAPPING.get(ua.getTipoUsuario().getCode());
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