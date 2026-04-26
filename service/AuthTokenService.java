package service;

import java.security.MessageDigest;
import java.util.Base64;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class AuthTokenService {

    private static final String SECRET_KEY = System.getenv("JWT_SECRET");
    private static final long EXPIRATION_MS = 3600000;

    private String generateSignature(String data) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest((data + SECRET_KEY).getBytes());
            return Base64.getEncoder().encodeToString(hash);
        } catch (Exception e) {
            return null;
        }
    }

    public String generateToken(int userId, String username) {
        long now = System.currentTimeMillis();
        long expiry = now + EXPIRATION_MS;
        
        String payload = userId + ":" + username + ":" + now + ":" + expiry;
        String signature = generateSignature(payload);
        
        String token = Base64.getEncoder().encodeToString(
                (payload + "." + signature).getBytes());
        
        return token.replaceAll("\\n", "").replaceAll("\\r", "");
    }

    public boolean validateToken(String token) {
        try {
            String decoded = new String(Base64.getDecoder().decode(token));
            String[] parts = decoded.split("\\.");
            if (parts.length != 2) {
                return false;
            }
            
            String payload = parts[0];
            String signature = parts[1];
            
            String expectedSig = generateSignature(payload);
            if (!signature.equals(expectedSig)) {
                return false;
            }
            
            String[] payloadParts = payload.split(":");
            if (payloadParts.length != 4) {
                return false;
            }
            
            long expiry = Long.parseLong(payloadParts[3]);
            if (System.currentTimeMillis() > expiry) {
                return false;
            }
            
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public Map<String, Object> getTokenClaims(String token) {
        try {
            String decoded = new String(Base64.getDecoder().decode(token));
            String[] parts = decoded.split("\\.");
            if (parts.length != 2) {
                return null;
            }
            
            String[] payloadParts = parts[0].split(":");
            if (payloadParts.length != 4) {
                return null;
            }
            
            Map<String, Object> result = new HashMap<>();
            result.put("userId", Integer.parseInt(payloadParts[0]));
            result.put("username", payloadParts[1]);
            result.put("issuedAt", new Date(Long.parseLong(payloadParts[2])));
            result.put("expiration", new Date(Long.parseLong(payloadParts[3])));
            return result;
        } catch (Exception e) {
            return null;
        }
    }

    public int getUserIdFromToken(String token) {
        Map<String, Object> claims = getTokenClaims(token);
        if (claims != null && claims.get("userId") != null) {
            return (int) claims.get("userId");
        }
        return -1;
    }

    public String getUsernameFromToken(String token) {
        Map<String, Object> claims = getTokenClaims(token);
        if (claims != null && claims.get("username") != null) {
            return (String) claims.get("username");
        }
        return null;
    }

    public boolean isTokenExpired(String token) {
        Map<String, Object> claims = getTokenClaims(token);
        if (claims == null || claims.get("expiration") == null) {
            return true;
        }
        return new Date().after((Date) claims.get("expiration"));
    }

    public String refreshToken(String token) {
        Map<String, Object> claims = getTokenClaims(token);
        if (claims == null) {
            return null;
        }
        
        int userId = (int) claims.get("userId");
        String username = (String) claims.get("username");
        
        return generateToken(userId, username);
    }
}