package com.educaflow.subsystem.security;

import com.axelor.auth.EduFlowAuthResolver;
import com.axelor.auth.db.Permission;
import com.axelor.auth.db.User;
import com.axelor.db.JPA;
import com.axelor.db.JpaSecurity.AccessType;
import com.educaflow.subsystem.expedientes.db.Expediente;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

public class EducaFlowAuthResolver implements EduFlowAuthResolver {

    private static final String PKG_EXPEDIENTE = "com.educaflow.subsystem.expedientes.db.Expediente";

    @Override
    public Optional<Set<Permission>> resolve(User user, String fqcn, AccessType type, Long... ids) {
        Class<?> modelClass;
        try {
            modelClass = Class.forName(fqcn);
        } catch (ClassNotFoundException e) {
            return Optional.empty();
        }


        if (!Expediente.class.isAssignableFrom(modelClass)) return Optional.empty();

        // Subclase de Expediente: reutilizar los permisos declarados en auth.xml para Expediente
        var perms = JPA.em()
            .createQuery("SELECT p FROM Permission p WHERE p.object = :obj", Permission.class)
            .setParameter("obj", PKG_EXPEDIENTE)
            .getResultList();

        return Optional.of(new HashSet<>(perms));
    }
}