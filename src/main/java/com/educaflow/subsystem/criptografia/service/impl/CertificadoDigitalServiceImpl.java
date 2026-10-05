package com.educaflow.subsystem.criptografia.service.impl;

import com.axelor.auth.db.User;
import com.axelor.auth.db.repo.UserRepository;
import com.axelor.db.JPA;
import com.axelor.db.Repository;
import com.axelor.db.modelservice.AllowProperties;
import com.axelor.db.modelservice.DefaultModelService;
import com.educaflow.base.infrastructure.async.EjecutorAsincrono;
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
import com.educaflow.base.util.TextUtil;
import com.educaflow.subsystem.criptografia.db.CertificadoDigital;
import com.educaflow.subsystem.criptografia.db.TipoUbicacionCertificado;
import com.educaflow.subsystem.criptografia.db.repo.CertificadoDigitalRepository;
import com.educaflow.subsystem.criptografia.service.CertificadoDigitalService;
import com.educaflow.subsystem.criptografia.service.DatosTitular;
import com.educaflow.subsystem.criptografia.service.SituacionFirma;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public class CertificadoDigitalServiceImpl extends DefaultModelService<CertificadoDigital> implements CertificadoDigitalService {

    private static final Logger log = LoggerFactory.getLogger(CertificadoDigitalServiceImpl.class);

    /**
     * Repositorio de la entidad {@code User} (que NO es la que gestiona este servicio), para resolver el usuario
     * titular a partir de su DNI. Es un repositorio, no un {@code ModelService}, así que se inyecta como campo.
     */
    @Inject
    private UserRepository userRepository;

    @Inject
    EjecutorAsincrono ejecutorAsincrono;

    public CertificadoDigitalServiceImpl(Class<CertificadoDigital> model, Repository<CertificadoDigital> repository) {
        super(model, repository);
    }

    @Override
    public CertificadoDigital insert(CertificadoDigital certificado) {
        validateInsert(certificado).ifPresent(BusinessMessages::throwIfInvalid);

        fireActionRule_AsignarTitular(certificado);
        fireActionRule_AsignarNuevaContrasena(certificado);

        return repository.save(certificado);
    }

    @Override
    public CertificadoDigital update(CertificadoDigital certificado, CertificadoDigital certificadoOriginal) {
        validateUpdate(certificado, certificadoOriginal).ifPresent(BusinessMessages::throwIfInvalid);

        fireActionRule_ConservarDni(certificado, certificadoOriginal);
        fireActionRule_ConservarDatosTitularDelUsuario(certificado, certificadoOriginal);
        fireActionRule_AsignarNuevaContrasena(certificado);

        MetaFile ficheroOriginal = (certificadoOriginal.getTipoCertificado() == TipoUbicacionCertificado.FICHERO_BD) ? certificadoOriginal.getFichero() : null;

        CertificadoDigital certificadoGuardado = repository.save(certificado);

        // Solo se borra si el certificado ya no lo referencia: si sigue apuntándolo, borrarlo rompería la clave ajena.
        MetaFile ficheroNuevo = certificadoGuardado.getFichero();
        if (ficheroOriginal != null && (ficheroNuevo == null || !Objects.equals(ficheroOriginal.getId(), ficheroNuevo.getId()))) {
            fireActionRule_BorrarFicheroTrasCommit(ficheroOriginal);
        }

        return certificadoGuardado;
    }

    @Override
    public void remove(CertificadoDigital certificado) {
        validateRemove(certificado).ifPresent(BusinessMessages::throwIfInvalid);

        MetaFile fichero = (certificado.getTipoCertificado() == TipoUbicacionCertificado.FICHERO_BD) ? certificado.getFichero() : null;

        repository.remove(certificado);

        if (fichero != null) {
            fireActionRule_BorrarFicheroTrasCommit(fichero);
        }
    }

    @Override
    public Optional<AlmacenClave> getAlmacenClaveByDni(String dni) {
        return getAlmacenClaveByDni(dni, null);
    }

    @Override
    public Optional<AlmacenClave> getAlmacenClaveByDni(String dni, String claveAcceso) {
        validateGetAlmacenClaveByDni(dni, claveAcceso).ifPresent(BusinessMessages::throwIfInvalid);
        Optional<CertificadoDigital> certificadoHabilitado = getCertificadoHabilitado(dni);

        if (certificadoHabilitado.isEmpty()) {
            return Optional.empty();
        }
        CertificadoDigital certificado = certificadoHabilitado.get();

        TipoUbicacionCertificado tipo = certificado.getTipoCertificado();

        AlmacenClave almacenClave = switch (tipo) {
            case FICHERO_BD, CLASSPATH, SISTEMA_ARCHIVOS -> {
                String passwordGuardada = certificado.getPassword();
                String clave = (passwordGuardada == null || passwordGuardada.isBlank()) ? claveAcceso : passwordGuardada;
                yield new AlmacenClaveFichero(getInputStreamCertificado(certificado), clave);
            }
            case DISPOSITIVO_PKCS11 -> new AlmacenClaveDispositivo(certificado.getDispositivoCriptografico().getSlot(), certificado.getAlias().getName());
        };

        return Optional.of(almacenClave);
    }

    @Override
    public SituacionFirma getSituacionFirmaByDni(String dni) {
        validateGetSituacionFirmaByDni(dni).ifPresent(BusinessMessages::throwIfInvalid);

        if (DniUtil.isValid(dni) == false) {
            return SituacionFirma.SIN_DNI;
        }

        Optional<CertificadoDigital> certificadoHabilitado = getCertificadoHabilitado(dni);

        if (certificadoHabilitado.isEmpty()) {
            return SituacionFirma.SIN_CERTIFICADO;
        }
        CertificadoDigital certificado = certificadoHabilitado.get();

        TipoUbicacionCertificado tipo = certificado.getTipoCertificado();

        return switch (tipo) {
            case DISPOSITIVO_PKCS11 -> {
                String pin = certificado.getDispositivoCriptografico().getPin();
                yield (pin == null || pin.isBlank()) ? SituacionFirma.DISPOSITIVO_SIN_PIN : SituacionFirma.DISPOSITIVO_CON_PIN;
            }
            case FICHERO_BD, CLASSPATH, SISTEMA_ARCHIVOS -> {
                String password = certificado.getPassword();
                yield (password == null || password.isBlank()) ? SituacionFirma.FICHERO_SIN_CLAVE : SituacionFirma.FICHERO_CON_CLAVE;
            }
        };
    }

    @Override
    public boolean isClaveCertificadoCorrecta(String dni, String clave) {
        validateIsClaveCertificadoCorrecta(dni, clave).ifPresent(BusinessMessages::throwIfInvalid);

        // Los llamantes comprueban antes que hay firma en servidor: sin certificado es un error de programación.
        AlmacenClave almacenClave = getAlmacenClaveByDni(dni, clave)
                .orElseThrow(() -> new IllegalStateException("El usuario con dni=" + DniUtil.enmascarar(dni) + " no tiene certificado digital con el que comprobar la clave"));

        if (almacenClave instanceof AlmacenClaveFichero almacenClaveFichero) {
            return almacenClaveFichero.isPasswordValid() == true;
        }

        return true;
    }

    @Override
    public DatosTitular getDatosTitularByDni(String dni) {
        validateGetDatosTitularByDni(dni).ifPresent(BusinessMessages::throwIfInvalid);

        return resolverDatosTitular(dni);
    }

    /**************************************************************************************/
    /******************************* Métodos de Validación ********************************/
    /**************************************************************************************/

    @Override
    public Optional<BusinessMessages> validateInsert(CertificadoDigital certificado) {
        BusinessMessages messages = new BusinessMessages();

        validateCertificado(certificado, messages);

        // En el alta el DNI del bean entrante ES el bueno (campo `cliente` de esta acción).
        validateUnicoCertificadoHabilitadoPorDni(certificado.getDni(), certificado, messages);

        // Si hay usuario con ese DNI, el nombre y los apellidos los pone el servidor en
        // fireActionRule_AsignarTitular y la validación no aplica.
        if (findUsuarioTitular(certificado.getDni()).isEmpty()) {
            validateNombreYApellidosIndicados(certificado, messages);
        }

        return messages.isValid() ? Optional.empty() : Optional.of(messages);
    }

    @Override
    public Optional<BusinessMessages> validateUpdate(CertificadoDigital certificado, CertificadoDigital certificadoOriginal) {
        BusinessMessages messages = new BusinessMessages();

        validateCertificado(certificado, messages);

        // CRITICAL: se consulta con el DNI del ORIGINAL, nunca con el del bean entrante.
        // El DNI es inmutable y las validaciones corren ANTES de la action rule que lo
        // restaura, así que usar aquí el del cliente dejaría la unicidad a merced del endpoint REST genérico.
        validateUnicoCertificadoHabilitadoPorDni(certificadoOriginal.getDni(), certificado, messages);

        // Se mira el flag del ORIGINAL: es un campo `servidor` inmutable y el cliente no puede dictarlo.
        if (certificadoOriginal.getNombreTomadoDelUsuario() == false) {
            validateNombreYApellidosIndicados(certificado, messages);
        }

        return messages.isValid() ? Optional.empty() : Optional.of(messages);
    }

    private void validateCertificado(CertificadoDigital certificado, BusinessMessages messages) {
        if (!DniUtil.isValid(certificado.getDni())) {
            messages.add(new BusinessMessage("dni", "El DNI no es válido"));
        }

        TipoUbicacionCertificado tipo = certificado.getTipoCertificado();
        if (tipo == null) {
            messages.add(new BusinessMessage("tipoCertificado", "El tipo de certificado es obligatorio"));
            return;
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
                            log.warn("No se pudo comprobar el alias id={} en el dispositivo id={}: {}",
                                    certificado.getAlias().getId(), certificado.getDispositivoCriptografico().getId(), e.getMessage());
                        }
                    }
                }
            }
            case CLASSPATH -> {
                if (certificado.getRutaClasspath() == null || certificado.getRutaClasspath().isBlank()) {
                    messages.add(new BusinessMessage("rutaClasspath", "La ruta classpath es obligatoria para certificados de tipo Classpath"));
                } else {
                    if (CertificadoDigitalServiceImpl.class.getClassLoader().getResource(certificado.getRutaClasspath()) == null) {
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
    }

    /**
     * El DNI llega como parámetro explícito, separado del bean, porque no siempre es el del bean: el alta pasa el
     * del propio certificado y la modificación el del original (el DNI es inmutable y el del bean entrante no es de
     * fiar).
     */
    private void validateUnicoCertificadoHabilitadoPorDni(String dni, CertificadoDigital certificado, BusinessMessages messages) {
        if (certificado.getEnabled() == false) {
            return;
        }

        List<CertificadoDigital> habilitados = ((CertificadoDigitalRepository) repository).findByDniHabilitados(dni).fetch();

        boolean existeOtroHabilitado = habilitados.stream()
                .anyMatch(habilitado -> !Objects.equals(habilitado.getId(), certificado.getId()));

        if (existeOtroHabilitado) {
            messages.add(new BusinessMessage("enabled", "Ya existe un certificado digital habilitado para el DNI " + dni));
        }
    }

    // Los dos se evalúan siempre para que el administrador vea de una vez los dos que le faltan.
    private void validateNombreYApellidosIndicados(CertificadoDigital certificado, BusinessMessages messages) {
        if (certificado.getNombre() == null || certificado.getNombre().isBlank()) {
            messages.add(new BusinessMessage("nombre", "El nombre es obligatorio"));
        }
        if (certificado.getApellidos() == null || certificado.getApellidos().isBlank()) {
            messages.add(new BusinessMessage("apellidos", "Los apellidos son obligatorios"));
        }
    }

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

    /**
     * La acción no tiene precondiciones de negocio: un DNI nulo, en blanco o inválido no es un error sino una
     * situación de firma más ({@code SIN_DNI}), que decide el propio {@code getSituacionFirmaByDni}.
     */
    @Override
    public Optional<BusinessMessages> validateGetSituacionFirmaByDni(String dni) {
        return Optional.empty();
    }

    @Override
    public Optional<BusinessMessages> validateIsClaveCertificadoCorrecta(String dni, String clave) {
        return validateGetAlmacenClaveByDni(dni, clave);
    }

    /**
     * La acción no tiene precondiciones de negocio: es una consulta de solo lectura que debe aceptar cualquier
     * DNI —nulo, en blanco, incompleto o con letra incorrecta— porque su cometido es reflejar en el formulario
     * si ese DNI corresponde o no a un usuario mientras el administrador lo teclea. La validez del DNI ya se
     * comprueba al guardar.
     */
    @Override
    public Optional<BusinessMessages> validateGetDatosTitularByDni(String dni) {
        return Optional.empty();
    }

    /************************************************************************************/
    /********************************* AllowProperties **********************************/
    /************************************************************************************/

    @Override
    public AllowProperties allowPropertiesInsert() {
        return AllowProperties.createAllowProperties(Map.ofEntries(
                Map.entry("dni", Map.of()),
                Map.entry("tipoCertificado", Map.of()),
                Map.entry("fichero", Map.of()),
                Map.entry("nuevaContrasena", Map.of()),
                Map.entry("dispositivoCriptografico", Map.of()),
                Map.entry("alias", Map.of()),
                Map.entry("rutaClasspath", Map.of()),
                Map.entry("rutaSistemaArchivos", Map.of()),
                Map.entry("enabled", Map.of()),
                Map.entry("nombre", Map.of()),
                Map.entry("apellidos", Map.of())
        ));
    }

    @Override
    public AllowProperties allowPropertiesUpdate() {
        return AllowProperties.createAllowProperties(Map.of(
                "tipoCertificado", Map.of(),
                "fichero", Map.of(),
                "nuevaContrasena", Map.of(),
                "dispositivoCriptografico", Map.of(),
                "alias", Map.of(),
                "rutaClasspath", Map.of(),
                "rutaSistemaArchivos", Map.of(),
                "enabled", Map.of(),
                "nombre", Map.of(),
                "apellidos", Map.of()
        ));
    }

    /***********************************************************************************/
    /********************************** Action Rules ***********************************/
    /***********************************************************************************/

    private void fireActionRule_AsignarTitular(CertificadoDigital certificado) {
        DatosTitular datos = resolverDatosTitular(certificado.getDni());

        if (datos.tomadoDelUsuario()) {
            certificado.setNombre(datos.nombre());
            certificado.setApellidos(datos.apellidos());
        }

        certificado.setNombreTomadoDelUsuario(datos.tomadoDelUsuario());
    }

    private void fireActionRule_ConservarDni(CertificadoDigital certificado, CertificadoDigital certificadoOriginal) {
        certificado.setDni(certificadoOriginal.getDni());
    }

    private void fireActionRule_ConservarDatosTitularDelUsuario(CertificadoDigital certificado, CertificadoDigital certificadoOriginal) {
        certificado.setNombreTomadoDelUsuario(certificadoOriginal.getNombreTomadoDelUsuario());

        if (certificadoOriginal.getNombreTomadoDelUsuario()) {
            certificado.setNombre(certificadoOriginal.getNombre());
            certificado.setApellidos(certificadoOriginal.getApellidos());
        }
    }

    private void fireActionRule_AsignarNuevaContrasena(CertificadoDigital certificado) {
        if (TextUtil.isNullOrBlank(certificado.getNuevaContrasena()) == false) {
            certificado.setPassword(certificado.getNuevaContrasena());
        }

        certificado.setNuevaContrasena(null);
    }

    private void fireActionRule_BorrarFicheroTrasCommit(MetaFile fichero) {
        // Tras el commit: MetaFiles.delete borra el fichero físico en el acto y, si la transacción hiciera
        // rollback después, el certificado seguiría apuntando a un MetaFile sin fichero. La tarea corre en
        // otro hilo, fuera de la transacción de la petición, así que abre la suya.
        ejecutorAsincrono.ejecutarTrasCommit(() -> JPA.runInTransaction(() -> MetaFileUtil.delete(fichero)));
    }

    /***********************************************************************************/
    /********************************* Otras funciones *********************************/
    /***********************************************************************************/

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

    /**
     * Cálculo único del titular de un DNI, compartido por {@code fireActionRule_AsignarTitular} (al guardar) y por
     * la acción de pantalla {@code getDatosTitularByDni} (al teclear el DNI), para que la pantalla no pueda
     * prometer un titular distinto del que el servidor va a persistir.
     */
    private DatosTitular resolverDatosTitular(String dni) {
        return findUsuarioTitular(dni)
                .map(titular -> new DatosTitular(titular.getNombre(), titular.getApellidos(), true))
                .orElseGet(DatosTitular::sinUsuario);
    }

    /**
     * Usuario de la aplicación cuyo documento coincide con el DNI recibido, o vacío si no hay ninguno o el
     * DNI es nulo o está en blanco (en cuyo caso ni siquiera se consulta).
     */
    private Optional<User> findUsuarioTitular(String dni) {
        if ((dni == null) || dni.isBlank()) {
            return Optional.empty();
        }

        return Optional.ofNullable(userRepository.findByDni(dni));
    }

    /**
     * Certificado habilitado de un DNI, o vacío si no hay ninguno. Es el punto único por el que la firma en
     * servidor obtiene el certificado de una persona: si no hay ninguno habilitado, se comporta como si la persona
     * no tuviera certificado.
     */
    private Optional<CertificadoDigital> getCertificadoHabilitado(String dni) {
        return Optional.ofNullable(((CertificadoDigitalRepository) repository).findByDniHabilitados(dni).fetchOne());
    }

}
