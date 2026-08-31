package com.educaflow.subsystem.expedientes.services.internal;

import com.axelor.db.JPA;
import com.axelor.db.JpaRepository;
import com.axelor.db.JpaSecurity;
import com.axelor.inject.Beans;
import com.educaflow.base.infrastructure.pdf.DocumentoPdf;
import com.educaflow.base.infrastructure.pdf.DocumentoPdfFactory;
import com.educaflow.base.infrastructure.pdf.DocumentoPdfUtil;
import com.educaflow.subsystem.expedientes.db.Expediente;
import com.educaflow.subsystem.expedientes.services.eventmanager.State;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.Map;

public class ExpedienteUtil {

    public static DocumentoPdf getDocumentoPdf(Expediente expediente,String documentoPdfFileName ) {
        try {
            Class<?> callerClass = expediente.getClass();

            Path pathFileName= Path.of(documentoPdfFileName);

            try (InputStream in = callerClass.getResourceAsStream(documentoPdfFileName)) {
                if (in == null) {
                    throw new IOException("No se encontró el recurso: " + documentoPdfFileName);
                }
                DocumentoPdf documentoPdfVacio= DocumentoPdfFactory.getDocumentoPdf(in.readAllBytes(), pathFileName.getFileName().toString());

                Map<String, Object> contexto = Map.of("self", expediente,"now", java.time.LocalDateTime.now());

                DocumentoPdf documentoPdfRelleno = DocumentoPdfUtil.generate(documentoPdfVacio, contexto);

                return documentoPdfRelleno;

            }
        } catch (IOException e) {
            throw new RuntimeException("Error al cargar el documento PDF: " + documentoPdfFileName, e);
        }
    }

    /**
     * Lleva el expediente a un estado. Es el <b>único</b> sitio que escribe la pareja
     * {@code (codePhase, codeState)} y el único que decide si el estado no ha cambiado.
     */
    public static void updateState(Expediente expediente, State state) {
        if (state == null) {
            throw new IllegalArgumentException("El state no puede ser nulo.");
        }

        String phaseCode = state.getPhase().getCode();
        String stateCode = state.getCode();

        // Barrera cross-tipo: los State generados son singletons, así que el estado de ESTE tipo de
        // expediente con estos códigos debe ser el mismo objeto que el recibido. Sin genéricos en
        // EventContext, un State de otro tipo compila, y el caso es realista: crear una versión
        // nueva es duplicar la carpeta de la anterior, y las dos tienen una clase llamada States.
        State propio = expediente.getTipoExpediente().getTipoExpedienteStates()
                .getState(phaseCode, stateCode).orElse(null);
        if (propio != state) {
            throw new IllegalArgumentException("El estado " + phaseCode + "/" + stateCode
                    + " no es del tipo de expediente " + expediente.getTipoExpediente().getCode()
                    + ": o no existe en su máquina de estados o el State es de la clase States de"
                    + " otro tipo (típicamente un import sin actualizar al duplicar una versión).");
        }

        if (stateCode.equals(expediente.getCodeState())
                && phaseCode.equals(expediente.getCodePhase())) {
            return;
        }

        expediente.setCodePhase(phaseCode);
        expediente.setNamePhase(state.getPhase().getName());
        expediente.setCodeState(stateCode);
        expediente.setNameState(state.getName());
        expediente.setFechaUltimoEstado(LocalDateTime.now());
        expediente.setAbierto(state.isFinal() == false);
    }

    public static Expediente getExpedienteFromIdExpediente(long idExpediente) {
        Class<? extends Expediente> claseConcreta = getClaseConcreta(idExpediente);

        // El idExpediente lo envía el cliente en el JSON (ExpedienteController.viewExpediente,
        // triggerEvent y validateChild; FirmaController) y JpaRepository.find delega en em.find sin
        // ningún filtro de fila, así que sin esta comprobación cualquier usuario autenticado puede
        // leer cualquier expediente por id.
        // La clase contra la que se comprueba sale de la BD, NUNCA del _model que envía el cliente,
        // que es precisamente el vector de bypass. Y MUST NOT filtrarse por self.centro: la
        // autorización de Expediente es por Ace (auth-expedientes.xml), no por centro.
        // El administrador no queda bloqueado: AuthSecurity.getUser() devuelve null para admin.
        Beans.get(JpaSecurity.class).check(JpaSecurity.CAN_READ, claseConcreta, idExpediente);

        Expediente expediente = JpaRepository.of(claseConcreta).find(idExpediente);
        if (expediente == null) {
            throw new RuntimeException("No existe el expediente con idExpediente: " + idExpediente);
        }

        return expediente;
    }

    /**
     * Obtiene la clase concreta de un expediente en función de su id.
     * Se usa este método porque de otra forma se trabajaría con {@link Expediente} y no con la
     * entidad concreta del tipo de expediente.
     *
     * @param idExpediente
     * @return
     */
    private static Class<? extends Expediente> getClaseConcreta(long idExpediente) {
        JpaRepository<Expediente> onlyExpedienteRepository = JpaRepository.of(Expediente.class);
        Expediente expediente = onlyExpedienteRepository.find(idExpediente);
        if (expediente == null) {
            throw new RuntimeException("No existe el expediente con idExpediente: " + idExpediente);
        }
        //La clase del modelo es la misma en todas las fases, así que aquí no hace falta resolver por estado.
        //ExpedienteLocator es un bean inyectable, pero esto es un método estático de utilidad al que
        //se llega desde varios sitios, así que se pide con Beans.get igual que el JpaSecurity de arriba.
        Class<? extends Expediente> claseConcreta = Beans.get(ExpedienteLocator.class).getModelClass(expediente.getTipoExpediente());
        JPA.em().detach(expediente);

        return claseConcreta;
    }

}
