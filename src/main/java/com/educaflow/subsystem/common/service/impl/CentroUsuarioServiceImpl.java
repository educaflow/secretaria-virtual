package com.educaflow.subsystem.common.service.impl;

import com.axelor.db.JpaRepository;
import com.axelor.db.Repository;
import com.axelor.db.modelservice.DefaultModelService;
import com.axelor.db.modelservice.ModelServiceFactory;
import com.educaflow.subsystem.common.db.Centro;
import com.educaflow.subsystem.common.db.CentroUsuario;
import com.educaflow.subsystem.common.db.CentroUsuarioTipoUsuario;
import com.educaflow.subsystem.common.db.TipoUsuario;
import com.educaflow.subsystem.common.db.repo.CentroUsuarioRepository;
import com.educaflow.subsystem.common.service.CentroUsuarioService;
import com.educaflow.subsystem.registrousuario.db.UsuarioAutorizado;
import com.educaflow.subsystem.registrousuario.service.UsuarioAutorizadoService;
import com.google.inject.Inject;
import com.google.inject.persist.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class CentroUsuarioServiceImpl extends DefaultModelService<CentroUsuario> implements CentroUsuarioService {

    private static final List<String> TIPOS_PRESERVADOS = List.of("PROFESOR_EXTERNO", "SUPERVISOR");

    @Inject
    ModelServiceFactory modelServiceFactory;

    public CentroUsuarioServiceImpl(Class<CentroUsuario> model, Repository<CentroUsuario> repository) {
        super(model, repository);
    }

    private CentroUsuarioRepository repo() {
        return (CentroUsuarioRepository) repository;
    }

    @Override
    public List<CentroUsuario> getCargosByCentro(Long centroId, String tipoCode) {
        return repo().getCargosByCentro(centroId, tipoCode);
    }

    @Override
    @Transactional
    public void calcularTiposUsuarioRegistrados(Long centroId) {
        Centro centro = JpaRepository.of(Centro.class).find(centroId);
        if (centro == null) return;

        List<CentroUsuario> anteriores = repo().findByCentro(centroId);

        UsuarioAutorizadoService usuarioAutorizadoService = (UsuarioAutorizadoService) modelServiceFactory.resolve(UsuarioAutorizado.class);
        List<UsuarioAutorizado> usuarioAutorizados = usuarioAutorizadoService.getByCentroAndCurso(centro, centro.getCurso());

        Map<String, List<TipoUsuario>> tiposPorDni = usuarioAutorizados.stream()
                .collect(Collectors.groupingBy(
                        UsuarioAutorizado::getDni,
                        Collectors.mapping(UsuarioAutorizado::getTipoUsuario, Collectors.toList())
                ));

        repo().deleteTiposUsuarioByCentroExcluyendo(centroId, TIPOS_PRESERVADOS);

        for (CentroUsuario centroUsuario : anteriores) {
            String dni = centroUsuario.getUsuario() != null ? centroUsuario.getUsuario().getDni() : null;
            for (TipoUsuario tipo : tiposPorDni.getOrDefault(dni, List.of())) {
                CentroUsuarioTipoUsuario cutu = new CentroUsuarioTipoUsuario();
                cutu.setTipoUsuario(tipo);
                centroUsuario.addCentroUsuarioTipoUsuario(cutu);
            }
            repository.save(centroUsuario);
        }
    }
}