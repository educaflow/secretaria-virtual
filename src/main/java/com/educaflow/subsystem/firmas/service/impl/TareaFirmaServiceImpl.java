package com.educaflow.subsystem.firmas.service.impl;

import com.axelor.auth.AuthUtils;
import com.axelor.auth.db.User;
import com.axelor.db.Repository;
import com.axelor.db.modelservice.DefaultModelService;
import com.axelor.db.modelservice.ModelServiceFactory;
import com.axelor.i18n.I18n;
import com.axelor.inject.Beans;
import com.axelor.meta.db.MetaFile;
import com.educaflow.base.infrastructure.criptografia.AlmacenClave;
import com.educaflow.base.infrastructure.criptografia.AlmacenClaveFichero;
import com.educaflow.base.infrastructure.metafile.MetaFileHelper;
import com.educaflow.base.infrastructure.pdf.CampoFirma;
import com.educaflow.base.infrastructure.pdf.DocumentoPdf;
import com.educaflow.base.infrastructure.pdf.Rectangulo;
import com.educaflow.base.infrastructure.pdf.DocumentoPdfUtil;
import com.axelor.db.modelservice.BusinessMessage;
import com.axelor.db.modelservice.BusinessMessages;
import com.axelor.db.modelservice.AllowProperties;
import com.educaflow.base.util.JsonUtil;
import com.educaflow.base.util.MetaFileUtil;
import com.educaflow.base.util.TextUtil;
import com.educaflow.subsystem.criptografia.db.CertificadoDigital;
import com.educaflow.subsystem.criptografia.service.CertificadoDigitalService;
import com.educaflow.subsystem.firmas.db.DocumentoFirma;
import com.educaflow.subsystem.firmas.db.EstadoTareaFirma;
import com.educaflow.subsystem.firmas.db.SituacionFirma;
import com.educaflow.subsystem.firmas.db.TareaFirma;
import com.educaflow.subsystem.firmas.service.TareaFirmaInsertDTO;
import com.educaflow.subsystem.firmas.service.TareaFirmaNotifier;
import com.educaflow.subsystem.firmas.service.TareaFirmaService;
import com.google.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.security.auth.login.LoginException;
import java.math.BigDecimal;
import java.security.UnrecoverableKeyException;
import java.time.LocalDateTime;
import java.util.*;

public class TareaFirmaServiceImpl extends DefaultModelService<TareaFirma> implements TareaFirmaService {

    private static final Logger log = LoggerFactory.getLogger(TareaFirmaServiceImpl.class);

    /** Profundidad máxima al recorrer la cadena de causas de un fallo de firma, para no colgarse con un ciclo. */
    private static final int LIMITE_CAUSAS_A_RECORRER = 20;

    /**
     * Forma canónica de obtener otro {@code ModelService} desde un {@code ModelService}: el
     * {@code CertificadoDigitalService} se resuelve dentro del método que lo necesita, nunca se inyecta
     * directamente con {@code @Inject}. {@code ModelServiceFactoryImpl.resolve} hace
     * {@code injectMembers} sobre el servicio que instancia, así que este campo queda inyectado.
     */
    @Inject
    private ModelServiceFactory modelServiceFactory;

    public TareaFirmaServiceImpl(Class<TareaFirma> model, Repository<TareaFirma> repository) {
        super(model, repository);
    }

    @Override
    public TareaFirma insert(TareaFirmaInsertDTO tareaFirmaInsertDTO)  {
        validateInsert(tareaFirmaInsertDTO).ifPresent(BusinessMessages::throwIfInvalid);

        TareaFirma tareaFirma=new TareaFirma();
        tareaFirma.setFirmante(tareaFirmaInsertDTO.firmante());
        tareaFirma.setFechaSolicitud(LocalDateTime.now());
        tareaFirma.setEstadoTareaFirma(EstadoTareaFirma.PENDIENTE);
        tareaFirma.setMotivoFirma(tareaFirmaInsertDTO.motivoFirma());
        tareaFirma.setMotivoRechazo(null);


        List<DocumentoFirma> documentosFirma=new ArrayList<>();
        for(MetaFile documento: tareaFirmaInsertDTO.documentos()) {
            DocumentoFirma documentoFirma = new DocumentoFirma();
            documentoFirma.setDocumentoOriginal(MetaFileUtil.cloneMetaFile(documento));
            documentoFirma.setTareaFirma(tareaFirma);
            documentosFirma.add(documentoFirma);
        }
        tareaFirma.setDocumentosFirma(documentosFirma);



        tareaFirma.setFqcnFirmaNotifier(tareaFirmaInsertDTO.firmaNotifierClass().getName());
        Object callBackData= tareaFirmaInsertDTO.callBackData();
        if(callBackData!=null){
            tareaFirma.setFqcnCallBackData(callBackData.getClass().getName());
            tareaFirma.setCallBackData(JsonUtil.toJson(callBackData));
        } else {
            tareaFirma.setFqcnCallBackData(null);
            tareaFirma.setCallBackData(null);
        }



        tareaFirma.setX(BigDecimal.valueOf(tareaFirmaInsertDTO.areaFirma().x()));
        tareaFirma.setY(BigDecimal.valueOf(tareaFirmaInsertDTO.areaFirma().y()));
        tareaFirma.setWidth(BigDecimal.valueOf(tareaFirmaInsertDTO.areaFirma().width()));
        tareaFirma.setHeight(BigDecimal.valueOf(tareaFirmaInsertDTO.areaFirma().height()));

        tareaFirma.setPage(tareaFirmaInsertDTO.page());

        tareaFirma = repository.save(tareaFirma);

        return tareaFirma;
    }

    @Override
    public TareaFirma marcarComoFirmada(TareaFirma tareaFirma, TareaFirma tareaFirmaOriginal)  {
        validateMarcarComoFirmada(tareaFirma, tareaFirmaOriginal).ifPresent(BusinessMessages::throwIfInvalid);

        fireActionRule_ResolverComoFirmada(tareaFirma);

        tareaFirma = repository.save(tareaFirma);

        fireActionRule_NotificarFirmaResuelta(tareaFirma);

        return tareaFirma;
    }

    @Override
    public TareaFirma marcarComoRechazada(TareaFirma tareaFirma, TareaFirma tareaFirmaOriginal)  {
        validateMarcarComoRechazada(tareaFirma, tareaFirmaOriginal).ifPresent(BusinessMessages::throwIfInvalid);

        tareaFirma.setEstadoTareaFirma(EstadoTareaFirma.RECHAZADO);
        tareaFirma.setFechaResolucion(LocalDateTime.now());

        tareaFirma = repository.save(tareaFirma);

        fireActionRule_NotificarFirmaResuelta(tareaFirma);

        return tareaFirma;
    }

    @Override
    public TareaFirma firmarEnServidor(TareaFirma tareaFirma, TareaFirma tareaFirmaOriginal) {
        try {
            validateFirmarEnServidor(tareaFirma, tareaFirmaOriginal).ifPresent(BusinessMessages::throwIfInvalid);

            fireActionRule_FirmarDocumentosEnServidor(tareaFirma);
            fireActionRule_ResolverComoFirmada(tareaFirma);

            tareaFirma = repository.save(tareaFirma);
        } finally {
            // La validación entra en el try a propósito: la clave se descarta también cuando la acción
            // termina sin firmar, sea porque una validación la rechazó o porque la firma falló.
            fireActionRule_DescartarClaveFirma(tareaFirma);
        }

        fireActionRule_NotificarFirmaResuelta(tareaFirma);

        return tareaFirma;
    }

    @Override
    public Optional<BusinessMessages> validarDocumentosFirmados(TareaFirma tareaFirma) {
        validateValidarDocumentosFirmados(tareaFirma).ifPresent(BusinessMessages::throwIfInvalid);

        BusinessMessages businessMessages=new BusinessMessages();

        for (DocumentoFirma documentoFirma : tareaFirma.getDocumentosFirma()) {
            DocumentoPdf documentoOriginal = MetaFileHelper.getDocumentoPdf(documentoFirma.getDocumentoOriginal());
            DocumentoPdf documentoFirmado = MetaFileHelper.getDocumentoPdf(documentoFirma.getDocumentoFirmado());
            Optional<String> errorFirma = DocumentoPdfUtil.validateFirmaPdf(documentoOriginal, documentoFirmado, tareaFirma.getFirmante().getDni());
            if (errorFirma.isPresent()) {
                businessMessages.add(new BusinessMessage(documentoFirmado.getFileName(),errorFirma.get()));
            }
        }

        if (businessMessages.isEmpty()) {
            return Optional.empty();
        } else {
            return Optional.of(businessMessages);
        }

    }


    /****************************************************************************************/
    /******************************** Métodos de Validación *********************************/
    /****************************************************************************************/

    public Optional<BusinessMessages> validateInsert(TareaFirmaInsertDTO tareaFirmaInsertDTO) {
        return Optional.empty();
    }
    public Optional<BusinessMessages> validateMarcarComoFirmada(TareaFirma tareaFirma, TareaFirma tareaFirmaOriginal) {
        return Optional.empty();
    }
    public Optional<BusinessMessages> validateMarcarComoRechazada(TareaFirma tareaFirma, TareaFirma tareaFirmaOriginal) {
        return Optional.empty();
    }
    public Optional<BusinessMessages> validateValidarDocumentosFirmados(TareaFirma tareaFirma) { return Optional.empty();}

    /**
     * Valida la acción de firma en el servidor (V-TareaFirma-001 .. V-TareaFirma-008).
     *
     * <p>Todas las comprobaciones se hacen sobre el estado <strong>real del servidor</strong>, nunca sobre lo
     * que la pantalla tuviera pintado: el estado y el firmante salen de la entidad cargada de base de datos y
     * la situación de firma de {@code getSituacionFirma()}, un campo derivado que se recalcula en cada lectura.
     *
     * <p>El parámetro {@code tareaFirmaOriginal} solo existe por simetría con el resto de acciones del
     * servicio: esta acción no compara con el original.
     */
    @Override
    public Optional<BusinessMessages> validateFirmarEnServidor(TareaFirma tareaFirma, TareaFirma tareaFirmaOriginal) {
        BusinessMessages businessMessages = new BusinessMessages();

        // V-TareaFirma-001 — estado de la tarea.
        if (tareaFirma.getEstadoTareaFirma() != EstadoTareaFirma.PENDIENTE) {
            businessMessages.add(new BusinessMessage(I18n.get("Solo se pueden firmar las tareas pendientes de firmar")));
        }

        // V-TareaFirma-002 — titularidad. Es la defensa real: el <domain> del action-view es solo UX.
        if (isFirmanteElUsuarioAutenticado(tareaFirma) == false) {
            businessMessages.add(new BusinessMessage(I18n.get("Solo puede firmar los documentos la persona a la que se le han encargado")));
        }

        SituacionFirma situacionFirma = tareaFirma.getSituacionFirma();

        // V-TareaFirma-003 — DNI del firmante.
        if (situacionFirma == SituacionFirma.SIN_DNI) {
            businessMessages.add(new BusinessMessage(I18n.get("No es posible firmar los documentos porque su usuario no tiene un DNI. Póngase en contacto con el administrador.")));
        }

        // V-TareaFirma-004 — certificado dado de alta.
        if (situacionFirma == SituacionFirma.SIN_CERTIFICADO) {
            businessMessages.add(new BusinessMessage(I18n.get("No es posible firmar en el servidor porque no tiene un certificado digital dado de alta")));
        }

        // V-TareaFirma-005 — PIN obligatorio.
        if (situacionFirma == SituacionFirma.DISPOSITIVO_SIN_PIN && isClaveFirmaVacia(tareaFirma)) {
            businessMessages.add(new BusinessMessage(I18n.get("El PIN es obligatorio")));
        }

        // V-TareaFirma-006 — contraseña obligatoria.
        if (situacionFirma == SituacionFirma.FICHERO_SIN_CLAVE && isClaveFirmaVacia(tareaFirma)) {
            businessMessages.add(new BusinessMessage(I18n.get("La contraseña es obligatoria")));
        }

        // V-TareaFirma-007 — documentos a firmar.
        if (tareaFirma.getDocumentosFirma() == null || tareaFirma.getDocumentosFirma().isEmpty()) {
            businessMessages.add(new BusinessMessage(I18n.get("La tarea de firma no tiene ningún documento que firmar")));
        }

        // V-TareaFirma-008 — clave correcta del certificado en fichero. Va la última y solo si todo lo demás
        // ha pasado: es la única comprobación que abre el certificado, y sin firmante, sin certificado o sin
        // clave no hay nada que comprobar.
        if (businessMessages.isValid() && isClaveCertificadoIncorrecta(tareaFirma)) {
            businessMessages.add(new BusinessMessage(mensajeClaveCertificadoIncorrecta(situacionFirma)));
        }

        return businessMessages.isValid() ? Optional.empty() : Optional.of(businessMessages);
    }


    /**************************************************************************************/
    /********************************   AllowProperties   *********************************/
    /**************************************************************************************/

    public AllowProperties allowPropertiesMarcarComoFirmada() {
        return AllowProperties.createAllowProperties(Map.of("documentosFirma", Map.of("documentoFirmado", Map.of())));
    };
    public AllowProperties allowPropertiesMarcarComoRechazada() {
        return AllowProperties.createAllowProperties(Map.of("motivoRechazo", Map.of()));
    };
    public AllowProperties allowPropertiesValidarDocumentosFirmados(){
        return AllowProperties.createAllowAllProperties();
    };

    /**
     * Whitelist de la acción de firma en el servidor: la clave de firma es el único dato que dicta el cliente.
     * Todo lo demás (estado, fechas, firmante, recuadro, documentos y situación de firma) lo dicta el servidor
     * y queda fuera.
     */
    @Override
    public AllowProperties allowPropertiesFirmarEnServidor() {
        return AllowProperties.createAllowProperties(Map.of("claveFirma", Map.of()));
    }

    /**
     * Las tareas de firma no se dan de alta desde la interfaz, solo con el DTO programático
     * {@code insert(TareaFirmaInsertDTO)}. Cerrar la whitelist impide que el endpoint REST automático
     * {@code /ws/rest/<FQN>} cuele campos.
     */
    @Override
    public AllowProperties allowPropertiesInsert() {
        return AllowProperties.createDenyAllProperties();
    }

    /**
     * La tarea solo cambia mediante sus acciones propias (marcarComoFirmada, marcarComoRechazada,
     * firmarEnServidor), nunca guardando el formulario: ninguna vista de TareaFirma usa {@code save}.
     */
    @Override
    public AllowProperties allowPropertiesUpdate() {
        return AllowProperties.createDenyAllProperties();
    }


    /*************************************************************************************/
    /********************************    Action Rules    *********************************/
    /*************************************************************************************/

    /**
     * R-TareaFirma-001 — firma en el servidor TODOS los documentos de la tarea con el certificado digital del
     * firmante. Momento: antes de {@code repository.save}.
     *
     * <p>Trabaja en dos fases para garantizar el «todo o nada»: primero firma todos los documentos en memoria y
     * solo si todos han salido bien crea sus {@code MetaFile} y los asigna. Si algo falla en la primera fase no
     * se ha creado ningún fichero ni se ha tocado ningún {@code DocumentoFirma}, así que la tarea sigue
     * pendiente y el firmante puede reintentar.
     */
    private void fireActionRule_FirmarDocumentosEnServidor(TareaFirma tareaFirma) {
        CampoFirma campoFirma = construirCampoFirma(tareaFirma);
        List<DocumentoFirmado> documentosFirmados = firmarDocumentosEnMemoria(tareaFirma, campoFirma);

        publicarDocumentosFirmados(documentosFirmados);
    }

    /**
     * R-TareaFirma-002 — deja la tarea resuelta como firmada. Momento: antes de {@code repository.save}.
     *
     * <p>Asignación incondicional: {@code estadoTareaFirma} y {@code fechaResolucion} son campos que dicta el
     * servidor, así que una guarda por nulo permitiría al cliente dictar el estado o falsificar la fecha por el
     * endpoint REST genérico. La comparten las dos acciones que resuelven una tarea como firmada.
     */
    private void fireActionRule_ResolverComoFirmada(TareaFirma tareaFirma) {
        tareaFirma.setEstadoTareaFirma(EstadoTareaFirma.FIRMADO);
        tareaFirma.setFechaResolucion(LocalDateTime.now());
    }

    /**
     * R-TareaFirma-003 — descarta la clave de firma. Momento: siempre, tanto si la firma se completó como si la
     * abortó una validación o un fallo.
     *
     * <p>El campo es transitorio, así que nunca llega a base de datos; esta regla se ocupa del resto del rastro
     * soltando la referencia en memoria en cuanto termina la acción. La clave no se loguea ni se devuelve al
     * cliente en ningún caso.
     */
    private void fireActionRule_DescartarClaveFirma(TareaFirma tareaFirma) {
        tareaFirma.setClaveFirma(null);
    }

    @SuppressWarnings("unchecked")
    private void fireActionRule_NotificarFirmaResuelta(TareaFirma tareaFirma) {
        try {
            Class<? extends TareaFirmaNotifier> firmaNotifierClass = (Class<? extends TareaFirmaNotifier>) Class.forName(tareaFirma.getFqcnFirmaNotifier());
            TareaFirmaNotifier tareaFirmaNotifier = Beans.get(firmaNotifierClass);

            Object callBackData = null;
            if (tareaFirma.getFqcnCallBackData() != null) {
                Class<?> callBackDataClass = Class.forName(tareaFirma.getFqcnCallBackData());
                callBackData = JsonUtil.fromJson(tareaFirma.getCallBackData(), callBackDataClass);
            }

            tareaFirmaNotifier.notify(tareaFirma, callBackData);
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException("No se encontró la clase necesaria para notificar la firma resuelta: " + e.getMessage(), e);
        }
    }

    /*************************************************************************************/
    /********************************    Otras funciones    ******************************/
    /*************************************************************************************/

    /**
     * Construye el {@link CampoFirma} —el recuadro y la página donde se estampa la firma— con los datos que ya
     * lleva la propia tarea.
     *
     * <p>Se construye una sola vez por acción porque no tiene estado consumible: la misma instancia sirve para
     * todos los documentos. Los cuatro valores del recuadro son {@code BigDecimal} en la entidad y
     * {@link Rectangulo} es un record de cuatro {@code float}, así que la conversión con {@code floatValue()}
     * es obligatoria.
     */
    private CampoFirma construirCampoFirma(TareaFirma tareaFirma) {
        return new CampoFirma(new Rectangulo(
                        tareaFirma.getX().floatValue(),
                        tareaFirma.getY().floatValue(),
                        tareaFirma.getWidth().floatValue(),
                        tareaFirma.getHeight().floatValue()))
                .setNumeroPagina(tareaFirma.getPage());
    }

    /**
     * Par intermedio de la fase de firma: un documento de la tarea junto al PDF que ya se ha firmado en memoria
     * pero todavía no se ha publicado.
     *
     * <p>Es a propósito un tipo <strong>privado</strong> de la implementación: la lista intermedia es una
     * estructura local de la regla y no debe convertirse en un tipo público del subsistema. Emparejar los dos
     * datos en un par hace imposible que la fase de publicación descuadre un documento con el PDF de otro.
     */
    private record DocumentoFirmado(DocumentoFirma documentoFirma, DocumentoPdf documentoPdfFirmado) {
    }

    /**
     * Fase de firma de R-TareaFirma-001: firma en memoria <strong>todos</strong> los documentos de la tarea y
     * devuelve los pares (documento, PDF firmado). No crea ningún {@code MetaFile} ni modifica ninguna entidad.
     *
     * <p>Si la firma de cualquier documento falla, el método no devuelve nada: registra el fallo con su traza y
     * lanza el error de negocio de RN-TareaFirma-007. Como la publicación solo trabaja sobre lo que este método
     * devuelve, un fallo no puede acabar publicando una lista parcial.
     */
    private List<DocumentoFirmado> firmarDocumentosEnMemoria(TareaFirma tareaFirma, CampoFirma campoFirma) {
        final CertificadoDigitalService certificadoDigitalService =
                (CertificadoDigitalService) modelServiceFactory.resolve(CertificadoDigital.class);

        String dni = tareaFirma.getFirmante().getDni();
        List<DocumentoFirmado> documentosFirmados = new ArrayList<>();

        for (DocumentoFirma documentoFirma : tareaFirma.getDocumentosFirma()) {
            try {
                // CRITICAL: un AlmacenClave NUEVO por documento. AlmacenClaveFichero envuelve un InputStream que
                // DocumentoPdf.firmar consume al construir el KeyStore, así que reutilizar la misma instancia
                // firmaría el segundo documento contra un stream ya agotado.
                AlmacenClave almacenClave = certificadoDigitalService.getAlmacenClaveByDni(dni, tareaFirma.getClaveFirma());
                DocumentoPdf documentoPdfOriginal = MetaFileHelper.getDocumentoPdf(documentoFirma.getDocumentoOriginal());

                documentosFirmados.add(new DocumentoFirmado(documentoFirma, documentoPdfOriginal.firmar(almacenClave, campoFirma)));
            } catch (RuntimeException ex) {
                // La traza queda en el log porque el mensaje de negocio llega al usuario sin ella. Se registran el
                // id de la tarea y el nombre del fichero; NUNCA la clave de firma ni un fragmento de ella.
                log.error("No se ha podido firmar en el servidor el documento '{}' de la tarea de firma id={}",
                        nombreDocumentoOriginal(documentoFirma), tareaFirma.getId(), ex);

                // El motivo que ve el usuario es de negocio: ni la clave de firma (ni entera ni truncada) ni el
                // texto técnico de la excepción, que se queda solo en el log.
                String motivo = motivoFirmaFallida(ex, tareaFirma.getSituacionFirma());
                BusinessMessages.single(I18n.get("No se han podido firmar los documentos: %s").formatted(motivo)).throwIfInvalid();

                // Inalcanzable: throwIfInvalid siempre lanza cuando hay un mensaje. El throw explícito deja
                // constancia de que la fase de firma nunca continúa ni devuelve una lista incompleta.
                throw ex;
            }
        }

        return documentosFirmados;
    }

    /**
     * Traduce el fallo técnico de la firma al motivo de negocio que ve el firmante.
     *
     * <p>El texto de la excepción <strong>nunca</strong> llega al usuario: es un mensaje del JDK, en inglés y
     * con nombres de clase Java. La traza completa ya se ha registrado en el log, que es donde puede
     * consultarla quien administre.
     *
     * <p>El único caso que se distingue es la clave incorrecta, porque es el que el firmante puede corregir
     * por sí mismo (RN-TareaFirma-007: el motivo sirve «para que sepa qué corregir antes de volver a
     * intentarlo»). Cuando la clave la tiene guardada la secretaría virtual, el firmante no puede hacer nada
     * y se le remite al administrador.
     */
    private String motivoFirmaFallida(RuntimeException ex, SituacionFirma situacionFirma) {
        final String motivoGenerico = I18n.get("ha fallado la firma en el servidor. Póngase en contacto con el administrador");

        if (isClaveIncorrecta(ex) == false || situacionFirma == null) {
            return motivoGenerico;
        }

        return switch (situacionFirma) {
            case DISPOSITIVO_SIN_PIN -> I18n.get("el PIN indicado no es correcto");
            case FICHERO_SIN_CLAVE -> I18n.get("la contraseña indicada no es correcta");
            case DISPOSITIVO_CON_PIN, FICHERO_CON_CLAVE ->
                    I18n.get("la clave guardada de su certificado digital no es correcta. Póngase en contacto con el administrador");
            case SIN_DNI, SIN_CERTIFICADO -> motivoGenerico;
        };
    }

    /**
     * Indica si el fallo se debe a que la clave (contraseña del fichero o PIN del dispositivo) no es correcta.
     *
     * <p>Se decide recorriendo la cadena de causas y no por el texto del mensaje: tanto
     * {@code CriptografiaUtil.getKeyStore} como {@code DocumentoPdf.firmar} envuelven el fallo original en una
     * {@code RuntimeException}, así que el marcador del JDK ({@code UnrecoverableKeyException} al abrir un
     * fichero PKCS#12, {@code LoginException} al abrir un dispositivo PKCS#11) queda varios niveles por debajo.
     * El recorrido está acotado para que una cadena de causas cíclica no lo cuelgue.
     */
    private boolean isClaveIncorrecta(Throwable ex) {
        Throwable causa = ex;

        for (int nivel = 0; causa != null && nivel < LIMITE_CAUSAS_A_RECORRER; nivel++) {
            if (causa instanceof UnrecoverableKeyException || causa instanceof LoginException) {
                return true;
            }
            causa = causa.getCause();
        }

        return false;
    }

    /**
     * Fase de publicación de R-TareaFirma-001: por cada par, crea el {@code MetaFile} del PDF firmado y lo
     * asigna a su {@code DocumentoFirma}.
     *
     * <p>Solo se invoca con la lista completa que devuelve la fase de firma, así que un fallo de firma no puede
     * llegar hasta aquí: es la garantía «todo o nada» de RN-TareaFirma-002.
     */
    private void publicarDocumentosFirmados(List<DocumentoFirmado> documentosFirmados) {
        for (DocumentoFirmado documentoFirmado : documentosFirmados) {
            MetaFile metaFileFirmado = MetaFileHelper.createMetaFile(documentoFirmado.documentoPdfFirmado());

            documentoFirmado.documentoFirma().setDocumentoFirmado(metaFileFirmado);
        }
    }

    /**
     * Nombre del fichero original de un documento, saneado para poder escribirlo en el log.
     *
     * <p>El nombre lo aporta quien subió el fichero, así que {@code TextUtil.sanitizeFileName} le quita los
     * caracteres de control —entre ellos los saltos de línea— que permitirían inyectar líneas falsas de log.
     * Devuelve un texto fijo cuando no hay nombre porque se usa dentro del tratamiento de un error y no puede
     * fallar a su vez.
     */
    private String nombreDocumentoOriginal(DocumentoFirma documentoFirma) {
        MetaFile documentoOriginal = documentoFirma.getDocumentoOriginal();

        if (documentoOriginal == null || TextUtil.isNullOrBlank(documentoOriginal.getFileName())) {
            return "desconocido";
        }

        return TextUtil.sanitizeFileName(documentoOriginal.getFileName());
    }

    private boolean isFirmanteElUsuarioAutenticado(TareaFirma tareaFirma) {
        User firmante = tareaFirma.getFirmante();
        User usuarioAutenticado = AuthUtils.getUser();

        if (firmante == null || firmante.getId() == null || usuarioAutenticado == null) {
            return false;
        }

        return firmante.getId().equals(usuarioAutenticado.getId());
    }

    private boolean isClaveFirmaVacia(TareaFirma tareaFirma) {
        String claveFirma = tareaFirma.getClaveFirma();

        return claveFirma == null || claveFirma.isBlank();
    }

    /**
     * V-TareaFirma-008 — comprueba contra el propio certificado si la clave con la que se va a firmar lo abre,
     * para avisar antes de firmar en vez de dejar que reviente la firma del primer documento.
     *
     * <p>Solo se comprueba en los certificados en fichero: en un dispositivo PKCS#11 la única forma de saber si
     * el PIN es correcto es intentar abrir el dispositivo con él, y los intentos fallidos bloquean la tarjeta.
     *
     * <p>Que el certificado no se pueda leer (fichero corrupto, ruta que ya no existe, blob ilegible) NO es una
     * clave incorrecta: se registra y se deja pasar, para que sea la fase de firma quien lo trate con su motivo
     * genérico y no se acuse al firmante de haber tecleado mal la contraseña.
     */
    private boolean isClaveCertificadoIncorrecta(TareaFirma tareaFirma) {
        SituacionFirma situacionFirma = tareaFirma.getSituacionFirma();

        if (situacionFirma != SituacionFirma.FICHERO_SIN_CLAVE && situacionFirma != SituacionFirma.FICHERO_CON_CLAVE) {
            return false;
        }

        try {
            CertificadoDigitalService certificadoDigitalService =
                    (CertificadoDigitalService) modelServiceFactory.resolve(CertificadoDigital.class);
            AlmacenClave almacenClave = certificadoDigitalService.getAlmacenClaveByDni(
                    tareaFirma.getFirmante().getDni(), tareaFirma.getClaveFirma());

            if (almacenClave instanceof AlmacenClaveFichero almacenClaveFichero) {
                return almacenClaveFichero.isPasswordValid() == false;
            }

            return false;
        } catch (RuntimeException ex) {
            // Igual que en la fase de firma: se registran el id de la tarea y la traza, NUNCA la clave de firma.
            log.warn("No se ha podido comprobar la clave del certificado de la tarea de firma id={}", tareaFirma.getId(), ex);
            return false;
        }
    }

    /**
     * Motivo que ve el firmante cuando la clave no abre su certificado. Se distingue quién puede corregirlo:
     * la contraseña que acaba de teclear la corrige él; la que guarda la secretaría virtual, no.
     */
    private String mensajeClaveCertificadoIncorrecta(SituacionFirma situacionFirma) {
        if (situacionFirma == SituacionFirma.FICHERO_CON_CLAVE) {
            return I18n.get("La clave guardada de su certificado digital no es correcta. Póngase en contacto con el administrador");
        }

        return I18n.get("La contraseña indicada no es correcta");
    }

}
