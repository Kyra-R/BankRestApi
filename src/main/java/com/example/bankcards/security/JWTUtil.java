package com.example.bankcards.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;
import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;

import java.time.ZonedDateTime;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

@Component
public class JWTUtil {

    @Value("${jwt_secret}")
    private String secret;

    @Value("${jwt.issuer}")
    private String issuer;

    public UsernamePasswordAuthenticationToken validateTokenAndRetrieveAuth(String token) throws JWTVerificationException {

        JWTVerifier verifier = JWT.require(Algorithm.HMAC256(secret))
                .withSubject("User details")
                .withIssuer(issuer)
                .withClaimPresence("username")
                .build();

        DecodedJWT jwt = verifier.verify(token);

        String role = jwt.getClaim("username").asString();



        Set<SimpleGrantedAuthority> grantedValues = new HashSet<>();
        grantedValues.add(new SimpleGrantedAuthority(jwt.getClaim("username").asString()));

        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken("", "", grantedValues);
        return auth;
    }


    public String generateToken(String login) {
        Date expirationDate = Date.from(ZonedDateTime.now().plusMinutes(120).toInstant());

        return JWT.create()
                .withSubject("User details")
                .withClaim("username", login)
                .withIssuedAt(new Date())
                .withIssuer(issuer)
                .withExpiresAt(expirationDate)
                .sign(Algorithm.HMAC256(secret));
    }

    public String validateTokenAndRetrieveClaim(String token) throws JWTVerificationException {
        JWTVerifier verifier = JWT.require(Algorithm.HMAC256(secret))
                .withSubject("User details")
                .withIssuer(issuer)
                .build();

        DecodedJWT jwt = verifier.verify(token);
        return jwt.getClaim("username").asString();
    }
}
