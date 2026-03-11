package com.educaflow.base.infrastructure.importer.util;

import com.axelor.data.ImportTask;

public class ImportTaskUtil {

    public static ImportTask getImportTask() {
        return new CustomImportTask();
    }
}
