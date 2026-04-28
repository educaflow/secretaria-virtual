package com.educaflow.subsystem.registrousuario.service.impl;

import com.axelor.auth.AuthService;
import com.axelor.auth.db.Group;
import com.axelor.auth.db.User;
import com.axelor.auth.db.repo.GroupRepository;
import com.axelor.auth.db.repo.UserRepository;
import com.axelor.db.JpaRepository;
import com.axelor.db.Repository;
import com.axelor.db.modelservice.DefaultModelService;
import com.educaflow.base.infrastructure.validation.messages.BusinessException;
import com.educaflow.subsystem.common.db.Centro;
import com.educaflow.subsystem.registrousuario.RegistroException;
import com.educaflow.subsystem.registrousuario.db.RegistroPendiente;
import com.educaflow.subsystem.registrousuario.db.repo.RegistroPendienteRepository;
import com.educaflow.subsystem.registrousuario.service.DatosBasicosUsuario;
import com.educaflow.subsystem.registrousuario.service.RegistroService;
import com.educaflow.subsystem.common.db.CentroUsuario;
import com.educaflow.subsystem.common.db.CentroUsuarioTipoUsuario;
import com.educaflow.subsystem.common.db.TipoUsuario;
import com.google.inject.persist.Transactional;

import java.util.List;
import java.util.Map;

public class RegistroServiceImpl extends DefaultModelService<User> implements RegistroService {

    private static final String GRUPO_USUARIOS = "users";

    public RegistroServiceImpl(Class<User> model, Repository repository) {
        super(model, repository);
    }

    @Override
    @Transactional
    public User registrarUsuario(DatosBasicosUsuario datos, String token) throws BusinessException {
        GroupRepository groupRepository = (GroupRepository) JpaRepository.of(Group.class);
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

        return user;
    }

    private void crearCentroUsuarios(User user, String dni) {
        RegistroPendienteRepository registroPendienteRepository = (RegistroPendienteRepository) JpaRepository.of(RegistroPendiente.class);
        //UserRepository userRepository = (UserRepository) JpaRepository.of(User.class);

        Map<Centro, List<TipoUsuario>> tiposPorCentro = registroPendienteRepository.findTiposUsuarioByDni(dni);

        Centro primerCentro = null;

        for (Map.Entry<Centro, List<TipoUsuario>> entry : tiposPorCentro.entrySet()) {
            Centro centro = entry.getKey();
            if (primerCentro == null) primerCentro = centro;

            CentroUsuario cu = new CentroUsuario();
            cu.setCentro(centro);
            cu.setUsuario(user);
            JpaRepository.of(CentroUsuario.class).save(cu);

            for (TipoUsuario tipoUsuario : entry.getValue()) {
                CentroUsuarioTipoUsuario cutu = new CentroUsuarioTipoUsuario();
                cutu.setCentroUsuario(cu);
                cutu.setTipoUsuario(tipoUsuario);
                JpaRepository.of(CentroUsuarioTipoUsuario.class).save(cutu);
            }
        }

        if (primerCentro != null) {
            user.setCentroActivo(primerCentro);
            super.update(user, null);
        }
    }

}
