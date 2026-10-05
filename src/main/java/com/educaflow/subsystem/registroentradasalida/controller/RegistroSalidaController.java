package com.educaflow.subsystem.registroentradasalida.controller;

import com.axelor.db.modelservice.BusinessMessages;
import com.axelor.db.modelservice.ModelServiceFactory;
import com.educaflow.base.infrastructure.fichero.Fichero;
import com.educaflow.subsystem.registroentradasalida.db.RegistroSalida;
import com.educaflow.subsystem.registroentradasalida.service.RegistroSalidaService;
import com.google.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.util.Map;
import java.util.Optional;

/**
 * La descarga pública de un registro de salida a partir de su CSV: es a donde lleva el código QR que
 * se estampa en el documento. No pide usuario: quien tiene el CSV tiene el documento.
 */
@Path(RegistroSalidaService.RUTA_DESCARGA_PUBLICA)
public class RegistroSalidaController {

    @Inject
    private ModelServiceFactory modelServiceFactory;

    /**
     * URL: /ws/public/registro-salida/download?CSV={csv}
     */
    @GET
    @Path("/download")
    public Response getDescargaByCsv(@QueryParam("CSV") String csv) {
        final RegistroSalidaService registroSalidaService = (RegistroSalidaService) modelServiceFactory.resolve(RegistroSalida.class);

        if (registroSalidaService.validateGetDescargaByCsv(csv).isPresent()) {
            return Response.status(Response.Status.NOT_FOUND).type(MediaType.TEXT_PLAIN).entity("No existe ningún documento con ese código de verificación.").build();
        }

        Fichero descarga = registroSalidaService.getDescargaByCsv(csv);

        return Response.ok(descarga.data(), descarga.mimeType())
                .header("Content-Disposition", "inline; filename=\"" + descarga.fileName() + "\"")
                .header("Cache-Control", "no-store")
                .build();
    }

    /************************************************************************************/
    /***************************** Acciones de Validaciones *****************************/
    /************************************************************************************/

    // Nadie lo llama (a una URL pública no llega ninguna vista que valide antes, así que la descarga
    // valida por sí misma): está para que la operación tenga su validate como las demás.
    @GET
    @Path("/validate-download")
    @Produces(MediaType.APPLICATION_JSON)
    public Response validateGetDescargaByCsv(@QueryParam("CSV") String csv) {
        final RegistroSalidaService registroSalidaService = (RegistroSalidaService) modelServiceFactory.resolve(RegistroSalida.class);

        Optional<BusinessMessages> validationResult = registroSalidaService.validateGetDescargaByCsv(csv);
        if (validationResult.isPresent()) {
            return Response.status(Response.Status.BAD_REQUEST).entity(Map.of("errors", validationResult.get())).build();
        }

        return Response.ok(Map.of("ok", true)).build();
    }

    /****************************************************************************/
    /***************************** Métodos privados *****************************/
    /****************************************************************************/

}
