package me.nuguri.account.controller.api;

import me.nuguri.account.common.BaseIntegrationTest;
import me.nuguri.common.entity.Account;
import me.nuguri.common.entity.Address;
import me.nuguri.common.enums.Gender;
import me.nuguri.common.enums.Role;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.hateoas.MediaTypes;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;

import java.util.UUID;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.restdocs.headers.HeaderDocumentation.*;
import static org.springframework.restdocs.hypermedia.HypermediaDocumentation.linkWithRel;
import static org.springframework.restdocs.hypermedia.HypermediaDocumentation.links;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.*;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;
import static org.springframework.restdocs.request.RequestDocumentation.pathParameters;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("계정 API 테스트")
public class AccountApiControllerTest extends BaseIntegrationTest {

    @Test
    @DisplayName("토근으로 유저 정보 성공적으로 얻는 경우")
    public void getMe_V1_Success_200() throws Exception {
        mockRestTemplate(HttpStatus.OK, accountService.find(properties.getAdminEmail()));
        mockMvc.perform(get("/api/v1/user/me")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + getAccessToken())
                .accept(MediaTypes.HAL_JSON))
                .andExpect(status().isOk())
                .andDo(print())
                .andDo(document("get-me",
                        links(
                                linkWithRel("self").description("self link"),
                                linkWithRel("document").description("document"),
                                linkWithRel("getUser").description("get user link"),
                                linkWithRel("updateUser").description("update user link"),
                                linkWithRel("mergeUser").description("merge user link"),
                                linkWithRel("deleteUser").description("delete user link")
                        ),
                        requestHeaders(
                                headerWithName(HttpHeaders.AUTHORIZATION).description("bearer token"),
                                headerWithName(HttpHeaders.ACCEPT).description("Accept Header")
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
                                fieldWithPath("_links.self.href").description("self link"),
                                fieldWithPath("_links.document.href").description("document"),
                                fieldWithPath("_links.getUser.href").description("get user link"),
                                fieldWithPath("_links.updateUser.href").description("update user link"),
                                fieldWithPath("_links.mergeUser.href").description("merge user link"),
                                fieldWithPath("_links.deleteUser.href").description("delete user link"),
                                fieldWithPath("_links.self.type").description("self link http method type"),
                                fieldWithPath("_links.getUser.type").description("get user link http method type"),
                                fieldWithPath("_links.updateUser.type").description("update user link  http method type"),
                                fieldWithPath("_links.mergeUser.type").description("merge user link  http method type"),
                                fieldWithPath("_links.deleteUser.type").description("delete user link  http method type"),
                                fieldWithPath("id").description("account id"),
                                fieldWithPath("email").description("account email"),
                                fieldWithPath("name").description("account name"),
                                fieldWithPath("gender").description("account gender"),
                                fieldWithPath("address.city").description("address city"),
                                fieldWithPath("address.street").description("address street"),
                                fieldWithPath("address.zipCode").description("address zipCode"),
                                fieldWithPath("role").description("account role"),
                                fieldWithPath("created").description("account created date"),
                                fieldWithPath("updated").description("account last updated date")
                        )
                        )
                );
    }

    @Test
    @DisplayName("토근으로 유저 정보 유효하지 않은 토큰으로 못 얻는 경우")
    public void getMe_V1_Success_401() throws Exception {
        mockRestTemplate(HttpStatus.UNAUTHORIZED, null);
        mockMvc.perform(get("/api/v1/user/me")
                .header(HttpHeaders.AUTHORIZATION, "Bearer Invalid Token")
                .accept(MediaTypes.HAL_JSON))
                .andExpect(status().isUnauthorized())
                .andDo(print());
    }

    @ParameterizedTest(name = "{index}. {displayName} parameter(sort: {arguments})")
    @DisplayName("유저 정보 리스트 성공적으로 얻는 경우")
    @ValueSource(strings = {"id,asc", "id,name,desc", "id,name,email,asc"})
    public void queryUsers_V1_Success_200(String sort) throws Exception {
        generateAccounts();
        mockRestTemplate(HttpStatus.OK, accountService.find(properties.getAdminEmail()));
        mockMvc.perform(get("/api/v1/users")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + getAccessToken())
                .accept(MediaTypes.HAL_JSON)
                .queryParam("page", "2")
                .queryParam("size", "10")
                .queryParam("sort", sort))
                .andExpect(status().isOk())
                .andDo(print())
                .andDo(document("query-users",
                        links(
                                linkWithRel("first").description("first page link"),
                                linkWithRel("prev").description("prev page link"),
                                linkWithRel("self").description("self link"),
                                linkWithRel("next").description("next page link"),
                                linkWithRel("last").description("last page link"),
                                linkWithRel("document").description("document")
                        ),
                        requestHeaders(
                                headerWithName(HttpHeaders.AUTHORIZATION).description("bearer token"),
                                headerWithName(HttpHeaders.ACCEPT).description("Accept Header")
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
                                fieldWithPath("_embedded.*[*].id").description("account id"),
                                fieldWithPath("_embedded.*[*].email").description("account email"),
                                fieldWithPath("_embedded.*[*].name").description("account name"),
                                fieldWithPath("_embedded.*[*].gender").description("account gender"),
                                fieldWithPath("_embedded.*[*].address.city").description("address city"),
                                fieldWithPath("_embedded.*[*].address.street").description("address street"),
                                fieldWithPath("_embedded.*[*].address.zipCode").description("address zipCode"),
                                fieldWithPath("_embedded.*[*].role").description("account role"),
                                fieldWithPath("_embedded.*[*].created").description("account created date"),
                                fieldWithPath("_embedded.*[*].updated").description("account last updated date"),
                                fieldWithPath("_embedded.*[*]._links.getUser.href").description("getUser link"),
                                fieldWithPath("_embedded.*[*]._links.updateUser.href").description("updateUser link"),
                                fieldWithPath("_embedded.*[*]._links.mergeUser.href").description("mergeUser link"),
                                fieldWithPath("_embedded.*[*]._links.deleteUser.href").description("deleteUser link"),
                                fieldWithPath("_embedded.*[*]._links.getUser.type").description("getUser link http method type"),
                                fieldWithPath("_embedded.*[*]._links.updateUser.type").description("updateUser link http method type"),
                                fieldWithPath("_embedded.*[*]._links.mergeUser.type").description("mergeUser link http method type"),
                                fieldWithPath("_embedded.*[*]._links.deleteUser.type").description("deleteUser link http method type"),
                                fieldWithPath("_links.first.href").description("first page link"),
                                fieldWithPath("_links.prev.href").description("prev page link"),
                                fieldWithPath("_links.self.href").description("self link"),
                                fieldWithPath("_links.next.href").description("next page link"),
                                fieldWithPath("_links.last.href").description("last page link"),
                                fieldWithPath("_links.document.href").description("document"),
                                fieldWithPath("page.size").description("page size"),
                                fieldWithPath("page.totalElements").description("total element count"),
                                fieldWithPath("page.totalPages").description("total page count"),
                                fieldWithPath("page.number").description("current page number")
                        )
                        )
                );
    }

    @ParameterizedTest(name = "{index}. {displayName} parameter(sort: {arguments})")
    @DisplayName("유저 정보 리스트 조건 값 조회 성공적으로 얻는 경우")
    @CsvSource(
            value = {
                    "id,asc:user@naver.com::F::USER:2019-01-01:2021-01-01:2018-05-05:2022-01-01",
                    "id,name,desc:admin@naver.com::M::ADMIN:2018-05-01:::2022-01-01",
                    "id,name,email,asc:::::::::",
                    ":::::::::"},
            delimiter = ':'
    )
    public void conditionQueryUsers_V1_Success_200(String sort, String email, String name, String gender, String address, String role,
                                                   String startCreated, String endCreated, String startUpdated, String endUpdated) throws Exception {
        generateAccounts();
        mockRestTemplate(HttpStatus.OK, accountService.find(properties.getAdminEmail()));
        mockMvc.perform(get("/api/v1/users")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + getAccessToken())
                .accept(MediaTypes.HAL_JSON)
                .queryParam("page", "1")
                .queryParam("size", "10")
                .queryParam("sort", sort)
                .queryParam("email", email)
                .queryParam("name", name)
                .queryParam("gender", gender)
                .queryParam("address", address)
                .queryParam("role", role)
                .queryParam("startCreated", startCreated)
                .queryParam("endCreated", endCreated)
                .queryParam("startUpdated", startUpdated)
                .queryParam("endUpdated", endUpdated))
                .andExpect(status().isOk())
                .andDo(print());
    }

    @Test
    @DisplayName("유저 정보 리스트 유효하지 않은 토큰으로 못 얻는 경우")
    public void queryUsers_V1_Unauthorized_401() throws Exception {
        mockRestTemplate(HttpStatus.UNAUTHORIZED, null);
        mockMvc.perform(get("/api/v1/users")
                .header(HttpHeaders.AUTHORIZATION, "Bearer Invalid Token")
                .accept(MediaTypes.HAL_JSON))
                .andExpect(status().isUnauthorized())
                .andDo(print());
    }

    @Test
    @DisplayName("유저 정보 리스트 관리자 권한 없어서 못 얻는 경우")
    public void queryUsers_V1_Forbidden_403() throws Exception {
        mockRestTemplate(HttpStatus.OK, accountService.find(properties.getUserEmail()));
        mockMvc.perform(get("/api/v1/users")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + getAccessToken())
                .accept(MediaTypes.HAL_JSON))
                .andExpect(status().isForbidden())
                .andDo(print());
    }

    @Test
    @DisplayName("유저 정보 리스트 요청 페이지 데이터 없어서 못 얻는 경우")
    public void queryUsers_V1_NotFound_404() throws Exception {
        generateAccounts();
        mockRestTemplate(HttpStatus.OK, accountService.find(properties.getAdminEmail()));
        mockMvc.perform(get("/api/v1/users")
                .queryParam("page", "18723671")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + getAccessToken())
                .accept(MediaTypes.HAL_JSON))
                .andExpect(status().isNotFound())
                .andDo(print());
    }

    @ParameterizedTest(name = "{index}. {displayName} parameter(page: {0} / size: {1} / sort : {2}")
    @DisplayName("유저 정보 리스트 잘못된 파라미터로 못 얻는 경우")
    @CsvSource(value = {"1-0:0:-98", "asd:08:-12", "0:-2:id,qwe"}, delimiter = ':')
    public void queryUsers_V1_Invalid_Params_400(String page, String size, String sort) throws Exception {
        mockRestTemplate(HttpStatus.OK, accountService.find(properties.getAdminEmail()));
        mockMvc.perform(get("/api/v1/users")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + getAccessToken())
                .accept(MediaTypes.HAL_JSON)
                .queryParam("page", page)
                .queryParam("size", size)
                .queryParam("sort", sort))
                .andDo(print())
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("유저 정보 성공적으로 얻는 경우")
    public void getUser_V1_Admin_Success_200() throws Exception {
        mockRestTemplate(HttpStatus.OK, accountService.find(properties.getUserEmail()));
        mockMvc.perform(get("/api/v1/user/{id}", accountService.find(properties.getUserEmail()).getId())
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + getAccessToken())
                .accept(MediaTypes.HAL_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andDo(document("get-user",
                        links(
                                linkWithRel("self").description("self link"),
                                linkWithRel("document").description("document"),
                                linkWithRel("updateUser").description("update user link"),
                                linkWithRel("mergeUser").description("merge user link"),
                                linkWithRel("deleteUser").description("delete user link")
                        ),
                        pathParameters(
                                parameterWithName("id").description("identifier of account")
                        ),
                        requestHeaders(
                                headerWithName(HttpHeaders.AUTHORIZATION).description("bearer token"),
                                headerWithName(HttpHeaders.ACCEPT).description("Accept Header")
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
                                fieldWithPath("_links.self.href").description("self link"),
                                fieldWithPath("_links.document.href").description("document"),
                                fieldWithPath("_links.updateUser.href").description("update user link"),
                                fieldWithPath("_links.mergeUser.href").description("merge user link"),
                                fieldWithPath("_links.deleteUser.href").description("delete user link"),
                                fieldWithPath("_links.self.type").description("self link http method type"),
                                fieldWithPath("_links.updateUser.type").description("update user link  http method type"),
                                fieldWithPath("_links.mergeUser.type").description("merge user link  http method type"),
                                fieldWithPath("_links.deleteUser.type").description("delete user link  http method type"),
                                fieldWithPath("id").description("account id"),
                                fieldWithPath("email").description("account email"),
                                fieldWithPath("name").description("account name"),
                                fieldWithPath("gender").description("account gender"),
                                fieldWithPath("address.city").description("address city"),
                                fieldWithPath("address.street").description("address street"),
                                fieldWithPath("address.zipCode").description("address zipCode"),
                                fieldWithPath("role").description("account role"),
                                fieldWithPath("created").description("account created date"),
                                fieldWithPath("updated").description("account last updated date")
                        )
                        )
                );
    }

    @Test
    @DisplayName("유저 정보 유효하지 않은 토큰으로 얻지 못하는 경우")
    public void getUser_V1_User_Unauthorized_401() throws Exception {
        mockRestTemplate(HttpStatus.UNAUTHORIZED, null);
        mockMvc.perform(get("/api/v1/user/{id}", accountService.find(properties.getUserEmail()))
                .header(HttpHeaders.AUTHORIZATION, "Bearer Invalid Token")
                .accept(MediaTypes.HAL_JSON))
                .andDo(print())
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("사용자가 자신의 유저 정보가 아닌 다른 정보를 얻지 못하는 경우")
    public void getUser_V1_User_Forbidden_403() throws Exception {
        mockRestTemplate(HttpStatus.OK, accountService.find(properties.getUserEmail()));
        Account account = new Account();
        account.setName("테스트");
        account.setEmail("bvcncvbncvnbt@test.com");
        account.setPassword("test");
        account.setGender(Gender.F);
        account.setAddress(new Address("경기도 과천시", "부림2길 76 2층", "13830"));
        account.setRole(Role.USER);

        mockMvc.perform(get("/api/v1/user/{id}", accountService.generate(account).getId())
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + getAccessToken())
                .accept(MediaTypes.HAL_JSON))
                .andDo(print())
                .andExpect(jsonPath("timestamp").exists())
                .andExpect(jsonPath("status").exists())
                .andExpect(jsonPath("error").exists())
                .andExpect(jsonPath("message").exists())
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("유저 정보 잘못된 식별자로 얻지 못하는 경우")
    public void getUser_V1_Invalid_400() throws Exception {
        mockRestTemplate(HttpStatus.OK, accountService.find(properties.getAdminEmail()));
        mockMvc.perform(get("/api/v1/user/{id}", "asdasd")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + getAccessToken())
                .accept(MediaTypes.HAL_JSON))
                .andDo(print())
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("유저 정보 존재하지 않는 식별자로 얻지 못하는 경우")
    public void getUser_No_Exist_404() throws Exception {
        mockRestTemplate(HttpStatus.OK, accountService.find(properties.getAdminEmail()));
        mockMvc.perform(get("/api/v1/user/{id}", "1928361836")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + getAccessToken())
                .accept(MediaTypes.HAL_JSON))
                .andDo(print())
                .andExpect(jsonPath("timestamp").exists())
                .andExpect(jsonPath("status").exists())
                .andExpect(jsonPath("error").exists())
                .andExpect(jsonPath("message").exists())
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("유저 정보 생성 성공적인 경우")
    public void generateUser_V1_Success_201() throws Exception {
        mockRestTemplate(HttpStatus.OK, accountService.find(properties.getAdminEmail()));
        AccountApiController.GenerateUserRequest request = new AccountApiController.GenerateUserRequest();
        request.setName("생성성공");
        request.setEmail("test200@naver.com");
        request.setPassword("123123");
        request.setGender(Gender.F);
        request.setAddress(new Address("경기도 과천시", "부림2길 76 2층", "13830"));
        request.setRole(Role.USER);

        mockMvc.perform(post("/api/v1/user")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + getAccessToken())
                .accept(MediaTypes.HAL_JSON)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isCreated())
                .andDo(document("generate-user",
                        links(
                                linkWithRel("self").description("self link"),
                                linkWithRel("document").description("document"),
                                linkWithRel("getUser").description("get user link"),
                                linkWithRel("updateUser").description("update user link"),
                                linkWithRel("mergeUser").description("merge user link"),
                                linkWithRel("deleteUser").description("delete user link")
                        ),
                        requestHeaders(
                                headerWithName(HttpHeaders.AUTHORIZATION).description("bearer token"),
                                headerWithName(HttpHeaders.CONTENT_TYPE).description("Content Type"),
                                headerWithName(HttpHeaders.ACCEPT).description("Accept Header")
                        ),
                        responseHeaders(
                                headerWithName(HttpHeaders.LOCATION).description("created resource link"),
                                headerWithName(HttpHeaders.CACHE_CONTROL).description("cache control"),
                                headerWithName(HttpHeaders.PRAGMA).description("pragma"),
                                headerWithName(HttpHeaders.CONTENT_TYPE).description("content type"),
                                headerWithName("X-Content-Type-Options").description("X-Content-Type-Options"),
                                headerWithName("X-XSS-Protection").description("X-XSS-Protection"),
                                headerWithName("X-Frame-Options").description("X-Frame-Options")
                        ),
                        responseFields(
                                fieldWithPath("_links.self.href").description("self link"),
                                fieldWithPath("_links.document.href").description("document"),
                                fieldWithPath("_links.getUser.href").description("get user link"),
                                fieldWithPath("_links.updateUser.href").description("update user link"),
                                fieldWithPath("_links.mergeUser.href").description("merge user link"),
                                fieldWithPath("_links.deleteUser.href").description("delete user link"),
                                fieldWithPath("_links.self.type").description("self link http method type"),
                                fieldWithPath("_links.getUser.type").description("get user link  http method type"),
                                fieldWithPath("_links.updateUser.type").description("update user link  http method type"),
                                fieldWithPath("_links.mergeUser.type").description("merge user link  http method type"),
                                fieldWithPath("_links.deleteUser.type").description("delete user link  http method type"),
                                fieldWithPath("id").description("account id"),
                                fieldWithPath("email").description("account email"),
                                fieldWithPath("name").description("account name"),
                                fieldWithPath("gender").description("account gender"),
                                fieldWithPath("address.city").description("address city"),
                                fieldWithPath("address.street").description("address street"),
                                fieldWithPath("address.zipCode").description("address zipCode"),
                                fieldWithPath("role").description("account role"),
                                fieldWithPath("created").description("account created date"),
                                fieldWithPath("updated").description("account last updated date")
                        )
                        )
                );
    }

    @Test
    @DisplayName("유저 정보 생성 잘못된 입력 정보로 실패하는 경우")
    public void generateUser_V1_Invalid_400() throws Exception {
        mockRestTemplate(HttpStatus.OK, accountService.find(properties.getAdminEmail()));
        AccountApiController.GenerateUserRequest request = new AccountApiController.GenerateUserRequest();
        request.setName("테스트");
        request.setEmail("isNotEmailType");
        request.setGender(Gender.M);
        request.setAddress(new Address("", "", ""));
        request.setPassword("1234");

        mockMvc.perform(post("/api/v1/user")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + getAccessToken())
                .accept(MediaTypes.HAL_JSON)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(jsonPath("timestamp").exists())
                .andExpect(jsonPath("status").exists())
                .andExpect(jsonPath("error").exists())
                .andExpect(jsonPath("message").exists())
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("유저 정보 생성 이미 존재하는 입력 정보로 실패하는 경우")
    public void generateUser_V1_Already_Exist_400() throws Exception {
        mockRestTemplate(HttpStatus.OK, accountService.find(properties.getAdminEmail()));
        String email = "already@test.com";
        AccountApiController.GenerateUserRequest request = new AccountApiController.GenerateUserRequest();
        request.setName("테스트");
        request.setEmail(email);
        request.setPassword("124331");
        request.setGender(Gender.F);
        request.setAddress(new Address("경기도 과천시", "부림2길 76 2층", "13830"));
        request.setRole(Role.USER);

        accountService.generate(modelMapper.map(request, Account.class));

        mockMvc.perform(post("/api/v1/user")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + getAccessToken())
                .accept(MediaTypes.HAL_JSON)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(jsonPath("timestamp").exists())
                .andExpect(jsonPath("status").exists())
                .andExpect(jsonPath("error").exists())
                .andExpect(jsonPath("message").exists())
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("유저 정보 부분 수정 성공적인 경우")
    public void updateUser_V1_Success_200() throws Exception {
        mockRestTemplate(HttpStatus.OK, accountService.find(properties.getAdminEmail()));
        String name = "테스트";
        String password = "123123";
        Role role = Role.ADMIN;

        AccountApiController.UpdateUserRequest request = new AccountApiController.UpdateUserRequest();
        request.setName(name);
        request.setPassword(password);
        request.setRole(role);

        mockMvc.perform(patch("/api/v1/user/{id}", accountService.find(properties.getUserEmail()).getId())
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + getAccessToken())
                .content(objectMapper.writeValueAsString(request))
                .accept(MediaTypes.HAL_JSON)
                .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andDo(document("update-user",
                        links(
                                linkWithRel("self").description("self link"),
                                linkWithRel("document").description("document"),
                                linkWithRel("getUser").description("get user link"),
                                linkWithRel("mergeUser").description("merge user link"),
                                linkWithRel("deleteUser").description("delete user link")
                        ),
                        pathParameters(
                                parameterWithName("id").description("identifier of account")
                        ),
                        requestHeaders(
                                headerWithName(HttpHeaders.AUTHORIZATION).description("bearer token"),
                                headerWithName(HttpHeaders.CONTENT_TYPE).description("Content Type"),
                                headerWithName(HttpHeaders.ACCEPT).description("Accept Header")
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
                                fieldWithPath("_links.self.href").description("self link"),
                                fieldWithPath("_links.document.href").description("document"),
                                fieldWithPath("_links.getUser.href").description("get user link"),
                                fieldWithPath("_links.mergeUser.href").description("merge user link"),
                                fieldWithPath("_links.deleteUser.href").description("delete user link"),
                                fieldWithPath("_links.self.type").description("self link http method type"),
                                fieldWithPath("_links.getUser.type").description("get user link  http method type"),
                                fieldWithPath("_links.mergeUser.type").description("merge user link  http method type"),
                                fieldWithPath("_links.deleteUser.type").description("delete user link  http method type"),
                                fieldWithPath("id").description("account id"),
                                fieldWithPath("email").description("account email"),
                                fieldWithPath("name").description("account name"),
                                fieldWithPath("gender").description("account gender"),
                                fieldWithPath("address.city").description("address city"),
                                fieldWithPath("address.street").description("address street"),
                                fieldWithPath("address.zipCode").description("address zipCode"),
                                fieldWithPath("role").description("account role"),
                                fieldWithPath("created").description("account created date"),
                                fieldWithPath("updated").description("account last updated date")
                        )
                        )
                );

        Account updated = accountService.find(properties.getUserEmail());
        assertEquals(name, updated.getName());
        assertTrue(passwordEncoder.matches(password, updated.getPassword()));
        assertEquals(role, updated.getRole());
    }

    @Test
    @DisplayName("유저 정보 부분 수정 유저 정보 존재하지 않는 경우")
    public void updateUser_V1_NotFound_404() throws Exception {
        mockRestTemplate(HttpStatus.OK, accountService.find(properties.getAdminEmail()));
        AccountApiController.UpdateUserRequest request = new AccountApiController.UpdateUserRequest();
        request.setName("테스트");
        request.setPassword("1123123");
        request.setRole(Role.ADMIN);

        mockMvc.perform(patch("/api/v1/user/{id}", "198237981")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + getAccessToken())
                .content(objectMapper.writeValueAsString(request))
                .accept(MediaTypes.HAL_JSON)
                .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("유저 정보 부분 수정 잘못된 입력 값으로 실패하는 경우")
    public void updateUser_V1_Invalid_400() throws Exception {
        mockRestTemplate(HttpStatus.OK, accountService.find(properties.getAdminEmail()));
        AccountApiController.UpdateUserRequest request = new AccountApiController.UpdateUserRequest();
        request.setPassword("1");

        mockMvc.perform(patch("/api/v1/user/{id}", accountService.find(properties.getUserEmail()).getId())
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + getAccessToken())
                .content(objectMapper.writeValueAsString(request))
                .accept(MediaTypes.HAL_JSON)
                .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("유저 정보 부분 수정 유효하지 않은 토큰으로 실패하는 경우")
    public void updateUser_V1_Unauthorized_401() throws Exception {
        mockRestTemplate(HttpStatus.UNAUTHORIZED, null);
        AccountApiController.UpdateUserRequest request = new AccountApiController.UpdateUserRequest();
        request.setPassword("1123123");
        request.setRole(Role.USER);

        mockMvc.perform(patch("/api/v1/user/{id}", accountService.find(properties.getAdminEmail()).getId())
                .header(HttpHeaders.AUTHORIZATION, "Bearer Invalid Token")
                .content(objectMapper.writeValueAsString(request))
                .accept(MediaTypes.HAL_JSON)
                .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("유저 정보 부분 수정 권한 없어서 실패하는 경우")
    public void updateUser_V1_Forbidden_403() throws Exception {
        mockRestTemplate(HttpStatus.OK, accountService.find(properties.getUserEmail()));
        AccountApiController.UpdateUserRequest request = new AccountApiController.UpdateUserRequest();
        request.setPassword("1123123");
        request.setRole(Role.USER);

        mockMvc.perform(patch("/api/v1/user/{id}", accountService.find(properties.getAdminEmail()).getId())
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + getAccessToken())
                .content(objectMapper.writeValueAsString(request))
                .accept(MediaTypes.HAL_JSON)
                .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("유저 정보 전체 수정 성공하는 경우")
    public void mergeUser_V1_Success_200() throws Exception {
        mockRestTemplate(HttpStatus.OK, accountService.find(properties.getAdminEmail()));
        String name = "수정성공";
        String password = "1123123";
        Gender gender = Gender.F;
        Address address = new Address("경기도 과천시", "부림2길 76 2층", "13830");
        Role role = Role.ADMIN;

        AccountApiController.UpdateUserRequest request = new AccountApiController.UpdateUserRequest();
        request.setName(name);
        request.setPassword(password);
        request.setGender(gender);
        request.setAddress(address);
        request.setRole(role);

        mockMvc.perform(put("/api/v1/user/{id}", accountService.find(properties.getUserEmail()).getId())
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + getAccessToken())
                .content(objectMapper.writeValueAsString(request))
                .accept(MediaTypes.HAL_JSON)
                .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andDo(document("merge-user",
                        links(
                                linkWithRel("self").description("self link"),
                                linkWithRel("document").description("document"),
                                linkWithRel("getUser").description("get user link"),
                                linkWithRel("updateUser").description("update user link"),
                                linkWithRel("deleteUser").description("delete user link")
                        ),
                        pathParameters(
                                parameterWithName("id").description("identifier of account")
                        ),
                        requestHeaders(
                                headerWithName(HttpHeaders.AUTHORIZATION).description("bearer token"),
                                headerWithName(HttpHeaders.CONTENT_TYPE).description("Content Type"),
                                headerWithName(HttpHeaders.ACCEPT).description("Accept Header")
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
                                fieldWithPath("_links.self.href").description("self link"),
                                fieldWithPath("_links.document.href").description("document"),
                                fieldWithPath("_links.getUser.href").description("get user link"),
                                fieldWithPath("_links.updateUser.href").description("update user link"),
                                fieldWithPath("_links.deleteUser.href").description("delete user link"),
                                fieldWithPath("_links.self.type").description("self link http method type"),
                                fieldWithPath("_links.getUser.type").description("get user link  http method type"),
                                fieldWithPath("_links.updateUser.type").description("update user link  http method type"),
                                fieldWithPath("_links.deleteUser.type").description("delete user link  http method type"),
                                fieldWithPath("id").description("account id"),
                                fieldWithPath("email").description("account email"),
                                fieldWithPath("name").description("account name"),
                                fieldWithPath("gender").description("account gender"),
                                fieldWithPath("address.city").description("address city"),
                                fieldWithPath("address.street").description("address street"),
                                fieldWithPath("address.zipCode").description("address zipCode"),
                                fieldWithPath("role").description("account role"),
                                fieldWithPath("created").description("account created date"),
                                fieldWithPath("updated").description("account last updated date")
                        )
                        )
                );

        Account merge = accountService.find(properties.getUserEmail());
        assertEquals(name, merge.getName());
        assertTrue(passwordEncoder.matches(password, merge.getPassword()));
        assertEquals(gender, merge.getGender());
        assertEquals(address, merge.getAddress());
        assertEquals(role, merge.getRole());
    }

    @Test
    @DisplayName("유저 정보 전체 수정 권한 없어서 실패하는 경우")
    public void mergeUser_V1_Unauthorized_401() throws Exception {
        mockRestTemplate(HttpStatus.UNAUTHORIZED, null);
        AccountApiController.UpdateUserRequest request = new AccountApiController.UpdateUserRequest();
        request.setName("테스트");
        request.setPassword("1123123");
        request.setGender(Gender.F);
        request.setAddress(new Address("경기도 과천시", "부림2길 76 2층", "13830"));
        request.setRole(Role.USER);

        mockMvc.perform(put("/api/v1/user/{id}", accountService.find(properties.getAdminEmail()).getId())
                .header(HttpHeaders.AUTHORIZATION, "Bearer Invalid Token")
                .content(objectMapper.writeValueAsString(request))
                .accept(MediaTypes.HAL_JSON)
                .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("유저 정보 전체 수정 권한 없어서 실패하는 경우")
    public void mergeUser_V1_Success_403() throws Exception {
        mockRestTemplate(HttpStatus.OK, accountService.find(properties.getUserEmail()));
        AccountApiController.UpdateUserRequest request = new AccountApiController.UpdateUserRequest();
        request.setName("테스트");
        request.setPassword("1123123");
        request.setGender(Gender.F);
        request.setAddress(new Address("경기도 과천시", "부림2길 76 2층", "13830"));
        request.setRole(Role.USER);

        mockMvc.perform(put("/api/v1/user/{id}", accountService.find(properties.getAdminEmail()).getId())
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + getAccessToken())
                .content(objectMapper.writeValueAsString(request))
                .accept(MediaTypes.HAL_JSON)
                .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("유저 정보 전체 수정 유저 정보 존재하지 않는 경우")
    public void mergeUser_V1_NotFound_404() throws Exception {
        mockRestTemplate(HttpStatus.OK, accountService.find(properties.getAdminEmail()));
        AccountApiController.UpdateUserRequest request = new AccountApiController.UpdateUserRequest();
        request.setName("테스트");
        request.setPassword("1123123");
        request.setGender(Gender.F);
        request.setAddress(new Address("경기도 과천시", "부림2길 76 2층", "13830"));
        request.setRole(Role.ADMIN);

        mockMvc.perform(put("/api/v1/user/{id}", "198237981")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + getAccessToken())
                .content(objectMapper.writeValueAsString(request))
                .accept(MediaTypes.HAL_JSON)
                .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("유저 정보 전체 수정 잘못된 입력 값으로 실패하는 경우")
    public void mergeUser_V1_Invalid_400() throws Exception {
        mockRestTemplate(HttpStatus.OK, accountService.find(properties.getAdminEmail()));
        AccountApiController.UpdateUserRequest request = new AccountApiController.UpdateUserRequest();
        request.setName("테스트");
        request.setPassword("1");
        request.setGender(Gender.F);
        request.setAddress(new Address("", "", ""));
        request.setRole(Role.ADMIN);

        mockMvc.perform(put("/api/v1/user/{id}", accountService.find(properties.getUserEmail()).getId())
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + getAccessToken())
                .content(objectMapper.writeValueAsString(request))
                .accept(MediaTypes.HAL_JSON)
                .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("유저 정보 삭제 성공적인 경우")
    public void deleteUser_V1_Success_200() throws Exception {
        mockRestTemplate(HttpStatus.OK, accountService.find(properties.getAdminEmail()));
        mockMvc.perform(delete("/api/v1/user/{id}", accountService.find(properties.getUserEmail()).getId())
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + getAccessToken())
                .accept(MediaTypes.HAL_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andDo(document("delete-user",
                        pathParameters(
                                parameterWithName("id").description("identifier of account")
                        ),
                        requestHeaders(
                                headerWithName(HttpHeaders.ACCEPT).description("Accept Header"),
                                headerWithName(HttpHeaders.AUTHORIZATION).description("bearer token")
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
                                fieldWithPath("count").description("count of delete"),
                                fieldWithPath("_links.self.href").description("self link"),
                                fieldWithPath("_links.document.href").description("document"),
                                fieldWithPath("_links.self.type").description("self link http method type")
                        )
                        )
                );
    }

    @Test
    @DisplayName("유저 정보 삭제 유효하지 않은 토큰으로 실패하는 경우")
    public void deleteUser_V1_Unauthorized_401() throws Exception {
        mockRestTemplate(HttpStatus.UNAUTHORIZED, null);
        mockMvc.perform(delete("/api/v1/user/{id}", accountService.find(properties.getAdminEmail()).getId())
                .header(HttpHeaders.AUTHORIZATION, "Bearer Invalid Token")
                .accept(MediaTypes.HAL_JSON))
                .andDo(print())
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("유저 정보 삭제 권한 없어서 실패하는 경우")
    public void deleteUser_V1_Forbidden_403() throws Exception {
        mockRestTemplate(HttpStatus.OK, accountService.find(properties.getUserEmail()));
        mockMvc.perform(delete("/api/v1/user/{id}", accountService.find(properties.getAdminEmail()).getId())
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + getAccessToken())
                .accept(MediaTypes.HAL_JSON))
                .andDo(print())
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("유저 정보 존재 하지 않아서 실패하는 경우")
    public void deleteUser_V1_Forbidden_404() throws Exception {
        mockRestTemplate(HttpStatus.OK, accountService.find(properties.getAdminEmail()));
        mockMvc.perform(delete("/api/v1/user/{id}", "123123123123")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + getAccessToken())
                .accept(MediaTypes.HAL_JSON))
                .andDo(print())
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("유저 정보 잘못된 입력 값으로 실패하는 경우")
    public void deleteUser_V1_Invalid_400() throws Exception {
        mockRestTemplate(HttpStatus.OK, accountService.find(properties.getAdminEmail()));
        mockMvc.perform(delete("/api/v1/user/{id}", "asdasd")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + getAccessToken())
                .accept(MediaTypes.HAL_JSON))
                .andDo(print())
                .andExpect(status().isBadRequest());
    }

    /**
     * 테스트 계정 및 클라이언트 생성
     */
    private void generateAccounts() {
        IntStream.range(0, 30).forEach(n -> {
                    Account account = new Account();
                    account.setName("테스트" + n);
                    account.setEmail(UUID.randomUUID().toString() + "@test.com");
                    account.setPassword("test");
                    account.setGender(n % 2 == 0 ? Gender.M : Gender.F);
                    account.setAddress(new Address("경기도 과천시", "부림2길 76 2층", "13830"));
                    account.setRole(n % 2 == 0 ? Role.USER : Role.ADMIN);
                    accountService.generate(account);
                }
        );
    }


}