package me.nuguri.auth.controller.api;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import me.nuguri.auth.annotation.AuthenticationUser;
import me.nuguri.auth.annotation.HasAuthority;
import me.nuguri.auth.annotation.Oauth2Authentication;
import me.nuguri.auth.domain.AccountAdapter;
import me.nuguri.auth.entity.Account;
import me.nuguri.auth.exception.UserNotExistException;
import me.nuguri.auth.service.AccountService;
import me.nuguri.common.domain.ErrorResponse;
import me.nuguri.common.domain.Pagination;
import me.nuguri.common.domain.PaginationResource;
import me.nuguri.common.enums.Role;
import me.nuguri.common.validator.PaginationValidator;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.MediaTypes;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.annotation.Secured;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.provider.OAuth2Authentication;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.validation.Errors;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;
import java.security.Principal;
import java.util.Set;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

@RestController
@RequiredArgsConstructor
public class AccountApiController {

    private final AccountService accountService;

    private final PaginationValidator paginationValidator;

    private final AccountValidator accountValidator;

    private final ModelMapper modelMapper;

    /**
     * 유저 정보 페이징 조회
     * @param pagination page 페이지 번호, size 페이지 당 갯수, sort 정렬(방식,기준)
     * @param errors 에러
     * @return
     */
    @GetMapping(value = "/api/v1/users", produces = MediaTypes.HAL_JSON_VALUE)
    @PreAuthorize("(hasRole('ADMIN') or #oauth2.clientHasRole('ADMIN')) and #oauth2.hasScope('read')")
    public ResponseEntity<?> queryUsers(Pagination pagination, Errors errors) {
        paginationValidator.validate(pagination, Account.class, errors);
        if (errors.hasErrors()) {
            return ResponseEntity.badRequest().body(new ErrorResponse(HttpStatus.BAD_REQUEST, "invalid parameter value", errors));
        }
        Page<Account> page = accountService.findAll(pagination.getPageable());
        if (page.getNumberOfElements() < 1) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ErrorResponse(HttpStatus.NOT_FOUND, page.getTotalElements() < 1 ? "content of all pages does not exist" : "content of current page does not exist"));
        }
        PaginationResource<QueryUsersResource> resource = new PaginationResource<>(page, account -> new QueryUsersResource(modelMapper.map(account, GetUserResponse.class)));
        resource.addPaginationLink(pagination, linkTo(methodOn(AccountApiController.class).queryUsers(null, null)));
        resource.add(linkTo(AccountApiController.class).slash("/docs/account.html").withRel("document"));
        return ResponseEntity.ok(resource);
    }

    /**
     * 유저 정보 조회
     * @param id 식별키
     * @return
     */
    @GetMapping(value = "/api/v1/user/{id}", produces = MediaTypes.HAL_JSON_VALUE)
    @PreAuthorize("#oauth2.hasScope('read')")
    @HasAuthority
    public ResponseEntity<?> getUser(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(new GetUserResource(modelMapper.map(accountService.find(id), GetUserResponse.class)));
        } catch (UserNotExistException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ErrorResponse(HttpStatus.NOT_FOUND, "not exist account of id"));
        }
    }

    /**
     * 유저 정보 생성
     * @param request email 이메일, password 비밀번호, roles 권한
     * @param errors 에러
     * @return
     */
    @PostMapping(value = "/api/v1/user", produces = MediaTypes.HAL_JSON_VALUE)
    public ResponseEntity<?> generateUser(@RequestBody @Valid GenerateUserRequest request, Errors errors) {
        Account account = modelMapper.map(request, Account.class);
        accountValidator.validate(account, errors);
        if (errors.hasErrors()) {
            return ResponseEntity.badRequest().body(new ErrorResponse(HttpStatus.BAD_REQUEST, "invalid value", errors));
        }
        if (accountService.exist(request.getEmail())) {
            return ResponseEntity.badRequest().body(new ErrorResponse(HttpStatus.BAD_REQUEST, "email is already exist"));
        }
        return ResponseEntity.created(linkTo(methodOn(AccountApiController.class)
                .generateUser(null, null))
                .toUri())
                .body(new GenerateUserResource(modelMapper.map(accountService.generate(account), GetUserResponse.class)));
    }

    /**
     * 유저 정보 입력된 값만 변경
     * @param id 식별키
     * @param request password 비밀번호, roles 권한
     * @param errors 에러
     * @return
     */
    @PatchMapping(value = "/api/v1/user/{id}")
    @PreAuthorize("#oauth2.hasScope('write')")
    @HasAuthority
    public ResponseEntity<?> updateUser(@PathVariable Long id, @RequestBody UpdateUserRequest request, Errors errors) {
        Account account = modelMapper.map(request, Account.class);
        account.setId(id);
        accountValidator.validate(account, errors);
        if (errors.hasErrors()) {
            return ResponseEntity.badRequest().body(new ErrorResponse(HttpStatus.BAD_REQUEST, "invalid value", errors));
        }
        try {
            return ResponseEntity.ok(new UpdateUserResource(modelMapper.map(accountService.update(account), GetUserResponse.class)));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ErrorResponse(HttpStatus.NOT_FOUND, "not exist account of id"));
        }
    }

    /**
     * 유저 정보 전체 값 변경, 없는 유저의 경우 생성
     * @param id 식별키
     * @param request password 비밀번호, roles 권한
     * @param errors 에러
     * @return
     */
    @PutMapping(value = "/api/v1/user/{id}")
    @PreAuthorize("#oauth2.hasScope('write')")
    @HasAuthority
    public ResponseEntity<?> mergeUser(@PathVariable Long id, @RequestBody @Valid UpdateUserRequest request, Errors errors) {
        Account account = modelMapper.map(request, Account.class);
        account.setId(id);
        accountValidator.validate(account, errors);
        if (errors.hasErrors()) {
            return ResponseEntity.badRequest().body(new ErrorResponse(HttpStatus.BAD_REQUEST, "invalid value", errors));
        }
        try {
            return ResponseEntity.ok(new MergeUserResource(modelMapper.map(accountService.merge(account), GetUserResponse.class)));
        } catch (UserNotExistException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ErrorResponse(HttpStatus.NOT_FOUND, "not exist account of id"));
        }
    }

    /**
     * 유저 정보 삭제
     * @param id 식별키
     * @return
     */
    @DeleteMapping(value = "/api/v1/user/{id}")
    @PreAuthorize("#oauth2.hasScope('write')")
    @HasAuthority
    public ResponseEntity<?> deleteUser(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(new DeleteUserResource(modelMapper.map(accountService.delete(id), GetUserResponse.class)));
        } catch (UserNotExistException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ErrorResponse(HttpStatus.NOT_FOUND, "not exist account of id"));
        }
    }

    // ==========================================================================================================================================
    // Domain
    @Getter @Setter
    public static class GenerateUserRequest {
        @NotBlank
        private String email;
        @NotBlank
        private String password;
        @NotBlank
        private String name;
        @NotEmpty
        private Set<Role> roles;
    }

    @Getter @Setter
    public static class UpdateUserRequest {
        @NotBlank
        private String password;
        @NotBlank
        private String name;
        @NotEmpty
        private Set<Role> roles;
    }

    @Getter @Setter
    public static class GetUserResponse {
        private Long id;
        private String email;
        private String name;
        private Set<Role> roles;
    }
    // ==========================================================================================================================================

    // ==========================================================================================================================================
    // Resource
    public static class QueryUsersResource extends EntityModel<GetUserResponse> {
        public QueryUsersResource(GetUserResponse content, Link... links) {
            super(content, links);
            add(linkTo(methodOn(AccountApiController.class).queryUsers( null, null)).withSelfRel().withType("GET"));
            add(linkTo(methodOn(AccountApiController.class).getUser(content.getId())).withRel("getUser").withType("GET"));
            add(linkTo(methodOn(AccountApiController.class).updateUser(content.getId(), null, null)).withRel("updateUser").withType("PATCH"));
            add(linkTo(methodOn(AccountApiController.class).mergeUser(content.getId(), null, null)).withRel("mergeUser").withType("PUT"));
            add(linkTo(methodOn(AccountApiController.class).deleteUser(content.getId())).withRel("deleteUser").withType("DELETE"));
        }
    }

    public static class GetUserResource extends EntityModel<GetUserResponse> {
        public GetUserResource(GetUserResponse content, Link... links) {
            super(content, links);
            add(linkTo(AccountApiController.class).slash("/docs/account.html").withRel("document"));
            add(linkTo(methodOn(AccountApiController.class).getUser(content.getId())).withSelfRel().withType("GET"));
            add(linkTo(methodOn(AccountApiController.class).updateUser(content.getId(), null, null)).withRel("updateUser").withType("PATCH"));
            add(linkTo(methodOn(AccountApiController.class).mergeUser(content.getId(), null, null)).withRel("mergeUser").withType("PUT"));
            add(linkTo(methodOn(AccountApiController.class).deleteUser(content.getId())).withRel("deleteUser").withType("DELETE"));
        }
    }

    public static class GenerateUserResource extends EntityModel<GetUserResponse> {
        public GenerateUserResource(GetUserResponse content, Link... links) {
            super(content, links);
            add(linkTo(AccountApiController.class).slash("/docs/account.html").withRel("document"));
            add(linkTo(methodOn(AccountApiController.class).generateUser(null, null)).withSelfRel().withType("POST"));
            add(linkTo(methodOn(AccountApiController.class).getUser(content.getId())).withRel("getUser").withType("GET"));
            add(linkTo(methodOn(AccountApiController.class).updateUser(content.getId(), null, null)).withRel("updateUser").withType("PATCH"));
            add(linkTo(methodOn(AccountApiController.class).mergeUser(content.getId(), null, null)).withRel("mergeUser").withType("PUT"));
            add(linkTo(methodOn(AccountApiController.class).deleteUser(content.getId())).withRel("deleteUser").withType("DELETE"));
        }
    }

    public static class UpdateUserResource extends EntityModel<GetUserResponse> {
        public UpdateUserResource(GetUserResponse content, Link... links) {
            super(content, links);
            add(linkTo(AccountApiController.class).slash("/docs/account.html").withRel("document"));
            add(linkTo(methodOn(AccountApiController.class).updateUser(content.getId(), null, null)).withSelfRel().withType("PATCH"));
            add(linkTo(methodOn(AccountApiController.class).getUser(content.getId())).withRel("getUser").withType("GET"));
            add(linkTo(methodOn(AccountApiController.class).mergeUser(content.getId(), null, null)).withRel("mergeUser").withType("PUT"));
            add(linkTo(methodOn(AccountApiController.class).deleteUser(content.getId())).withRel("deleteUser").withType("DELETE"));
        }
    }

    public static class MergeUserResource extends EntityModel<GetUserResponse> {
        public MergeUserResource(GetUserResponse content, Link... links) {
            super(content, links);
            add(linkTo(AccountApiController.class).slash("/docs/account.html").withRel("document"));
            add(linkTo(methodOn(AccountApiController.class).mergeUser(content.getId(), null, null)).withSelfRel().withType("PUT"));
            add(linkTo(methodOn(AccountApiController.class).getUser(content.getId())).withRel("getUser").withType("GET"));
            add(linkTo(methodOn(AccountApiController.class).updateUser(content.getId(), null, null)).withRel("updateUser").withType("PATCH"));
            add(linkTo(methodOn(AccountApiController.class).deleteUser(content.getId())).withRel("deleteUser").withType("DELETE"));
        }
    }

    public static class DeleteUserResource extends EntityModel<GetUserResponse> {
        public DeleteUserResource(GetUserResponse content, Link... links) {
            super(content, links);
            add(linkTo(AccountApiController.class).slash("/docs/account.html").withRel("document"));
            add(linkTo(methodOn(AccountApiController.class).deleteUser(content.getId())).withSelfRel().withType("DELETE"));
            add(linkTo(methodOn(AccountApiController.class).getUser(content.getId())).withRel("getUser").withType("GET"));
            add(linkTo(methodOn(AccountApiController.class).mergeUser(content.getId(), null, null)).withRel("mergeUser").withType("PUT"));
            add(linkTo(methodOn(AccountApiController.class).updateUser(content.getId(), null, null)).withRel("updateUser").withType("PATCH"));
        }
    }
    // ==========================================================================================================================================

    // ==========================================================================================================================================
    // Validator
    @Component
    public static class AccountValidator {
        /**
         * Account 도메인 condition 값 중 이메일, 비밀번호 검증
         * @param request email 이메일, password 비밀번호
         * @param errors 에러
         */
        public void validate(Account request, Errors errors) {
            String email = request.getEmail();
            String password = request.getPassword();
            if (!StringUtils.isEmpty(email)) {
                if (!email.matches("^[a-zA-Z0-9_-]{5,15}@[a-zA-Z0-9-]{1,10}\\.[a-zA-Z]{2,6}$")) {
                    errors.rejectValue("email", "wrongValue", "email is wrong ex) [alphabet or number 10~15]@[alphabet or number 1~10].[alphabet 2~6]");
                }
            }
            if (!StringUtils.isEmpty(password)) {
                if (!password.matches("^.{5,15}$")) {
                    errors.rejectValue("password", "wrongValue", "password is wrong, any character from 5 to 15");
                }
            }
        }
    }
    // ==========================================================================================================================================
    
}
