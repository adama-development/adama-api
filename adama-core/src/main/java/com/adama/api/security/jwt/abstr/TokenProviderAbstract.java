package com.adama.api.security.jwt.abstr;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import com.adama.api.config.AdamaProperties;
import com.adama.api.domain.user.AdamaUser;
import com.adama.api.domain.util.domain.abst.delete.DeleteEntityAbstract;
import com.adama.api.service.user.AdamaUserServiceInterface;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public abstract class TokenProviderAbstract<D extends DeleteEntityAbstract, A extends AdamaUser<D>> {

    public static final String AUTHORITIES_KEY = "auth";
    public static final String TENANT_ID = "tenant";

    private AdamaProperties adamaProperties;
    private AdamaUserServiceInterface<A> userService;

    @PostConstruct
    public abstract void init();

    public String createAccessToken(Authentication authentication, ZonedDateTime validity) {
	String authorities = authentication.getAuthorities().stream().map(authority -> authority.getAuthority()).collect(Collectors.joining(","));
	Date expiredDate = Date.from(validity.toInstant());
	// if we get the user with a tenant, we add the tenant Id in the claim
	Optional<A> user = userService.findOneByLogin(authentication.getName());
	return user.filter(u -> u.getTenant() != null).map(u -> Jwts.builder().setSubject(authentication.getName()).claim(AUTHORITIES_KEY, authorities).claim(TENANT_ID, u.getTenant().getId()).signWith(SignatureAlgorithm.HS512, adamaProperties.getSecurity().getAuthentication().getJwt().getSecret()).setExpiration(expiredDate).compact())
		.orElse(Jwts.builder().setSubject(authentication.getName()).claim(AUTHORITIES_KEY, authorities).signWith(SignatureAlgorithm.HS512, adamaProperties.getSecurity().getAuthentication().getJwt().getSecret()).setExpiration(expiredDate).compact());
    }

    public String createRefreshToken(Authentication authentication) {
	Date expiredDate = Date.from(ZonedDateTime.now().plusSeconds(adamaProperties.getSecurity().getAuthentication().getJwt().getRefreshTokenValidityInSeconds()).toInstant());
	String authorities = authentication.getAuthorities().stream().map(authority -> authority.getAuthority()).collect(Collectors.joining(","));
	return Jwts.builder().setSubject(authentication.getName()).claim(AUTHORITIES_KEY, authorities).signWith(SignatureAlgorithm.HS512, adamaProperties.getSecurity().getAuthentication().getJwt().getSecret()).setExpiration(expiredDate).setIssuedAt(Date.from(ZonedDateTime.now().toInstant())).compact();
    }

    public Authentication getAuthentication(String token) {
	Claims claims = Jwts.parser().setSigningKey(adamaProperties.getSecurity().getAuthentication().getJwt().getSecret()).parseClaimsJws(token).getBody();
	String principal = claims.getSubject();
	Collection<? extends GrantedAuthority> authorities = Arrays.asList(claims.get(AUTHORITIES_KEY).toString().split(",")).stream().map(authority -> new SimpleGrantedAuthority(authority)).collect(Collectors.toList());
	UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(principal, "", authorities);
	Object details = claims.get(TENANT_ID);
	if (details != null) {
	    authentication.setDetails((String) details);
	}
	return authentication;
    }

    public ZonedDateTime getExpiredTokenDate(Boolean rememberMe) {
	final ZonedDateTime validity;
	if (rememberMe) {
	    validity = ZonedDateTime.now().plusSeconds(adamaProperties.getSecurity().getAuthentication().getJwt().getTokenValidityInSecondsForRememberMe());
	} else {
	    validity = ZonedDateTime.now().plusSeconds(adamaProperties.getSecurity().getAuthentication().getJwt().getTokenValidityInSeconds());
	}
	return validity;
    }

    public boolean validateToken(String authToken) throws JwtException {
	// if any problem with the token an exception will be thrown: throws ExpiredJwtException, UnsupportedJwtException, MalformedJwtException, SignatureException;
	Jwts.parser().setSigningKey(adamaProperties.getSecurity().getAuthentication().getJwt().getSecret()).parseClaimsJws(authToken);
	return true;
    }

    public Boolean validateRefreshToken(String token, String login, ZonedDateTime lastPasswordReset) {
	try {
	    final Claims claims = Jwts.parser().setSigningKey(adamaProperties.getSecurity().getAuthentication().getJwt().getSecret()).parseClaimsJws(token).getBody();
	    final ZonedDateTime created = ZonedDateTime.from(claims.getIssuedAt().toInstant().atZone(ZoneId.systemDefault()));
	    final String username = claims.getSubject();
	    return (username.equals(login) && !(this.isCreatedBeforeLastPasswordReset(created, lastPasswordReset)) && (!(this.isTokenExpired(claims))));
	} catch (JwtException e) {
	    log.info("Invalid Refresh Token: " + e.getMessage());
	    return false;
	}
    }

    public Boolean isAccessTokenExpired(String token) {
	try {
	    Claims claims = Jwts.parser().setSigningKey(adamaProperties.getSecurity().getAuthentication().getJwt().getSecret()).parseClaimsJws(token).getBody();
	    LocalDateTime expiration = LocalDateTime.ofInstant(claims.getExpiration().toInstant(), ZoneId.systemDefault());
	    return expiration.isBefore(LocalDateTime.now());
	} catch (ExpiredJwtException eje) {
	    return true;
	} catch (JwtException je) {
	    return false;
	}
    }

    private Boolean isCreatedBeforeLastPasswordReset(ZonedDateTime created, ZonedDateTime lastPasswordReset) {
	return (lastPasswordReset != null && created.isBefore(lastPasswordReset));
    }

    private Boolean isTokenExpired(Claims claims) {
	LocalDateTime expiration = LocalDateTime.ofInstant(claims.getExpiration().toInstant(), ZoneId.systemDefault());
	return expiration.isBefore(LocalDateTime.now());
    }

    public void setAdamaProperties(AdamaProperties adamaProperties) {
	this.adamaProperties = adamaProperties;
    }

    public void setUserService(AdamaUserServiceInterface<A> userService) {
	this.userService = userService;
    }

}
