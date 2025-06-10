package com.eviahealth.eviahealth.models.alivecor;

import com.alivecor.api.AliveCorServer;

public interface AuthAlivecor {

    /*
    Hacer un POST a:    https://alivecor.eviahealth.net/token
    Body:
        {
         "bundleId": "com.eviahealth.eviahealth",
         "partnerId": "I6KjBh3K7cU6vodYGbh5d6450c13htcp",
         "patientMrn": "12345",
         "teamId": "Kmo7ifJa6uhnXssd7MkYd6450bxfv0f7"
        }

     */

    public final static AliveCorServer TYPE_SERVER_ALIVECOR = AliveCorServer.STAGING_US;      // SERVIDOR DESARROLLO
//    public final static AliveCorServer TYPE_SERVER_ALIVECOR = AliveCorServer.PRODUCTION_US;   // SERVIDOR PRODUCCION

    public final static String API_ALIVECOR = "https://alivecor.eviahealth.net/token";
    public final static String BUNDLEID = "com.eviahealth.eviahealth";
    public final static String PARTNERID = "I6KjBh3K7cU6vodYGbh5d6450c13htcp";
    public final static String PATIENTMRN = "12345";
    public final static String TEAMID = "Kmo7ifJa6uhnXssd7MkYd6450bxfv0f7";

}
