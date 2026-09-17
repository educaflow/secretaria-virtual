package com.educaflow.subsystem.expedientes.services.internal;

import com.axelor.auth.db.User;
import com.educaflow.base.util.SecurityUtil;
import com.educaflow.subsystem.common.db.Cargo;
import com.educaflow.subsystem.common.db.Centro;
import com.educaflow.subsystem.common.db.CentroUsuario;
import com.educaflow.subsystem.common.db.CentroUsuarioCargo;
import com.educaflow.subsystem.common.db.CentroUsuarioTipoUsuario;
import com.educaflow.subsystem.common.db.TipoUsuario;
import com.educaflow.subsystem.expedientes.db.Profile;
import com.educaflow.subsystem.expedientes.db.TipoTramite;
import com.educaflow.subsystem.expedientes.db.Tramite;
import com.educaflow.subsystem.expedientes.services.eventmanager.State;
import org.apache.shiro.authz.UnauthorizedException;

import java.util.Collection;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Autorización del motor de tramitación: quién puede crear un expediente y quién puede disparar los
 * eventos de un estado.
 *
 * <p>Es el sitio <b>común</b> a los dos servicios: {@code ContextoTramitacionService} lo usa para
 * decidir qué se pinta en la pantalla «Nuevo expediente» y {@code ExpedienteService} para autorizar
 * de verdad el alta y los eventos. Las dos puertas contestan con la misma regla.
 *
 * <p>Todos sus métodos son estáticos y <b>puros</b>: reciben lo que necesitan y no inyectan nada, así
 * que se prueban sin Guice.
 */
public final class ExpedienteSecurity {

    private static final String TIPO_USUARIO_FAMILIAR = "FAMILIAR";

    private ExpedienteSecurity() {
    }

    /**
     * Si el usuario puede crear en ese centro un expediente del trámite presentándolo de esa forma.
     *
     * <p>Sale solo de los tipos de usuario y los cargos que el usuario tiene en el centro, sin
     * {@code Ace}. El administrador no tiene trato especial: puede lo que le den sus tipos de usuario
     * y sus cargos, igual que cualquier otro.
     *
     * <p>Las cuatro combinaciones:
     * <ul>
     *   <li><b>Presenta él, para sí mismo</b>: es interesado del tipo de trámite.</li>
     *   <li><b>Presenta él, en representación</b>: el tipo de trámite la admite y él es {@code FAMILIAR}.</li>
     *   <li><b>Registra una solicitud en papel</b>: el tipo de trámite admite papel y él es registrador.
     *       Quien registra no presenta nada suyo, así que las opciones son las de la persona que
     *       entregó el papel, de la que el sistema no sabe nada: puede haberla traído para sí misma o
     *       en representación de otra, si el tipo de trámite la admite.</li>
     * </ul>
     */
    public static boolean puedeCrear(Tramite tramite, User user, Centro centro, boolean presentadoEnPapel, boolean presentadoEnRepresentacion) {
        if ((tramite == null) || (user == null) || (centro == null)) {
            return false;
        }

        TipoTramite tipoTramite = tramite.getTipoTramite();
        CentroUsuario centroUsuario = user.getCentroUsuario(centro);
        if ((tipoTramite == null) || (centroUsuario == null)) {
            return false;
        }

        if (presentadoEnRepresentacion && (Boolean.TRUE.equals(tipoTramite.getAdmiteRepresentacion()) == false)) {
            return false;
        }

        if (presentadoEnPapel) {
            return Boolean.TRUE.equals(tipoTramite.getAdmitePresentacionEnPapel())
                    && tieneAlguno(centroUsuario, tipoTramite.getTiposUsuarioRegistradores(), tipoTramite.getCargosRegistradores());
        }

        if (presentadoEnRepresentacion) {
            return getCodigosTiposUsuario(centroUsuario).contains(TIPO_USUARIO_FAMILIAR);
        }

        return tieneAlguno(centroUsuario, tipoTramite.getTiposUsuarioInteresados(), tipoTramite.getCargosInteresados());
    }

    /**
     * Exige lo que {@link #puedeCrear} responde.
     *
     * <p>Lo que el cliente envía al crear (centro, papel y representación) llega tal cual desde el
     * navegador: los {@code hidden} y {@code readonly} que pinta la pantalla no son defensa.
     */
    public static void checkPuedeCrear(Tramite tramite, User user, Centro centro, boolean presentadoEnPapel, boolean presentadoEnRepresentacion) {
        if (puedeCrear(tramite, user, centro, presentadoEnPapel, presentadoEnRepresentacion)) {
            return;
        }

        throw new UnauthorizedException("El usuario no puede crear expedientes del trámite '"
                + ((tramite == null) ? null : tramite.getCode()) + "' en el centro '"
                + ((centro == null) ? null : centro.getCode()) + "' "
                + (presentadoEnRepresentacion ? "en representación de otra persona" : "para sí mismo")
                + (presentadoEnPapel ? " registrando una solicitud entregada en papel." : "."));
    }

    /**
     * Comprueba que el usuario tenga el perfil que el estado declara para su actor.
     *
     * <p>Es <b>pertenencia a un conjunto</b>, no derivación: un usuario puede tener varios perfiles a
     * la vez sobre el mismo expediente. El {@code _profile} que envía el cliente no participa — solo
     * elige qué vista se pinta.
     *
     * <p>Un estado <b>sin perfil</b> no exige ninguno: hay estados que no declaran actor (típicamente
     * los finales) y ahí la única barrera es el acceso al propio expediente.
     */
    public static void checkPerfilDelEstado(State state, Set<String> perfilesDelUsuario) {
        Profile profileDelEstado = state.getProfile();
        if (profileDelEstado == null) {
            return;
        }

        //El administrador ve y tramita expedientes de cualquier centro y no tiene filas Ace.
        if (SecurityUtil.isAdmin(SecurityUtil.getUser())) {
            return;
        }

        if (perfilesDelUsuario.contains(profileDelEstado.name()) == false) {
            throw new UnauthorizedException("El usuario no tiene el perfil '" + profileDelEstado.name()
                    + "', que es el que atiende el estado '" + state.getPhase().getCode() + "/"
                    + state.getCode() + "'.");
        }
    }


    /*******************************************************************/
    /********************** Funciones de Utilidad **********************/
    /*******************************************************************/

    private static boolean tieneAlguno(CentroUsuario centroUsuario, Collection<TipoUsuario> tiposUsuario, Collection<Cargo> cargos) {
        Set<String> codigosTiposUsuario = getCodigos(tiposUsuario, TipoUsuario::getCodigo);
        Set<String> codigosCargos = getCodigos(cargos, Cargo::getCode);

        return getCodigosTiposUsuario(centroUsuario).stream().anyMatch(codigosTiposUsuario::contains)
                || getCodigosCargos(centroUsuario).stream().anyMatch(codigosCargos::contains);
    }

    private static Set<String> getCodigosTiposUsuario(CentroUsuario centroUsuario) {
        if (centroUsuario.getCentroUsuarioTipoUsuario() == null) {
            return Set.of();
        }

        return getCodigos(centroUsuario.getCentroUsuarioTipoUsuario().stream()
                .map(CentroUsuarioTipoUsuario::getTipoUsuario)
                .toList(), TipoUsuario::getCodigo);
    }

    private static Set<String> getCodigosCargos(CentroUsuario centroUsuario) {
        if (centroUsuario.getCentroUsuarioCargo() == null) {
            return Set.of();
        }

        return getCodigos(centroUsuario.getCentroUsuarioCargo().stream()
                .map(CentroUsuarioCargo::getCargo)
                .toList(), Cargo::getCode);
    }

    private static <T> Set<String> getCodigos(Collection<T> entidades, Function<T, String> codigo) {
        if (entidades == null) {
            return Set.of();
        }

        return entidades.stream()
                .filter(Objects::nonNull)
                .map(codigo)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
    }

}
