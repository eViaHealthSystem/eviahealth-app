package com.eviahealth.eviahealth.meeting.initsdk;

import java.util.HashMap;
import java.util.Map;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;

public class JWTTokenGenerator {

//    private static final String APP_KEY = AuthConstants.SDK_APPKEY;
//    private static final String CLIENT_SECRET = AuthConstants.SDK_APPSECRET;

    /**
     * Genera un token JWT para las credenciales que necesita meeting de zoom
     * @param appKey
     * @param clienteSecret
     * @return
     */
    public static String generateToken(String appKey, String clienteSecret) {
        SignatureAlgorithm signatureAlgorithm = SignatureAlgorithm.HS256;

        try {
            Map<String, Object> claims = new HashMap<>();
            claims.put("appKey", appKey);
            claims.put("iat", System.currentTimeMillis() / 1000);
            claims.put("exp", System.currentTimeMillis() / 1000 + 86400); // 24 horas
            claims.put("tokenExp", System.currentTimeMillis() / 1000 + 3600); // 1 hora

            String jwttoken = Jwts.builder()
                    .setClaims(claims)
                    .signWith(signatureAlgorithm, clienteSecret.getBytes())
                    .compact();

            return jwttoken;
        }
        catch (Exception e) {
            return "none";
        }
    }

}
