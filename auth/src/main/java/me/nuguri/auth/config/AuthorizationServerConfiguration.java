package me.nuguri.auth.config;

import lombok.RequiredArgsConstructor;
import me.nuguri.auth.service.AuthorizationService;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.config.annotation.configurers.ClientDetailsServiceConfigurer;
import org.springframework.security.oauth2.config.annotation.web.configuration.AuthorizationServerConfigurerAdapter;
import org.springframework.security.oauth2.config.annotation.web.configuration.EnableAuthorizationServer;
import org.springframework.security.oauth2.config.annotation.web.configuration.EnableResourceServer;
import org.springframework.security.oauth2.config.annotation.web.configuration.ResourceServerConfigurerAdapter;
import org.springframework.security.oauth2.config.annotation.web.configurers.AuthorizationServerEndpointsConfigurer;
import org.springframework.security.oauth2.config.annotation.web.configurers.AuthorizationServerSecurityConfigurer;
import org.springframework.security.oauth2.config.annotation.web.configurers.ResourceServerSecurityConfigurer;
import org.springframework.security.oauth2.provider.error.OAuth2AccessDeniedHandler;
import org.springframework.security.oauth2.provider.token.TokenEnhancer;
import org.springframework.security.oauth2.provider.token.TokenEnhancerChain;
import org.springframework.security.oauth2.provider.token.TokenStore;
import org.springframework.security.oauth2.provider.token.store.JwtAccessTokenConverter;

import javax.sql.DataSource;
import java.util.Arrays;

@Configuration
@EnableAuthorizationServer
@EnableResourceServer
public class AuthorizationServerConfiguration {

    @Configuration
    @RequiredArgsConstructor
    public static class AuthorizationConfiguration extends AuthorizationServerConfigurerAdapter {

        private final PasswordEncoder passwordEncoder;

        private final DataSource dataSource;

        private final TokenStore tokenStore;

        private final TokenEnhancer tokenEnhancer;

        private final JwtAccessTokenConverter jwtAccessTokenConverter;

        private final AuthenticationManager authenticationManager;

        private final AuthorizationService authorizationService;

        /**
         * 인증 서버 설정
         *
         * @param security
         * @throws Exception
         */
        @Override
        public void configure(AuthorizationServerSecurityConfigurer security) throws Exception {
            security
                    // client_id, client_secret을 basic 인코딩(header Authorization)방식과 더불어 parameter로 인코딩 없이 전달해도 받을 수 있게끔 설정
//                    .allowFormAuthenticationForClients()
                    .passwordEncoder(passwordEncoder)
//                    .tokenKeyAccess("permitAll()") // JWT 복호화용 public key 엔드포인트
                    .checkTokenAccess("isAuthenticated()");
        }

        /**
         * 인증 서버 클라이언트 설정
         *
         * @param clients
         * @throws Exception
         */
        @Override
        public void configure(ClientDetailsServiceConfigurer clients) throws Exception {
            clients.withClientDetails(authorizationService);
//            clients.jdbc(dataSource); // JDBC 방식 클라이언트 등록
/*        인메모리 클라이언트 세팅
        clients.inMemory()
                .withClient(properties.getClientId())
                .secret(passwordEncoder.encode(properties.getClientSecret()))
                .scopes(Scope.READ.toString(), Scope.WRITE.toString())
                .authorizedGrantTypes(GrantType.PASSWORD.toString(), GrantType.AUTHORIZATION_CODE.toString(),
                        GrantType.IMPLICIT.toString(), GrantType.CLIENT_CREDENTIALS.toString(), GrantType.REFRESH_TOKEN.toString())
                .redirectUris(properties.getRedirectUri())
                .accessTokenValiditySeconds(60 * 10)
                .refreshTokenValiditySeconds(60 * 10 * 6);*/
        }

        /**
         * 인증 서버 엔드포인트 설정
         *
         * @param endpoints
         * @throws Exception
         */
        @Override
        public void configure(AuthorizationServerEndpointsConfigurer endpoints) throws Exception {
            TokenEnhancerChain tokenEnhancerChain = new TokenEnhancerChain();
            tokenEnhancerChain.setTokenEnhancers(Arrays.asList(tokenEnhancer, jwtAccessTokenConverter));
            endpoints
                    .tokenEnhancer(tokenEnhancerChain)
                    .tokenStore(tokenStore)
                    .accessTokenConverter(jwtAccessTokenConverter)
                    .userDetailsService(authorizationService)
                    .authenticationManager(authenticationManager);
        }

    }

    @Configuration // TODO 추후 분리할 시간 있으면 분리...
    @EnableGlobalMethodSecurity(prePostEnabled = true) // 애노테이션 기반 권한 검사 사용
    @Order(100) // 시큐리티 필터 체인보다 우선순위를 낮게 하여 우선적으로 시큐리티 필터 체인의 url 패턴으로 검사
    @RequiredArgsConstructor
    public static class ResourceConfiguration extends ResourceServerConfigurerAdapter {

        /**
         * 리소스 서버 설정
         *
         * @param resources
         * @throws Exception
         */
        @Override
        public void configure(ResourceServerSecurityConfigurer resources) throws Exception {
            resources.resourceId("account");
        }

        /**
         * 리소스 서버 필터 체인 설정, /api/** url 패턴에 대한 권한 처리
         *
         * @param http
         * @throws Exception
         */
        @Override
        public void configure(HttpSecurity http) throws Exception {
            http
                    .requestMatchers()
                    .mvcMatchers("/api/**")
                    .and()
                    .authorizeRequests()
                    .mvcMatchers(HttpMethod.POST, "/api/**/user").permitAll()
                    .anyRequest().authenticated();
            http.sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS);
            http.logout().disable();
            http.csrf().disable();
            http.exceptionHandling().accessDeniedHandler(new OAuth2AccessDeniedHandler());
        }

    }

}
