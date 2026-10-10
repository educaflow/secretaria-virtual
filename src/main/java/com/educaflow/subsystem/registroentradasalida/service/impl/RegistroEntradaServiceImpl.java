package com.educaflow.subsystem.registroentradasalida.service.impl;

import com.axelor.db.modelservice.ModelServiceFactory;
import com.educaflow.base.util.Idioma;
import com.educaflow.subsystem.common.db.CargoCodigo;
import com.educaflow.subsystem.criptografia.db.CertificadoDigital;
import com.educaflow.subsystem.criptografia.service.CertificadoDigitalService;
import com.axelor.db.Repository;
import com.axelor.db.modelservice.AllowProperties;
import com.axelor.db.modelservice.BusinessMessages;
import com.axelor.db.modelservice.DefaultModelService;
import com.axelor.i18n.I18n;
import com.axelor.meta.db.MetaFile;
import com.educaflow.base.infrastructure.criptografia.AlmacenClave;
import com.educaflow.base.infrastructure.metafile.MetaFileHelper;
import com.educaflow.base.infrastructure.numeradores.db.repo.NumeradorRepository;
import com.educaflow.base.infrastructure.pdf.*;
import com.educaflow.base.infrastructure.pdfgenerator.PdfGeneratorFactory;
import com.educaflow.base.util.TextUtil;
import com.educaflow.subsystem.common.db.Centro;
import com.educaflow.subsystem.registroentradasalida.service.RegistroEntradaInsertDTO;
import com.educaflow.subsystem.registroentradasalida.service.PersonaRegistro;
import com.educaflow.subsystem.registroentradasalida.db.RegistroEntrada;
import com.educaflow.subsystem.registroentradasalida.service.RegistroEntradaService;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import com.educaflow.base.util.Convert;


public class RegistroEntradaServiceImpl extends DefaultModelService<RegistroEntrada> implements RegistroEntradaService {

    private static final Logger log = LoggerFactory.getLogger(RegistroEntradaServiceImpl.class);

    /** El {@code campoFirma} del hueco de la firma del secretario en {@code documentospdf/registro_entrada_plantilla.xml}. */
    private static final String CAMPO_FIRMA_REGISTRO_ENTRADA = "firmaSecretario";

    @Inject
    NumeradorRepository numeradorRepository;

    @Inject
    ModelServiceFactory modelServiceFactory;

    public RegistroEntradaServiceImpl(Class<RegistroEntrada> model, Repository<RegistroEntrada> repository) {
        super(model, repository);
    }

    @Override
    public RegistroEntrada insert(RegistroEntrada registroEntrada) {
        throw new UnsupportedOperationException(I18n.get("Los registros de entrada solo los crea el servidor."));
    }

    @Override
    public RegistroEntrada update(RegistroEntrada nuevo, RegistroEntrada original) {
        throw new UnsupportedOperationException(I18n.get("Los registros de entrada no se pueden modificar."));
    }

    @Override
    public void remove(RegistroEntrada registroEntrada) {
        throw new UnsupportedOperationException(I18n.get("Los registros de entrada no se pueden borrar."));
    }

    @Override
    public RegistroEntrada createRegistroEntrada(RegistroEntradaInsertDTO registroEntradaInsertDTO, MetaFile documentoOriginalFirmado, List<MetaFile> anexos) {
        validateCreateRegistroEntrada(registroEntradaInsertDTO, documentoOriginalFirmado, anexos).ifPresent(BusinessMessages::throwIfInvalid);

        LocalDateTime ahora=LocalDateTime.now(Convert.defaultZoneId);
        RegistroEntrada registroEntrada=new RegistroEntrada();

        String numeroRegistro=getNumeroRegistro(registroEntradaInsertDTO.centro(),ahora);
        registroEntrada.setNumeroRegistro(numeroRegistro);

        DocumentoPdf documentoPdfEntrada=MetaFileHelper.getDocumentoPdf(documentoOriginalFirmado);
        DatosRegistroEntradaPdf datosRegistroEntradaPdf=new DatosRegistroEntradaPdf(
                registroEntradaInsertDTO.centro(),
                registroEntradaInsertDTO.solicitante(),
                registroEntradaInsertDTO.interesado(),
                registroEntradaInsertDTO.numeroExpediente(),
                registroEntradaInsertDTO.asunto(),
                ahora,
                numeroRegistro,
                Idioma.deCodigo(registroEntradaInsertDTO.idioma())
        );
        DocumentoPdf primeraPaginaRegistroEntrada=getPrimeraPaginaRegistroEntrada( datosRegistroEntradaPdf);
        DocumentoPdf documentoPdfFinal=primeraPaginaRegistroEntrada.anyadirDocumentoPdf(documentoPdfEntrada);
        DocumentoPdf documentoPdfFinalFirmado=firmarPorSecretario(documentoPdfFinal, registroEntradaInsertDTO.centro());
        MetaFile metaFilePdfFinal= MetaFileHelper.createMetaFile(documentoPdfFinalFirmado);

        documentoOriginalFirmado.setFileName(getNombreDocumentoOriginalFirmado(datosRegistroEntradaPdf));
        registroEntrada.setDocumentoOriginalFirmado(documentoOriginalFirmado);
        registroEntrada.setFecha(ahora);
        registroEntrada.setDocumentoResguardoPresentacion(metaFilePdfFinal);
        registroEntrada.setAnexos(anexos);
        registroEntrada.setAsunto(registroEntradaInsertDTO.asunto());
        registroEntrada.setCentro(registroEntradaInsertDTO.centro());
        return registroEntrada;
    }

    /**************************************************************************************/
    /******************************* Métodos de Validación ********************************/
    /**************************************************************************************/

    @Override
    public Optional<BusinessMessages> validateInsert(RegistroEntrada registroEntrada) {
        return Optional.of(BusinessMessages.single(I18n.get("Los registros de entrada solo los crea el servidor.")));
    }

    @Override
    public Optional<BusinessMessages> validateUpdate(RegistroEntrada nuevo, RegistroEntrada original) {
        return Optional.of(BusinessMessages.single(I18n.get("Los registros de entrada no se pueden modificar.")));
    }

    @Override
    public Optional<BusinessMessages> validateRemove(RegistroEntrada registroEntrada) {
        return Optional.of(BusinessMessages.single(I18n.get("Los registros de entrada no se pueden borrar.")));
    }

    @Override
    public Optional<BusinessMessages> validateCreateRegistroEntrada(RegistroEntradaInsertDTO registroEntradaInsertDTO, MetaFile documentoOriginalFirmado, List<MetaFile> anexos) {
        if (MetaFileHelper.isPdf(documentoOriginalFirmado) == false) {
            return Optional.of(BusinessMessages.single(I18n.get("El fichero proporcionado no es un PDF válido.")));
        }

        return Optional.empty();
    }

    /************************************************************************************/
    /********************************* AllowProperties **********************************/
    /************************************************************************************/

    // Un registro de entrada solo lo crea el servidor (createRegistroEntrada): por el REST automático
    // no se acepta ningún campo, que si no cualquiera con permiso de escritura dictaría el número o el documento.
    @Override
    public AllowProperties allowPropertiesInsert() {
        return AllowProperties.createDenyAllProperties();
    }

    @Override
    public AllowProperties allowPropertiesUpdate() {
        return AllowProperties.createDenyAllProperties();
    }

    /***********************************************************************************/
    /********************************** Action Rules ***********************************/
    /***********************************************************************************/

    /***********************************************************************************/
    /********************************* Otras funciones *********************************/
    /***********************************************************************************/

    private String getNumeroRegistro(Centro centro, LocalDateTime ahora) {
        String anyoActual= String.valueOf(ahora.getYear());
        String codigoCentro = centro.getCode();
        long numeroRegistroSinAnyo = numeradorRepository.getSiguienteNumeroRegistroEntrada(codigoCentro, anyoActual);
        String numeroRegistro = String.format("%05d", numeroRegistroSinAnyo) + "/" + anyoActual + "-" + codigoCentro;

        return numeroRegistro;
    }

    private DocumentoPdf firmarPorSecretario(DocumentoPdf documentoPdf,Centro centro) {
        CertificadoDigitalService certificadoDigitalService = (CertificadoDigitalService) modelServiceFactory.resolve(CertificadoDigital.class);
        AlmacenClave almacenClave= certificadoDigitalService.getByCentroCargo(centro, CargoCodigo.SECRETARIO);
        CampoFirma campoFirma=new CampoFirma(CAMPO_FIRMA_REGISTRO_ENTRADA);

        DocumentoPdf documentoPdfFirmado=documentoPdf.firmar(almacenClave,campoFirma);

        return documentoPdfFirmado;
    }

    private DocumentoPdf getPrimeraPaginaRegistroEntrada(DatosRegistroEntradaPdf datosRegistroEntradaPdf) {
        String xmlFileName="registro_entrada_plantilla.xml";

        try (InputStream in = getInputStreamFromDocumentosPdf(xmlFileName)) {
            if (in == null) {
                log.error("No se encontró el recurso: {}", xmlFileName);
                throw new IllegalStateException("No se encontró el recurso: " + xmlFileName);
            }
            Map<String, Object> contexto = Map.of("self", datosRegistroEntradaPdf);

            byte[] pdf = PdfGeneratorFactory.getPdfGenerator().generate(in.readAllBytes(), contexto, datosRegistroEntradaPdf.idioma());

            return DocumentoPdfFactory.getDocumentoPdf(pdf, getNombreDocumentoResguardoPresentacion(datosRegistroEntradaPdf));
        } catch (IOException e) {
            log.error("Error al cargar el documento PDF: {}", xmlFileName, e);
            throw new IllegalStateException("Error al cargar el documento PDF: " + xmlFileName, e);
        }
    }

    private String getNombreDocumentoOriginalFirmado(DatosRegistroEntradaPdf datosRegistroEntradaPdf) {
        String nombreDocumento;
        if ((datosRegistroEntradaPdf.numeroExpediente!=null) && !datosRegistroEntradaPdf.numeroExpediente.isBlank()) {
            nombreDocumento="solicitud_expediente_" + datosRegistroEntradaPdf.numeroExpediente + ".pdf";
        } else {
            nombreDocumento="registro_entrada_" + datosRegistroEntradaPdf.numeroRegistro + ".pdf";
        }

        return TextUtil.sanitizeFileName(nombreDocumento);
    }

    private String getNombreDocumentoResguardoPresentacion(DatosRegistroEntradaPdf datosRegistroEntradaPdf) {
        String nombreDocumento;
        if ((datosRegistroEntradaPdf.numeroExpediente!=null) && !datosRegistroEntradaPdf.numeroExpediente.isBlank()) {
            nombreDocumento="resguardo_solicitud_expediente_" + datosRegistroEntradaPdf.numeroExpediente + ".pdf";
        } else {
            nombreDocumento="resguardo_registro_entrada_" + datosRegistroEntradaPdf.numeroRegistro + ".pdf";
        }

        return TextUtil.sanitizeFileName(nombreDocumento);
    }

    private InputStream getInputStreamFromDocumentosPdf(String nombreFicheroPdf) {
        String nombreCompletoDocumentoPdf = "/com/educaflow/subsystem/registroentradasalida/documentospdf/" + nombreFicheroPdf;

        return getClass().getResourceAsStream(nombreCompletoDocumentoPdf);
    }

    private record DatosRegistroEntradaPdf(Centro centro, PersonaRegistro solicitante, PersonaRegistro interesado, String numeroExpediente, String asunto, LocalDateTime fecha, String numeroRegistro, Idioma idioma) {
    }

}
