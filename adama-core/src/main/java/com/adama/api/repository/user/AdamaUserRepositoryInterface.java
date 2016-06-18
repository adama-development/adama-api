package com.adama.api.repository.user;

import java.util.Optional;

import com.adama.api.domain.user.AdamaUser;
import com.adama.api.domain.util.domain.abst.delete.DeleteEntityAbstract;
import com.adama.api.repository.util.repository.AdamaMongoRepository;

/**
 * Spring Data MongoDB interface for the AdamaUser entity.
 */
public interface AdamaUserRepositoryInterface<D extends DeleteEntityAbstract,A extends AdamaUser<D>> extends AdamaMongoRepository<A, String>{

    Optional<A> findOneByResetKey(String resetKey);

    Optional<A> findOneByEmail(String email);

    Optional<A> findOneByLogin(String login);

    long count();

}
