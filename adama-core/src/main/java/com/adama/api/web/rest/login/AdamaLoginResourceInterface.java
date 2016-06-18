package com.adama.api.web.rest.login;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import com.adama.api.domain.user.AdamaUser;
import com.adama.api.domain.util.domain.abst.delete.DeleteEntityAbstract;
import com.adama.api.web.rest.login.dto.LoginDTO;
import com.adama.api.web.rest.login.dto.RefreshDTO;
import com.adama.api.web.rest.login.dto.TokenDTO;

import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;

/**
 * Rest resource for login and refresh token
 */
public interface AdamaLoginResourceInterface<U extends AdamaUser<? extends DeleteEntityAbstract>> {

	/**
	 * Authorize a user and give a token to access the resources and a
	 * refreshtoken to refresh this token
	 * 
	 * @param loginDTO
	 * @param response
	 * @return {@link TokenDTO}
	 */

	@ApiOperation(value = "Authorize a user and give a token to access the resources and a refreshtoken to refresh this token", produces = MediaType.APPLICATION_JSON_VALUE)
	@ApiResponses(value = { @ApiResponse(code = 200, response = TokenDTO.class, message = "Authorized"),
			@ApiResponse(code = 401, message = "Not Authorized", response = String.class)})
	public abstract ResponseEntity<?> authorize(LoginDTO loginDTO, HttpServletResponse response);

	/**
	 * Refresh a token
	 * 
	 * @param refreshDTO
	 * @param request
	 * @return {@link TokenDTO}
	 */
	@ApiOperation(value = "Refresh a token", produces = MediaType.APPLICATION_JSON_VALUE)
	@ApiResponses(value = { @ApiResponse(code = 200, response = TokenDTO.class, message = "Authorized"),
			@ApiResponse(code = 400, message = "Refresh Token, Access Token or User not valid", response = String.class) })
	public abstract ResponseEntity<?> refresh(RefreshDTO refreshDTO, HttpServletRequest request);

}