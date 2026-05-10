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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class CentroUsuarioServiceImpl extends DefaultModelService<CentroUsuario> implements CentroUsuarioService {

    private static final List<String> TIPOS_PRESERVADOS = List.of("PROFESOR_EXTERNO", "SUPERVISOR");

    private static final Map<String, String> EX_MAPPING = Map.of(
            "PROFESOR", "EXPROFESOR",
            "ALUMNO",   "EXALUMNO",
            "FAMILIAR", "EXFAMILIAR"
    );

    @Inject
    ModelServiceFactory modelServiceFactory;

    public CentroUsuarioServiceImpl(Class<CentroUsuario> model, Repository repository) {
        super(model, (CentroUsuarioRepository) repository);
    }

    private CentroUsuarioRepository repo() {
        return (CentroUsuarioRepository) repository;
    }

    @Override
    @Transactional
    public List<String> calcularTiposUsuarioRegistrados(Long centroId, TipoUsuario tipoUsuario) {
        Centro centro = JpaRepository.of(Centro.class).find(centroId);
        if (centro == null) return List.of();

        List<String> codigosAfectados = codigosAfectados(tipoUsuario.getCodigo());

        List<CentroUsuario> registrados = repo().findByCentro(centroId);

        UsuarioAutorizadoService usuarioAutorizadoService =
                (UsuarioAutorizadoService) modelServiceFactory.resolve(UsuarioAutorizado.class);
        Map<String, List<TipoUsuario>> tiposPorDni = usuarioAutorizadoService.getByCentro(centro)
                .stream()
                .filter(ua -> codigosAfectados.contains(ua.getTipoUsuario().getCodigo()))
                .collect(Collectors.groupingBy(
                        UsuarioAutorizado::getDni,
                        Collectors.mapping(UsuarioAutorizado::getTipoUsuario, Collectors.toList())
                ));

        repo().deleteTiposUsuarioByCentroParaCodigos(centroId, codigosAfectados);

        for (CentroUsuario centroUsuario : registrados) {
            String dni = centroUsuario.getUsuario() != null ? centroUsuario.getUsuario().getDni() : null;
            for (TipoUsuario tipo : tiposPorDni.getOrDefault(dni, List.of())) {
                CentroUsuarioTipoUsuario cutu = new CentroUsuarioTipoUsuario();
                cutu.setTipoUsuario(tipo);
                centroUsuario.addCentroUsuarioTipoUsuario(cutu);
            }
            repository.save(centroUsuario);
        }

        return List.of();
    }

    private List<String> codigosAfectados(String codigoBase) {
        List<String> codigos = new ArrayList<>();
        if (!TIPOS_PRESERVADOS.contains(codigoBase)) {
            codigos.add(codigoBase);
        }
        String codigoEx = EX_MAPPING.get(codigoBase);
        if (codigoEx != null) {
            codigos.add(codigoEx);
        }
        return codigos;
    }
}