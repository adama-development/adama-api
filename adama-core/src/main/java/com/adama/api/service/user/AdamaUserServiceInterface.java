package com.adama.api.service.user;

import java.util.Optional;

import com.adama.api.domain.user.AdamaUser;
import com.adama.api.domain.util.domain.abst.delete.DeleteEntityAbstract;

/**
 * Interface Service class for managing users.
 */
public interface AdamaUserServiceInterface<A extends AdamaUser<? extends DeleteEntityAbstract>> {
	/**
	 * Change the password if the user have a reset key and if the request to
	 * change password have been done within one day
	 * 
	 * @param newPassword
	 * @param key
	 * @return
	 */
	abstract Optional<A> completePasswordReset(String newPassword, String key);

	/**
	 * Prepare a reset Key for change the password
	 * 
	 * @param mail
	 * @return
	 */
	abstract Optional<A> requestPasswordReset(String mail);

	/**
	 * create a new user with password and reset key
	 * 
	 * @param managedUser
	 * @return
	 */
	abstract A createUser(A managedUser);

	/**
	 * Change password of an user
	 * 
	 * @param password
	 */
	abstract void changePassword(String password);

	/**
	 * Find one user by login
	 * 
	 * @param login
	 * @return
	 */
	Optional<A> findOneByLogin(String login);

	/**
	 * Find one user by email
	 * 
	 * @param email
	 * @return
	 */
	Optional<A> findOneByEmail(String email);

	//
	// /**
	// * Find all by Tenant
	// *
	// * @param tenant
	// * @return
	// */
	// List<A> findAllByClient(D tenant);
	/**
	 * @return current user
	 */
	Optional<A> findCurrent();
}