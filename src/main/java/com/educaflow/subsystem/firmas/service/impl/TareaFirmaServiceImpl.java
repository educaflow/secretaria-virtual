package com.educaflow.subsystem.firmas.service.impl;

import com.educaflow.base.util.SecurityUtil;
import com.axelor.auth.db.User;
import com.axelor.db.Repository;
import com.axelor.db.modelservice.DefaultModelService;
import com.axelor.i18n.I18n;
import com.axelor.inject.Beans;
import com.axelor.meta.db.MetaFile;
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
import com.educaflow.subsystem.criptografia.service.CredentialsFailureException;
import com.educaflow.subsystem.criptografia.service.FirmaEnServidorService;
import com.educaflow.subsystem.criptografia.util.CertificadoDigitalHelper;
import com.educaflow.subsystem.firmas.db.DocumentoFirma;
import com.educaflow.subsystem.firmas.db.EstadoTareaFirma;
import com.educaflow.subsystem.criptografia.service.SituacionFirma;
import com.educaflow.subsystem.firmas.db.TareaFirma;
import com.educaflow.subsystem.firmas.service.TareaFirmaInsertDTO;
import com.educaflow.subsystem.firmas.service.TareaFirmaNotifier;
import com.educaflow.subsystem.firmas.service.TareaFirmaService;
import com.google.inject.Inject;
import jakarta.validation.ValidationException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

public class TareaFirmaServiceImpl extends DefaultModelService<TareaFirma> implements TareaFirmaService {

    private static final Logger log = LoggerFactory.getLogger(TareaFirmaServiceImpl.class);

    @Inject
    private FirmaEnServidorService firmaEnServidorService;

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
    public TareaFirma firmarEnServidor(TareaFirma tareaFirma, TareaFirma tareaFirmaOriginal, String claveFirma) {
        validateFirmarEnServidor(tareaFirma, tareaFirmaOriginal, claveFirma).ifPresent(BusinessMessages::throwIfInvalid);

        fireActionRule_FirmarDocumentosEnServidor(tareaFirma, claveFirma);
        fireActionRule_ResolverComoFirmada(tareaFirma);

        tareaFirma = repository.save(tareaFirma);

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

    @Override
    public Optional<BusinessMessages> validateFirmarEnServidor(TareaFirma tareaFirma, TareaFirma tareaFirmaOriginal, String claveFirma) {
        BusinessMessages businessMessages = new BusinessMessages();

        // V-TareaFirma-001 — estado de la tarea.
        if (tareaFirma.getEstadoTareaFirma() != EstadoTareaFirma.PENDIENTE) {
            businessMessages.add(new BusinessMessage(I18n.get("Solo se pueden firmar las tareas pendientes de firmar")));
        }

        // V-TareaFirma-002 — titularidad. Es la defensa real: el <domain> del action-view es solo UX.
        if (isFirmanteElUsuarioAutenticado(tareaFirma) == false) {
            businessMessages.add(new BusinessMessage(I18n.get("Solo puede firmar los documentos la persona a la que se le han encargado")));
        }

        SituacionFirma situacionFirma = getSituacionFirma(tareaFirma);

        // V-TareaFirma-003 — DNI del firmante.
        if (situacionFirma == SituacionFirma.SIN_DNI) {
            businessMessages.add(new BusinessMessage(I18n.get("No es posible firmar los documentos porque su usuario no tiene un DNI. Póngase en contacto con el administrador.")));
        }

        // V-TareaFirma-004 — certificado dado de alta.
        if (situacionFirma == SituacionFirma.SIN_CERTIFICADO) {
            businessMessages.add(new BusinessMessage(I18n.get("No es posible firmar en el servidor porque no tiene un certificado digital dado de alta")));
        }

        // V-TareaFirma-005 — PIN obligatorio.
        if (situacionFirma == SituacionFirma.DISPOSITIVO_SIN_PIN && TextUtil.isNullOrBlank(claveFirma)) {
            businessMessages.add(new BusinessMessage(I18n.get("El PIN es obligatorio")));
        }

        // V-TareaFirma-006 — contraseña obligatoria.
        if (situacionFirma == SituacionFirma.FICHERO_SIN_CLAVE && TextUtil.isNullOrBlank(claveFirma)) {
            businessMessages.add(new BusinessMessage(I18n.get("La contraseña es obligatoria")));
        }

        // V-TareaFirma-007 — documentos a firmar.
        if (tareaFirma.getDocumentosFirma() == null || tareaFirma.getDocumentosFirma().isEmpty()) {
            businessMessages.add(new BusinessMessage(I18n.get("La tarea de firma no tiene ningún documento que firmar")));
        }

        // V-TareaFirma-008 — clave correcta del certificado en fichero. Va la última y solo si todo lo demás
        // ha pasado: es la única comprobación que abre el certificado, y sin firmante, sin certificado o sin
        // clave no hay nada que comprobar.
        if (businessMessages.isValid() && isClaveCertificadoCorrecta(tareaFirma, claveFirma)==false) {
            businessMessages.add(new BusinessMessage(I18n.get("No es posible firmar los documentos: %s")
                    .formatted(CertificadoDigitalHelper.motivoClaveErronea(situacionFirma))));
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

    @Override
    public AllowProperties allowPropertiesFirmarEnServidor() {
        return AllowProperties.createDenyAllProperties();
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
    private void fireActionRule_FirmarDocumentosEnServidor(TareaFirma tareaFirma, String claveFirma) {
        // Un solo CampoFirma para todos los documentos: no tiene estado consumible. El recuadro es BigDecimal
        // en la entidad y float en Rectangulo, de ahí los floatValue().
        CampoFirma campoFirma = new CampoFirma(new Rectangulo(
                        tareaFirma.getX().floatValue(),
                        tareaFirma.getY().floatValue(),
                        tareaFirma.getWidth().floatValue(),
                        tareaFirma.getHeight().floatValue()))
                .setNumeroPagina(tareaFirma.getPage());
        List<DocumentoFirmado> documentosFirmados = firmarDocumentosEnMemoria(tareaFirma, claveFirma, campoFirma);

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
    private List<DocumentoFirmado> firmarDocumentosEnMemoria(TareaFirma tareaFirma, String claveFirma, CampoFirma campoFirma) {
        String dni = tareaFirma.getFirmante().getDni();
        SituacionFirma situacionFirma = getSituacionFirma(tareaFirma);

        List<DocumentoFirmado> documentosFirmados = new ArrayList<>();

        for (DocumentoFirma documentoFirma : tareaFirma.getDocumentosFirma()) {
            try {
                DocumentoPdf documentoPdfOriginal = MetaFileHelper.getDocumentoPdf(documentoFirma.getDocumentoOriginal());

                documentosFirmados.add(new DocumentoFirmado(documentoFirma, firmaEnServidorService.firmar(
                        dni, claveFirma, documentoPdfOriginal, campoFirma)));
            } catch (RuntimeException ex) {
                String motivo;
                if (ex instanceof CredentialsFailureException) {
                    motivo = CertificadoDigitalHelper.motivoClaveErronea(situacionFirma);
                    log.error("No se ha podido firmar en el servidor el documento id={} de la tarea de firma id={}", documentoFirma.getId(), tareaFirma.getId());
                } else {
                    motivo = I18n.get("ha fallado la firma en el servidor. Póngase en contacto con el administrador");
                    log.error("No se ha podido firmar en el servidor el documento id={} de la tarea de firma id={}", documentoFirma.getId(), tareaFirma.getId(), ex);
                }

                throw new ValidationException(BusinessMessages.single(I18n.get("No se han podido firmar los documentos: %s").formatted(motivo)).toString());
            }
        }

        return documentosFirmados;
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

    private boolean isFirmanteElUsuarioAutenticado(TareaFirma tareaFirma) {
        User firmante = tareaFirma.getFirmante();
        User usuarioAutenticado = SecurityUtil.getUser();

        if (firmante == null || firmante.getId() == null || usuarioAutenticado == null) {
            return false;
        }

        return firmante.getId().equals(usuarioAutenticado.getId());
    }

    private boolean isClaveCertificadoCorrecta(TareaFirma tareaFirma, String claveFirma) {
        String dni = tareaFirma.getFirmante().getDni();

        return CertificadoDigitalHelper.isClaveCertificadoCorrecta(dni, claveFirma);

    }


    private SituacionFirma getSituacionFirma(TareaFirma tareaFirma) {
        User firmante = tareaFirma.getFirmante();

        return CertificadoDigitalHelper.getSituacionFirmaByDni(firmante == null ? null : firmante.getDni());
    }

}
