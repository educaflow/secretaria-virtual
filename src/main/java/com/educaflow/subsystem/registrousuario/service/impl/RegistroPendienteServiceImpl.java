package com.educaflow.subsystem.registrousuario.service.impl;

import com.axelor.db.Repository;
import com.axelor.db.modelservice.BusinessMessages;
import com.axelor.db.modelservice.DefaultModelService;
import com.educaflow.base.infrastructure.validation.messages.BusinessException;

import com.educaflow.subsystem.registrousuario.db.RegistroPendiente;
import com.educaflow.subsystem.registrousuario.service.RegistroPendienteService;
import com.google.inject.persist.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

public class RegistroPendienteServiceImpl extends DefaultModelService<RegistroPendiente> implements RegistroPendienteService {

    private static final int EXPIRACION_MINUTOS = 30;

    private final Logger logger = LoggerFactory.getLogger(RegistroPendienteServiceImpl.class);

    public RegistroPendienteServiceImpl(Class<RegistroPendiente> model, Repository repository) {
        super(model, repository);
    }

    @Override
    @Transactional
    public RegistroPendiente insertar(RegistroPendiente registroPendiente) throws BusinessException {
        Optional<BusinessMessages> validation = validateInsertar(registroPendiente);
        if (validation.isPresent()) {
            throw new IllegalArgumentException(validation.get().toString());
        }
        /*Optional<BusinessMessages> validation = validarEmailDni(registroPendiente);
        if (validation.isPresent()) {
            throw new BusinessException(validation.get());
        }
        String codigo = CodigoVerificacionUtil.generar();
        String token = TokenUtil.generar();
        RegistroPendiente pendiente = new RegistroPendiente();
        pendiente.setEmail(registroPendiente.getEmail());
        pendiente.setDni(registroPendiente.getDni());
        pendiente.setCodigo(codigo);
        pendiente.setToken(token);
        super.insert(pendiente);
        enviarCodigoPorEmail(registroPendiente.getEmail(), codigo);
        return registroPendiente;*/
        return null;
    }

    /****************************************************************************************/
    /******************************** Métodos de Validación *********************************/
    /****************************************************************************************/

    public Optional<BusinessMessages> validateInsertar(RegistroPendiente registroPendiente) {
        return Optional.empty();
    }

    @Override
    @Transactional
    public void validarCodigo(String codigo, String token) throws BusinessException{
        /*RegistroPendienteRepository registroPendienteRepository = (RegistroPendienteRepository) JpaRepository.of(RegistroPendiente.class);
        if (codigo.isBlank() || token.isBlank()) {
            throw new BusinessException(new BusinessMessage("Token y código son obligatorios."));
        }
        RegistroPendiente pendiente = registroPendienteRepository.findByToken(token)
                .orElseThrow(() -> new BusinessException(new BusinessMessage("Registro pendiente no encontrado. Inicie el proceso de nuevo.")));

        if (pendiente.getCreatedOn().plusMinutes(EXPIRACION_MINUTOS).isBefore(LocalDateTime.now())) {
            throw new BusinessException(new BusinessMessage("El código ha expirado. Inicie el proceso de nuevo."));
        }
        if (!pendiente.getCodigo().equalsIgnoreCase(codigo.trim())) {
            throw new BusinessException(new BusinessMessage("Código incorrecto."));
        }
        pendiente.setVerificado(true);
        super.update(pendiente, null);*/
    }
}