package com.eviahealth.eviahealth.models.devices;

public class EquipoPaciente {

    private Dispositivo dispositivo;
    private boolean enabled;
    private String desc;
    private String extra;

    public EquipoPaciente(Dispositivo disp, boolean enabled, String desc, String extra) {
        this.dispositivo = disp;
        this.enabled = enabled;
        this.desc = desc;
        this.extra = extra;
    }

    public Dispositivo getDispositivo() {
        return dispositivo;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public String getDesc() {
        return desc;
    }

    public String getExtra() {
        return extra;
    }

    public void setExtra(String extra) {
        this.extra = extra;
    }
}
