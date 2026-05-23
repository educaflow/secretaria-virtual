package com.educaflow.subsystem.registroentradasalida.service.impl;

import com.axelor.db.Repository;
import com.axelor.db.modelservice.BusinessMessages;
import com.axelor.db.modelservice.DefaultModelService;
import com.axelor.meta.db.MetaFile;
import com.educaflow.base.infrastructure.criptografia.AlmacenClave;
import com.educaflow.base.infrastructure.metafile.MetaFileHelper;
import com.educaflow.base.infrastructure.numeradores.db.repo.NumeradorRepository;
import com.educaflow.base.infrastructure.pdf.*;
import com.educaflow.base.util.TextUtil;
import com.educaflow.subsystem.criptografia.service.AlmacenClaveResolver;
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


public class RegistroEntradaServiceImpl extends DefaultModelService<RegistroEntrada> implements RegistroEntradaService {

    private static final Logger log = LoggerFactory.getLogger(RegistroEntradaServiceImpl.class);

    private static final Rectangulo rectanguloPosicionFirmaPDFRegistroEntrada =new Rectangulo(80,200,400,100);

    @Inject
    NumeradorRepository numeradorRepository;

    @Inject
    AlmacenClaveResolver almacenClaveResolver;

    @Inject
    public RegistroEntradaServiceImpl(Class<RegistroEntrada> model, Repository<RegistroEntrada> repository) {
        super(model, repository);
    }

    @Override
    public RegistroEntrada createRegistroEntrada(RegistroEntradaInsertDTO registroEntradaInsertDTO, MetaFile documentoOriginalFirmado, List<MetaFile> anexos) {
        validateCreateRegistroEntrada(registroEntradaInsertDTO, documentoOriginalFirmado, anexos).ifPresent(BusinessMessages::throwIfInvalid);

        if (MetaFileHelper.isPdf(documentoOriginalFirmado)==false) {
            throw new IllegalArgumentException("El fichero proporcionado no es un PDF válido.");
        }

        LocalDateTime ahora=LocalDateTime.now();
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
                numeroRegistro
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

    /****************************************************************************************/
    /******************************** Métodos de Validación *********************************/
    /****************************************************************************************/

    @Override
    public Optional<BusinessMessages> validateCreateRegistroEntrada(RegistroEntradaInsertDTO registroEntradaInsertDTO, MetaFile documentoOriginalFirmado, List<MetaFile> anexos) {
        return Optional.empty();
    }

    /**************************************************************************************/
    /********************************   AllowProperties   *********************************/
    /**************************************************************************************/

    /*************************************************************************************/
    /********************************    Action Rules    *********************************/
    /*************************************************************************************/

    /*************************************************************************************/
    /********************************    Otras funciones    ******************************/
    /*************************************************************************************/

    private String getNumeroRegistro(Centro centro, LocalDateTime ahora) {
        String anyoActual= String.valueOf(ahora.getYear());
        String codigoCentro = centro.getCode();
        long numeroRegistroSinAnyo = numeradorRepository.getSiguienteNumeroRegistroEntrada(codigoCentro, anyoActual);
        String numeroRegistro = String.format("%05d", numeroRegistroSinAnyo) + "/" + anyoActual;

        return numeroRegistro;
    }


    private DocumentoPdf firmarPorSecretario(DocumentoPdf documentoPdf,Centro centro) {
        AlmacenClave almacenClave= almacenClaveResolver.getSecretario(centro);
        CampoFirma campoFirma=new CampoFirma(rectanguloPosicionFirmaPDFRegistroEntrada).setNumeroPagina(1);

        DocumentoPdf documentoPdfFirmado=documentoPdf.firmar(almacenClave,campoFirma);

        return documentoPdfFirmado;
    }


    private DocumentoPdf getPrimeraPaginaRegistroEntrada(DatosRegistroEntradaPdf datosRegistroEntradaPdf) {
        String pdfFileName="registro_entrada_plantilla.pdf";

        try (InputStream in = getInputStreamFromDocumentosPdf(pdfFileName)) {
            if (in == null) {
                log.error("No se encontró el recurso: {}", pdfFileName);
                throw new IllegalStateException("No se encontró el recurso: " + pdfFileName);
            }
            DocumentoPdf documentoPdfVacio = DocumentoPdfFactory.getDocumentoPdf(in.readAllBytes(), getNombreDocumentoResguardoPresentacion(datosRegistroEntradaPdf));

            Map<String, Object> contexto = Map.of("self", datosRegistroEntradaPdf);

            DocumentoPdf documentoPdfRelleno = DocumentoPdfUtil.generate(documentoPdfVacio, contexto);

            return documentoPdfRelleno;
        } catch (IOException e) {
            log.error("Error al cargar el documento PDF: {}", pdfFileName, e);
            throw new IllegalStateException("Error al cargar el documento PDF: " + pdfFileName, e);
        }
    }

    private String getNombreDocumentoOriginalFirmado(DatosRegistroEntradaPdf datosRegistroEntradaPdf) {
        String nombreDocumento;
        if ((datosRegistroEntradaPdf.numeroExpediente!=null) && (!datosRegistroEntradaPdf.numeroExpediente.isBlank())) {
            nombreDocumento="solicitud_expediente_" + datosRegistroEntradaPdf.numeroExpediente + ".pdf";
        } else {
            nombreDocumento="registro_entrada_" + datosRegistroEntradaPdf.numeroRegistro + ".pdf";
        }

        return TextUtil.sanitizeFileName(nombreDocumento);
    }

    private String getNombreDocumentoResguardoPresentacion(DatosRegistroEntradaPdf datosRegistroEntradaPdf) {
        String nombreDocumento;
        if ((datosRegistroEntradaPdf.numeroExpediente!=null) && (!datosRegistroEntradaPdf.numeroExpediente.isBlank())) {
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




    private record DatosRegistroEntradaPdf(Centro centro, PersonaRegistro solicitante, PersonaRegistro interesado, String numeroExpediente, String asunto, LocalDateTime fecha, String numeroRegistro) {
    }


}
