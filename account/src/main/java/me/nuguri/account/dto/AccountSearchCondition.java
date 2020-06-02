package me.nuguri.account.dto;

import lombok.Getter;
import lombok.Setter;
import me.nuguri.common.dto.BaseSearchCondition;
import me.nuguri.common.dto.PageableCondition;
import me.nuguri.common.entity.Address;
import me.nuguri.common.enums.Gender;
import me.nuguri.common.enums.Role;

import javax.persistence.*;

@Getter
@Setter
public class AccountSearchCondition extends PageableCondition {

    /** 이메일 */
    private String email;

    /** 이름 */
    private String name;

    /** 성별 */
    private Gender gender;

    /** 주소 */
    private Address address;

    /** 권한 */
    private Role role;

}
