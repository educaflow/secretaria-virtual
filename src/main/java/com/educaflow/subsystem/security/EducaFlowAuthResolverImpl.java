package com.educaflow.subsystem.security;

import com.axelor.auth.EduFlowAuthResolver;
import com.axelor.auth.db.Permission;
import com.axelor.auth.db.User;
import com.axelor.db.JpaSecurity.AccessType;
import com.educaflow.subsystem.expedientes.db.Expediente;

import java.util.Optional;
import java.util.Set;

public class EducaFlowAuthResolverImpl implements EduFlowAuthResolver {

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

        // Subclase de Expediente: reutilizar los permisos declarados en auth.xml para Expediente,
        // resueltos para ESTE usuario y ESTE tipo de acceso (no todas las filas Permission del objeto).
        // Un Set vacío MUST devolverse como Optional.empty(): AuthSecurity.resolvePermissions solo cae
        // al resolver por defecto cuando el Optional viene vacío, y con un Set vacío dentro las
        // subclases perderían sus permisos propios.
        Set<Permission> perms = resolveForUser(user, PKG_EXPEDIENTE, type);
        return perms.isEmpty() ? Optional.empty() : Optional.of(perms);
    }

    public boolean hasAccess(Permission permission, AccessType accessType) {
        if (accessType == null) {
            return true;
        }
        switch (accessType) {
            case READ:
                return Boolean.TRUE.equals(permission.getCanRead());
            case WRITE:
                return Boolean.TRUE.equals(permission.getCanWrite());
            case CREATE:
                return Boolean.TRUE.equals(permission.getCanCreate());
            case REMOVE:
                return Boolean.TRUE.equals(permission.getCanRemove());
            case IMPORT:
                return Boolean.TRUE.equals(permission.getCanImport());
            case EXPORT:
                return Boolean.TRUE.equals(permission.getCanExport());
            default:
                return false;
        }
    }
}