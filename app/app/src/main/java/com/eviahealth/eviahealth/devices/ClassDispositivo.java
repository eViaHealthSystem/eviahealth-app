package com.eviahealth.eviahealth.devices;

import com.eviahealth.eviahealth.ui.ensayo.MonitorPulmonar.get_dataLUNG;
import com.eviahealth.eviahealth.ui.ensayo.bascula.bf600.get_dataBF600;
import com.eviahealth.eviahealth.ui.ensayo.bascula.gbs2012b.get_dataGBS2012B;
import com.eviahealth.eviahealth.ui.ensayo.bascula.hs2s.get_dataHS2S;
import com.eviahealth.eviahealth.ui.ensayo.ecg.get_data2Kardia6L;
import com.eviahealth.eviahealth.ui.ensayo.oximetro.po3m.get_dataPO3M;
import com.eviahealth.eviahealth.ui.ensayo.oximetro.po60.get_dataPO60;
import com.eviahealth.eviahealth.ui.ensayo.pulsera.am4.get_dataAM4;
import com.eviahealth.eviahealth.ui.ensayo.pulsera.mambo6.get_dataMAMBO6;
import com.eviahealth.eviahealth.ui.ensayo.tensiometro.bc54.get_dataBC54;
import com.eviahealth.eviahealth.ui.ensayo.tensiometro.bc85.get_dataBC85;
import com.eviahealth.eviahealth.ui.ensayo.tensiometro.bc87.get_dataBC87;
import com.eviahealth.eviahealth.ui.ensayo.tensiometro.bm57.get_dataBM57;
import com.eviahealth.eviahealth.ui.ensayo.tensiometro.bp3l.get_dataBP3L;
import com.eviahealth.eviahealth.ui.ensayo.termometro.ft95.get_dataFT95;
import com.eviahealth.eviahealth.ui.ensayo.termometro.nt13b.get_dataNT13B;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

public class ClassDispositivo {

    public static List<String> devices_OXI = new ArrayList<>(Arrays.asList("PO3M","PO60"));
    public static List<String> devices_TEN = new ArrayList<>(Arrays.asList("BP3L","BC54","BC85","BC87","BM57"));
    public static List<String> devices_ACT = new ArrayList<>(Arrays.asList("AM4","MAMBO6"));
    public static List<String> devices_TER = new ArrayList<>(Arrays.asList("NT13B","FT95"));
    public static List<String> devices_BAS = new ArrayList<>(Arrays.asList("HS2S","BF600","GBS2012B"));
    public static List<String> devices_MONPUL = new ArrayList<>(Arrays.asList("LUNG"));
    public static List<String> devices_ECG= new ArrayList<>(Arrays.asList("K6L"));

    public static HashMap<String, Class> ActivityDevice = new HashMap();
    static {
        // Oximetros
        ActivityDevice.put("PO3M", get_dataPO3M.class);
        ActivityDevice.put("PO60", get_dataPO60.class);

        //Tensiometros
        ActivityDevice.put("BP3L", get_dataBP3L.class);
        ActivityDevice.put("BC54", get_dataBC54.class);
        ActivityDevice.put("BC85", get_dataBC85.class);
        ActivityDevice.put("BC87", get_dataBC87.class);
        ActivityDevice.put("BM57", get_dataBM57.class);

        //Actividad
        ActivityDevice.put("AM4", get_dataAM4.class);
        ActivityDevice.put("MAMBO6", get_dataMAMBO6.class);

        //Termómetros
        ActivityDevice.put("NT13B", get_dataNT13B.class);
        ActivityDevice.put("FT95", get_dataFT95.class);

        //Básculas
        ActivityDevice.put("HS2S", get_dataHS2S.class);
        ActivityDevice.put("BF600", get_dataBF600.class);
        ActivityDevice.put("GBS2012B", get_dataGBS2012B.class);

        //Monitor Pulmonar
        ActivityDevice.put("LUNG", get_dataLUNG.class);

        //ECG
        ActivityDevice.put("K6L", get_data2Kardia6L.class);
    }
}
