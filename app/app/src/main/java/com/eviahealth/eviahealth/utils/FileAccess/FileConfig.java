package com.eviahealth.eviahealth.utils.FileAccess;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public enum FileConfig {
    FC_SERIAL(FilePath.CONFIG_SERIAL.getNameFile()),
    FC_ADMIN(FilePath.CONFIG_ADMIN.getNameFile()),
    FC_TOKEN(FilePath.CONFIG_TOKEN.getNameFile()),
    FC_ENCUESTA(FilePath.CONFIG_ENCUESTA.getNameFile()),
    FC_DISPOSITIVOS(FilePath.CONFIG_DISPOSITIVOS.getNameFile()),
    FC_PACIENTE(FilePath.CONFIG_PACIENTE.getNameFile()),
    FC_LOG(FilePath.CONFIG_LOG.getNameFile());

    private String path;
    FileConfig(String path ) { this.path = path; }

    String getPath() {
        return this.path;
    }

    /**
     * Método para obtener la lista de paths de los ficheros que se consideran de configuración
     */
    public static List<String> getAllPaths() {
        return Arrays.stream(FileConfig.values())
                .map(FileConfig::getPath)
                .collect(Collectors.toList());
    }
}