package com.educaflow.subsystem.security;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.axelor.auth.db.Permission;
import com.axelor.db.JpaSecurity.AccessType;
import java.util.function.BiConsumer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

class EducaFlowAuthResolverImplTest {

    private final EducaFlowAuthResolverImpl resolver = new EducaFlowAuthResolverImpl();

    @Test
    void hasAccess_sinTipoDeAcceso_lanzaNullPointerException() {
        assertThrows(NullPointerException.class, () -> resolver.hasAccess(new Permission(), null));
    }

    @ParameterizedTest
    @EnumSource(AccessType.class)
    void hasAccess_conSuFlagATrue_devuelveTrue(AccessType accessType) {
        Permission permission = new Permission();
        setterDe(accessType).accept(permission, Boolean.TRUE);

        assertTrue(resolver.hasAccess(permission, accessType));
    }

    @ParameterizedTest
    @EnumSource(AccessType.class)
    void hasAccess_conSuFlagAFalse_devuelveFalse(AccessType accessType) {
        Permission permission = new Permission();
        setterDe(accessType).accept(permission, Boolean.FALSE);

        assertFalse(resolver.hasAccess(permission, accessType));
    }

    @ParameterizedTest
    @EnumSource(AccessType.class)
    void hasAccess_conSuFlagANull_devuelveFalse(AccessType accessType) {
        Permission permission = new Permission();
        setterDe(accessType).accept(permission, null);

        assertFalse(resolver.hasAccess(permission, accessType));
    }

    @ParameterizedTest
    @EnumSource(AccessType.class)
    void hasAccess_soloMiraElFlagDeSuTipoDeAcceso(AccessType accessType) {
        Permission permission = new Permission();
        for (AccessType otro : AccessType.values()) {
            setterDe(otro).accept(permission, otro != accessType);
        }

        assertFalse(resolver.hasAccess(permission, accessType));
    }

    private static BiConsumer<Permission, Boolean> setterDe(AccessType accessType) {
        return switch (accessType) {
            case READ -> Permission::setCanRead;
            case WRITE -> Permission::setCanWrite;
            case CREATE -> Permission::setCanCreate;
            case REMOVE -> Permission::setCanRemove;
            case IMPORT -> Permission::setCanImport;
            case EXPORT -> Permission::setCanExport;
        };
    }
}
