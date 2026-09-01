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
import com.axelor.meta.db.MetaFile;
import com.educaflow.base.util.DniUtil;
import com.educaflow.base.util.MetaFileUtil;
import com.educaflow.subsystem.criptografia.db.CertificadoDigital;
import com.educaflow.subsystem.criptografia.db.TipoUbicacionCertificado;
import com.educaflow.subsystem.criptografia.db.repo.CertificadoDigitalRepository;
import com.educaflow.subsystem.criptografia.service.CertificadoDigitalService;
import com.educaflow.subsystem.criptografia.service.TipoAlmacenClave;

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
        return getAlmacenClaveByDni(dni, null);
    }

    @Override
    public AlmacenClave getAlmacenClaveByDni(String dni, String claveAcceso) {
        validateGetAlmacenClaveByDni(dni, claveAcceso).ifPresent(BusinessMessages::throwIfInvalid);
        CertificadoDigital certificado = ((CertificadoDigitalRepository) repository).findByDni(dni);

        if ((certificado == null) || (certificado.getEnabled() == false)) {
            throw new RuntimeException("No existe certificado para el DNI: " + dni);
        }

        TipoUbicacionCertificado tipo = certificado.getTipoCertificado();

        return switch (tipo) {
            case FICHERO_BD, CLASSPATH, SISTEMA_ARCHIVOS -> {
                String passwordGuardada = certificado.getPassword();
                String clave = (passwordGuardada == null || passwordGuardada.isBlank()) ? claveAcceso : passwordGuardada;
                yield new AlmacenClaveFichero(getInputStreamCertificado(certificado), clave);
            }
            case DISPOSITIVO_PKCS11 -> new AlmacenClaveDispositivo(certificado.getDispositivoCriptografico().getSlot(), certificado.getAlias().getName());
        };
    }

    @Override
    public void remove(CertificadoDigital certificado) {
        validateRemove(certificado).ifPresent(BusinessMessages::throwIfInvalid);

        MetaFile fichero = (certificado.getTipoCertificado() == TipoUbicacionCertificado.FICHERO_BD) ? certificado.getFichero() : null;

        repository.remove(certificado);

        if (fichero != null) {
            MetaFileUtil.delete(fichero);
        }
    }


    @Override
    public TipoAlmacenClave getTipoAlmacenClaveByDni(String dni) {
        validateGetTipoAlmacenClaveByDni(dni).ifPresent(BusinessMessages::throwIfInvalid);
        CertificadoDigital certificado = ((CertificadoDigitalRepository) repository).findByDni(dni);

        if ((certificado == null) || (certificado.getEnabled() == false)) {
            return null;
        }

        TipoUbicacionCertificado tipo = certificado.getTipoCertificado();

        return switch (tipo) {
            case DISPOSITIVO_PKCS11 -> {
                String pin = certificado.getDispositivoCriptografico().getPin();
                yield (pin == null || pin.isBlank()) ? TipoAlmacenClave.DISPOSITIVO_SIN_PIN : TipoAlmacenClave.DISPOSITIVO_CON_PIN;
            }
            case FICHERO_BD, CLASSPATH, SISTEMA_ARCHIVOS -> {
                String password = certificado.getPassword();
                yield (password == null || password.isBlank()) ? TipoAlmacenClave.FICHERO_SIN_CLAVE : TipoAlmacenClave.FICHERO_CON_CLAVE;
            }
        };
    }


    /****************************************************************************************/
    /******************************** Métodos de Validación *********************************/
    /****************************************************************************************/

    @Override
    public Optional<BusinessMessages> validateGetAlmacenClaveByDni(String dni) {
        BusinessMessages messages = new BusinessMessages();

        if (!DniUtil.isValid(dni)) {
            messages.add(new BusinessMessage("dni", "El DNI no es válido"));
        }

        return messages.isValid() ? Optional.empty() : Optional.of(messages);
    }

    @Override
    public Optional<BusinessMessages> validateGetAlmacenClaveByDni(String dni, String claveAcceso) {
        return validateGetAlmacenClaveByDni(dni);
    }

    @Override
    public Optional<BusinessMessages> validateGetTipoAlmacenClaveByDni(String dni) {
        BusinessMessages messages = new BusinessMessages();

        if (!DniUtil.isValid(dni)) {
            messages.add(new BusinessMessage("dni", "El DNI no es válido"));
        }

        return messages.isValid() ? Optional.empty() : Optional.of(messages);
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

    private InputStream getInputStreamCertificado(CertificadoDigital certificado) {
        return switch (certificado.getTipoCertificado()) {
            case FICHERO_BD -> new ByteArrayInputStream(MetaFileUtil.downloadContent(certificado.getFichero()));
            case CLASSPATH -> CertificadoDigitalServiceImpl.class.getClassLoader().getResourceAsStream(certificado.getRutaClasspath());
            case SISTEMA_ARCHIVOS -> {
                try {
                    yield Files.newInputStream(Path.of(certificado.getRutaSistemaArchivos()));
                } catch (IOException e) {
                    throw new RuntimeException("No se puede leer el certificado desde el sistema de archivos: " + certificado.getRutaSistemaArchivos(), e);
                }
            }
            case DISPOSITIVO_PKCS11 -> throw new RuntimeException("Un certificado en dispositivo PKCS#11 no tiene fichero de certificado");
        };
    }

    private Optional<BusinessMessages> validateCertificado(CertificadoDigital certificado) {
        BusinessMessages messages = new BusinessMessages();

        if (!DniUtil.isValid(certificado.getDni())) {
            messages.add(new BusinessMessage("dni", "El DNI no es válido"));
        } else {
            CertificadoDigital existente = ((CertificadoDigitalRepository) repository).findByDni(certificado.getDni());
            if (existente != null && !existente.getId().equals(certificado.getId())) {
                messages.add(new BusinessMessage("dni", "Ya existe un certificado digital con el DNI '" + certificado.getDni() + "'"));
            }
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
                if (certificado.getDispositivoCriptografico() == null) {
                    messages.add(new BusinessMessage("dispositivoCriptografico", "El dispositivo criptográfico es obligatorio para certificados de tipo Dispositivo PKCS#11"));
                }
                if (certificado.getAlias() == null) {
                    messages.add(new BusinessMessage("alias", "El alias es obligatorio para certificados de tipo Dispositivo PKCS#11"));
                }

                if (certificado.getDispositivoCriptografico() != null && certificado.getAlias() != null) {
                    if (!certificado.getAlias().getDispositivoCriptografico().getId().equals(certificado.getDispositivoCriptografico().getId())) {
                        messages.add(new BusinessMessage("alias", "El alias seleccionado no pertenece al dispositivo criptográfico '" + certificado.getDispositivoCriptografico().getName() + "'"));
                    } else {
                        try {
                            DispositivoCriptografico dispositivo = EntornoCriptografico.getDispositivoCriptografico(certificado.getDispositivoCriptografico().getSlot());
                            List<String> aliasesDisponibles = dispositivo.getAliases();
                            if (!aliasesDisponibles.contains(certificado.getAlias().getName())) {
                                messages.add(new BusinessMessage("alias", "El alias '" + certificado.getAlias().getName() + "' no existe en el dispositivo '" + certificado.getDispositivoCriptografico().getName() + "'. Los alias disponibles son: " + String.join(", ", aliasesDisponibles)));
                            }
                        } catch (RuntimeException e) {
                            // Si el dispositivo no está configurado aún, no se puede validar el alias
                        }
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
