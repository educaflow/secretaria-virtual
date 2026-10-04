package com.educaflow.subsystem.registroentradasalida.service.impl;

import com.axelor.app.AppSettings;
import com.axelor.db.Repository;
import com.axelor.db.modelservice.AllowProperties;
import com.axelor.i18n.I18n;
import com.axelor.db.modelservice.BusinessMessages;
import com.axelor.db.modelservice.DefaultModelService;
import com.axelor.meta.db.MetaFile;
import com.educaflow.base.infrastructure.criptografia.AlmacenClave;
import com.educaflow.base.infrastructure.fichero.Fichero;
import com.educaflow.base.infrastructure.metafile.MetaFileHelper;
import com.educaflow.base.infrastructure.numeradores.db.repo.NumeradorRepository;
import com.educaflow.base.infrastructure.pdf.CampoFirma;
import com.educaflow.base.infrastructure.pdf.DocumentoPdf;
import com.educaflow.base.infrastructure.pdf.PosicionImagen;
import com.educaflow.base.infrastructure.pdf.Rectangulo;
import com.educaflow.base.infrastructure.pdfgenerator.HuecoCabeceraTexto;
import com.educaflow.base.util.MetaFileUtil;
import com.educaflow.base.util.QrUtil;
import com.educaflow.base.util.TextUtil;
import com.educaflow.base.util.TokenUtil;
import com.educaflow.subsystem.criptografia.service.AlmacenClaveResolver;
import com.educaflow.subsystem.common.db.Centro;
import com.educaflow.subsystem.registroentradasalida.db.repo.RegistroSalidaRepository;
import com.educaflow.subsystem.registroentradasalida.service.RegistroSalidaInsertDTO;
import com.educaflow.subsystem.registroentradasalida.db.RegistroSalida;
import com.educaflow.subsystem.registroentradasalida.service.RegistroSalidaService;
import jakarta.inject.Inject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import com.educaflow.base.util.Convert;

public class RegistroSalidaServiceImpl extends DefaultModelService<RegistroSalida> implements RegistroSalidaService {

    /**
     * El recuadro de la firma: el código QR y, debajo, las dos líneas del número de registro. Es el hueco
     * que la cabecera de un documento de texto deja en blanco a su derecha, así que sus medidas son las de él.
     */
    private static final float WIDTH_FIRMA = (float) HuecoCabeceraTexto.ANCHO;
    private static final float HEIGHT_FIRMA = (float) HuecoCabeceraTexto.ALTO;
    private static final int FONT_SIZE_FIRMA = 7;
    /** Lo que se separa el recuadro de la firma de los bordes de arriba y de la derecha de la página. */
    private static final float MARGEN_FIRMA = (float) HuecoCabeceraTexto.MARGEN;
    private static final String ZIP_MIME_TYPE = "application/zip";

    @Inject
    NumeradorRepository numeradorRepository;

    @Inject
    AlmacenClaveResolver almacenClaveResolver;

    public RegistroSalidaServiceImpl(Class<RegistroSalida> model, Repository<RegistroSalida> repository) {
        super(model, repository);
    }


    @Override
    public RegistroSalida createRegistroSalida(RegistroSalidaInsertDTO registroSalidaInsertDTO, MetaFile documentoOriginal, List<MetaFile> anexos) {
        validateCreateRegistroSalida(registroSalidaInsertDTO, documentoOriginal, anexos).ifPresent(BusinessMessages::throwIfInvalid);

        LocalDateTime ahora=LocalDateTime.now(Convert.defaultZoneId);
        String asunto= registroSalidaInsertDTO.asunto();
        Centro centro= registroSalidaInsertDTO.centro();
        String numeroRegistro=getNumeroRegistro(centro,ahora);
        AlmacenClave almacenClave= almacenClaveResolver.getSecretario(centro);
        String csv= TokenUtil.generateCodigoSeguroVerificacion();
        MetaFile documento=firmarRegistroSalidaPorSecretario(MetaFileHelper.getDocumentoPdf(documentoOriginal),almacenClave,numeroRegistro,csv);

        RegistroSalida registroSalida=new RegistroSalida();
        registroSalida.setAsunto(asunto);
        registroSalida.setNumeroRegistro(numeroRegistro);
        registroSalida.setDocumentoOriginal(documentoOriginal);
        registroSalida.setDocumento(documento);
        registroSalida.setFecha(ahora);
        registroSalida.setAnexos(anexos);
        registroSalida.setCentro(registroSalidaInsertDTO.centro());
        registroSalida.setCsv(csv);

        return registroSalida;
    }

    @Override
    public Fichero getDescargaByCsv(String csv) {
        validateGetDescargaByCsv(csv).ifPresent(BusinessMessages::throwIfInvalid);

        RegistroSalida registroSalida = ((RegistroSalidaRepository) repository).findByCsv(csv);
        MetaFile documento = registroSalida.getDocumento();
        if (registroSalida.getAnexos().isEmpty()) {
            return new Fichero(TextUtil.sanitizeFileName(documento.getFileName()), MetaFileUtil.downloadContent(documento), MetaFileHelper.PDF_MIME_TYPE);
        }

        List<MetaFile> ficheros = new ArrayList<>(List.of(documento));
        ficheros.addAll(registroSalida.getAnexos());
        String fileName = TextUtil.sanitizeFileName("registro_salida_" + registroSalida.getNumeroRegistro() + ".zip");

        return new Fichero(fileName, createZip(ficheros), ZIP_MIME_TYPE);
    }


    /****************************************************************************************/
    /******************************** Métodos de Validación *********************************/
    /****************************************************************************************/

    @Override
    public Optional<BusinessMessages> validateInsert(RegistroSalida registroSalida) {
        return Optional.of(BusinessMessages.single(I18n.get("Los registros de salida solo los crea el servidor.")));
    }

    @Override
    public Optional<BusinessMessages> validateUpdate(RegistroSalida nuevo, RegistroSalida original) {
        return Optional.of(BusinessMessages.single(I18n.get("Los registros de salida no se pueden modificar.")));
    }

    @Override
    public Optional<BusinessMessages> validateRemove(RegistroSalida registroSalida) {
        return Optional.of(BusinessMessages.single(I18n.get("Los registros de salida no se pueden borrar.")));
    }

    @Override
    public Optional<BusinessMessages> validateCreateRegistroSalida(RegistroSalidaInsertDTO registroSalidaInsertDTO, MetaFile documentoOriginal, List<MetaFile> anexos) {
        if (MetaFileHelper.isPdf(documentoOriginal) == false) {
            return Optional.of(BusinessMessages.single(I18n.get("El fichero proporcionado no es un PDF válido.")));
        }

        return Optional.empty();
    }

    @Override
    public Optional<BusinessMessages> validateGetDescargaByCsv(String csv) {
        // La forma se comprueba antes de buscar: el CSV llega de una URL pública
        if ((TokenUtil.isCodigoSeguroVerificacion(csv) == false) || (((RegistroSalidaRepository) repository).findByCsv(csv) == null)) {
            return Optional.of(BusinessMessages.single(I18n.get("No existe ningún documento con ese código de verificación.")));
        }

        return Optional.empty();
    }


    /****************************************************************************************/
    /********************************    AllowProperties    *********************************/
    /****************************************************************************************/

    // Un registro de salida solo lo crea el servidor (createRegistroSalida): por el REST automático
    // no se acepta ningún campo, que si no cualquiera con permiso de escritura dictaría el CSV o el documento.
    @Override
    public AllowProperties allowPropertiesInsert() {
        return AllowProperties.createDenyAllProperties();
    }

    @Override
    public AllowProperties allowPropertiesUpdate() {
        return AllowProperties.createDenyAllProperties();
    }


    /*************************************************************************************/
    /********************************    Action Rules    *********************************/
    /*************************************************************************************/




    /*************************************************************************************/
    /********************************    Otras funciones    ******************************/
    /*************************************************************************************/

    private String getNumeroRegistro(Centro centro, LocalDateTime ahora) {
        String anyoActual= String.valueOf(ahora.getYear());
        String codigoCentro = centro.getCode();
        long numeroRegistroSinAnyo = numeradorRepository.getSiguienteNumeroRegistroSalida(codigoCentro, anyoActual);
        String numeroRegistro = String.format("%05d", numeroRegistroSinAnyo) + "/" + anyoActual +  "-" + codigoCentro;

        return numeroRegistro;
    }

    private MetaFile firmarRegistroSalidaPorSecretario(DocumentoPdf documentoPdf, AlmacenClave almacenClave , String numeroRegistro, String csv) {
        CampoFirma campoFirma=new CampoFirma(getRectanguloFirma(documentoPdf)).setNumeroPagina(1).setFontSize(FONT_SIZE_FIRMA)
                .setMensaje("Nº Reg Salida:\n"+numeroRegistro).setImage(QrUtil.generarPng(getUrlDescarga(csv)), PosicionImagen.ARRIBA)
                .setMotivo("Firma del Registro de Salida Nº "+numeroRegistro);

        DocumentoPdf documentoPdfFirmado=documentoPdf.setMetadato(METADATO_CSV,csv).firmar(almacenClave,campoFirma);

        return MetaFileHelper.createMetaFile(documentoPdfFirmado);

    }

    /** Arriba a la derecha de la primera página. */
    private static Rectangulo getRectanguloFirma(DocumentoPdf documentoPdf) {
        Rectangulo pagina = documentoPdf.getTamanyoPagina(1);

        return new Rectangulo(pagina.x() + pagina.width() - WIDTH_FIRMA - MARGEN_FIRMA, pagina.y() + pagina.height() - HEIGHT_FIRMA - MARGEN_FIRMA, WIDTH_FIRMA, HEIGHT_FIRMA);
    }

    /** La URL del código QR: la de la descarga pública del registro de salida que tiene ese CSV. */
    private static String getUrlDescarga(String csv) {
        return AppSettings.get().getBaseURL() + "/ws" + RUTA_DESCARGA_PUBLICA + "/download?CSV=" + csv;
    }

    private static byte[] createZip(List<MetaFile> ficheros) {
        ByteArrayOutputStream zip = new ByteArrayOutputStream();
        try (ZipOutputStream zipOutputStream = new ZipOutputStream(zip)) {
            Set<String> nombres = new HashSet<>();
            for (MetaFile fichero : ficheros) {
                zipOutputStream.putNextEntry(new ZipEntry(getNombreUnico(TextUtil.sanitizeFileName(fichero.getFileName()), nombres)));
                zipOutputStream.write(MetaFileUtil.downloadContent(fichero));
                zipOutputStream.closeEntry();
            }
        } catch (IOException ex) {
            throw new UncheckedIOException(ex);
        }

        return zip.toByteArray();
    }

    /** Dos ficheros de un ZIP no pueden llamarse igual: al repetido se le antepone un número. */
    private static String getNombreUnico(String nombre, Set<String> nombres) {
        String nombreUnico = nombre;
        for (int i = 2; nombres.add(nombreUnico) == false; i++) {
            nombreUnico = i + "_" + nombre;
        }

        return nombreUnico;
    }


}
