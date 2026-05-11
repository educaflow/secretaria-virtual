package com.educaflow.subsystem.common.service.impl;

import com.axelor.db.JpaRepository;
import com.axelor.db.Repository;
import com.axelor.db.modelservice.DefaultModelService;
import com.axelor.db.modelservice.ModelServiceFactory;
import com.educaflow.subsystem.common.db.CentroUsuario;
import com.educaflow.subsystem.common.db.CentroUsuarioTipoUsuario;
import com.educaflow.subsystem.common.db.TipoUsuario;
import com.educaflow.subsystem.common.db.repo.CentroUsuarioRepository;
import com.educaflow.subsystem.common.service.CentroUsuarioService;
import com.educaflow.subsystem.common.service.TipoUsuarioService;
import com.educaflow.subsystem.registrousuario.db.UsuarioAutorizado;
import com.educaflow.subsystem.registrousuario.service.UsuarioAutorizadoService;
import com.google.inject.Inject;
import com.google.inject.persist.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public class CentroUsuarioServiceImpl extends DefaultModelService<CentroUsuario> implements CentroUsuarioService {

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

    private CentroUsuarioRepository getCentroUsuarioRepository() {
        return (CentroUsuarioRepository) repository;
    }

    @Override
    @Transactional
    public void calcularTiposUsuarioRegistrados(Long centroId, TipoUsuario tipoUsuario, boolean esActual) {
        TipoUsuarioService tipoUsuarioService =
                (TipoUsuarioService) modelServiceFactory.resolve(TipoUsuario.class);
        UsuarioAutorizadoService usuarioAutorizadoService =
                (UsuarioAutorizadoService) modelServiceFactory.resolve(UsuarioAutorizado.class);

        String codigoEx = EX_MAPPING.get(tipoUsuario.getCodigo());
        TipoUsuario tipoEx = codigoEx != null
                ? tipoUsuarioService.findByCodigo(codigoEx).orElse(null)
                : null;

        if (esActual) {
            // Paso 1: todos los que tenían el tipo base pasan a EX
            if (tipoEx != null) {
                getCentroUsuarioRepository().convertirTipo(centroId, tipoUsuario.getId(), tipoEx.getId());
            }

            // Paso 2: activos del fichero → recuperan el tipo base
            List<String> dnisActivos = usuarioAutorizadoService
                    .findActivosByCentroAndCodigo(centroId, tipoUsuario.getCodigo())
                    .stream().map(UsuarioAutorizado::getDni).toList();

            if (!dnisActivos.isEmpty()) {
                if (tipoEx != null) {
                    getCentroUsuarioRepository().deleteTipoParaDnis(centroId, tipoEx.getId(), dnisActivos);
                }
                for (CentroUsuario cu : getCentroUsuarioRepository().findByCentroAndUsuarioDniIn(centroId, dnisActivos)) {
                    agregarTipo(cu, tipoUsuario);
                }
            }
        } else {
            // Fichero histórico: añadir EX solo si el usuario no tiene ninguno de los tipos afectados
            if (tipoEx == null) return;
            List<String> codigosAfectados = List.of(tipoUsuario.getCodigo(), tipoEx.getCodigo());

            for (UsuarioAutorizado ua : usuarioAutorizadoService.findByCentroAndCodigoTipoUsuario(centroId, tipoUsuario.getCodigo())) {
                getCentroUsuarioRepository().findByCentroAndUsuarioDni(centroId, ua.getDni()).ifPresent(cu -> {
                    boolean tieneAfectado = cu.getCentroUsuarioTipoUsuario().stream()
                            .anyMatch(cutu -> codigosAfectados.contains(cutu.getTipoUsuario().getCodigo()));
                    if (!tieneAfectado) {
                        agregarTipo(cu, tipoEx);
                    }
                });
            }
        }
    }

    @Override
    public void agregarTipo(CentroUsuario cu, TipoUsuario tipo) {
        CentroUsuarioTipoUsuario cutu = new CentroUsuarioTipoUsuario();
        cutu.setCentroUsuario(cu);
        cutu.setTipoUsuario(tipo);
        JpaRepository.of(CentroUsuarioTipoUsuario.class).save(cutu);
    }

    @Override
    public Optional<CentroUsuario> findByCentroAndUsuarioDni(Long centroId, String dni) {
        return getCentroUsuarioRepository().findByCentroAndUsuarioDni(centroId, dni);
    }
}