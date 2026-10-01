package com.carddemo.batch.domain;

import org.springframework.data.jpa.repository.JpaRepository;

public interface TransactionCategoryBalanceRepository extends JpaRepository<TransactionCategoryBalance, TransactionCategoryBalanceId> {
}
