package com.educaflow.system.gruposnotas.service.impl;

import com.axelor.auth.db.User;
import com.axelor.db.Repository;
import com.axelor.db.modelservice.AllowProperties;
import com.axelor.db.modelservice.BusinessMessages;
import com.axelor.db.modelservice.DefaultModelService;
import com.axelor.db.modelservice.ModelServiceFactory;
import com.axelor.i18n.I18n;
import com.educaflow.base.util.SecurityUtil;
import com.educaflow.subsystem.common.db.Centro;
import com.educaflow.subsystem.sistemaeducativo.db.CursoModulo;
import com.educaflow.system.gruposnotas.db.AlumnoGrupo;
import com.educaflow.system.gruposnotas.db.EstadoGrupo;
import com.educaflow.system.gruposnotas.db.Grupo;
import com.educaflow.system.gruposnotas.db.ModuloGrupo;
import com.educaflow.system.gruposnotas.db.Nota;
import com.educaflow.system.gruposnotas.db.repo.AlumnoGrupoRepository;
import com.educaflow.system.gruposnotas.db.repo.GrupoRepository;
import com.educaflow.system.gruposnotas.db.repo.ModuloGrupoRepository;
import com.educaflow.system.gruposnotas.db.repo.NotaRepository;
import com.educaflow.system.gruposnotas.service.AlumnoGrupoService;
import com.educaflow.system.gruposnotas.service.GrupoService;
import com.educaflow.system.gruposnotas.service.ModuloGrupoInsertDTO;
import com.educaflow.system.gruposnotas.service.ModuloGrupoService;
import com.educaflow.system.gruposnotas.service.NotaInsertDTO;
import com.educaflow.system.gruposnotas.service.NotaService;
import com.google.inject.Inject;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

public class GrupoServiceImpl extends DefaultModelService<Grupo> implements GrupoService {

    @Inject
    private ModelServiceFactory modelServiceFactory;

    @Inject
    private ModuloGrupoRepository moduloGrupoRepository;

    @Inject
    private AlumnoGrupoRepository alumnoGrupoRepository;

    @Inject
    private NotaRepository notaRepository;

    public GrupoServiceImpl(Class<Grupo> model, Repository<Grupo> repository) {
        super(model, repository);
    }

    @Override
    public Grupo insert(Grupo grupo) {
        // V-Grupo-001/002/003.
        validateInsert(grupo).ifPresent(BusinessMessages::throwIfInvalid);

        // R-Grupo-002: para el supervisor el servidor fija centro y curso académico.
        fireActionRule_AsignarCentroYCursoAcademicoSiSupervisor(grupo);

        // R-Grupo-001 (estado): estado inicial ABIERTO.
        fireActionRule_AsignarEstadoInicial(grupo);

        // V-AlumnoGrupo-002..008 (incl. V-AlumnoGrupo-005): valida y fija el padre de cada AlumnoGrupo
        // nuevo del panel "Alumnos" antes de persistir. El framework ya los persistió por cascade con id
        // ANTES de insert (auto-flush de JPA.manage); por eso se validan todos y la validación excluye el
        // propio id. Si uno es inválido, la ValidationException aborta y revierte la cascade (coherencia).
        validarAlumnosGrupo(grupo);

        grupo = repository.save(grupo);

        // R-Grupo-001 (módulos): genera los ModuloGrupo a partir del curso. Después de save.
        fireActionRule_GenerarModulosGrupo(grupo);

        fireActionRule_GenerarNotasNoEvaluadoFaltantes(grupo);

        return grupo;
    }

    @Override
    public Grupo update(Grupo grupo, Grupo original) {
        // V-Grupo-004/005.
        validateUpdate(grupo, original).ifPresent(BusinessMessages::throwIfInvalid);

        // Restaura los campos inmutables desde original.
        fireActionRule_RestaurarCamposInmutables(grupo, original);

        // Valida cada AlumnoGrupo de la colección con su propio servicio (V-AlumnoGrupo-002..008,
        // incluida V-AlumnoGrupo-005) antes de persistir. El framework ya ha persistido por cascade
        // los AlumnoGrupo nuevos del panel "Alumnos" y les ha asignado id ANTES de update, por lo que
        // NO se distinguen de los existentes; por eso se validan TODOS. Si alguno es inválido, la
        // ValidationException aborta la transacción y no se persiste ninguno (coherencia).
        validarAlumnosGrupo(grupo);

        grupo = repository.save(grupo);

        fireActionRule_GenerarNotasNoEvaluadoFaltantes(grupo);

        return grupo;
    }

    /**
     * V-AlumnoGrupo-002..008 (incluida V-AlumnoGrupo-005): valida cada AlumnoGrupo de la colección del
     * grupo —existentes y nuevos añadidos por el panel «Alumnos»— con su propio servicio antes de
     * persistir, tanto en el alta del grupo (insert) como al añadir alumnos a un grupo existente
     * (update). El framework (Resource.save -> JPA.manage, con auto-flush activo) ya ha persistido por
     * cascade y asignado id a los AlumnoGrupo nuevos ANTES de update, por lo que no se pueden distinguir
     * de los existentes por id; por eso se validan todos (para un AlumnoGrupo existente y válido la
     * validación pasa: V-AlumnoGrupo-005 excluye su propio id). Si alguno es inválido, la
     * ValidationException aborta la transacción y revierte el insert por cascade, de modo que no se
     * persiste ninguno de los AlumnoGrupo del guardado (coherencia transaccional). El servidor fija el
     * grupo padre de cada AlumnoGrupo (k-secure-coding §3.6: el cliente no dicta el padre; defensa IDOR).
     */
    private void validarAlumnosGrupo(Grupo grupo) {
        if (grupo.getAlumnosGrupo() == null || grupo.getAlumnosGrupo().isEmpty()) {
            return;
        }
        AlumnoGrupoService alumnoGrupoService =
                (AlumnoGrupoService) modelServiceFactory.resolve(AlumnoGrupo.class);
        for (AlumnoGrupo alumnoGrupo : grupo.getAlumnosGrupo()) {
            alumnoGrupo.setGrupo(grupo);
            alumnoGrupoService.validateInsert(alumnoGrupo)
                    .ifPresent(BusinessMessages::throwIfInvalid);
        }
    }

    @Override
    public void remove(Grupo grupo) {
        // V-Grupo-009.
        validateRemove(grupo).ifPresent(BusinessMessages::throwIfInvalid);

        repository.remove(grupo);
    }

    @Override
    public Grupo cerrarGrupo(Grupo grupo, Grupo original) {
        // V-Grupo-006.
        validateCerrarGrupo(grupo, original).ifPresent(BusinessMessages::throwIfInvalid);

        // R-Grupo-003: marca CERRADO y sella la fecha de cierre.
        fireActionRule_Cerrar(grupo);

        return repository.save(grupo);
    }

    @Override
    public Grupo reabrirGrupo(Grupo grupo, Grupo original) {
        // V-Grupo-007/008.
        validateReabrirGrupo(grupo, original).ifPresent(BusinessMessages::throwIfInvalid);

        // R-Grupo-004: marca ABIERTO y borra la fecha de cierre.
        fireActionRule_Reabrir(grupo);

        return repository.save(grupo);
    }

    /****************************************************************************************/
    /******************************** Métodos de Validación *********************************/
    /****************************************************************************************/

    @Override
    public Optional<BusinessMessages> validateInsert(Grupo grupo) {
        // V-Grupo-001 (VAL-001): nombre obligatorio.
        if (grupo.getNombre() == null || grupo.getNombre().isBlank()) {
            return Optional.of(BusinessMessages.single(I18n.get("El nombre del grupo es obligatorio")));
        }

        // V-Grupo-002 (VAL-002): curso obligatorio.
        if (grupo.getCurso() == null) {
            return Optional.of(BusinessMessages.single(I18n.get("El curso es obligatorio")));
        }

        // V-Grupo-003 (VAL-003, RES-001): nombre único por centro + curso académico.
        // Centro/curso académico EFECTIVOS calculados aquí (no dependemos del orden de fireActionRule):
        // supervisor -> los del centro activo del servidor; admin -> los del bean.
        // En el alta Axelor (Resource.save -> JPA.manage) YA ha persistido el bean y le ha asignado id
        // por secuencia ANTES de que corra validateInsert, así que grupo.getId() es NO nulo. Por eso se
        // excluye con existeOtroGrupoConNombre(...): el auto-flush de Hibernate haría que el finder se
        // encontrara a sí mismo (falso positivo de duplicado) si no se excluyera la propia fila.
        User usuario = SecurityUtil.getUser();
        Centro centroEfectivo;
        Integer cursoAcademicoEfectivo;
        if (!SecurityUtil.isAdmin(usuario)) {
            centroEfectivo = usuario.getCentroActivo();
            cursoAcademicoEfectivo = centroEfectivo != null ? centroEfectivo.getCurso() : null;
        } else {
            centroEfectivo = grupo.getCentro();
            cursoAcademicoEfectivo = grupo.getCursoAcademico();
        }
        if (existeOtroGrupoConNombre(grupo.getNombre(), centroEfectivo, cursoAcademicoEfectivo, grupo.getId())) {
            return Optional.of(BusinessMessages.single(
                    I18n.get("Ya existe un grupo con ese nombre en este centro y curso académico")));
        }

        return Optional.empty();
    }

    @Override
    public Optional<BusinessMessages> validateUpdate(Grupo grupo, Grupo original) {
        // V-Grupo-004 (VAL-004): el grupo original debe estar ABIERTO.
        if (original.getEstado() == EstadoGrupo.CERRADO) {
            return Optional.of(BusinessMessages.single(
                    I18n.get("No se puede modificar un grupo cerrado")));
        }

        // V-Grupo-005 (VAL-005, RES-001): no existe OTRO grupo con el mismo nombre en su centro y
        // curso académico (usa centro/cursoAcademico de original -inmutables- y excluye su propio id).
        if (existeOtroGrupoConNombre(grupo.getNombre(), original.getCentro(),
                original.getCursoAcademico(), grupo.getId())) {
            return Optional.of(BusinessMessages.single(
                    I18n.get("Ya existe un grupo con ese nombre en este centro y curso académico")));
        }

        return Optional.empty();
    }

    @Override
    public Optional<BusinessMessages> validateRemove(Grupo grupo) {
        // V-Grupo-009 (VAL-009): el grupo debe estar ABIERTO.
        if (grupo.getEstado() == EstadoGrupo.CERRADO) {
            return Optional.of(BusinessMessages.single(
                    I18n.get("No se puede borrar un grupo cerrado")));
        }
        return Optional.empty();
    }

    @Override
    public Optional<BusinessMessages> validateCerrarGrupo(Grupo grupo, Grupo original) {
        // V-Grupo-006 (VAL-006): el grupo original debe estar ABIERTO.
        if (original.getEstado() == EstadoGrupo.CERRADO) {
            return Optional.of(BusinessMessages.single(I18n.get("El grupo ya está cerrado")));
        }
        return Optional.empty();
    }

    @Override
    public Optional<BusinessMessages> validateReabrirGrupo(Grupo grupo, Grupo original) {
        // V-Grupo-007 (VAL-007): el grupo original debe estar CERRADO.
        if (original.getEstado() == EstadoGrupo.ABIERTO) {
            return Optional.of(BusinessMessages.single(I18n.get("El grupo ya está abierto")));
        }

        // V-Grupo-008 (VAL-008): solo el ADMINISTRADOR puede reabrir.
        if (!SecurityUtil.isAdmin(SecurityUtil.getUser())) {
            return Optional.of(BusinessMessages.single(
                    I18n.get("No tiene permisos para reabrir el grupo")));
        }

        return Optional.empty();
    }

    /**
     * V-Grupo-003 / V-Grupo-005 (VAL-003/VAL-005, RES-001): ¿existe OTRO grupo (id distinto) con ese
     * nombre en ese centro + curso académico? Usa el finder del repositorio (devuelve un único Grupo o
     * null) y excluye el propio id. Tanto en alta como en modificación Axelor persiste el bean
     * (Resource.save -> JPA.manage) ANTES de validar, así que su id ya es NO nulo cuando corre la
     * validación; pasar ese id excluye la propia fila auto-volcada por el auto-flush de Hibernate y
     * evita el falso positivo de duplicado.
     */
    private boolean existeOtroGrupoConNombre(String nombre, Centro centro, Integer cursoAcademico, Long id) {
        Grupo existente = ((GrupoRepository) repository)
                .findByNombreAndCentroAndCursoAcademico(nombre, centro, cursoAcademico);
        return existente != null && !Objects.equals(existente.getId(), id);
    }

    /**************************************************************************************/
    /********************************   AllowProperties   *********************************/
    /**************************************************************************************/

    @Override
    public AllowProperties allowPropertiesInsert() {
        return AllowProperties.createAllowProperties(Map.of(
                "nombre", Map.of(),
                "curso", Map.of(),
                "centro", Map.of(),
                "cursoAcademico", Map.of(),
                "alumnosGrupo", Map.of("alumno", Map.of())));
    }

    @Override
    public AllowProperties allowPropertiesUpdate() {
        return AllowProperties.createAllowProperties(Map.of(
                "nombre", Map.of(),
                "alumnosGrupo", Map.of("alumno", Map.of())));
    }

    @Override
    public AllowProperties allowPropertiesCerrarGrupo() {
        return AllowProperties.createAllowProperties(Map.of());
    }

    @Override
    public AllowProperties allowPropertiesReabrirGrupo() {
        return AllowProperties.createAllowProperties(Map.of());
    }

    /*************************************************************************************/
    /********************************    Action Rules    *********************************/
    /*************************************************************************************/

    /**
     * R-Grupo-001 (estado): estado inicial ABIERTO. Campo servidor, asignación incondicional.
     */
    private void fireActionRule_AsignarEstadoInicial(Grupo grupo) {
        grupo.setEstado(EstadoGrupo.ABIERTO);
    }

    /**
     * R-Grupo-002: para el supervisor (no administrador) el servidor sobrescribe centro y curso
     * académico desde su centro activo. El administrador respeta el bean. Campos servidor en el caso
     * supervisor: asignación incondicional (k-secure-coding §3.3).
     */
    private void fireActionRule_AsignarCentroYCursoAcademicoSiSupervisor(Grupo grupo) {
        User user = SecurityUtil.getUser();
        if (!SecurityUtil.isAdmin(user)) {
            Centro centroActivo = user.getCentroActivo();
            grupo.setCentro(centroActivo);
            grupo.setCursoAcademico(centroActivo.getCurso());
        }
    }

    /**
     * R-Grupo-001 (módulos): por cada módulo del curso del grupo crea un ModuloGrupo mediante
     * {@link ModuloGrupoService} (alta programática). Efecto colateral, después de save.
     */
    private void fireActionRule_GenerarModulosGrupo(Grupo grupo) {
        ModuloGrupoService moduloGrupoService = (ModuloGrupoService) modelServiceFactory.resolve(ModuloGrupo.class);
        for (CursoModulo cursoModulo : grupo.getCurso().getModulos()) {
            moduloGrupoService.insert(new ModuloGrupoInsertDTO(grupo, cursoModulo.getModulo()));
        }
    }

    /**
     * R-AlumnoGrupo-001 / RN-005 (red de seguridad idempotente): tras persistir el grupo, crea la
     * Nota NO_EVALUADO que falte para cada par (móduloGrupo, alumnoGrupo) del grupo. Se ejecuta en un
     * punto en el que AMBAS colecciones (módulos y alumnos) ya están persistidas, por lo que cubre
     * tanto el alta del grupo como el alta incremental de alumnos/módulos. Idempotente: no duplica las
     * notas ya existentes (respeta la unique-constraint moduloGrupo+alumnoGrupo). El estado NO_EVALUADO
     * y la fijación de móduloGrupo/alumnoGrupo los pone NotaServiceImpl.insert vía el DTO (whitelist).
     */
    private void fireActionRule_GenerarNotasNoEvaluadoFaltantes(Grupo grupo) {
        NotaService notaService = (NotaService) modelServiceFactory.resolve(Nota.class);
        List<ModuloGrupo> modulos = moduloGrupoRepository.findByGrupo(grupo);
        List<AlumnoGrupo> alumnos = alumnoGrupoRepository.findByGrupo(grupo);
        Set<String> pares = new HashSet<>();
        for (Nota nota : notaRepository.findByGrupo(grupo)) {
            pares.add(claveNota(nota.getModuloGrupo().getId(), nota.getAlumnoGrupo().getId()));
        }
        for (ModuloGrupo moduloGrupo : modulos) {
            for (AlumnoGrupo alumnoGrupo : alumnos) {
                if (!pares.contains(claveNota(moduloGrupo.getId(), alumnoGrupo.getId()))) {
                    notaService.insert(new NotaInsertDTO(moduloGrupo, alumnoGrupo));
                }
            }
        }
    }

    private String claveNota(Long moduloGrupoId, Long alumnoGrupoId) {
        return moduloGrupoId + "-" + alumnoGrupoId;
    }

    /**
     * R-Grupo-003: cierra el grupo. Campos servidor, asignación incondicional.
     */
    private void fireActionRule_Cerrar(Grupo grupo) {
        grupo.setEstado(EstadoGrupo.CERRADO);
        grupo.setFechaCierre(LocalDateTime.now());
    }

    /**
     * R-Grupo-004: reabre el grupo. Campos servidor, asignación incondicional.
     */
    private void fireActionRule_Reabrir(Grupo grupo) {
        grupo.setEstado(EstadoGrupo.ABIERTO);
        grupo.setFechaCierre(null);
    }

    /**
     * Restaura los campos inmutables desde original (curso, centro, curso académico, estado y fecha
     * de cierre) para que el cliente no los pueda pisar en un update (k-secure-coding §3, §9).
     */
    private void fireActionRule_RestaurarCamposInmutables(Grupo grupo, Grupo original) {
        grupo.setCurso(original.getCurso());
        grupo.setCentro(original.getCentro());
        grupo.setCursoAcademico(original.getCursoAcademico());
        grupo.setEstado(original.getEstado());
        grupo.setFechaCierre(original.getFechaCierre());
    }
}
