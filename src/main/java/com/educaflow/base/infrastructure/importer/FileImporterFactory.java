package com.educaflow.base.infrastructure.importer;

import com.axelor.data.xml.XMLImporter;
import com.axelor.inject.Beans;
import com.educaflow.base.infrastructure.importer.impl.XmlFileImporter;

import java.util.Map;

public class FileImporterFactory {

    private static final Map<FileType, FileImporter> importerMap = Map.of(
        FileType.XML, new XmlFileImporter()
    );

    public static FileImporter getFileImporter(FileType fileType) {
        //return importerMap.get(fileType);
        if (fileType == FileType.XML) {
            return Beans.get(XmlFileImporter.class);
        }
        return null;
    }
}
