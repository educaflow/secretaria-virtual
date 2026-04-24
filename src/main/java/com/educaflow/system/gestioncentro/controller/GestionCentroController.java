package com.educaflow.system.gestioncentro.controller;

import com.axelor.meta.CallMethod;
import com.axelor.rpc.ActionRequest;
import com.axelor.rpc.ActionResponse;
import com.google.inject.persist.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GestionCentroController {

    private final Logger logger = LoggerFactory.getLogger(GestionCentroController.class);

    /*@Inject
    GestionCentroService gestionCentroService;

    @Inject
    CentroService centroService;

    @Inject
    GestionCentroImporterService gestionCentroImporterService;*/

    // -------------------------------------------------------------------------
    // Importación de datos
    // -------------------------------------------------------------------------

    /*@CallMethod
    @Transactional
    public void importarDatos(ActionRequest request, ActionResponse response) {
        final Repository gestionCentroRepository = JpaRepository.of(GestionCentroRepository.class);
        final GestionCentroImporterService gestionCentroImporterService = (GestionCentroImporterService) ModelServiceFactory
                .resolve(GestionCentroImporterService.class, gestionCentroRepository);

        ViewGestionCentroImport view = null;
        boolean importacionExitosa = false;
        try {
            view = request.getContext().asType(ViewGestionCentroImport.class);
            Long centroId = view.getCentroId();
            List<GestionCentrosDataImport> ficheros = buildFicheros(view);

            List<String> logs = gestionCentroImporterService.importarFicheros(ficheros, centroId);


            importacionExitosa = logs.stream().noneMatch(l -> l.startsWith("Error"));
            response.setValue("importLog", String.join(System.lineSeparator(), logs));
            if (importacionExitosa) response.setNotify("Proceso de importación finalizado con éxito.");
        } catch (Exception e) {
            logger.error("Error durante la importación", e);
            throw new RuntimeException(e);
        } finally {
            if (view != null) {
                limpiarCampo(response, view.getProfesoresFile(), "profesoresFile", importacionExitosa);
                limpiarCampo(response, view.getAlumnosFile(),    "alumnosFile",    importacionExitosa);
                limpiarCampo(response, view.getFamiliaresFile(), "familiaresFile", importacionExitosa);
            }
        }
    }

    private List<GestionCentrosDataImport> buildFicheros(ViewGestionCentroImport view) {
        List<GestionCentrosDataImport> ficheros = new ArrayList<>();
        addSiPresente(ficheros, view.getProfesoresFile(), view.getProfesoresConfigPath(), view.getProfesoresSchemaPath(), "PROFESOR");
        addSiPresente(ficheros, view.getAlumnosFile(),    view.getAlumnosConfigPath(),    view.getAlumnosSchemaPath(),    "ALUMNO");
        addSiPresente(ficheros, view.getFamiliaresFile(), view.getFamiliaresConfigPath(), view.getFamiliaresSchemaPath(), "FAMILIAR");
        return ficheros;
    }

    private void addSiPresente(List<GestionCentrosDataImport> ficheros, MetaFile file,
                               String config, String schema, String tipoUsuarioCode) {
        if (file == null) return;
        byte[] data = MetaFileUtil.downloadContent(file);
        if (data != null) ficheros.add(new GestionCentrosDataImport(tipoUsuarioCode, new DataImport(data, config, schema), file));
    }

    private void limpiarCampo(ActionResponse response, MetaFile file, String fieldName, boolean mantenerFichero) {
        response.setValue(fieldName, null);
        if (!mantenerFichero && file != null) MetaFileUtil.delete(file);
    }*/

    // -------------------------------------------------------------------------
    // Cambio de curso
    // -------------------------------------------------------------------------

    @CallMethod
    public void cargarCursosDisponibles(ActionRequest request, ActionResponse response) {
        /*final Repository gestionCentroRepository = JpaRepository.of(GestionCentroRepository.class);
        final GestionCentroImporterService gestionCentroImporterService = (GestionCentroImporterService) ModelServiceFactory
                .resolve(GestionCentroImporterService.class, gestionCentroRepository);

        Object raw = request.getContext().get("centroId");
        if (raw == null) return;
        Long centroId = ((Number) raw).longValue();

        List<String> cursos = gestionCentroService.getCursosDisponibles(centroId);

        String texto = cursos.isEmpty()
                ? "No hay cursos disponibles. Se necesitan los 3 ficheros importados (profesores, alumnos y familiares)."
                : "Cursos disponibles: " + String.join(", ", cursos);

        response.setValue("cursosDisponibles", texto);*/
    }

    @CallMethod
    @Transactional
    public void cambiarCurso(ActionRequest request, ActionResponse response) {
        /*Object raw = request.getContext().get("centroId");
        if (raw == null) {
            response.setError("No se ha podido determinar el centro.");
            return;
        }
        Long centroId = ((Number) raw).longValue();

        Integer nuevoCurso = (Integer) request.getContext().get("nuevoCurso");
        if (nuevoCurso == null || nuevoCurso == 0) {
            response.setError("Debe introducir el curso destino.");
            return;
        }

        Centro centro = JpaRepository.of(Centro.class).find(centroId);

        if (nuevoCurso.equals(centro.getCurso())) {
            response.setError("El curso " + nuevoCurso + " ya es el curso activo del centro.");
            return;
        }

        List<String> cursosDisponibles = gestionCentroService.getCursosDisponibles(centroId);
        if (!cursosDisponibles.contains(String.valueOf(nuevoCurso))) {
            response.setError("El curso " + nuevoCurso + " no tiene los 3 ficheros de importación (profesores, alumnos y familiares). Cursos disponibles: "
                    + String.join(", ", cursosDisponibles));
            return;
        }

        Integer cursoAnterior = centro.getCurso();
        List<String> resultado = centroService.calcularTipoUsuarioRegistrado(centroId, nuevoCurso);

        centro.setCurso(nuevoCurso);
        JpaRepository.of(Centro.class).save(centro);

        resultado.addFirst("Cambio de curso: " + cursoAnterior + " → " + nuevoCurso);
        response.setValue("resultado", String.join(System.lineSeparator(), resultado));
        response.setNotify("Cambio de curso completado.");*/
    }

    /*@CallMethod
    public void validarMismoCentro(ActionRequest request, ActionResponse response) {
        final GestionCentroImporterService gestionCentroImporterService = (GestionCentroImporterService) ModelServiceFactory.resolve(TareaCentroImportacion.class, null);
        BusinessMessages errors = new BusinessMessages();


        ActionRequestHelper<ViewGestionCentroImport> actionRequestHelper = new ActionRequestHelper(request, ViewGestionCentroImport.class);
        ActionResponseHelper actionResponseHelper = new ActionResponseHelper(response);

        AllowProperties allowProperties = AllowProperties.createAllowAllProperties();
        ViewGestionCentroImport viewGestionCentroImport  = actionRequestHelper.getModel(allowProperties);

        Long centroId = viewGestionCentroImport.getCentroId();
        List<GestionCentrosDataImport> ficheros = buildFicheros(viewGestionCentroImport);

        for (GestionCentrosDataImport f : ficheros) {
            Optional<BusinessMessage> validationResult = gestionCentroImporterService.validarCodigoCentro(new ByteArrayInputStream(f.dataImport().data()), centroId);
            validationResult.ifPresent(errors::add);
        }

        if (errors.hasMessages()) {
            actionResponseHelper.doResponseBusinessMessagesAsError(validationResult.get());
        }
    }*/
}