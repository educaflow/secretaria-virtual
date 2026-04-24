package com.educaflow.subsystem.registrousuario.service.impl;

import com.axelor.auth.AuthService;
import com.axelor.auth.db.Group;
import com.axelor.auth.db.User;
import com.axelor.auth.db.repo.UserRepository;
import com.axelor.db.JpaRepository;
import com.axelor.db.Repository;
import com.axelor.db.modelservice.DefaultModelService;
import com.educaflow.base.infrastructure.mail.Mail;
import com.educaflow.base.infrastructure.validation.messages.BusinessException;
import com.educaflow.base.infrastructure.validation.messages.BusinessMessage;
import com.educaflow.base.infrastructure.validation.messages.BusinessMessages;
import com.educaflow.base.util.CodigoVerificacionUtil;
import com.educaflow.base.util.DniUtil;
import com.educaflow.base.util.TokenUtil;
import com.educaflow.subsystem.common.db.Centro;
import com.educaflow.subsystem.registrousuario.RegistroException;
import com.educaflow.subsystem.registrousuario.db.RegistroPendiente;
import com.educaflow.subsystem.registrousuario.db.UsuarioAutorizado;
import com.educaflow.subsystem.registrousuario.db.repo.RegistroPendienteRepository;
import com.educaflow.subsystem.registrousuario.db.repo.UsuarioAutorizadoRepository;
import com.educaflow.subsystem.registrousuario.service.DatosBasicosUsuario;
import com.educaflow.subsystem.registrousuario.service.RegistroPendienteService;
import com.educaflow.subsystem.security.db.CentroUsuario;
import com.educaflow.subsystem.security.db.CentroUsuarioTipoUsuario;
import com.educaflow.subsystem.security.db.TipoUsuario;
import com.google.inject.persist.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
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
        Optional<BusinessMessages> validation = validarEmailDni(registroPendiente);
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
        return registroPendiente;
    }

    /****************************************************************************************/
    /******************************** Métodos de Validación *********************************/
    /****************************************************************************************/

    private Optional<BusinessMessages> validarEmailDni(RegistroPendiente registroPendiente) {
        BusinessMessages businessMessages=new BusinessMessages();
        UsuarioAutorizadoRepository usuarioAutorizadoRepository = (UsuarioAutorizadoRepository) JpaRepository.of(UsuarioAutorizado.class);
        UserRepository userRepository = (UserRepository) JpaRepository.of(User.class);

        if (registroPendiente.getEmail().isBlank()) {
            businessMessages.add(new BusinessMessage(registroPendiente.getEmail(), "El email es obligatorio."));
        } else if (!registroPendiente.getEmail().matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")) {
            businessMessages.add(new BusinessMessage(registroPendiente.getEmail(), "El formato del email no es válido."));
        }
        if (registroPendiente.getDni().isBlank()) {
            businessMessages.add(new BusinessMessage(registroPendiente.getDni(), "El DNI/NIE es obligatorio."));
        }
        if (!DniUtil.isValid(registroPendiente.getDni())) {
            businessMessages.add(new BusinessMessage(registroPendiente.getDni(), "El número de DNI/NIE no es válido."));
        } else if (!usuarioAutorizadoRepository.isAuthorized(registroPendiente.getDni())) {
            businessMessages.add(new BusinessMessage(registroPendiente.getDni(), "El documento no está autorizado para registrarse. Contacte con secretaría."));
        }
        if (userRepository.findByEmail(registroPendiente.getEmail()) != null) {
            businessMessages.add(new BusinessMessage("email", "El email ya está registrado."));
        }

        return Optional.of(businessMessages);
    }

    @Override
    @Transactional
    public void validarCodigo(String codigo, String token) throws BusinessException{
        RegistroPendienteRepository registroPendienteRepository = (RegistroPendienteRepository) JpaRepository.of(RegistroPendiente.class);
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
        super.update(pendiente, null);
    }

    private void enviarCodigoPorEmail(String email, String codigo) {
        String asunto = "Código de verificación - Secretaría Virtual CIPFP Mislata";
        String htmlBody = "<p>Su código de verificación es:</p>"
                + "<h2 style='letter-spacing:4px;font-family:monospace'>" + codigo + "</h2>"
                + "<p>Válido durante " + EXPIRACION_MINUTOS + " minutos.</p>";
        String textBody = "Su código de verificación es: " + codigo
                + "\nVálido durante " + EXPIRACION_MINUTOS + " minutos.";

        logger.info("[REGISTRO] Código de verificación para {}: {}", email, codigo);

        /*Mail mail = new Mail(
                List.of(email),
                "secretaria@mislata.es",
                asunto,
                htmlBody,
                textBody,
                List.of()
        );
        try {
            mailSender.send(mail);
        } catch (Exception e) {
            logger.warn("[REGISTRO] No se pudo enviar el email a {}: {}", email, e.getMessage());
        }*/
    }
}