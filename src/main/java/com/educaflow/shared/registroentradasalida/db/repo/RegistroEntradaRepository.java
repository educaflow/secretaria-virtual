package com.educaflow.shared.registroentradasalida.db.repo;

import com.axelor.meta.db.MetaFile;
import com.educaflow.base.infrastructure.criptografia.AlmacenClave;
import com.educaflow.base.infrastructure.criptografia.AlmacenClaveDispositivo;
import com.educaflow.base.infrastructure.metafile.MetaFileHelper;
import com.educaflow.base.infrastructure.numeradores.db.repo.NumeradorRepository;
import com.educaflow.base.infrastructure.pdf.*;
import com.educaflow.shared.certificados.AlmacenClaveLoader;
import com.educaflow.shared.common.db.Centro;
import com.educaflow.shared.registroentradasalida.db.DatosRegistroEntrada;
import com.educaflow.shared.registroentradasalida.db.PersonaRegistro;
import com.educaflow.shared.registroentradasalida.db.RegistroEntrada;
import jakarta.inject.Inject;

import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.Map;

public class RegistroEntradaRepository extends AbstractRegistroEntradaRepository  {

    @Inject
    NumeradorRepository numeradorRepository;

    @Inject
    AlmacenClaveLoader almacenClaveLoader;

    public RegistroEntradaRepository() {
        super();
    }

    public RegistroEntrada createRegistroEntrada(DatosRegistroEntrada datosRegistroEntrada, MetaFile metaFilePdf) {

        if (MetaFileHelper.isPdf(metaFilePdf)==false) {
            throw new IllegalArgumentException("El fichero proporcionado no es un PDF válido.");
        }

        LocalDateTime ahora=LocalDateTime.now();
        RegistroEntrada registroEntrada=new RegistroEntrada();

        String numeroRegistro=getNumeroRegistro(datosRegistroEntrada.centro(),ahora);
        registroEntrada.setNumeroRegistro(numeroRegistro);

        DocumentoPdf documentoPdfEntrada=MetaFileHelper.getDocumentoPdf(metaFilePdf);
        DatosRegistroEntradaPdf datosRegistroEntradaPdf=new DatosRegistroEntradaPdf(
                datosRegistroEntrada.centro(),
                datosRegistroEntrada.presentador(),
                datosRegistroEntrada.interesado(),
                datosRegistroEntrada.numeroExpediente(),
                datosRegistroEntrada.asunto(),
                ahora,
                numeroRegistro
        );
        DocumentoPdf primeraPaginaRegistroEntrada=getPrimeraPaginaRegistroEntrada( datosRegistroEntradaPdf);
        DocumentoPdf documentoPdfFinal=primeraPaginaRegistroEntrada.anyadirDocumentoPdf(documentoPdfEntrada);
        DocumentoPdf documentoPdfFinalFirmado=firmarPorSecretario(documentoPdfFinal,datosRegistroEntrada.centro());
        MetaFile metaFilePdfFinal= MetaFileHelper.createMetaFile(documentoPdfFinalFirmado);

        registroEntrada.setDocumentoOriginalFirmado(metaFilePdf);
        registroEntrada.setFecha(ahora);
        registroEntrada.setDocumentoResguardoPresentacion(metaFilePdfFinal);
        return registroEntrada;
    }


    private String getNumeroRegistro(Centro centro,LocalDateTime ahora) {
        String anyoActual= String.valueOf(ahora.getYear());
        String codigoCentro = centro.getCode();
        long numeroRegistroSinAnyo = numeradorRepository.getSiguienteNumeroRegistroEntrada(codigoCentro, anyoActual);
        String numeroRegistro = String.format("%05d", numeroRegistroSinAnyo) + "/" + anyoActual;

        return numeroRegistro;
    }


    private DocumentoPdf firmarPorSecretario(DocumentoPdf documentoPdf,Centro centro) {
        AlmacenClave almacenClave=almacenClaveLoader.getSecretario(centro);
        CampoFirma campoFirma=new CampoFirma(new Rectangulo(80,140,400,100)).setNumeroPagina(1);

        DocumentoPdf documentoPdfFirmado=documentoPdf.firmar(almacenClave,campoFirma);

        return documentoPdfFirmado;
    }


    private DocumentoPdf getPrimeraPaginaRegistroEntrada(DatosRegistroEntradaPdf datosRegistroEntradaPdf) {
        String pdfFileName="registro_entrada_plantilla.pdf";

        try (InputStream in = getInputStreamFromDocumentosPdf(pdfFileName)) {
            if (in == null) {
                throw new RuntimeException("No se encontró el recurso: " + pdfFileName);
            }
            DocumentoPdf documentoPdfVacio = DocumentoPdfFactory.getDocumentoPdf(in.readAllBytes(), pdfFileName);

            Map<String, Object> contexto = Map.of("self", datosRegistroEntradaPdf);

            DocumentoPdf documentoPdfRelleno = DocumentoPdfUtil.generate(documentoPdfVacio, contexto);

            return documentoPdfRelleno;
        } catch (IOException e) {
            throw new RuntimeException("Error al cargar el documento PDF: " + pdfFileName, e);
        }
    }

    private InputStream getInputStreamFromDocumentosPdf(String nombreFicheroPdf) {
        final int upperPackages=2; //Este numero es los paquetes que hay que subir para llegar a la carpeta 'documentospdf'

        String packagePath = this.getClass().getPackage().getName().replace('.', '/');
        String[] parts = packagePath.split("/");
        StringBuilder sb = new StringBuilder("/");
        for (int i = 0; i < parts.length - upperPackages; i++) {
            sb.append(parts[i]).append("/");
        }
        sb.append("documentospdf/"+nombreFicheroPdf);

        String nombreCompletoDocumentoPdf = sb.toString();

        System.out.println("-------------------Cargando recurso PDF desde: " + nombreCompletoDocumentoPdf);

        return getClass().getClassLoader().getResourceAsStream(nombreCompletoDocumentoPdf);
    }





    private record DatosRegistroEntradaPdf(Centro centro, PersonaRegistro presentador, PersonaRegistro interesado, String numeroExpediente, String asunto,LocalDateTime fecha,String numeroRegistro) {
    }

}
