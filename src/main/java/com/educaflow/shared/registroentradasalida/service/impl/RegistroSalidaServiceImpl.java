package com.educaflow.shared.registroentradasalida.service.impl;

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
import com.educaflow.shared.certificados.AlmacenClaveLoader;
import com.educaflow.shared.common.db.Centro;
import com.educaflow.shared.registroentradasalida.service.DatosRegistroSalida;
import com.educaflow.shared.registroentradasalida.db.RegistroSalida;
import com.educaflow.shared.registroentradasalida.service.RegistroSalidaService;
import jakarta.inject.Inject;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class RegistroSalidaServiceImpl implements RegistroSalidaService {


    NumeradorRepository numeradorRepository;
    AlmacenClaveLoader almacenClaveLoader;

    @Inject
    public RegistroSalidaServiceImpl(NumeradorRepository numeradorRepository,AlmacenClaveLoader almacenClaveLoader) {
        this.numeradorRepository=numeradorRepository;
        this.almacenClaveLoader=almacenClaveLoader;
    }


    public RegistroSalida createRegistroSalida(DatosRegistroSalida datosRegistroSalida, MetaFile documentoOriginal, List<MetaFile> anexos) {

        if (MetaFileHelper.isPdf(documentoOriginal)==false) {
            throw new IllegalArgumentException("El fichero proporcionado no es un PDF válido.");
        }

        LocalDateTime ahora=LocalDateTime.now();
        Centro centro=datosRegistroSalida.centro();
        String numeroRegistro=getNumeroRegistro(centro,ahora);
        AlmacenClave almacenClave=almacenClaveLoader.getSecretario(centro);
        MetaFile documento=firmarRegistroSalidaPorSecretario(MetaFileHelper.getDocumentoPdf(documentoOriginal),almacenClave,numeroRegistro);

        RegistroSalida registroSalida=new RegistroSalida();
        registroSalida.setNumeroRegistro(numeroRegistro);
        registroSalida.setDocumentoOriginal(documentoOriginal);
        registroSalida.setDocumento(documento);
        registroSalida.setFecha(ahora);
        registroSalida.setAnexos(anexos);
        registroSalida.setCentro(datosRegistroSalida.centro());



        return registroSalida;
    }




    private String getNumeroRegistro(Centro centro, LocalDateTime ahora) {
        String anyoActual= String.valueOf(ahora.getYear());
        String codigoCentro = centro.getCode();
        long numeroRegistroSinAnyo = numeradorRepository.getSiguienteNumeroRegistroSalida(codigoCentro, anyoActual);
        String numeroRegistro = String.format("%05d", numeroRegistroSinAnyo) + "/" + anyoActual;

        return numeroRegistro;
    }


    private MetaFile firmarRegistroSalidaPorSecretario(DocumentoPdf documentoPdf, AlmacenClave almacenClave , String numeroRegistro) {

        CampoFirma campoFirma=new CampoFirma(new Rectangulo(10,10,300,20)).setNumeroPagina(1).setMensaje("Nº Registro Salida:"+numeroRegistro).setMotivo("Firma del Registro de Salida Nº "+numeroRegistro);

        DocumentoPdf documentoPdfFirmado=documentoPdf.firmar(almacenClave,campoFirma);

        return MetaFileHelper.createMetaFile(documentoPdfFirmado);

    }






}
