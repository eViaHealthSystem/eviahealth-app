package com.eviahealth.eviahealth.utils.log;

import com.eviahealth.eviahealth.utils.log.models.LogApp;

public class EVLogNull implements IEVLog {

    @Override
    public void log(LogApp logApp) {}

    @Override
    public void setFecha(String fecha) {}
}
