package com.educaflow.system.expedientes.controller;

import com.axelor.auth.db.User;
import com.axelor.db.JPA;
import com.axelor.meta.CallMethod;
import com.educaflow.base.util.SecurityUtil;
import com.educaflow.subsystem.expedientes.db.Expediente;
import com.educaflow.subsystem.expedientes.db.Profile;
import com.educaflow.subsystem.security.service.PerfilesUsuarioService;
import jakarta.inject.Inject;

import java.util.List;

/**
 * El punto de consulta que usa el {@code <domain>} de una bandeja para quedarse con los expedientes
 * sobre los que <b>el usuario autenticado</b> ostenta un perfil concreto.
 *
 * <p>Responde <b>siempre</b> sobre el usuario autenticado, resuelto aquí dentro con
 * {@link SecurityUtil#getUser()}: no recibe usuario, ni centros, ni estado, así que llamarlo con otro
 * perfil o con otro trámite solo devuelve los expedientes sobre los que quien llama ostenta ese
 * perfil, que es justo lo que ya puede ver.
 *
 * <p>Quién ostenta qué perfil sobre un expediente <b>no</b> se reescribe aquí en JPQL: esa pregunta
 * tiene dueño, {@link PerfilesUsuarioService#getPerfilesSobreExpediente}, y este controlador le
 * pregunta una vez por expediente candidato. El conjunto candidato está acotado por construcción
 * (los expedientes abiertos de un trámite en los centros del usuario), así que es el tamaño de la propia bandeja;
 * si ese coste llegara a molestar, el arreglo va en el dueño —un método masivo en el servicio o en
 * los repositorios {@code AceProfile*}—, nunca devolviendo el JPQL de perfiles a las vistas.
 *
 * <p>El JPQL que acota los expedientes candidatos sí vive aquí, y no en un repositorio como el resto
 * del JPQL del proyecto: es una decisión declarada, no un descuido. Su dueño natural sería
 * {@code subsystem/expedientes}, pero llevarlo allí obligaría a ampliar el motor de tramitación, que
 * debe mantenerse lo más pequeño posible porque todo lo que se le añade lo heredan todos los tipos de
 * expediente; y colgar un repositorio de {@link Expediente} de {@code tramites/util} rompería que el
 * repositorio es un detalle interno del subsistema dueño de la entidad. Si algún día se rehace, el
 * arreglo va en el dueño —una consulta ofrecida por {@code subsystem/expedientes}—, nunca en un
 * repositorio propio aquí.
 *
 * <p><b>Esta pieza no autoriza nada.</b> El resultado viaja al cliente dentro del {@code <context>} de
 * la {@code <action-view>} y vuelve del navegador en cada búsqueda, así que la lista de ids es
 * manipulable desde el cliente. Es un filtro de <b>interfaz</b>, de comodidad —que la bandeja enseñe
 * lo que toca—, y <b>no</b> un control de acceso: quién puede <b>actuar</b> lo siguen decidiendo
 * {@code ExpedienteSecurity.checkPerfilDelEstado} y las guardas de la clase de utilidad del tipo al disparar cada
 * evento. Nada debe construirse encima suponiendo lo contrario.
 *
 * <p>Las tres condiciones que toda bandeja promete —expedientes abiertos, de este trámite y de mis
 * centros— son asunto de esta pieza, {@code self.abierto = true} incluido. Una condición de
 * <b>estado concreto</b> es asunto del {@code <domain>} del llamante, que añade lo suyo al filtro
 * (p. ej. la bandeja del director: {@code AND self.codeState = 'PENDIENTE_FIRMA_DIRECTOR'}).
 */
public class BandejaPorPerfilController {

    /**
     * Lo que se devuelve cuando no hay nada que listar: {@code self.id IN (:lista)} con la lista
     * vacía no es portable, y {@code -1} no es el id de ninguna fila.
     */
    private static final List<Long> NINGUNO = List.of(-1L);

    @Inject
    PerfilesUsuarioService perfilesUsuarioService;

    /**
     * Los ids de los expedientes abiertos del trámite indicado, en los centros del usuario
     * autenticado, sobre los que ese usuario ostenta el perfil indicado.
     *
     * @param nombrePerfil nombre de la constante de {@link Profile} (no de un cargo) que hay que
     *                     ostentar; un nombre que no sea de ningún perfil es una errata de la vista
     *                     que llama, así que revienta
     * @param tramiteCode  {@code code} del trámite cuyos expedientes se listan
     * @return los ids encontrados, o {@link #NINGUNO} si no hay ninguno
     */
    @CallMethod
    public List<Long> idsExpedientesConPerfil(String nombrePerfil, String tramiteCode) {
        Profile perfil = Profile.valueOf(nombrePerfil);

        User user = SecurityUtil.getUser();
        if (user == null) {
            return NINGUNO;
        }

        List<Long> ids = JPA.all(Expediente.class)
                .filter("self.abierto = true AND self.tipoExpediente.tramite.code = :tramiteCode"
                        + " AND self.centro IN (SELECT cu.centro FROM CentroUsuario cu WHERE cu.usuario = :usuario)")
                .bind("tramiteCode", tramiteCode)
                .bind("usuario", user)
                .fetch()
                .stream()
                .filter(expediente -> perfilesUsuarioService
                        .getPerfilesSobreExpediente(expediente, user)
                        .contains(perfil))
                .map(Expediente::getId)
                .toList();

        return ids.isEmpty() ? NINGUNO : ids;
    }

}
