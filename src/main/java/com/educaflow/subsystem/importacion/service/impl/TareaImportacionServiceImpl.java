package com.educaflow.subsystem.importacion.service.impl;

import com.axelor.db.JpaRepository;
import com.axelor.db.Repository;
import com.axelor.db.modelservice.DefaultModelService;
import com.educaflow.base.util.AsciiTableUtil;
import com.educaflow.base.util.MetaFileUtil;
import com.educaflow.subsystem.common.db.Centro;
import com.educaflow.subsystem.importacion.db.TareaImportacion;
import com.educaflow.subsystem.importacion.db.TipoFicheroImportacion;
import com.educaflow.subsystem.importacion.util.MensajeImportacion;
import com.educaflow.subsystem.importacion.util.ImportadorException;
import com.educaflow.subsystem.importacion.util.ImportadorFichero;
import com.educaflow.subsystem.importacion.util.ImportadorFicheroFactory;
import com.educaflow.subsystem.importacion.util.ResultadoImportacion;
import com.educaflow.subsystem.importacion.service.TareaImportacionService;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class TareaImportacionServiceImpl extends DefaultModelService<TareaImportacion> implements TareaImportacionService {

    public TareaImportacionServiceImpl(Class<TareaImportacion> model, Repository repository) {
        super(model, repository);
    }

    @Override
    public TareaImportacion insert(TareaImportacion tareaImportacion) {
        try {
            ResultadoImportacion resultadoImportacion = this.importar(tareaImportacion);
            String logs = buildResultado(resultadoImportacion);
            tareaImportacion.setExito(true);
            tareaImportacion.setImportLog(logs);
        } catch (ImportadorException e) {
            tareaImportacion.setExito(false);
            tareaImportacion.setImportLog(e.getMessage());
        }

        return super.insert(tareaImportacion);

    }

    @Override
    public TareaImportacion update(TareaImportacion newTareaImportacion, TareaImportacion oldTareaImportacion){
        throw new UnsupportedOperationException("No se permite modificar una tarea de importación. Crea una nueva tarea para realizar otra importación.");
    }

    @Override
    public void remove(TareaImportacion tareaImportacion) {
        throw new UnsupportedOperationException("No se permite eliminar una tarea de importación. Si quieres eliminar la referencia al fichero, borra el fichero desde su ubicación original.");
    }

    @Override
    public Optional<LocalDate> findFechaUltimaImportacion(Long centroId, TipoFicheroImportacion tipoFichero) {
        return Optional.ofNullable(
                JpaRepository.of(TareaImportacion.class)
                        .all()
                        .filter("self.centro.id = :centroId" +
                                " AND self.tipoFichero = :tipoFichero" +
                                " AND self.fechaExportacion IS NOT NULL")
                        .bind("centroId", centroId)
                        .bind("tipoFichero", tipoFichero)
                        .order("-fechaExportacion")
                        .fetchOne()
        ).map(TareaImportacion::getFechaExportacion);
    }

    private ResultadoImportacion importar(TareaImportacion tareaImportacion) {
        byte[] contenido = MetaFileUtil.downloadContent(tareaImportacion.getFichero());
        ImportadorFichero importador = ImportadorFicheroFactory.getImportadorFichero(tareaImportacion.getTipoFichero(), contenido);

        ResultadoImportacion resultadoImportacion = importador.importar();
        Centro centro = resultadoImportacion.centro();
        Integer curso = resultadoImportacion.curso();

        tareaImportacion.setCentro(centro);
        tareaImportacion.setCurso(curso);
        tareaImportacion.setFechaExportacion(resultadoImportacion.fechaExportacion());
        return resultadoImportacion;
    }

    private String buildResultado(ResultadoImportacion resultadoImportacion) {
        StringBuilder logs = new StringBuilder();
        logs.append(resultadoImportacion.resumen()).append("\n");
        if (!resultadoImportacion.mensajes().isEmpty()) {
            List<List<Object>> rows = new ArrayList<>();
            for (MensajeImportacion m : resultadoImportacion.mensajes()) {
                List<Object> row = new ArrayList<>();
                row.add(m.fila() != null ? m.fila() : "");
                row.add(m.dato());
                row.add(m.mensaje());
                rows.add(row);
            }
            logs.append(AsciiTableUtil.renderTable("Detalle", List.of("Fila", "Dato", "Mensaje"), rows));
        }
        return logs.toString();
    }

}
