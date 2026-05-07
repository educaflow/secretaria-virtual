package com.educaflow.subsystem.importacion.service.impl;

import com.axelor.db.Repository;
import com.axelor.db.modelservice.DefaultModelService;
import com.axelor.inject.Beans;
import com.educaflow.base.util.AsciiTableUtil;
import com.educaflow.base.util.MetaFileUtil;
import com.educaflow.base.util.XMLUtil;
import com.educaflow.subsystem.common.db.Centro;
import com.educaflow.subsystem.common.db.repo.CentroRepository;
import com.educaflow.subsystem.importacion.db.TareaImportacion;
import com.educaflow.subsystem.importacion.db.TipoFicheroImportacion;
import com.educaflow.subsystem.importacion.service.tipoimportador.ImportadorException;
import com.educaflow.subsystem.importacion.service.tipoimportador.ImportadorFichero;
import com.educaflow.subsystem.importacion.service.tipoimportador.ImportadorFicheroFactory;
import com.educaflow.subsystem.importacion.service.tipoimportador.ResultadoImportacion;
import com.educaflow.subsystem.importacion.service.TareaImportacionService;
import org.w3c.dom.Element;

import java.util.ArrayList;
import java.util.List;

public class TareaImportacionServiceImpl extends DefaultModelService<TareaImportacion> implements TareaImportacionService {

    public TareaImportacionServiceImpl(Class<TareaImportacion> model, Repository repository) {
        super(model, repository);
    }

    @Override
    public TareaImportacion insert(TareaImportacion tareaImportacion) {
        try {
            ResultadoImportacion resultado = this.importar(tareaImportacion);
            String logs = buildResultado(resultado);
            tareaImportacion.setExito(true);
            tareaImportacion.setImportLog(logs);
        } catch (ImportadorException e) {
            tareaImportacion.setExito(false);
            tareaImportacion.setImportLog(AsciiTableUtil.renderTable("Error", e));
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

    private ResultadoImportacion importar(TareaImportacion tareaImportacion) {
        ImportadorFichero importador = ImportadorFicheroFactory.getImportadorFichero(tareaImportacion.getTipoFichero());

        byte[] contenido = MetaFileUtil.downloadContent(tareaImportacion.getFichero());

        Centro centro;
        Integer curso;

        if (tareaImportacion.getTipoFichero() == TipoFicheroImportacion.PROFESOR_EXTERNO) {
            centro = tareaImportacion.getUsuario().getCentroActivo();
            if (centro == null) {
                throw new ImportadorException("El usuario no tiene un centro activo asignado");
            }
            curso = centro.getCurso();
            if (curso == null) {
                throw new ImportadorException("El centro activo no tiene un curso académico configurado");
            }
        } else {
            Element root = XMLUtil.getDocument(contenido).getDocumentElement();
            String codigoCentro = XMLUtil.getStringAttribute(root, "codigo", null);
            String cursoStr = XMLUtil.getStringAttribute(root, "curso", null);
            centro = codigoCentro != null ? Beans.get(CentroRepository.class).findByCode(codigoCentro) : null;
            curso = cursoStr != null ? Integer.parseInt(cursoStr) : null;
        }

        tareaImportacion.setCentro(centro);
        tareaImportacion.setCurso(curso);
        return importador.importar(contenido, centro, curso);
    }

    private String buildResultado(ResultadoImportacion resultadoImportacion) {
        StringBuilder logs = new StringBuilder();
        logs.append(resultadoImportacion.resumen()).append("\n");
        if (!resultadoImportacion.mensajes().isEmpty()) {
            List<List<Object>> rows = new ArrayList<>();
            for (ResultadoImportacion.MensajeImportacion m : resultadoImportacion.mensajes()) {
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
