package com.educaflow.subsystem.registrousuario.db.repo;

import com.axelor.db.JpaRepository;
import com.axelor.inject.Beans;
import com.educaflow.subsystem.common.db.Centro;
import com.educaflow.subsystem.registrousuario.db.RegistroPendiente;
import com.educaflow.subsystem.registrousuario.db.UsuarioAutorizado;
import com.educaflow.subsystem.common.db.TipoUsuario;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

public class RegistroPendienteRepository extends AbstractRegistroPendienteRepository {

    //static final Map<String, String> EX_MAPPING = UsuarioAutorizadoRepository.EX_MAPPING;
    static final Map<String, String> EX_MAPPING = Map.of();

    public Optional<RegistroPendiente> findByToken(String token) {
        return Optional.ofNullable(
                all().filter("self.token = :token").bind("token", token).fetchOne()
        );
    }

    public Map<Centro, List<TipoUsuario>> findTiposUsuarioByDni(String dni) {
        List<UsuarioAutorizado> entradas = Beans.get(UsuarioAutorizadoRepository.class)
                .all()
                .filter("self.dni = :dni" +
                        " AND self.centro IS NOT NULL" +
                        " AND self.tipoUsuario IS NOT NULL" +
                        " AND self.curso = (" +
                        "   SELECT MAX(ua2.curso) FROM UsuarioAutorizado ua2" +
                        "   WHERE ua2.dni = self.dni" +
                        "   AND ua2.centro = self.centro" +
                        "   AND ua2.tipoUsuario = self.tipoUsuario" +
                        " )")
                .bind("dni", dni)
                .fetch();

        Map<String, TipoUsuario> tiposPorCodigo = JpaRepository.of(TipoUsuario.class)
                .all()
                .fetch()
                .stream()
                .collect(Collectors.toMap(TipoUsuario::getCode, t -> t, (a, b) -> a));

        return resolverPerfiles(entradas, tiposPorCodigo);
    }

    static Map<Centro, List<TipoUsuario>> resolverPerfiles(
            List<UsuarioAutorizado> entradas,
            Map<String, TipoUsuario> tiposPorCodigo) {

        Map<Centro, List<TipoUsuario>> resultado = new LinkedHashMap<>();

        for (UsuarioAutorizado ua : entradas) {
            Centro centro = ua.getCentro();
            String codigoBase = ua.getTipoUsuario().getCode();
            boolean esCursoActual = Objects.equals(ua.getCurso(), centro.getCurso());

            TipoUsuario tipo;
            if (esCursoActual) {
                tipo = ua.getTipoUsuario();
            } else {
                String codigoEx = EX_MAPPING.get(codigoBase);
                if (codigoEx == null) continue; // FAMILIAR y similares sin versión "ex" → omitir
                tipo = tiposPorCodigo.get(codigoEx);
                if (tipo == null) continue;
            }

            resultado.computeIfAbsent(centro, k -> new ArrayList<>()).add(tipo);
        }

        return resultado;
    }
}
