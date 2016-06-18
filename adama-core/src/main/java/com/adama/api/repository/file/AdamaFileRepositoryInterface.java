package com.adama.api.repository.file;

import com.adama.api.domain.file.AdamaFileAbstract;
import com.adama.api.repository.util.repository.AdamaMongoRepository;

public interface AdamaFileRepositoryInterface<A extends AdamaFileAbstract> extends AdamaMongoRepository<A, String> {
}
