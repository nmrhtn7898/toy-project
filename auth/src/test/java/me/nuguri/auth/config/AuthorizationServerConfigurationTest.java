package me.nuguri.auth.config;

import me.nuguri.auth.common.BaseIntegrationTest;
import me.nuguri.common.enums.GrantType;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.json.JacksonJsonParser;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.ResultActions;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.restdocs.headers.HeaderDocumentation.*;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.get;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.post;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;
import static org.springframework.restdocs.request.RequestDocumentation.requestParameters;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("Oauth2 인증 서버 설정 테스트")
public class AuthorizationServerConfigurationTest extends BaseIntegrationTest {

    @Autowired
    AuthenticationManager authenticationManager;

    @Test
    @DisplayName("인증 서버 엑세스 토큰 유효한 경우")
    public void checkAccessToken_Success_200() throws Exception {
        mockMvc.perform(post("/oauth/check_token")
                .with(httpBasic(properties.getClientId(), properties.getClientSecret()))
                .param("token", getAccessToken(properties.getAdminEmail(), properties.getAdminPassword(), properties.getClientId(), properties.getClientSecret())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("active").exists())
                .andExpect(jsonPath("active").value(true))
                .andExpect(jsonPath("exp").exists())
                .andExpect(jsonPath("user_name").exists())
                .andExpect(jsonPath("authorities").exists())
                .andExpect(jsonPath("client_id").exists())
                .andExpect(jsonPath("scope").exists())
                .andExpect(jsonPath("id").exists())
                .andDo(print())
                .andDo(document("check-access_token",
                        requestParameters(
                                parameterWithName("token").description("access token")
                        ),
                        responseHeaders(
                                headerWithName(HttpHeaders.CACHE_CONTROL).description("cache control"),
                                headerWithName(HttpHeaders.PRAGMA).description("pragma"),
                                headerWithName(HttpHeaders.CONTENT_TYPE).description("content type"),
                                headerWithName("X-Content-Type-Options").description("X-Content-Type-Options"),
                                headerWithName("X-XSS-Protection").description("X-XSS-Protection"),
                                headerWithName("X-Frame-Options").description("X-Frame-Options")
                        ),
                        responseFields(
                                fieldWithPath("aud").description("aud"),
                                fieldWithPath("active").description("access token active"),
                                fieldWithPath("exp").description("access token expires"),
                                fieldWithPath("user_name").description("username"),
                                fieldWithPath("authorities").description("authorities"),
                                fieldWithPath("client_id").description("client_id"),
                                fieldWithPath("scope").description("access scopes"),
                                fieldWithPath("id").description("account id")
                        )
                        )
                );
    }

    @Test
    @DisplayName("인증 서버 엑세스 토큰 유효하지 않는 경우")
    public void checkAccessToken_Invalid_AccessToken_400() throws Exception {
        String access_token = getAccessToken(properties.getAdminEmail(), properties.getAdminPassword(), properties.getClientId(), properties.getClientSecret());
        mockMvc.perform(post("/oauth/revoke_token")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + access_token));
        mockMvc.perform(post("/oauth/check_token")
                .with(httpBasic(properties.getClientId(), properties.getClientSecret()))
                .param("token", access_token))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("인증 서버 엑세스 토큰 Authorization Code 방식으로 정상적으로 얻는 경우")
    public void getAccessToken_GrantType_Authorization_Code_Success_200() throws Exception {
        setAuthentication(properties.getUserEmail(), properties.getUserPassword());
        mockMvc.perform(get("/oauth/authorize")
                .param("response_type", "code")
                .param("client_id", properties.getClientId())
                .param("redirect_uri", properties.getRedirectUri())
                .param("scope", "read"))
                .andDo(print())
                .andDo(r -> {
                            mockMvc.perform(post("/oauth/authorize")
                                    .cookie(r.getResponse().getCookies())
                                    .with(csrf())
                                    .param("response_type", "code")
                                    .param("client_id", properties.getClientId())
                                    .param("redirect_uri", properties.getRedirectUri())
                                    .param("scope", "read")
                                    .param("scope.read", "true")
                                    .param("user_oauth_approval", "true"))
                                    .andDo(print())
                                    .andDo(r2 -> {
                                        String redirectedUrl = r2.getResponse().getRedirectedUrl();
                                        mockMvc.perform(post("/oauth/token")
                                                .with(httpBasic(properties.getClientId(), properties.getClientSecret()))
                                                .param("code", redirectedUrl.substring(redirectedUrl.lastIndexOf("=") + 1))
                                                .param("grant_type", GrantType.AUTHORIZATION_CODE.toString())
                                                .param("redirect_uri", properties.getRedirectUri()))
                                                .andDo(print())
                                                .andExpect(status().isOk())
                                                .andExpect(jsonPath("access_token").exists())
                                                .andExpect(jsonPath("token_type").exists())
                                                .andExpect(jsonPath("refresh_token").exists())
                                                .andExpect(jsonPath("expires_in").exists())
                                                .andExpect(jsonPath("scope").exists())
                                                .andExpect(jsonPath("id").exists()
                                                );
                                    });
                        }
                );
    }

    @Test
    @DisplayName("인증 서버 엑세스 토큰 Implicit 방식으로 정상적으로 얻는 경우")
    public void getAccessToken_GrantType_Implicit_Success_200() throws Exception {
        setAuthentication(properties.getUserEmail(), properties.getUserPassword());
        mockMvc.perform(get("/oauth/authorize")
                .param("response_type", "token")
                .param("client_id", properties.getClientId())
                .param("redirect_uri", properties.getRedirectUri())
                .param("scope", "read"))
                .andDo(print())
                .andDo(r -> {
                            String redirectedUrl = mockMvc.perform(post("/oauth/authorize")
                                    .cookie(r.getResponse().getCookies())
                                    .with(csrf())
                                    .param("response_type", "token")
                                    .param("client_id", properties.getClientId())
                                    .param("redirect_uri", properties.getRedirectUri())
                                    .param("scope", "read")
                                    .param("scope.read", "true")
                                    .param("user_oauth_approval", "true"))
                                    .andExpect(status().is3xxRedirection())
                                    .andDo(print())
                                    .andReturn().getResponse().getRedirectedUrl();
                            assertThat(redirectedUrl).contains("access_token");
                            assertThat(redirectedUrl).contains("token_type");
                            assertThat(redirectedUrl).contains("expires_in");
                            assertThat(redirectedUrl).contains("id");
                        }
                );
    }

    @Test
    @DisplayName("인증 서버 엑세스 토큰 Password 방식으로 정상적으로 얻는 경우")
    public void getAccessToken_GrantType_Password_Success_200() throws Exception {
        getAccessTokenPasswordGrantTypeResponse(properties.getAdminEmail(), properties.getAdminPassword())
                .andExpect(status().isOk())
                .andExpect(jsonPath("access_token").exists())
                .andExpect(jsonPath("token_type").exists())
                .andExpect(jsonPath("refresh_token").exists())
                .andExpect(jsonPath("expires_in").exists())
                .andExpect(jsonPath("scope").exists())
                .andExpect(jsonPath("id").exists())
                .andDo(print())
                .andDo(document("get-access_token-password-grantType",
                        requestHeaders(
                                headerWithName(HttpHeaders.AUTHORIZATION).description("Basic [clientId:clientSecret](Base64 Encoding Value)")
                        ),
                        requestParameters(
                                parameterWithName("username").description("user account email"),
                                parameterWithName("password").description("user account password"),
                                parameterWithName("scope").description("access token scope").optional(),
                                parameterWithName("grant_type").description("access token grant type")
                        ),
                        responseHeaders(
                                headerWithName(HttpHeaders.CACHE_CONTROL).description("cache control"),
                                headerWithName(HttpHeaders.PRAGMA).description("pragma"),
                                headerWithName(HttpHeaders.CONTENT_TYPE).description("content type"),
                                headerWithName("X-Content-Type-Options").description("X-Content-Type-Options"),
                                headerWithName("X-XSS-Protection").description("X-XSS-Protection"),
                                headerWithName("X-Frame-Options").description("X-Frame-Options")
                        ),
                        responseFields(
                                fieldWithPath("access_token").description("access token"),
                                fieldWithPath("token_type").description("access token type"),
                                fieldWithPath("refresh_token").description("refresh token"),
                                fieldWithPath("expires_in").description("access token expires time"),
                                fieldWithPath("scope").description("access scopes"),
                                fieldWithPath("id").description("account id")
                        )
                        )
                );
    }

    @Test
    @DisplayName("인증 서버 엑세스 토큰 Password 방식으로 HttpBasic 헤더 값 없어서 얻지 못하는 경우")
    public void getAccessToken_GrantType_Password_No_HttpBasic_401() throws Exception {
        mockMvc.perform(post("/oauth/token")
                .param("username", properties.getAdminEmail())
                .param("password", properties.getAdminPassword())
                .param("grant_type", GrantType.PASSWORD.toString()))
                .andDo(print())
                .andExpect(status().isUnauthorized())
        ;
    }

    @Test
    @DisplayName("인증 서버 엑세스 토큰 Password 방식으로 부정확한 username 및 password 입력으로 얻지 못하는 경우")
    public void getAccessToken_GrantType_Password_Invalid_Username_400() throws Exception {
        getAccessTokenPasswordGrantTypeResponse("noexistemail@test.com", "12341234")
                .andDo(print())
                .andExpect(status().isBadRequest())
        ;
    }

    @Test
    @DisplayName("인증 서버 엑세스 토큰 Refresh Token 방식으로 정상적으로 얻는 경우")
    public void getAccessToken_GrantType_RefreshToken_Success_200() throws Exception {
        getAccessTokenRefreshTokenGrantTypeResponse(getRefreshToken(properties.getAdminEmail(), properties.getAdminPassword()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("access_token").exists())
                .andExpect(jsonPath("token_type").exists())
                .andExpect(jsonPath("refresh_token").exists())
                .andExpect(jsonPath("expires_in").exists())
                .andExpect(jsonPath("scope").exists())
                .andExpect(jsonPath("id").exists())
                .andDo(print())
                .andDo(document("get-access_token-refresh_token-grantType",
                        requestHeaders(
                                headerWithName(HttpHeaders.AUTHORIZATION).description("Basic [clientId:clientSecret](Base64 Encoding Value)")
                        ),
                        requestParameters(
                                parameterWithName("refresh_token").description("refresh token"),
                                parameterWithName("scope").description("access token scope").optional(),
                                parameterWithName("grant_type").description("access token grant type")
                        ),
                        responseHeaders(
                                headerWithName(HttpHeaders.CACHE_CONTROL).description("cache control"),
                                headerWithName(HttpHeaders.PRAGMA).description("pragma"),
                                headerWithName(HttpHeaders.CONTENT_TYPE).description("content type"),
                                headerWithName("X-Content-Type-Options").description("X-Content-Type-Options"),
                                headerWithName("X-XSS-Protection").description("X-XSS-Protection"),
                                headerWithName("X-Frame-Options").description("X-Frame-Options")
                        ),
                        responseFields(
                                fieldWithPath("access_token").description("access token"),
                                fieldWithPath("token_type").description("access token type"),
                                fieldWithPath("refresh_token").description("refresh token"),
                                fieldWithPath("expires_in").description("access token expires time"),
                                fieldWithPath("scope").description("access scopes"),
                                fieldWithPath("id").description("account id")
                        )
                        )
                );
    }

    @Test
    @DisplayName("인증 서버 엑세스 토큰 Refresh Token 방식으로 HttpBasic 헤더 값 없어서 얻지 못하는 경우")
    public void getAccessToken_GrantType_RefreshToken_No_HttpBasic_401() throws Exception {
        mockMvc.perform(post("/oauth/token")
                .param("refresh_token", getRefreshToken(properties.getAdminEmail(), properties.getAdminPassword()))
                .param("grant_type", GrantType.REFRESH_TOKEN.toString()))
                .andExpect(status().isUnauthorized())
                .andDo(print())
        ;
    }

    @Test
    @DisplayName("인증 서버 엑세스 토큰 Refresh Token 방식으로 부정확한 refresh token 입력으로 얻지 못하는 경우")
    public void getAccessToken_GrantType_RefreshToken_Invalid_Token_400() throws Exception {
        getAccessTokenRefreshTokenGrantTypeResponse("invalid_token!@(#*&!@(*#")
                .andExpect(status().isBadRequest())
                .andDo(print())
        ;
    }

    @Test
    @DisplayName("인증 서버 엑세스 토큰 Client Credentials 방식으로 정상적으로 얻는 경우")
    public void getAccessToken_GrantType_ClientCredentials_Success_200() throws Exception {
        getAccessTokenClientCredentialsGrantTypeResponse(properties.getClientId(), properties.getClientSecret())
                .andExpect(status().isOk())
                .andDo(print())
                .andDo(document("get-access_token-client_credentials-grantType",
                        requestHeaders(
                                headerWithName(HttpHeaders.AUTHORIZATION).description("Basic [clientId:clientSecret](Base64 Encoding Value)")
                        ),
                        requestParameters(
                                parameterWithName("scope").description("access token scope").optional(),
                                parameterWithName("grant_type").description("access token grant type")
                        ),
                        responseHeaders(
                                headerWithName(HttpHeaders.CACHE_CONTROL).description("cache control"),
                                headerWithName(HttpHeaders.PRAGMA).description("pragma"),
                                headerWithName(HttpHeaders.CONTENT_TYPE).description("content type"),
                                headerWithName("X-Content-Type-Options").description("X-Content-Type-Options"),
                                headerWithName("X-XSS-Protection").description("X-XSS-Protection"),
                                headerWithName("X-Frame-Options").description("X-Frame-Options")
                        ),
                        responseFields(
                                fieldWithPath("access_token").description("access token"),
                                fieldWithPath("token_type").description("access token type"),
                                fieldWithPath("expires_in").description("access token expires time"),
                                fieldWithPath("scope").description("access scopes")
                        )
                        )
                );
    }

    @Test
    @DisplayName("인증 서버 엑세스 토큰 Client Credentials 방식으로 HttpBasic 헤더 값 없어서 얻지 못하는 경우")
    public void getAccessToken_GrantType_ClientCredentials_No_HttpBasic_401() throws Exception {
        mockMvc.perform(post("/oauth/token")
                .param("grant_type", GrantType.CLIENT_CREDENTIALS.toString()))
                .andExpect(status().isUnauthorized())
                .andDo(print())
        ;
    }

    @Test
    @DisplayName("인증 서버 엑세스 토큰 Client Credentials 방식으로 부정확한 HttpBasic 헤더 값 입력으로 얻지 못하는 경우")
    public void getAccessToken_GrantType_ClientCredentials_Invalid_HttpBasic_401() throws Exception {
        getAccessTokenClientCredentialsGrantTypeResponse("invalid_client_id", "invalid_client_secret")
                .andExpect(status().isUnauthorized())
                .andDo(print())
        ;
    }

    /**
     * Client Credentials 방식 엑세스 토큰 발급 요청 공통 로직
     *
     * @param clientId     클라이언트 아이디
     * @param clientSecret 클라이언트 시크릿
     * @return Client Credentials 방식 엑세스 토큰 발급 요청 결과
     * @throws Exception
     */
    private ResultActions getAccessTokenClientCredentialsGrantTypeResponse(String clientId, String clientSecret) throws Exception {
        return mockMvc.perform(post("/oauth/token")
                .with(httpBasic(clientId, clientSecret))
                .param("grant_type", GrantType.CLIENT_CREDENTIALS.toString()));
    }

    /**
     * Refresh Token 방식 엑세스 토큰 발급 요청 공통 로직
     *
     * @param refresh_token 재발급 토큰
     * @return Refresh Token 방식 엑세스 토큰 발급 요청 결과
     * @throws Exception
     */
    private ResultActions getAccessTokenRefreshTokenGrantTypeResponse(String refresh_token) throws Exception {
        return mockMvc.perform(post("/oauth/token")
                .with(httpBasic(properties.getClientId(), properties.getClientSecret()))
                .param("refresh_token", refresh_token)
                .param("grant_type", GrantType.REFRESH_TOKEN.toString()));
    }

    /**
     * Password 방식 엑세스 토근 발급 요청 공통 로직
     *
     * @param email    이메일
     * @param password 비밀번호
     * @return Password 방식 엑세스 토큰 발급 요청 결과
     * @throws Exception
     */
    private ResultActions getAccessTokenPasswordGrantTypeResponse(String email, String password) throws Exception {
        return mockMvc.perform(post("/oauth/token")
                .with(httpBasic(properties.getClientId(), properties.getClientSecret()))
                .param("username", email)
                .param("password", password)
                .param("grant_type", GrantType.PASSWORD.toString())).andDo(print());
    }

    /**
     * Password 방식 엑세스 토큰 요청 후 재발급 토큰 반환 공통 로직
     *
     * @param username 이메일
     * @param password 비밀번호
     * @return 재발급 토큰
     * @throws Exception
     */
    private String getRefreshToken(String username, String password) throws Exception {
        return (String) new JacksonJsonParser()
                .parseMap(getAccessTokenPasswordGrantTypeResponse(username, password)
                        .andReturn()
                        .getResponse()
                        .getContentAsString())
                .get("refresh_token");
    }

    private void setAuthentication(String username, String password) {
        SecurityContext context = SecurityContextHolder.getContext();
        context.setAuthentication(authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(username, password)));
    }

}