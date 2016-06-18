package com.adama.api.web.rest.login.abst;

import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.Collection;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import com.adama.api.config.AdamaProperties;
import com.adama.api.domain.user.AdamaUser;
import com.adama.api.domain.util.domain.abst.delete.DeleteEntityAbstract;
import com.adama.api.security.jwt.abstr.TokenProviderAbstract;
import com.adama.api.service.user.AdamaUserServiceInterface;
import com.adama.api.util.jwt.JWTUtils;
import com.adama.api.web.rest.login.AdamaLoginResourceInterface;
import com.adama.api.web.rest.login.dto.LoginDTO;
import com.adama.api.web.rest.login.dto.RefreshDTO;
import com.adama.api.web.rest.login.dto.TokenDTO;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;

public abstract class AdamaLoginResourceAbstract<D extends DeleteEntityAbstract, A extends AdamaUser<D>> implements AdamaLoginResourceInterface<A> {

	@Inject
	private TokenProviderAbstract<D, A> tokenProvider;

	@Inject
	private AuthenticationManager authenticationManager;

	@Inject
	private AdamaUserServiceInterface<A> userService;

	@Inject
	private AdamaProperties adamaProperties;

	@Override
	@RequestMapping(value = "/authenticate", method = RequestMethod.POST)
	public ResponseEntity<?> authorize(@Valid @RequestBody LoginDTO loginDTO, HttpServletResponse response) {
		UsernamePasswordAuthenticationToken authenticationToken = new UsernamePasswordAuthenticationToken(loginDTO.getUsername(),
				loginDTO.getPassword());
		try {
			Authentication authentication = authenticationManager.authenticate(authenticationToken);
			SecurityContextHolder.getContext().setAuthentication(authentication);
			boolean rememberMe = (loginDTO.getRememberMe() == null) ? false : loginDTO.getRememberMe();

			ZonedDateTime validityDate = tokenProvider.getExpiredTokenDate(rememberMe);
			String accessToken = tokenProvider.createAccessToken(authentication, validityDate);
			String refreshToken = tokenProvider.createRefreshToken(authentication);
			TokenDTO tokenDTO = new TokenDTO();
			tokenDTO.setAccessToken(accessToken);
			tokenDTO.setRefreshToken(refreshToken);
			tokenDTO.setRememberMe(loginDTO.getRememberMe());

			response.addHeader(JWTUtils.AUTHORIZATION_HEADER, "Bearer " + accessToken);
			return ResponseEntity.ok(tokenDTO);
		} catch (AuthenticationException exception) {
			return new ResponseEntity<>(exception.getLocalizedMessage(), HttpStatus.UNAUTHORIZED);
		}
	}

	@Override
	@RequestMapping(value = "/refresh", method = RequestMethod.POST)
	public ResponseEntity<?> refresh(@Valid @RequestBody RefreshDTO refreshDTO, HttpServletRequest request) {
		return JWTUtils
				.resolveToken(request)
				.map(token -> {
					try {
						Jwts.parser().setSigningKey(adamaProperties.getSecurity().getAuthentication().getJwt().getSecret()).parseClaimsJws(token)
								.getBody();
					} catch (ExpiredJwtException eje) {
						Claims claims = eje.getClaims();
						String principal = claims.getSubject();
						Collection<? extends GrantedAuthority> authorities = Arrays
								.asList(claims.get(TokenProviderAbstract.AUTHORITIES_KEY).toString().split(",")).stream()
								.map(authority -> new SimpleGrantedAuthority(authority)).collect(Collectors.toList());
						UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(principal, "", authorities);
						Object details = claims.get(TokenProviderAbstract.TENANT_ID);
						if (details != null) {
							authentication.setDetails((String) details);
						}
						return userService.findOneByLogin(authentication.getName()).map(user -> {
							if (refreshDTO != null && StringUtils.hasText(refreshDTO.getRefreshToken())) {
								if (tokenProvider.validateRefreshToken(refreshDTO.getRefreshToken(), user.getLogin(), user.getResetDate())) {
									boolean rememberMe = (refreshDTO.getRememberMe() == null) ? false : refreshDTO.getRememberMe();
									ZonedDateTime validityDate = tokenProvider.getExpiredTokenDate(rememberMe);
									String accessToken = tokenProvider.createAccessToken(authentication, validityDate);
									TokenDTO tokenDTO = new TokenDTO();
									tokenDTO.setAccessToken(accessToken);
									tokenDTO.setRefreshToken(refreshDTO.getRefreshToken());
									return new ResponseEntity<>(tokenDTO, HttpStatus.OK);
								} else {
									return new ResponseEntity<>("Refresh Token is not valid", HttpStatus.BAD_REQUEST);
								}
							}
							return new ResponseEntity<>("Refresh Token not provided", HttpStatus.BAD_REQUEST);
						}).orElse(new ResponseEntity<>("Cannot find user in database", HttpStatus.BAD_REQUEST));
					}
					return new ResponseEntity<>("Access Token not expired", HttpStatus.BAD_REQUEST);
				}).orElse(new ResponseEntity<>("Current Access Token is not valid", HttpStatus.BAD_REQUEST));

	}
}
