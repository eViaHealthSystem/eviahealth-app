package com.eviahealth.eviahealth.utils.log;

import com.eviahealth.eviahealth.utils.log.models.LogApp;

public interface IEVLog {
    void log(LogApp logApp);
    void setFecha(String fecha);
}
