package com.adama.api.service.user.abst;

import java.time.ZonedDateTime;
import java.util.Optional;

import javax.annotation.PostConstruct;
import javax.inject.Inject;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.util.Assert;

import com.adama.api.domain.user.AdamaUser;
import com.adama.api.domain.util.domain.abst.delete.DeleteEntityAbstract;
import com.adama.api.repository.user.AdamaUserRepositoryInterface;
import com.adama.api.repository.util.repository.AdamaMongoRepository;
import com.adama.api.security.TenantChecker;
import com.adama.api.service.user.AdamaUserServiceInterface;
import com.adama.api.service.util.service.abst.AdamaServiceAbstract;
import com.adama.api.util.random.RandomUtil;
import com.adama.api.util.security.SecurityUtils;

import lombok.extern.slf4j.Slf4j;

/**
 * Abstract Service class for managing users.
 */
@Slf4j
public abstract class AdamaUserServiceAbstract<D extends DeleteEntityAbstract, A extends AdamaUser<D>, R extends AdamaMongoRepository<A, String>> extends AdamaServiceAbstract<A, R> implements
		AdamaUserServiceInterface<A> {
	private PasswordEncoder passwordEncoder;
	private AdamaUserRepositoryInterface<D, A> userRepository;
	@Inject
	private TenantChecker tenantChecker;

	@Override
	@PostConstruct
	public abstract void init();

	@Override
	public Optional<A> completePasswordReset(String newPassword, String key) {
		log.debug("Reset user password for reset key {}", key);
		return userRepository.findOneByResetKey(key).filter(user -> {
			ZonedDateTime oneDayAgo = ZonedDateTime.now().minusHours(72);
			return user.getResetDate().isAfter(oneDayAgo);
		}).map(user -> {
			user.setPassword(passwordEncoder.encode(newPassword));
			user.setResetKey(null);
			user.setResetDate(null);
			userRepository.save(user);
			return user;
		});
	}

	@Override
	public Optional<A> requestPasswordReset(String mail) {
		log.debug("ask for requestPasswordReset with email : {}", mail);
		return userRepository.findOneByEmail(mail).filter(AdamaUser::getActive).map(user -> {
			user.setResetKey(RandomUtil.generateResetKey());
			user.setResetDate(ZonedDateTime.now());
			userRepository.save(user);
			return user;
		});
	}

	@Override
	public A createUser(A user) {
		if (user.getLangKey() == null) {
			user.setLangKey("en"); // default language
		}
		String encryptedPassword = passwordEncoder.encode(RandomUtil.generatePassword());
		user.setPassword(encryptedPassword);
		user.setResetKey(RandomUtil.generateResetKey());
		user.setResetDate(ZonedDateTime.now());
		if (tenantChecker.isTenantable(user)) {
			Assert.notNull(user.getTenant(), "For " + user.getAuthority() + " authority, the user must have a tenant");
		} else {
			// For authority with no tenant, the user must have no tenant
			user.setTenant(null);
		}
		A newUser = userRepository.save(user);
		log.debug("Created Information for User: {}", newUser);
		return newUser;
	}

	@Override
	public void changePassword(String password) {
		SecurityUtils.getCurrentUserLogin().ifPresent(login -> {
			userRepository.findOneByLogin(login).ifPresent(u -> {
				String encryptedPassword = passwordEncoder.encode(password);
				u.setPassword(encryptedPassword);
				userRepository.save(u);
				log.debug("Changed password for User: {}", u);
			});
		});
	}

	@Override
	public Optional<A> findOneByLogin(String login) {
		return userRepository.findOneByLogin(login);
	}

	@Override
	public Optional<A> findOneByEmail(String email) {
		return userRepository.findOneByEmail(email);
	}

	public void setPasswordEncoder(PasswordEncoder passwordEncoder) {
		this.passwordEncoder = passwordEncoder;
	}

	public void setUserRepository(AdamaUserRepositoryInterface<D, A> userRepository) {
		this.userRepository = userRepository;
	}

	@Override
	public Optional<A> findCurrent() {
		Optional<String> currentUserLogin = SecurityUtils.getCurrentUserLogin();
		if (!currentUserLogin.isPresent()) {
			return Optional.empty();
		}
		return findOneByLogin(currentUserLogin.get());
	}
}
