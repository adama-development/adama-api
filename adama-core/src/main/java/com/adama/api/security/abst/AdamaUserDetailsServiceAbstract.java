package com.adama.api.security.abst;

import java.util.Collections;
import java.util.Optional;

import javax.annotation.PostConstruct;

import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

import com.adama.api.config.AdamaProperties;
import com.adama.api.domain.user.AdamaUser;
import com.adama.api.domain.util.domain.abst.delete.DeleteEntityAbstract;
import com.adama.api.repository.user.AdamaUserRepositoryInterface;
import com.adama.api.security.AdamaAuthoritiesConstants;

import lombok.extern.slf4j.Slf4j;

/**
 * Authenticate a user from the database.
 */
@Slf4j
public abstract class AdamaUserDetailsServiceAbstract<D extends DeleteEntityAbstract,A extends AdamaUser<D>> implements UserDetailsService {

    private AdamaUserRepositoryInterface<D,A> userRepository;
    private PasswordEncoder passwordEncoder;
    private AdamaProperties adamaProperties;
    
    @PostConstruct
    public abstract void init();

    @Override
    @Transactional
    public UserDetails loadUserByUsername(final String login) {
	log.debug("Authenticating {}", login);
	String lowercaseLogin = login.toLowerCase();
	Optional<A> userFromDatabase = userRepository.findOneByLogin(lowercaseLogin);

	if (!userFromDatabase.isPresent() && userRepository.count() == 0) {
	    // if no user created, we give access as admin (for the first login)
	    return new org.springframework.security.core.userdetails.User(adamaProperties.getSecurity().getDefaultFirstLogin(), passwordEncoder.encode(adamaProperties.getSecurity().getDefaultFirstPassword()), Collections.singletonList(new SimpleGrantedAuthority(AdamaAuthoritiesConstants.ADMIN)));
	}
	return userFromDatabase.map(user -> {
	    return new org.springframework.security.core.userdetails.User(lowercaseLogin, user.getPassword(), Collections.singletonList(user.getAuthority()));
	}).orElseThrow(() -> new UsernameNotFoundException("User " + lowercaseLogin + " was not found in the database"));

    }

    public void setUserRepository(AdamaUserRepositoryInterface<D,A> userRepository) {
        this.userRepository = userRepository;
    }

    public void setPasswordEncoder(PasswordEncoder passwordEncoder) {
        this.passwordEncoder = passwordEncoder;
    }

    public void setAdamaProperties(AdamaProperties adamaProperties) {
        this.adamaProperties = adamaProperties;
    }
}
