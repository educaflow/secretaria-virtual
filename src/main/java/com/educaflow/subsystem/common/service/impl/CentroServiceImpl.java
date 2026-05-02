package com.educaflow.subsystem.common.service.impl;

import com.axelor.db.JpaRepository;
import com.axelor.db.Repository;
import com.axelor.db.modelservice.DefaultModelService;
import com.educaflow.subsystem.common.db.Centro;
import com.educaflow.subsystem.common.db.repo.CentroRepository;
import com.educaflow.subsystem.common.service.CentroService;
import com.educaflow.subsystem.registrousuario.db.UsuarioAutorizado;
import com.educaflow.subsystem.registrousuario.db.repo.UsuarioAutorizadoRepository;
import com.educaflow.subsystem.common.db.CentroUsuario;
import com.educaflow.subsystem.common.db.repo.CentroUsuarioRepository;
import com.educaflow.subsystem.common.db.repo.CentroUsuarioTipoUsuarioRepository;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class CentroServiceImpl extends DefaultModelService<Centro> implements CentroService {

    /*@Inject
    CentroUsuarioRepository centroUsuarioRepository;

    @Inject
    CentroUsuarioTipoUsuarioRepository centroUsuarioTipoUsuarioRepo;

    @Inject
    UsuarioAutorizadoRepository usuarioAutorizadoRepo;*/

    private Logger logger = LoggerFactory.getLogger(CentroServiceImpl.class);

    public CentroServiceImpl(Class<Centro> model, Repository repository) {
        super(model, repository);
    }

    /*@Override
    public Centro insert(Centro centro) {
        logger.info("Insertando Centro: code={}, name={}", centro.getCode(), centro.getName());
        return null;
    }

    @Override
    public Centro update(Centro centroActualizado, Centro centroOriginal) {
        logger.info("Actualizando centro: code={}, name={}", centroActualizado.getCode(), centroActualizado.getName());
        return super.update(centroActualizado, centroOriginal);
    }*/


    @Override
    public List<CentroUsuario> getAdministradoresByCentro(Long id) {
        return getCargosByCentro(id, "ADMINISTRADOR");
    }

    @Override
    public List<CentroUsuario> getJefesEstudioByCentro(Long id) {
        return getCargosByCentro(id, "JEFE_ESTUDIOS");
    }

    private List<CentroUsuario> getCargosByCentro(Long centroId, String tipoCode) {
        CentroUsuarioRepository centroUsuarioRepo = (CentroUsuarioRepository) JpaRepository.of(CentroUsuario.class);
        return centroUsuarioRepo.getCargosByCentro(centroId, tipoCode);
    }

    private Optional<Centro> findByCode(String centroCodigo) {
        //return repository.findByCode
        return Optional.empty();
    }

    /*@Override
    public List<String> calcularTipoUsuarioRegistrado(Long centroId, Integer curso) {
        Repository usuarioAutorizadoRepo = JpaRepository.of(UsuarioAutorizadoRepository.class);
        Repository centroUsuarioTipoUsuarioRepo = JpaRepository.of(CentroUsuarioTipoUsuarioRepository.class);
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
    }*/


}