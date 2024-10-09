package in.mm.main.security.jwt;

import in.mm.main.security.services.UserDetailsImpl;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.UnsupportedJwtException;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import org.springframework.web.util.WebUtils;

import javax.crypto.SecretKey;
import java.security.Key;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;

@Component
public class JwtUtils {

    private static final Logger logger = LoggerFactory.getLogger(JwtUtils.class);

    @Value("${spring.app.jwtSecret}")
    private String jwtSecret;

    @Value("${spring.app.jwtExpirationMs}")
    private Long jwtExpirationMs;

    @Value("${spring.ecom.app.jwtCookieName}")
    private String jwtCookie;

    public String getJwtFromCookies(HttpServletRequest request){
        Cookie cookie = WebUtils.getCookie(request,jwtCookie);
        if (cookie != null) {
//            System.out.println("Cookie: "+cookie.getValue());
            return cookie.getValue(); //val is noting but a jwt token
        }else {
            return null;
        }
    }

    public ResponseCookie generateJwtCookie(UserDetailsImpl userPrinciple){
        String jwt = generateTokenFromUsername(userPrinciple.getUsername());
        ResponseCookie cookie = ResponseCookie.from(jwtCookie,jwt)
                .path("/api").maxAge(24 * 60 * 60) // the cookie is valid only for /api
                                                                // after this we setting the experition item 24hrs
                .httpOnly(false)
                .build();
        return cookie;
    }

    public ResponseCookie getCleanJwtCookie(){
        ResponseCookie cookie = ResponseCookie.from(jwtCookie,null).path("/api").build();
        return cookie;
    }

    public String generateTokenFromUsername(String username) {
        Instant now = Instant.now(); //Get the current time
        Instant expirationTime = now.plus(jwtExpirationMs, ChronoUnit.MILLIS); // Set the expiration time

        return Jwts.builder()
                .subject(username)
                .issuedAt(Date.from(now)) // Convert Instant to Date
                .expiration(Date.from(expirationTime)) // Convert Instant to Date
                .signWith(key())
                .compact();
    }

    public String getUsernameFromJwtToken(String token) {
        return Jwts.parser()
                .verifyWith((SecretKey) key())
                .build().parseSignedClaims(token)
                .getPayload().getSubject();
    }

    private Key key(){
//        byte[] keyBytes = Decoders.BASE64.decode(jwtSecret); // Assuming secretKey is a base64-encoded string
//        return Keys.hmacShaKeyFor(keyBytes);
        return Keys.hmacShaKeyFor(Decoders.BASE64.decode(jwtSecret));
    }

    public boolean validateJwtToken(String authToken) {
        try {
            System.out.println("Validate");
            Jwts.parser().verifyWith((SecretKey) key()).build().parseSignedClaims(authToken);
            return true;
        }catch (MalformedJwtException e){
            logger.error("Invalid JWT token : {}",e.getMessage());
        }catch (ExpiredJwtException e){
            logger.error("JWT token is expired : {}",e.getMessage());
        }catch (UnsupportedJwtException e){
            logger.error("JWT token is unsupported : {}",e.getMessage());
        }catch (IllegalArgumentException e){
            logger.error("JWT claims string is empty : {}",e.getMessage());
        }
        return false;
    }
}
