package com.educaflow.subsystem.notificaciones.util;

import com.axelor.auth.db.User;
import com.axelor.script.ScriptAllowed;
import com.educaflow.subsystem.common.db.CargoCodigo;
import com.educaflow.subsystem.common.db.CentroUsuario;
import com.educaflow.subsystem.common.db.CentroUsuarioCargo;
import com.educaflow.subsystem.common.db.CentroUsuarioTipoUsuario;
import com.educaflow.subsystem.common.db.TipoUsuarioCodigo;
import java.util.Collection;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Stream;

// @ScriptAllowed es imprescindible: los conditionParams de los permisos *.propio-centro-gestion la invocan
// desde Groovy y la ScriptPolicy de Axelor solo deja llamar a clases de su lista blanca o anotadas así.
// Sin ella la condición lanza ScriptPolicyException.
@ScriptAllowed
public final class GestorNotificacionesUtil {

    private static final Set<TipoUsuarioCodigo> TIPOS_GESTORES =
            EnumSet.of(TipoUsuarioCodigo.SUPERVISOR, TipoUsuarioCodigo.ADMINISTRATIVO);
    private static final Set<CargoCodigo> CARGOS_GESTORES =
            EnumSet.of(CargoCodigo.DIRECTOR, CargoCodigo.JEFE_ESTUDIOS, CargoCodigo.SECRETARIO);

    private GestorNotificacionesUtil() {
    }

    public static boolean esGestorEnAlgunCentro(User user) {
        return user != null
                && user.getCentroUsuarios() != null
                && user.getCentroUsuarios().stream().anyMatch(GestorNotificacionesUtil::gestiona);
    }

    public static List<Long> idsCentrosGestionados(User user) {
        if (esGestorEnAlgunCentro(user) == false) {
            // Un IN vacío no es portable en JPQL y -1 no es el id de ninguna fila.
            return List.of(-1L);
        }
        return user.getCentroUsuarios().stream()
                .filter(GestorNotificacionesUtil::gestiona)
                .map(centroUsuario -> centroUsuario.getCentro().getId())
                .distinct()
                .toList();
    }

    private static boolean gestiona(CentroUsuario centroUsuario) {
        boolean porTipo = Stream.ofNullable(centroUsuario.getCentroUsuarioTipoUsuario())
                .flatMap(Collection::stream)
                .map(CentroUsuarioTipoUsuario::getTipoUsuario)
                .filter(Objects::nonNull)
                .anyMatch(tipoUsuario -> TIPOS_GESTORES.contains(tipoUsuario.getCodigo()));
        boolean porCargo = Stream.ofNullable(centroUsuario.getCentroUsuarioCargo())
                .flatMap(Collection::stream)
                .map(CentroUsuarioCargo::getCargo)
                .filter(Objects::nonNull)
                .anyMatch(cargo -> CARGOS_GESTORES.contains(cargo.getCode()));
        return porTipo || porCargo;
    }
}
