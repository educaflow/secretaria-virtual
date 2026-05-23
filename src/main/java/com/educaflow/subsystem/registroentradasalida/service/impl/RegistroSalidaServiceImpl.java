package com.educaflow.subsystem.registroentradasalida.service.impl;

import com.axelor.db.Repository;
import com.axelor.db.modelservice.BusinessMessages;
import com.axelor.db.modelservice.DefaultModelService;
import com.axelor.meta.db.MetaFile;
import com.educaflow.base.infrastructure.criptografia.AlmacenClave;
import com.educaflow.base.infrastructure.mail.Attach;
import com.educaflow.base.infrastructure.mail.Mail;
import com.educaflow.base.infrastructure.mail.MailSender;
import com.educaflow.base.infrastructure.metafile.MetaFileHelper;
import com.educaflow.base.infrastructure.numeradores.db.repo.NumeradorRepository;
import com.educaflow.base.infrastructure.pdf.CampoFirma;
import com.educaflow.base.infrastructure.pdf.DocumentoPdf;
import com.educaflow.base.infrastructure.pdf.Rectangulo;
import com.educaflow.base.util.MetaFileUtil;
import com.educaflow.subsystem.criptografia.service.AlmacenClaveResolver;
import com.educaflow.subsystem.common.db.Centro;
import com.educaflow.subsystem.registroentradasalida.service.RegistroSalidaInsertDTO;
import com.educaflow.subsystem.registroentradasalida.db.RegistroSalida;
import com.educaflow.subsystem.registroentradasalida.service.RegistroSalidaService;
import jakarta.inject.Inject;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class RegistroSalidaServiceImpl extends DefaultModelService<RegistroSalida> implements RegistroSalidaService {

    private static final Rectangulo rectanguloPosicionFirmaPDFRegistroSalida =new Rectangulo(10,10,300,20);

    @Inject
    NumeradorRepository numeradorRepository;

    @Inject
    AlmacenClaveResolver almacenClaveResolver;

    @Inject
    MailSender mailSender;

    @Inject
    public RegistroSalidaServiceImpl(Class<RegistroSalida> model, Repository<RegistroSalida> repository) {
        super(model, repository);
    }


    @Override
    public RegistroSalida insert(RegistroSalida entity) {
        entity = super.insert(entity);
        fireActionRule_NotificarRegistroSalida(entity);
        return entity;
    }

    @Override
    public RegistroSalida update(RegistroSalida entity, RegistroSalida original) {
        entity = super.update(entity, original);
        fireActionRule_NotificarRegistroSalida(entity);
        return entity;
    }

    @Override
    public RegistroSalida createRegistroSalida(RegistroSalidaInsertDTO registroSalidaInsertDTO, MetaFile documentoOriginal, List<MetaFile> anexos) {
        validateCreateRegistroSalida(registroSalidaInsertDTO, documentoOriginal, anexos).ifPresent(BusinessMessages::throwIfInvalid);

        if (MetaFileHelper.isPdf(documentoOriginal)==false) {
            throw new IllegalArgumentException("El fichero proporcionado no es un PDF válido.");
        }

        LocalDateTime ahora=LocalDateTime.now();
        String asunto= registroSalidaInsertDTO.asunto();
        Centro centro= registroSalidaInsertDTO.centro();
        String numeroRegistro=getNumeroRegistro(centro,ahora);
        AlmacenClave almacenClave= almacenClaveResolver.getSecretario(centro);
        MetaFile documento=firmarRegistroSalidaPorSecretario(MetaFileHelper.getDocumentoPdf(documentoOriginal),almacenClave,numeroRegistro);

        RegistroSalida registroSalida=new RegistroSalida();
        registroSalida.setAsunto(asunto);
        registroSalida.setNumeroRegistro(numeroRegistro);
        registroSalida.setDocumentoOriginal(documentoOriginal);
        registroSalida.setDocumento(documento);
        registroSalida.setFecha(ahora);
        registroSalida.setAnexos(anexos);
        registroSalida.setCentro(registroSalidaInsertDTO.centro());



        return registroSalida;
    }


    /****************************************************************************************/
    /******************************** Métodos de Validación *********************************/
    /****************************************************************************************/

    @Override
    public Optional<BusinessMessages> validateCreateRegistroSalida(RegistroSalidaInsertDTO registroSalidaInsertDTO, MetaFile documentoOriginal, List<MetaFile> anexos) {
        return Optional.empty();
    }


    /*************************************************************************************/
    /********************************    Action Rules    *********************************/
    /*************************************************************************************/

    private void fireActionRule_NotificarRegistroSalida(RegistroSalida registroSalida) {
        List<Attach> attachs = createAttachFromMetaFiles(registroSalida.getAnexos());
        attachs.add(createAttachFromMetaFile(registroSalida.getDocumento()));
        String subject = "Nuevo Registro de Salida Nº " + registroSalida.getNumeroRegistro();
        String body = "Se ha creado un nuevo registro de salida con número " + registroSalida.getNumeroRegistro() + " en el centro " + registroSalida.getCentro().getName();
        Mail mail = new Mail(List.of("nada@gmail.com"), "secretariavirtual@fpmislata.com", subject, body, body, attachs);

        mailSender.send(mail);
    }


    /*************************************************************************************/
    /********************************    Otras funciones    ******************************/
    /*************************************************************************************/

    private String getNumeroRegistro(Centro centro, LocalDateTime ahora) {
        String anyoActual= String.valueOf(ahora.getYear());
        String codigoCentro = centro.getCode();
        long numeroRegistroSinAnyo = numeradorRepository.getSiguienteNumeroRegistroSalida(codigoCentro, anyoActual);
        String numeroRegistro = String.format("%05d", numeroRegistroSinAnyo) + "/" + anyoActual;

        return numeroRegistro;
    }

    private Attach createAttachFromMetaFile(MetaFile metaFile) {
        return new Attach(metaFile.getFileName(), MetaFileUtil.downloadContent(metaFile), metaFile.getFileType());
    }

    private List<Attach> createAttachFromMetaFiles(List<MetaFile> metaFiles) {
        List<Attach> attachs = new ArrayList<>();
        for (MetaFile metaFile : metaFiles) {
            attachs.add(createAttachFromMetaFile(metaFile));
        }
        return attachs;
    }


    private MetaFile firmarRegistroSalidaPorSecretario(DocumentoPdf documentoPdf, AlmacenClave almacenClave , String numeroRegistro) {

        CampoFirma campoFirma=new CampoFirma(rectanguloPosicionFirmaPDFRegistroSalida).setNumeroPagina(1).setMensaje("Nº Registro Salida:"+numeroRegistro).setMotivo("Firma del Registro de Salida Nº "+numeroRegistro);

        DocumentoPdf documentoPdfFirmado=documentoPdf.firmar(almacenClave,campoFirma);

        return MetaFileHelper.createMetaFile(documentoPdfFirmado);

    }




}
