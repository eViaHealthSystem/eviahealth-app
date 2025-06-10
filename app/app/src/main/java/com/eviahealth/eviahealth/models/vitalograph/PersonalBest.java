package com.eviahealth.eviahealth.models.vitalograph;

import com.eviahealth.eviahealth.api.BackendConector.ApiMethods;

public class PersonalBest {

    public static Integer calculateFEV1PersonalBest(String idPaciente, Patient patient) {

//        Double fev1pb = 0.0;
//        Integer age = patient.getAge();
//        Integer height = patient.getHeight();
//
//        // height en cm
//        if (patient.getGender().equals("Hombre")) {
//            fev1pb = (0.0414 * height) - (0.0244 * age) - 2.190;
//        }
//        else {
//            fev1pb = (0.0342 * height) - (0.0255 * age) - 1.578;
//        }
//
//        Integer fev1pbInt = (int)(fev1pb * 100);

        Integer fev1pbInt = ApiMethods.generateFEV1PB(idPaciente, patient);
        return fev1pbInt;
    }
}
