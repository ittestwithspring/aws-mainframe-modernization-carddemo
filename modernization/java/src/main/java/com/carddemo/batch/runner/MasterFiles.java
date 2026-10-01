package com.carddemo.batch.runner;

import com.carddemo.batch.domain.AccountRepository;
import com.carddemo.batch.domain.CardXrefRepository;
import com.carddemo.batch.domain.DisclosureGroupRepository;
import com.carddemo.batch.domain.TransactionCategoryBalanceRepository;
import com.carddemo.batch.domain.TransactionRepository;
import com.carddemo.batch.layout.AccountLayout;
import com.carddemo.batch.layout.CardXrefLayout;
import com.carddemo.batch.layout.DisclosureGroupLayout;
import com.carddemo.batch.layout.Fields;
import com.carddemo.batch.layout.TransactionCategoryBalanceLayout;
import com.carddemo.batch.layout.TransactionLayout;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.function.Function;

/**
 * Loads and unloads the VSAM master files as fixed-width text, the format used by app/data/ASCII
 * and the parity harness. Unloads are in primary-key order, like an IDCAMS REPRO.
 */
@Component
public class MasterFiles {

    private final AccountRepository accounts;
    private final CardXrefRepository cardXrefs;
    private final TransactionCategoryBalanceRepository categoryBalances;
    private final DisclosureGroupRepository disclosureGroups;
    private final TransactionRepository transactions;

    public MasterFiles(AccountRepository accounts, CardXrefRepository cardXrefs,
                       TransactionCategoryBalanceRepository categoryBalances,
                       DisclosureGroupRepository disclosureGroups, TransactionRepository transactions) {
        this.accounts = accounts;
        this.cardXrefs = cardXrefs;
        this.categoryBalances = categoryBalances;
        this.disclosureGroups = disclosureGroups;
        this.transactions = transactions;
    }

    public void loadAccounts(Path file) throws IOException {
        accounts.saveAll(read(file, AccountLayout.LENGTH, AccountLayout::parse));
    }

    public void loadCardXrefs(Path file) throws IOException {
        cardXrefs.saveAll(read(file, CardXrefLayout.LENGTH, CardXrefLayout::parse));
    }

    public void loadCategoryBalances(Path file) throws IOException {
        categoryBalances.saveAll(read(file, TransactionCategoryBalanceLayout.LENGTH,
                TransactionCategoryBalanceLayout::parse));
    }

    public void loadDisclosureGroups(Path file) throws IOException {
        disclosureGroups.saveAll(read(file, DisclosureGroupLayout.LENGTH, DisclosureGroupLayout::parse));
    }

    public void loadTransactions(Path file) throws IOException {
        transactions.saveAll(read(file, TransactionLayout.LENGTH, TransactionLayout::parse));
    }

    public void unloadAccounts(Path file) throws IOException {
        write(file, accounts.findAll(Sort.by("id")).stream().map(AccountLayout::format).toList());
    }

    public void unloadCategoryBalances(Path file) throws IOException {
        write(file, categoryBalances.findAll(Sort.by("id.accountId", "id.typeCode", "id.categoryCode"))
                .stream().map(TransactionCategoryBalanceLayout::format).toList());
    }

    public void unloadTransactions(Path file) throws IOException {
        write(file, transactions.findAll(Sort.by("id")).stream().map(TransactionLayout::format).toList());
    }

    private static <T> List<T> read(Path file, int length, Function<String, T> parser) throws IOException {
        return Files.readAllLines(file, StandardCharsets.ISO_8859_1).stream()
                .map(line -> parser.apply(Fields.text(line, length)))
                .toList();
    }

    private static void write(Path file, List<String> records) throws IOException {
        StringBuilder text = new StringBuilder();
        records.forEach(record -> text.append(record).append('\n'));
        Files.writeString(file, text, StandardCharsets.ISO_8859_1);
    }
}
