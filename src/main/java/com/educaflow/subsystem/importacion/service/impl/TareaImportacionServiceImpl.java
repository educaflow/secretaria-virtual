package com.educaflow.subsystem.importacion.service.impl;

import com.axelor.db.Repository;
import com.axelor.db.modelservice.DefaultModelService;
import com.axelor.inject.Beans;
import com.educaflow.base.infrastructure.validation.messages.BusinessException;
import com.educaflow.base.infrastructure.validation.messages.BusinessMessage;
import com.educaflow.base.infrastructure.validation.messages.BusinessMessages;
import com.educaflow.base.util.MetaFileUtil;
import com.educaflow.base.util.XMLUtil;
import com.educaflow.subsystem.common.db.Centro;
import com.educaflow.subsystem.common.db.repo.CentroRepository;
import com.educaflow.subsystem.importacion.db.TareaImportacion;
import com.educaflow.subsystem.importacion.db.TipoFicheroImportacion;
import com.educaflow.subsystem.importacion.service.tipoimportador.ImportadorTipoFichero;
import com.educaflow.subsystem.importacion.service.TareaImportacionService;

import com.educaflow.subsystem.importacion.service.tipoimportador.impl.ImportadorProfesoresExternos;
import com.educaflow.subsystem.importacion.service.tipoimportador.impl.ImportadorUsuarioXml;
import org.w3c.dom.Element;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class TareaImportacionServiceImpl extends DefaultModelService<TareaImportacion> implements TareaImportacionService {

    private static final Map<TipoFicheroImportacion, ImportadorTipoFichero> IMPORTADORES = Map.of(
        TipoFicheroImportacion.PROFESORES,       new ImportadorUsuarioXml("docente",  "PROFESOR",  "/data-import/schemas/profesores.xsd"),
        TipoFicheroImportacion.ALUMNOS,          new ImportadorUsuarioXml("alumno",   "ALUMNO",    "/data-import/schemas/alumnos.xsd"),
        TipoFicheroImportacion.FAMILIARES,       new ImportadorUsuarioXml("familiar", "FAMILIAR",  "/data-import/schemas/familiares.xsd"),
        TipoFicheroImportacion.PROFESOR_EXTERNO, new ImportadorProfesoresExternos()
    );

    public TareaImportacionServiceImpl(Class<TareaImportacion> model, Repository repository) {
        super(model, repository);
    }

    @Override
    public TareaImportacion insert(TareaImportacion tareaImportacion) {
        List<String> logs = new ArrayList<>();

        try {
            BusinessMessages businessMessages = this.importar(tareaImportacion);

            for (BusinessMessage businessMessage : businessMessages) {
                logs.add(businessMessage.getMessage());
            }
            String logText = String.join(System.lineSeparator(), logs);

            tareaImportacion.setImportLog(logText);
            return super.insert(tareaImportacion);
        } catch (BusinessException e) {
            for (BusinessMessage businessMessage : e.getBusinessMessages()) {
                logs.add(businessMessage.getMessage());
            }
            String logText = String.join(System.lineSeparator(), logs);
            throw new RuntimeException("Error durante la importación: " + logText);
        }
    }

    @Override
    public TareaImportacion update(TareaImportacion newTareaImportacion, TareaImportacion oldTareaImportacion){
        throw new UnsupportedOperationException("No se permite modificar una tarea de importación. Crea una nueva tarea para realizar otra importación.");
    }

    @Override
    public void remove(TareaImportacion tareaImportacion) {
        throw new UnsupportedOperationException("No se permite eliminar una tarea de importación. Si quieres eliminar la referencia al fichero, borra el fichero desde su ubicación original.");
    }

    private BusinessMessages importar(TareaImportacion tareaImportacion) throws BusinessException {
        ImportadorTipoFichero importador = IMPORTADORES.get(tareaImportacion.getTipoFichero());
        if (importador == null) {
            throw new BusinessException(new BusinessMessage("Tipo de fichero sin importador: " + tareaImportacion.getTipoFichero()));
        }

        byte[] contenido = MetaFileUtil.downloadContent(tareaImportacion.getFichero());

        Centro centro;
        Integer curso;

        if (tareaImportacion.getTipoFichero() == TipoFicheroImportacion.PROFESOR_EXTERNO) {
            centro = tareaImportacion.getUsuario().getCentroActivo();
            if (centro == null) {
                throw new BusinessException(new BusinessMessage("El usuario no tiene un centro activo asignado"));
            }
            curso = centro.getCurso();
            if (curso == null) {
                throw new BusinessException(new BusinessMessage("El centro activo no tiene un curso académico configurado"));
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


}
