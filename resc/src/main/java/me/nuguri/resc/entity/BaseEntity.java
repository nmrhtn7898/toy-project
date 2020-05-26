package me.nuguri.resc.entity;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import javax.persistence.*;
import java.time.LocalDateTime;

/**
 * 공통 엔티티
 */
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
public abstract class BaseEntity {

    /** 생성 날짜 */
    @CreatedDate
    @Column(updatable = false)
    private LocalDateTime created;

    /** 수정 날짜 */
    @LastModifiedDate
    private LocalDateTime updated;

}
