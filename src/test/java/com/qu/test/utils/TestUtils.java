package com.qu.test.utils;

import org.jboss.logging.Logger;

import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;

import static java.lang.String.format;

public class TestUtils {
    private final static Logger LOG = Logger.getLogger(TestUtils.class);

    public static String readTestResourceAsString(String resourceRelativePath){
        Path workingDir = FileSystems.getDefault().getPath(".").toAbsolutePath();
        Path path = workingDir.resolve("src/test/resources").resolve(resourceRelativePath);
        try {
            return Files.readString(path);
        } catch (IOException e) {
            LOG.error(e);
            throw new RuntimeException(format("Failed to read resource[%s]!", path.toAbsolutePath().toString()), e);
        }
    }
}
