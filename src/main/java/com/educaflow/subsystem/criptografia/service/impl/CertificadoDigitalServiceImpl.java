package com.educaflow.subsystem.criptografia.service.impl;

import com.axelor.db.Repository;
import com.axelor.db.modelservice.DefaultModelService;
import com.educaflow.base.infrastructure.criptografia.AlmacenClave;
import com.educaflow.base.infrastructure.criptografia.AlmacenClaveDispositivo;
import com.educaflow.base.infrastructure.criptografia.AlmacenClaveFichero;
import com.educaflow.base.infrastructure.criptografia.DispositivoCriptografico;
import com.educaflow.base.infrastructure.criptografia.EntornoCriptografico;
import com.axelor.db.modelservice.BusinessMessage;
import com.axelor.db.modelservice.BusinessMessages;
import com.educaflow.base.util.DniUtil;
import com.educaflow.base.util.MetaFileUtil;
import com.educaflow.subsystem.criptografia.db.CertificadoDigital;
import com.educaflow.subsystem.criptografia.db.TipoUbicacionCertificado;
import com.educaflow.subsystem.criptografia.db.repo.CertificadoDigitalRepository;
import com.educaflow.subsystem.criptografia.service.CertificadoDigitalService;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

public class CertificadoDigitalServiceImpl extends DefaultModelService<CertificadoDigital> implements CertificadoDigitalService {

    public CertificadoDigitalServiceImpl(Class<CertificadoDigital> model, Repository<CertificadoDigital> repository) {
        super(model, repository);
    }

    @Override
    public AlmacenClave getAlmacenClaveByDni(String dni) {
        validateGetAlmacenClaveByDni(dni).ifPresent(BusinessMessages::throwIfInvalid);
        CertificadoDigital certificado = ((CertificadoDigitalRepository) repository).findByDni(dni);

        if (certificado == null) {
            throw new RuntimeException("No existe certificado para el DNI: " + dni);
        }

        TipoUbicacionCertificado tipo = certificado.getTipoCertificado();

        return switch (tipo) {
            case FICHERO_BD -> {
                byte[] bytes = MetaFileUtil.downloadContent(certificado.getFichero());
                yield new AlmacenClaveFichero(new ByteArrayInputStream(bytes), certificado.getPassword());
            }
            case DISPOSITIVO_PKCS11 -> new AlmacenClaveDispositivo(certificado.getSlot(), certificado.getAlias());
            case CLASSPATH -> new AlmacenClaveFichero(
                    CertificadoDigitalServiceImpl.class.getClassLoader().getResourceAsStream(certificado.getRutaClasspath()),
                    certificado.getPassword()
            );
            case SISTEMA_ARCHIVOS -> {
                try {
                    yield new AlmacenClaveFichero(
                            Files.newInputStream(Path.of(certificado.getRutaSistemaArchivos())),
                            certificado.getPassword()
                    );
                } catch (IOException e) {
                    throw new RuntimeException("No se puede leer el certificado desde el sistema de archivos: " + certificado.getRutaSistemaArchivos(), e);
                }
            }
        };
    }

    /****************************************************************************************/
    /******************************** Métodos de Validación *********************************/
    /****************************************************************************************/

    @Override
    public Optional<BusinessMessages> validateGetAlmacenClaveByDni(String dni) {
        return Optional.empty();
    }

    @Override
    public Optional<BusinessMessages> validateInsert(CertificadoDigital certificado) {
        return validateCertificado(certificado);
    }

    @Override
    public Optional<BusinessMessages> validateUpdate(CertificadoDigital certificado, CertificadoDigital certificadoOriginal) {
        return validateCertificado(certificado);
    }

    /**************************************************************************************/
    /********************************    Otras funciones    *******************************/
    /**************************************************************************************/

    private Optional<BusinessMessages> validateCertificado(CertificadoDigital certificado) {
        BusinessMessages messages = new BusinessMessages();

        if (!DniUtil.isValid(certificado.getDni())) {
            messages.add(new BusinessMessage("dni", "El DNI no es válido"));
        }

        TipoUbicacionCertificado tipo = certificado.getTipoCertificado();
        if (tipo == null) {
            messages.add(new BusinessMessage("tipoCertificado", "El tipo de certificado es obligatorio"));
            return Optional.of(messages);
        }

        switch (tipo) {
            case FICHERO_BD -> {
                if (certificado.getFichero() == null) {
                    messages.add(new BusinessMessage("fichero", "El fichero es obligatorio para certificados de tipo Fichero en base de datos"));
                }
            }
            case DISPOSITIVO_PKCS11 -> {
                if (certificado.getSlot() == null) {
                    messages.add(new BusinessMessage("slot", "El slot es obligatorio para certificados de tipo Dispositivo PKCS#11"));
                }
                if (certificado.getAlias() == null || certificado.getAlias().isBlank()) {
                    messages.add(new BusinessMessage("alias", "El alias es obligatorio para certificados de tipo Dispositivo PKCS#11"));
                }

                if (certificado.getSlot() != null && certificado.getAlias() != null && !certificado.getAlias().isBlank()) {
                    try {
                        DispositivoCriptografico dispositivo = EntornoCriptografico.getDispositivoCriptografico(certificado.getSlot());
                        List<String> aliasesDisponibles = dispositivo.getAliases();
                        if (!aliasesDisponibles.contains(certificado.getAlias())) {
                            messages.add(new BusinessMessage("alias", "El alias '" + certificado.getAlias() + "' no existe en el slot " + certificado.getSlot() + ". Los alias disponibles son: " + String.join(", ", aliasesDisponibles)));
                        }
                    } catch (RuntimeException e) {
                        // Si el dispositivo no está configurado aún, no se puede validar el alias
                    }
                }
            }
            case CLASSPATH -> {
                if (certificado.getRutaClasspath() == null || certificado.getRutaClasspath().isBlank()) {
                    messages.add(new BusinessMessage("rutaClasspath", "La ruta classpath es obligatoria para certificados de tipo Classpath"));
                } else {
                    InputStream resourceStream = CertificadoDigitalServiceImpl.class.getClassLoader()
                            .getResourceAsStream(certificado.getRutaClasspath());
                    if (resourceStream == null) {
                        messages.add(new BusinessMessage("rutaClasspath", "No se encuentra el recurso en el classpath: " + certificado.getRutaClasspath()));
                    }
                }
            }
            case SISTEMA_ARCHIVOS -> {
                if (certificado.getRutaSistemaArchivos() == null || certificado.getRutaSistemaArchivos().isBlank()) {
                    messages.add(new BusinessMessage("rutaSistemaArchivos", "La ruta del sistema de archivos es obligatoria para certificados de tipo Sistema de archivos"));
                } else if (!Files.exists(Path.of(certificado.getRutaSistemaArchivos()))) {
                    messages.add(new BusinessMessage("rutaSistemaArchivos", "No existe el fichero en la ruta indicada: " + certificado.getRutaSistemaArchivos()));
                }
            }
        }

        return messages.isValid() ? Optional.empty() : Optional.of(messages);
    }

}
