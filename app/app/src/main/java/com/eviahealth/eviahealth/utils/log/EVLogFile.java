package com.eviahealth.eviahealth.utils.log;

import com.eviahealth.eviahealth.utils.FileAccess.FileAccess;
import com.eviahealth.eviahealth.utils.FileAccess.FilePath;
import com.eviahealth.eviahealth.utils.log.models.LogApp;

import java.io.IOException;

public class EVLogFile implements IEVLog {

    private String fecha = null;

    @Override
    public void log(LogApp logApp) {
        try {
            FileAccess.appendToLogFile(FileAccess.getPATH(), FilePath.EVLOG, this.fecha, logApp);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void setFecha(String fecha) {
        this.fecha = fecha;
    }
}
