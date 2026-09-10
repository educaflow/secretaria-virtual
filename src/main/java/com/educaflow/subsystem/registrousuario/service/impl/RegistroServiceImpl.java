package com.educaflow.subsystem.registrousuario.service.impl;

import com.axelor.auth.AuthService;
import com.axelor.auth.db.Group;
import com.axelor.auth.db.User;
import com.axelor.auth.db.repo.GroupRepository;
import com.axelor.db.JpaRepository;
import com.axelor.db.Repository;
import com.axelor.db.modelservice.BusinessMessages;
import com.axelor.db.modelservice.DefaultModelService;
import com.educaflow.base.infrastructure.validation.messages.BusinessException;
import com.educaflow.subsystem.registrousuario.RegistroException;
import com.educaflow.subsystem.registrousuario.db.RegistroPendiente;
import com.educaflow.subsystem.registrousuario.db.repo.RegistroPendienteRepository;
import com.educaflow.subsystem.registrousuario.service.DatosBasicosUsuario;
import com.educaflow.subsystem.registrousuario.service.RegistroService;
import com.google.inject.persist.Transactional;

import java.util.List;
import java.util.Optional;

public class RegistroServiceImpl extends DefaultModelService<User> implements RegistroService {

    private static final String GRUPO_USUARIOS = "users";

    public RegistroServiceImpl(Class<User> model, Repository repository) {
        super(model, repository);
    }

    @Override
    @Transactional
    public User registrarUsuario(DatosBasicosUsuario datos, String token) throws BusinessException {
        Optional<BusinessMessages> validation = validateRegistrarUsuario(datos, token);
        if (validation.isPresent()) {
            throw new IllegalArgumentException(validation.get().toString());
        }
        /*GroupRepository groupRepository = (GroupRepository) JpaRepository.of(Group.class);
        RegistroPendienteRepository registroPendienteRepository = (RegistroPendienteRepository) JpaRepository.of(RegistroPendiente.class);

        RegistroPendiente pendiente = registroPendienteRepository.findByToken(token)
                .orElseThrow(() -> new BusinessException("Token no válido"));

        if (!Boolean.TRUE.equals(pendiente.getVerificado())) {
            throw new RegistroException("El email no ha sido verificado.");
        }

        String dni = pendiente.getDni();
        String email = pendiente.getEmail();

        Group group = groupRepository.findByCode(GRUPO_USUARIOS);
        if (group == null) {
            throw new BusinessException("Configuración incorrecta: no existe el grupo '" + GRUPO_USUARIOS + "'.");
        }

        User user = new User();
        user.setCode(email);
        user.setName(datos.nombre().trim() + " " + datos.apellidos().trim());
        user.setEmail(email);
        user.setPassword(AuthService.getInstance().encrypt(datos.password()));
        user.setNombre(datos.nombre().trim());
        user.setApellidos(datos.apellidos().trim());
        user.setDni(dni);
        user.setGroup(group);
        user.setLanguage(List.of("es", "ca").contains(datos.idioma()) ? datos.idioma() : "es");
        super.insert(user);

        crearCentroUsuarios(user, dni);

        registroPendienteRepository.remove(pendiente);

        return user;*/
        return null;
    }

    /**
     * Devuelve el email del User cuyo dni coincide con el indicado, si existe y tiene email no vacío.
     * La consulta inline está justificada porque {@link User} pertenece a Axelor y no hay un
     * repositorio personalizado del proyecto editable para esta entidad.
     */
    public Optional<BusinessMessages> validateRegistrarUsuario(DatosBasicosUsuario datos, String token) {
        return Optional.empty();
    }

    public Optional<BusinessMessages> validateFindEmailByDni(String dni) {
        return Optional.empty();
    }

    @Override
    public Optional<String> findEmailByDni(String dni) {
        Optional<BusinessMessages> validation = validateFindEmailByDni(dni);
        if (validation.isPresent()) {
            throw new IllegalArgumentException(validation.get().toString());
        }
        if (dni == null || dni.isBlank()) {
            return Optional.empty();
        }
        User user = JpaRepository.of(User.class).all()
                .filter("self.dni = :dni")
                .bind("dni", dni)
                .fetchOne();
        if (user == null) {
            return Optional.empty();
        }
        String email = user.getEmail();
        if (email == null || email.isBlank()) {
            return Optional.empty();
        }
        return Optional.of(email);
    }

}
