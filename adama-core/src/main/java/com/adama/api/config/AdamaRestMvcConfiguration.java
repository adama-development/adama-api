package com.adama.api.config;

import java.util.List;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter;

import com.adama.api.domain.util.domain.abst.audit.AuditingEntityAbstract;

@Configuration
public class AdamaRestMvcConfiguration extends WebMvcConfigurerAdapter {
	@Override
	public void addArgumentResolvers(List<HandlerMethodArgumentResolver> argumentResolvers) {
		PageableHandlerMethodArgumentResolver resolver = new PageableHandlerMethodArgumentResolver();
		resolver.setFallbackPageable(new PageRequest(0, 10, new Sort(Sort.Direction.DESC, AuditingEntityAbstract.LASTMODIFIEDDATE_FIELD_NAME)));
		argumentResolvers.add(resolver);
	}
}
