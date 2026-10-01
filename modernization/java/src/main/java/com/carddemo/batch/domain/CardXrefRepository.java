package com.carddemo.batch.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CardXrefRepository extends JpaRepository<CardXref, String> {

    Optional<CardXref> findFirstByAccountIdOrderByCardNumberAsc(Long accountId);
}
