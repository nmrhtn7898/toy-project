package me.nuguri.common.entity;

import lombok.*;

import javax.persistence.*;

/**
 * 클라이언트, 접근 권한 매핑 엔티티
 */
@Entity
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EqualsAndHashCode(of = "id", callSuper = false)
public class ClientAuthority extends BaseEntity {

    /**
     * 식별키
     */
    @Id
    @GeneratedValue
    private Long id;

    /**
     * 클라이언트 엔티티
     */
    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    private Client client;

    /**
     * 클라이언트 접근 권한 엔티티, 단방향
     */
    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    private Authority authority;

    @Builder
    protected ClientAuthority(Long id, Client client, Authority authority) {
        this.id = id;
        this.addClient(client);
        this.authority = authority;
    }

    /**
     * 양방향 관계 설정
     *
     * @param client 클라이언트
     */
    public void addClient(Client client) {
        this.client = client;
        client.getClientAuthorities().add(this);
    }

}
