package com.hccake.ballcat.common.swagger;

import com.hccake.ballcat.common.swagger.constant.SwaggerConstants;
import com.hccake.ballcat.common.swagger.property.SwaggerProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import springfox.documentation.builders.ApiInfoBuilder;
import springfox.documentation.builders.PathSelectors;
import springfox.documentation.builders.RequestHandlerSelectors;
import springfox.documentation.service.*;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spi.service.contexts.SecurityContext;
import springfox.documentation.spring.web.plugins.Docket;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * @author Hccake
 * @version 1.0
 * @date 2019/11/1 19:43
 */
@RequiredArgsConstructor
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(SwaggerProperties.class)
public class SwaggerConfiguration {

	private final SwaggerProperties swaggerProperties;

	@Bean
	public Docket api() {
		// @formatter:off
		// 1. 文档信息构建
		Docket docket = new Docket(swaggerProperties.getDocumentationType().getType())
				.host(swaggerProperties.getHost())
				.apiInfo(apiInfo())
				.groupName(swaggerProperties.getGroupName());

		// 2. 安全配置
		docket.securitySchemes(securitySchema())
				.securityContexts(securityContext());

		// 3. 文档筛选
		docket.select()
			.apis(RequestHandlerSelectors.basePackage(swaggerProperties.getBasePackage()))
			.paths(paths())
			.build();

		return docket;
		// @formatter:on

	}

	private Predicate<String> paths() {
		// base-path 和 exclude-path 的默认值处理
		if (swaggerProperties.getBasePath().isEmpty()) {
			swaggerProperties.getBasePath().add(SwaggerConstants.DEFAULT_BASE_PATH);
		}
		if (swaggerProperties.getExcludePath().isEmpty()) {
			swaggerProperties.getExcludePath().addAll(SwaggerConstants.DEFAULT_EXCLUDE_PATH);
		}

		List<Predicate<String>> basePath = new ArrayList<>();
		for (String path : swaggerProperties.getBasePath()) {
			basePath.add(PathSelectors.ant(path));
		}
		List<Predicate<String>> excludePath = new ArrayList<>();
		for (String path : swaggerProperties.getExcludePath()) {
			excludePath.add(PathSelectors.ant(path));
		}
		// 必须满足basePath 且不满足 exclude-path
		return s -> basePath.stream().anyMatch(x -> x.test(s)) && excludePath.stream().noneMatch(x -> x.test(s));
	}

	private ApiInfo apiInfo() {
		// @formatter:off
		return new ApiInfoBuilder()
				.title(swaggerProperties.getTitle())
				.description(swaggerProperties.getDescription())
				.license(swaggerProperties.getLicense())
				.licenseUrl(swaggerProperties.getLicenseUrl())
				.termsOfServiceUrl(swaggerProperties.getTermsOfServiceUrl())
				.contact(new Contact(swaggerProperties.getContact().getName(),
						swaggerProperties.getContact().getUrl(),
						swaggerProperties.getContact().getEmail()))
				.version(swaggerProperties.getVersion())
				.build();
		// @formatter:on
	}

	private List<SecurityScheme> securitySchema() {
		SwaggerProperties.Authorization authorizationProps = swaggerProperties.getAuthorization();

		List<AuthorizationScope> authorizationScopeList = authorizationProps.getAuthorizationScopeList().stream()
				.map(scope -> new AuthorizationScope(scope.getScope(), scope.getDescription()))
				.collect(Collectors.toList());

		String tokenUrl = authorizationProps.getTokenUrl();

		DocumentationType documentationType = swaggerProperties.getDocumentationType().getType();
		SecurityScheme securityScheme;
		if (documentationType.equals(DocumentationType.SWAGGER_2)) {
			// swagger2 OAuth2
			List<GrantType> grantTypes = Collections.singletonList(new ResourceOwnerPasswordCredentialsGrant(tokenUrl));
			securityScheme = new OAuth(authorizationProps.getName(), authorizationScopeList, grantTypes);
		}
		else {
			// Swagger3 Oauth2
			securityScheme = OAuth2Scheme.OAUTH2_PASSWORD_FLOW_BUILDER.name(authorizationProps.getName())
					.tokenUrl(tokenUrl).scopes(authorizationScopeList).build();
		}

		return Collections.singletonList(securityScheme);
	}

	/**
	 * 配置默认的全局鉴权策略的开关，通过正则表达式进行匹配；默认匹配所有URL
	 * @return SecurityContext
	 */
	private List<SecurityContext> securityContext() {
		// @formatter:off
		SecurityContext securityContext = SecurityContext.builder()
				.securityReferences(defaultAuth())
				.build();
		// @formatter:on

		return Collections.singletonList(securityContext);
	}

	/**
	 * 默认的全局鉴权策略
	 * @return List<SecurityReference>
	 */
	private List<SecurityReference> defaultAuth() {
		SwaggerProperties.Authorization authorization = swaggerProperties.getAuthorization();
		List<AuthorizationScope> authorizationScopeList = authorization.getAuthorizationScopeList().stream()
				.map(scope -> new AuthorizationScope(scope.getScope(), scope.getDescription()))
				.collect(Collectors.toList());

		AuthorizationScope[] authorizationScopes = new AuthorizationScope[authorizationScopeList.size()];

		SecurityReference securityReference = SecurityReference.builder().reference(authorization.getName())
				.scopes(authorizationScopeList.toArray(authorizationScopes)).build();

		return Collections.singletonList(securityReference);
	}

}
