package com.educaflow.subsystem.registrousuario.controllers;

import com.axelor.db.modelservice.ModelServiceFactory;
import com.educaflow.base.infrastructure.validation.messages.BusinessException;
import com.axelor.db.modelservice.BusinessMessages;
import com.educaflow.base.util.DniUtil;
import com.educaflow.subsystem.registrousuario.db.RegistroPendiente;
import com.educaflow.subsystem.registrousuario.service.RegistroPendienteService;
import com.google.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;

@Path("/public/registro")
public class RegistroController {

    private final Logger logger = LoggerFactory.getLogger(RegistroController.class);

    @Inject
    private ModelServiceFactory modelServiceFactory;

    /**
     * Sirve la página HTML del formulario de registro.
     * URL: /ws/public/registro
     */
    @GET
    @Produces(MediaType.TEXT_HTML)
    public Response index() {
        return recursoEstatico("/web/registro.html", MediaType.TEXT_HTML);
    }


    /**
     * Paso 1: valida email/DNI, guarda el registro pendiente y devuelve el token de sesión.
     * Body: { "email": "...", "dni": "...", "tipoDoc": "..." }
     * Respuesta OK: { "token": "..." }
     */
    @POST
    @Path("/registrosPendientes")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response guardarRegistroPendiente(Map<String, String> body) {
        RegistroPendienteService registroPendienteService = (RegistroPendienteService) modelServiceFactory.resolve(RegistroPendiente.class);

        String email   = body.getOrDefault("email", "").trim();
        String dni     = body.getOrDefault("dni", "").trim();
        String tipoDoc = body.getOrDefault("tipoDoc", "OTRO").trim();

        if (tipoDoc.contains("DNI")) {
            dni = DniUtil.clean(dni);
        }

        try {
            RegistroPendiente registroPendiente = new RegistroPendiente();
            registroPendiente.setDni(dni);
            registroPendiente.setEmail(email);
            RegistroPendiente nuevoRegistroPendiente = registroPendienteService.insertar(registroPendiente);
            return Response.ok(Map.of("token", nuevoRegistroPendiente.getToken())).build();
        } catch (BusinessException businessException) {
            return errors(businessException.getBusinessMessages());
        } catch (Exception e) {
            logger.error("Error en iniciarRegistro", e);
            return error("Error interno. Inténtelo de nuevo más tarde.");
        }
    }

    /**
     * Paso 2: valida el código recibido por email.
     * Body: { "token": "...", "codigo": "..." }
     * Respuesta OK: { "ok": true }
     */
    @POST
    @Path("/validarCodigo")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response verificarEmail(Map<String, String> body) {
        RegistroPendienteService registroPendienteService = (RegistroPendienteService) modelServiceFactory.resolve(RegistroPendiente.class);

        String token = body.getOrDefault("token", "").trim();
        String codigo = body.getOrDefault("codigo", "").trim();

        try {
            registroPendienteService.validarCodigo(codigo, token);
            return Response.ok(Map.of("ok", true)).build();
        } catch (BusinessException businessException) {
            return errors(businessException.getBusinessMessages());
        } catch (Exception e) {
            logger.error("Error en verificarEmail", e);
            return error("Error interno. Inténtelo de nuevo más tarde.");
        }
    }

    /**
     * Paso 3: crea el usuario.
     * Body: { "token": "...", "nombre": "...", "apellidos": "...", "password": "...", "passwordRepeat": "..." }
     * Respuesta OK: { "ok": true }
     */
    @POST
    @Path("/usuarios")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response registrarUsuario(Map<String, String> body) {
        /*RegistroService registroService = (RegistroService) modelServiceFactory.resolve(Registro.class);

        String token         = body.getOrDefault("token", "").trim();
        String nombre        = body.getOrDefault("nombre", "").trim();
        String apellidos     = body.getOrDefault("apellidos", "").trim();
        String password      = body.getOrDefault("password", "");
        String passwordRepeat = body.getOrDefault("passwordRepeat", "");
        String idioma        = body.getOrDefault("idioma", "es").trim();

        DatosBasicosUsuario datos = new DatosBasicosUsuario(nombre, apellidos, password, passwordRepeat, idioma);

        try {
            registroService.registrarUsuario(datos, token);
            return Response.ok(Map.of("ok", true)).build();
        } catch (BusinessException businessException) {
            return errors(businessException.getBusinessMessages());
        } catch (Exception e) {
            logger.error("Error en registrarUsuario", e);
            return error("Error interno. Inténtelo de nuevo más tarde.");
        }*/
        return Response.ok(Map.of("ok", true)).build();
    }

    private Response recursoEstatico(String classpath, String mediaType) {
        try (InputStream is = getClass().getResourceAsStream(classpath)) {
            if (is == null) {
                return Response.status(Response.Status.NOT_FOUND).build();
            }
            String content = new String(is.readAllBytes(), StandardCharsets.UTF_8);
            return Response.ok(content, mediaType).build();
        } catch (Exception e) {
            logger.error("Error sirviendo {}", classpath, e);
            return Response.serverError().build();
        }
    }

    private Response error(String mensaje) {
        return errors(BusinessMessages.single(mensaje));
    }

    private Response errors(BusinessMessages businessMessages) {
        return Response.status(Response.Status.BAD_REQUEST)
                .type(MediaType.APPLICATION_JSON)
                .entity(Map.of("errors", businessMessages))
                .build();
    }

    @GET
    @Path("/registro.css")
    @Produces("text/css")
    public Response css() {
        return recursoEstatico("/web/registro.css", "text/css");
    }

    @GET
    @Path("/registro.js")
    @Produces("application/javascript")
    public Response js() {
        return recursoEstatico("/web/registro.js", "application/javascript");
    }

}
