package com.educaflow.subsystem.importacion.service.impl;

import com.axelor.db.Repository;
import com.axelor.db.modelservice.DefaultModelService;
import com.axelor.inject.Beans;
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

import de.vandermeer.asciitable.AsciiTable;
import de.vandermeer.asciitable.CWC_LongestLine;
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
            StringBuilder logs = new StringBuilder();
            logs.append(resultado.resumen()).append("\n");
            if (!resultado.mensajes().isEmpty()) {
                List<List<Object>> rows = new ArrayList<>();
                for (ResultadoImportacion.MensajeImportacion m : resultado.mensajes()) {
                    List<Object> row = new ArrayList<>();
                    row.add(m.fila() != null ? m.fila() : "");
                    row.add(m.dato());
                    row.add(m.mensaje());
                    rows.add(row);
                }
                logs.append(renderTable("Detalle", List.of("Fila", "Dato", "Mensaje"), rows));
            }
            tareaImportacion.setExito(true);
            tareaImportacion.setImportLog(logs.toString());
        } catch (ImportadorException e) {
            tareaImportacion.setImportLog(renderTable("Error", e));
            tareaImportacion.setExito(false);
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

    private static String renderTable(String tableName, Exception ex) {
        List<List<Object>> rows = new ArrayList<>();
        for (String trace : getStackTrace(ex, 0)) {
            List<Object> row = new ArrayList<>();
            row.add(trace);
            rows.add(row);
        }
        return renderTable(tableName, List.of("Error"), rows);
    }

    private static String renderTable(String tableName, List<String> heads, List<List<Object>> rows) {
        List<String> titulo = new ArrayList<>();
        if ((heads != null) && (!heads.isEmpty())) {
            for (int i = 0; i < heads.size() - 1; i++) {
                titulo.add(null);
            }
        }
        titulo.add(tableName);

        AsciiTable at = new AsciiTable();
        at.addRule();
        at.addRow(titulo.toArray());
        if ((heads != null) && (!heads.isEmpty())) {
            at.addRule();
            at.addRow(heads.toArray());
        }
        at.addRule();

        for (List<Object> row : rows) {
            for (int i = 0; i < row.size(); i++) {
                if (row.get(i) == null) {
                    row.set(i, "__null__");
                }
            }
        }

        if (!rows.isEmpty()) {
            for (List<Object> row : rows) {
                at.addRow(row.toArray());
            }
            at.addRule();
        }
        at.getRenderer().setCWC(new CWC_LongestLine());
        return at.render();
    }

    private static List<String> getStackTrace(Throwable ex, int deep) {
        String tabulador = "·".repeat(4);
        List<String> stackTrace = new ArrayList<>();
        if (deep == 0) {
            stackTrace.add(ex.getLocalizedMessage());
        }
        for (StackTraceElement element : ex.getStackTrace()) {
            stackTrace.add(tabulador.repeat(deep) + element.toString());
        }
        if (ex.getCause() != null) {
            stackTrace.add(tabulador.repeat(deep) + "Caused by:" + ex.getCause().getLocalizedMessage());
            stackTrace.addAll(getStackTrace(ex.getCause(), deep + 1));
        }
        return stackTrace;
    }

}
