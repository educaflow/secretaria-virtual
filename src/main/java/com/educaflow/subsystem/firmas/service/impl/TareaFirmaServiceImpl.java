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
import java.util.stream.Collectors;
import com.educaflow.base.util.Convert;

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
        tareaFirma.setCentro(tareaFirmaInsertDTO.centro());
        tareaFirma.setFechaSolicitud(LocalDateTime.now(Convert.defaultZoneId));
        tareaFirma.setEstadoTareaFirma(EstadoTareaFirma.PENDIENTE);
        tareaFirma.setMotivoFirma(tareaFirmaInsertDTO.motivoFirma());
        tareaFirma.setMotivoRechazo(null);


        List<DocumentoFirma> documentosFirma = tareaFirmaInsertDTO.documentos().stream()
                .map(documento -> {
                    DocumentoFirma documentoFirma = new DocumentoFirma();
                    documentoFirma.setDocumentoOriginal(MetaFileUtil.cloneMetaFile(documento));
                    documentoFirma.setTareaFirma(tareaFirma);
                    return documentoFirma;
                })
                .collect(Collectors.toCollection(ArrayList::new));
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



        asignarLugarFirma(tareaFirma, tareaFirmaInsertDTO);

        return repository.save(tareaFirma);
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
        tareaFirma.setFechaResolucion(LocalDateTime.now(Convert.defaultZoneId));

        tareaFirma = repository.save(tareaFirma);

        fireActionRule_NotificarFirmaResuelta(tareaFirma);

        return tareaFirma;
    }

    @Override
    public TareaFirma firmarEnServidor(TareaFirma tareaFirma, TareaFirma tareaFirmaOriginal, String claveCertificado) {
        validateFirmarEnServidor(tareaFirma, tareaFirmaOriginal, claveCertificado).ifPresent(BusinessMessages::throwIfInvalid);

        fireActionRule_FirmarDocumentosEnServidor(tareaFirma, claveCertificado);
        fireActionRule_ResolverComoFirmada(tareaFirma);

        tareaFirma = repository.save(tareaFirma);

        fireActionRule_NotificarFirmaResuelta(tareaFirma);

        return tareaFirma;
    }


    /****************************************************************************************/
    /******************************** Métodos de Validación *********************************/
    /****************************************************************************************/

    @Override
    public Optional<BusinessMessages> validateInsert(TareaFirma tareaFirma) {
        return Optional.of(BusinessMessages.single(I18n.get("Las tareas de firma solo las crea el servidor.")));
    }

    @Override
    public Optional<BusinessMessages> validateUpdate(TareaFirma nuevo, TareaFirma original) {
        return Optional.of(BusinessMessages.single(I18n.get("Las tareas de firma solo se pueden firmar o rechazar.")));
    }

    @Override
    public Optional<BusinessMessages> validateRemove(TareaFirma tareaFirma) {
        return Optional.of(BusinessMessages.single(I18n.get("Las tareas de firma no se pueden borrar.")));
    }

    @Override
    public Optional<BusinessMessages> validateInsert(TareaFirmaInsertDTO tareaFirmaInsertDTO) {
        return Optional.empty();
    }

    @Override
    public Optional<BusinessMessages> validateFirmarConAutoFirma(TareaFirma tareaFirma) {
        BusinessMessages businessMessages = new BusinessMessages();

        if (isPendiente(tareaFirma) == false) {
            businessMessages.add(new BusinessMessage(I18n.get("Solo se pueden firmar las tareas pendientes de firmar")));
        }

        // Es la defensa real: el <domain> del action-view es solo UX.
        if (isFirmanteElUsuarioAutenticado(tareaFirma) == false) {
            businessMessages.add(new BusinessMessage(I18n.get("Solo puede firmar los documentos la persona a la que se le han encargado")));
        }

        return businessMessages.isValid() ? Optional.empty() : Optional.of(businessMessages);
    }

    @Override
    public Optional<BusinessMessages> validateMarcarComoFirmada(TareaFirma tareaFirma, TareaFirma tareaFirmaOriginal) {
        Optional<BusinessMessages> validacionTarea = validateFirmarConAutoFirma(tareaFirma);
        if (validacionTarea.isPresent()) {
            return validacionTarea;
        }

        // La lista documentosFirma la manda el cliente: sin esto, quitando documentos (o colando uno de otra tarea) la tarea
        // se daría por firmada sin haber firmado todos los que se pusieron a firmar.
        if (tareaFirma.getDocumentosFirma() == null || tareaFirma.getDocumentosFirma().isEmpty()) {
            return Optional.of(BusinessMessages.single(I18n.get("La tarea de firma no tiene ningún documento que firmar")));
        }
        if (isMismosDocumentosQueOriginal(tareaFirma, tareaFirmaOriginal) == false) {
            return Optional.of(BusinessMessages.single(I18n.get("Los documentos firmados no coinciden con los que se pusieron a firmar")));
        }

        BusinessMessages businessMessages = validateDocumentosFirmados(tareaFirma);

        return businessMessages.isValid() ? Optional.empty() : Optional.of(businessMessages);
    }
    @Override
    public Optional<BusinessMessages> validateMarcarComoRechazada(TareaFirma tareaFirma, TareaFirma tareaFirmaOriginal) {
        BusinessMessages businessMessages = new BusinessMessages();

        if (isPendiente(tareaFirma) == false) {
            businessMessages.add(new BusinessMessage(I18n.get("Solo se pueden rechazar las tareas pendientes de firmar")));
        }

        // Es la defensa real: el <domain> del action-view es solo UX.
        if (isFirmanteElUsuarioAutenticado(tareaFirma) == false) {
            businessMessages.add(new BusinessMessage(I18n.get("Solo puede rechazar la firma de los documentos la persona a la que se le han encargado")));
        }

        return businessMessages.isValid() ? Optional.empty() : Optional.of(businessMessages);
    }

    @Override
    public Optional<BusinessMessages> validateFirmarEnServidor(TareaFirma tareaFirma, TareaFirma tareaFirmaOriginal, String claveCertificado) {
        BusinessMessages businessMessages = new BusinessMessages();

        if (isPendiente(tareaFirma) == false) {
            businessMessages.add(new BusinessMessage(I18n.get("Solo se pueden firmar las tareas pendientes de firmar")));
        }

        // Es la defensa real: el <domain> del action-view es solo UX.
        if (isFirmanteElUsuarioAutenticado(tareaFirma) == false) {
            businessMessages.add(new BusinessMessage(I18n.get("Solo puede firmar los documentos la persona a la que se le han encargado")));
        }

        SituacionFirma situacionFirma = getSituacionFirma(tareaFirma);

        if (situacionFirma == SituacionFirma.SIN_DNI) {
            businessMessages.add(new BusinessMessage(I18n.get("No es posible firmar los documentos porque su usuario no tiene un DNI. Póngase en contacto con el administrador.")));
        }

        if (situacionFirma == SituacionFirma.SIN_CERTIFICADO) {
            businessMessages.add(new BusinessMessage(I18n.get("No es posible firmar en el servidor porque no tiene un certificado digital dado de alta")));
        }

        if (situacionFirma == SituacionFirma.DISPOSITIVO_SIN_PIN && TextUtil.isNullOrBlank(claveCertificado)) {
            businessMessages.add(new BusinessMessage(I18n.get("El PIN es obligatorio")));
        }

        if (situacionFirma == SituacionFirma.FICHERO_SIN_CLAVE && TextUtil.isNullOrBlank(claveCertificado)) {
            businessMessages.add(new BusinessMessage(I18n.get("La contraseña es obligatoria")));
        }

        if (tareaFirma.getDocumentosFirma() == null || tareaFirma.getDocumentosFirma().isEmpty()) {
            businessMessages.add(new BusinessMessage(I18n.get("La tarea de firma no tiene ningún documento que firmar")));
        }

        if (businessMessages.isValid() && isClaveCertificadoCorrecta(tareaFirma, claveCertificado)==false) {
            businessMessages.add(new BusinessMessage(I18n.get("No es posible firmar los documentos: %s")
                    .formatted(CertificadoDigitalHelper.motivoClaveErronea(situacionFirma))));
        }

        return businessMessages.isValid() ? Optional.empty() : Optional.of(businessMessages);
    }


    /**************************************************************************************/
    /********************************   AllowProperties   *********************************/
    /**************************************************************************************/

    @Override
    public AllowProperties allowPropertiesMarcarComoFirmada() {
        return AllowProperties.createAllowProperties(Map.of("documentosFirma", Map.of("documentoFirmado", Map.of())));
    }

    @Override
    public AllowProperties allowPropertiesMarcarComoRechazada() {
        return AllowProperties.createAllowProperties(Map.of("motivoRechazo", Map.of()));
    }

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
     * Trabaja en dos fases para garantizar el «todo o nada»: primero firma todos los documentos en memoria y
     * solo si todos han salido bien crea sus {@code MetaFile} y los asigna. Si algo falla en la primera fase no
     * se ha creado ningún fichero ni se ha tocado ningún {@code DocumentoFirma}, así que la tarea sigue
     * pendiente y el firmante puede reintentar.
     */
    private void fireActionRule_FirmarDocumentosEnServidor(TareaFirma tareaFirma, String claveCertificado) {
        CampoFirma campoFirma = getCampoFirma(tareaFirma);
        List<DocumentoFirmado> documentosFirmados = firmarDocumentosEnMemoria(tareaFirma, claveCertificado, campoFirma);

        publicarDocumentosFirmados(documentosFirmados);
    }

    /**
     * Asignación incondicional: {@code estadoTareaFirma} y {@code fechaResolucion} son campos que dicta el
     * servidor, así que una guarda por nulo permitiría al cliente dictar el estado o falsificar la fecha por el
     * endpoint REST genérico. La comparten las dos acciones que resuelven una tarea como firmada.
     */
    private void fireActionRule_ResolverComoFirmada(TareaFirma tareaFirma) {
        tareaFirma.setEstadoTareaFirma(EstadoTareaFirma.FIRMADO);
        tareaFirma.setFechaResolucion(LocalDateTime.now(Convert.defaultZoneId));
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
     * Dónde se firma cada documento de la tarea: en su campo de firma {@code nombreCampoFirma} o, si la tarea
     * no lo indica, en su rectángulo. El recuadro es BigDecimal en la entidad y float en Rectangulo, de ahí
     * los floatValue().
     */
    private static CampoFirma getCampoFirma(TareaFirma tareaFirma) {
        CampoFirma campoFirma;

        if (tareaFirma.getNombreCampoFirma() != null) {
            campoFirma = new CampoFirma(tareaFirma.getNombreCampoFirma());
        } else {
            campoFirma = new CampoFirma(new Rectangulo(
                    tareaFirma.getX().floatValue(),
                    tareaFirma.getY().floatValue(),
                    tareaFirma.getWidth().floatValue(),
                    tareaFirma.getHeight().floatValue()))
                    .setNumeroPagina(tareaFirma.getPage());
        }

        return campoFirma;
    }

    /**
     * Dónde se firma: el DTO trae o el nombre del campo de firma o el rectángulo y su página, nunca los dos.
     * Se asignan todos los campos, también los de la forma que no se usa, que quedan a null.
     */
    private static void asignarLugarFirma(TareaFirma tareaFirma, TareaFirmaInsertDTO tareaFirmaInsertDTO) {
        Rectangulo areaFirma = tareaFirmaInsertDTO.areaFirma();

        tareaFirma.setNombreCampoFirma(tareaFirmaInsertDTO.nombreCampoFirma());
        tareaFirma.setPage(tareaFirmaInsertDTO.page());
        if (areaFirma == null) {
            tareaFirma.setX(null);
            tareaFirma.setY(null);
            tareaFirma.setWidth(null);
            tareaFirma.setHeight(null);
        } else {
            tareaFirma.setX(BigDecimal.valueOf(areaFirma.x()));
            tareaFirma.setY(BigDecimal.valueOf(areaFirma.y()));
            tareaFirma.setWidth(BigDecimal.valueOf(areaFirma.width()));
            tareaFirma.setHeight(BigDecimal.valueOf(areaFirma.height()));
        }
    }

    /**
     * Par intermedio de la fase de firma: un documento de la tarea junto al PDF que ya se ha firmado en memoria
     * pero todavía no se ha publicado.
     */
    private record DocumentoFirmado(DocumentoFirma documentoFirma, DocumentoPdf documentoPdfFirmado) {
    }

    /**
     * No crea ningún {@code MetaFile} ni modifica ninguna entidad. Como la publicación solo trabaja sobre lo que este método
     * devuelve, un fallo no puede acabar publicando una lista parcial.
     */
    private List<DocumentoFirmado> firmarDocumentosEnMemoria(TareaFirma tareaFirma, String claveCertificado, CampoFirma campoFirma) {
        String dni = tareaFirma.getFirmante().getDni();
        SituacionFirma situacionFirma = getSituacionFirma(tareaFirma);

        List<DocumentoFirmado> documentosFirmados = new ArrayList<>();

        for (DocumentoFirma documentoFirma : tareaFirma.getDocumentosFirma()) {
            try {
                DocumentoPdf documentoPdfOriginal = MetaFileHelper.getDocumentoPdf(documentoFirma.getDocumentoOriginal());

                documentosFirmados.add(new DocumentoFirmado(documentoFirma, firmaEnServidorService.firmar(
                        dni, claveCertificado, documentoPdfOriginal, campoFirma)));
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
     * Solo se invoca con la lista completa que devuelve la fase de firma, así que un fallo de firma no puede
     * llegar hasta aquí: es la garantía «todo o nada».
     */
    private void publicarDocumentosFirmados(List<DocumentoFirmado> documentosFirmados) {
        for (DocumentoFirmado documentoFirmado : documentosFirmados) {
            MetaFile metaFileFirmado = MetaFileHelper.createMetaFile(documentoFirmado.documentoPdfFirmado());

            documentoFirmado.documentoFirma().setDocumentoFirmado(metaFileFirmado);
        }
    }

    /**
     * Una tarea solo se resuelve una vez: FIRMADO y RECHAZADO son estados finales, así que
     * todas las acciones sobre la tarea exigen que siga PENDIENTE.
     */
    private static boolean isPendiente(TareaFirma tareaFirma) {
        return tareaFirma.getEstadoTareaFirma() == EstadoTareaFirma.PENDIENTE;
    }

    private static boolean isMismosDocumentosQueOriginal(TareaFirma tareaFirma, TareaFirma tareaFirmaOriginal) {
        List<Long> ids = getIdsDocumentosFirma(tareaFirma);
        List<Long> idsOriginal = getIdsDocumentosFirma(tareaFirmaOriginal);

        return ids.contains(null) == false
                && ids.size() == idsOriginal.size()
                && new HashSet<>(ids).equals(new HashSet<>(idsOriginal));
    }

    private static List<Long> getIdsDocumentosFirma(TareaFirma tareaFirma) {
        if (tareaFirma == null || tareaFirma.getDocumentosFirma() == null) {
            return List.of();
        }

        return tareaFirma.getDocumentosFirma().stream().map(DocumentoFirma::getId).toList();
    }

    private static BusinessMessages validateDocumentosFirmados(TareaFirma tareaFirma) {
        BusinessMessages businessMessages = new BusinessMessages();

        for (DocumentoFirma documentoFirma : tareaFirma.getDocumentosFirma()) {
            if (documentoFirma.getDocumentoFirmado() == null) {
                businessMessages.add(new BusinessMessage(I18n.get("El documento '%s' debe estar firmado")
                        .formatted(documentoFirma.getDocumentoOriginal().getFileName())));
                continue;
            }

            DocumentoPdf documentoOriginal = MetaFileHelper.getDocumentoPdf(documentoFirma.getDocumentoOriginal());
            DocumentoPdf documentoFirmado = MetaFileHelper.getDocumentoPdf(documentoFirma.getDocumentoFirmado());
            Optional<String> errorFirma = DocumentoPdfUtil.validateFirmaPdf(documentoOriginal, documentoFirmado, tareaFirma.getFirmante().getDni());
            if (errorFirma.isPresent()) {
                businessMessages.add(new BusinessMessage(documentoFirmado.getFileName(),errorFirma.get()));
            }
        }

        return businessMessages;
    }

    private boolean isFirmanteElUsuarioAutenticado(TareaFirma tareaFirma) {
        User firmante = tareaFirma.getFirmante();
        User usuarioAutenticado = SecurityUtil.getUser();

        if (firmante == null || firmante.getId() == null || usuarioAutenticado == null) {
            return false;
        }

        return firmante.getId().equals(usuarioAutenticado.getId());
    }

    private boolean isClaveCertificadoCorrecta(TareaFirma tareaFirma, String claveCertificado) {
        String dni = tareaFirma.getFirmante().getDni();

        return CertificadoDigitalHelper.isClaveCertificadoCorrecta(dni, claveCertificado);

    }


    private SituacionFirma getSituacionFirma(TareaFirma tareaFirma) {
        User firmante = tareaFirma.getFirmante();

        return CertificadoDigitalHelper.getSituacionFirmaByDni(firmante == null ? null : firmante.getDni());
    }

}
