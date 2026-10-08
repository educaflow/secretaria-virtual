package com.educaflow.base.util;

import com.axelor.meta.MetaFiles;
import com.axelor.meta.db.MetaFile;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;

class MetaFileUtilTest {

    @TempDir
    Path tempDir;

    private MockedStatic<MetaFiles> metaFiles;
    private MetaFile metaFile;

    @BeforeEach
    void setUp() {
        metaFiles = Mockito.mockStatic(MetaFiles.class);
        metaFile = new MetaFile();
    }

    @AfterEach
    void tearDown() {
        metaFiles.close();
    }

    @Test
    void getSize_ficheroExistente_devuelveSuTamanoReal() throws IOException {
        byte[] contenido = {1, 2, 3, 4, 5};
        Path fichero = Files.write(tempDir.resolve("fichero.bin"), contenido);
        metaFile.setFileSize(1L);
        metaFiles.when(() -> MetaFiles.getPath(metaFile)).thenReturn(fichero);

        assertEquals(contenido.length, MetaFileUtil.getSize(metaFile));
    }

    @Test
    void getSize_ficheroVacio_devuelveCero() throws IOException {
        Path fichero = Files.createFile(tempDir.resolve("vacio.bin"));
        metaFiles.when(() -> MetaFiles.getPath(metaFile)).thenReturn(fichero);

        assertEquals(0L, MetaFileUtil.getSize(metaFile));
    }

    @Test
    void getSize_ficheroInexistente_lanzaUncheckedIOException() {
        metaFiles.when(() -> MetaFiles.getPath(metaFile)).thenReturn(tempDir.resolve("inexistente.bin"));

        UncheckedIOException ex = assertThrows(UncheckedIOException.class, () -> MetaFileUtil.getSize(metaFile));

        assertInstanceOf(IOException.class, ex.getCause());
    }
}
