package me.nuguri.account.repository;

import me.nuguri.account.dto.AccountSearchCondition;
import me.nuguri.common.entity.Account;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Transactional
public interface AccountRepositoryCustom {

    Page<Account> pageByCondition(AccountSearchCondition condition, Pageable pageable);

    Optional<Account> findByEmailFetchClients(String email);

    long deleteByIdsBatchInQuery(List<Long> ids);

}
