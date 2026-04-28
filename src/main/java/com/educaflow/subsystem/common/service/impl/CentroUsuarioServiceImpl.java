package com.educaflow.subsystem.common.service.impl;

import com.axelor.db.JpaRepository;
import com.axelor.db.Repository;
import com.axelor.db.modelservice.DefaultModelService;
import com.educaflow.subsystem.common.db.CentroUsuario;
import com.educaflow.subsystem.common.db.CentroUsuarioTipoUsuario;
import com.educaflow.subsystem.common.db.repo.CentroUsuarioRepository;
import com.educaflow.subsystem.common.db.repo.CentroUsuarioTipoUsuarioRepository;
import com.educaflow.subsystem.common.service.CentroUsuarioService;
import com.educaflow.subsystem.registrousuario.db.UsuarioAutorizado;
import com.educaflow.subsystem.registrousuario.db.repo.UsuarioAutorizadoRepository;

import java.util.ArrayList;
import java.util.List;

public class CentroUsuarioServiceImpl extends DefaultModelService<CentroUsuario> implements CentroUsuarioService {

    public CentroUsuarioServiceImpl(Class<CentroUsuario> model, Repository repository) {
        super(model, repository);
    }

    @Override
    public List<String> calcularTipoUsuarioRegistrado(Long centroId, Integer curso) {
        UsuarioAutorizadoRepository usuarioAutorizadoRepo = (UsuarioAutorizadoRepository) JpaRepository.of(UsuarioAutorizado.class);
        CentroUsuarioTipoUsuarioRepository centroUsuarioTipoUsuarioRepo = (CentroUsuarioTipoUsuarioRepository) JpaRepository.of(CentroUsuarioTipoUsuario.class);

        List<UsuarioAutorizado> autorizados = usuarioAutorizadoRepo.findByCentroAndCurso(centroId, curso);

        List<String> profesorDnis = autorizados.stream()
                .filter(ua -> "PROFESOR".equals(ua.getTipoUsuario().getCode()))
                .map(UsuarioAutorizado::getDni)
                .toList();

        List<String> alumnoDnis = autorizados.stream()
                .filter(ua -> "ALUMNO".equals(ua.getTipoUsuario().getCode()))
                .map(UsuarioAutorizado::getDni)
                .toList();

        int degradadosProfesores = centroUsuarioTipoUsuarioRepo.degradarProfesores(centroId);
        int degradadosAlumnos    = centroUsuarioTipoUsuarioRepo.degradarAlumnos(centroId);
        int profesoresActivos    = profesorDnis.isEmpty() ? 0 : centroUsuarioTipoUsuarioRepo.promoverAProfesor(centroId, profesorDnis);
        int alumnosActivos       = alumnoDnis.isEmpty()   ? 0 : centroUsuarioTipoUsuarioRepo.promoverAAlumno(centroId, alumnoDnis);

        List<String> resultado = new ArrayList<>();
        resultado.add("Profesores activos en el nuevo curso: " + profesoresActivos);
        resultado.add("Convertidos a exprofesor: " + (degradadosProfesores - profesoresActivos));
        resultado.add("Alumnos activos en el nuevo curso: " + alumnosActivos);
        resultado.add("Convertidos a exalumno: " + (degradadosAlumnos - alumnosActivos));
        return resultado;
    }
}