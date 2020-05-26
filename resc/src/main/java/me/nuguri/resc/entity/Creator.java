package me.nuguri.resc.entity;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import me.nuguri.resc.enums.Gender;

import javax.persistence.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * 저자 엔티티
 */
@Entity
@Getter
@Setter
@EqualsAndHashCode(of = "id", callSuper = false)
public class Creator extends BaseEntity {

    /** 식별키 */
    @Id
    @GeneratedValue
    private Long id;

    /** 이름 */
    @Column(nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Gender gender;

    /** 출생 날짜 */
    private LocalDate birth;

    /** 사망 날짜 */
    private LocalDate death;

    @OneToMany(mappedBy = "creator", cascade = CascadeType.ALL)
    private List<Product> products = new ArrayList<>();

}
