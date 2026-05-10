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
    public boolean isAuthorized(String dni) {
        return getUsuarioAutorizadoRepository().isAuthorized(dni);
    }

    @Override
    public List<TipoUsuario> getByCentroAndDni(Centro centro, String dni) {
        return getUsuarioAutorizadoRepository().findByCentroAndDocumento(centro, dni)
                .stream()
                .map(UsuarioAutorizado::getTipoUsuario)
                .toList();
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
    public void activarOInsertar(String dni, Centro centro, TipoUsuario tipoUsuario) {
        getUsuarioAutorizadoRepository()
                .findByCentroAndDocumento(centro, dni)
                .stream()
                .filter(ua -> ua.getTipoUsuario().getId().equals(tipoUsuario.getId()))
                .findFirst()
                .ifPresentOrElse(
                        ua -> { ua.setActivo(true); update(ua, null); },
                        () -> {
                            UsuarioAutorizado ua = new UsuarioAutorizado();
                            ua.setCentro(centro);
                            ua.setDni(dni);
                            ua.setTipoUsuario(tipoUsuario);
                            ua.setActivo(true);
                            insert(ua);
                        }
                );
    }

    @Override
    public void insertarInactivoSiNoExiste(String dni, Centro centro, TipoUsuario tipoUsuario) {
        boolean existe = getUsuarioAutorizadoRepository()
                .findByCentroAndDocumento(centro, dni)
                .stream()
                .anyMatch(ua -> ua.getTipoUsuario().getId().equals(tipoUsuario.getId()));
        if (existe) return;

        UsuarioAutorizado ua = new UsuarioAutorizado();
        ua.setCentro(centro);
        ua.setDni(dni);
        ua.setTipoUsuario(tipoUsuario);
        ua.setActivo(false);
        insert(ua);
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