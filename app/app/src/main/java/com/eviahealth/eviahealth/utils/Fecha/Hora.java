package com.eviahealth.eviahealth.utils.Fecha;

public class Hora {
    private int hora;
    private int min;

    public Hora(int hora, int min) {
        this.hora = hora;
        this.min = min;
    }

    public Hora(String hora) {
        String[] parts = hora.split(":");
        this.hora = Integer.parseInt(parts[0]);
        this.min = Integer.parseInt(parts[1]);
    }

    public int getHora() {
        return hora;
    }

    public int getMin() {
        return min;
    }

    @Override
    public String toString() {
        return String.format("%02d", this.hora) + ":" + String.format("%02d", this.min);
    }

}
