package com.carddemo.batch;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Runs POSTTRAN then INTCALC on the repository sample data and compares every output file with the
 * golden output recorded from the COBOL programs (modernization/golden/daily-cycle).
 */
class DailyCycleParityTest {

    private static final Path REPO = Path.of("../..").toAbsolutePath().normalize();
    private static final Path SAMPLE_DATA = REPO.resolve("app/data/ASCII");
    private static final Path GOLDEN = REPO.resolve("modernization/golden/daily-cycle");

    @TempDir
    Path work;

    @Test
    void dailyCycleMatchesCobolGoldenOutput() throws IOException {
        Path seed = work.resolve("00-seed");
        Files.createDirectories(seed);
        pad("acctdata.txt", 300, seed);
        pad("cardxref.txt", 50, seed);
        pad("tcatbal.txt", 50, seed);
        pad("discgrp.txt", 50, seed);
        pad("dailytran.txt", 350, seed);
        Files.writeString(seed.resolve("transact.txt"), "");

        Path posttran = work.resolve("01-posttran");
        int posttranRc = run("posttran", seed, posttran, "");

        Path intcalcIn = work.resolve("01-state");
        Files.createDirectories(intcalcIn);
        for (String f : List.of("acctdata.txt", "tcatbal.txt")) {
            Files.copy(posttran.resolve(f), intcalcIn.resolve(f));
        }
        for (String f : List.of("cardxref.txt", "discgrp.txt")) {
            Files.copy(seed.resolve(f), intcalcIn.resolve(f));
        }
        Path intcalc = work.resolve("02-intcalc");
        int intcalcRc = run("intcalc", intcalcIn, intcalc, "2022071800");

        assertThat(posttranRc).isEqualTo(goldenReturnCode("01-posttran/posttran.rc"));
        assertThat(intcalcRc).isEqualTo(goldenReturnCode("02-intcalc/intcalc.rc"));
        assertMatchesGolden("01-posttran", posttran);
        assertMatchesGolden("02-intcalc", intcalc);
    }

    private static int run(String step, Path in, Path out, String parm) {
        ConfigurableApplicationContext context = SpringApplication.run(CardDemoBatchApplication.class,
                "--carddemo.step=" + step, "--carddemo.in=" + in, "--carddemo.out=" + out,
                "--carddemo.parm=" + parm, "--carddemo.clock.fixed=2022-07-18T10:11:12Z");
        return SpringApplication.exit(context);
    }

    private static void pad(String name, int length, Path dir) throws IOException {
        StringBuilder text = new StringBuilder();
        for (String line : Files.readAllLines(SAMPLE_DATA.resolve(name), StandardCharsets.ISO_8859_1)) {
            text.append(line.length() >= length ? line.substring(0, length) : line + " ".repeat(length - line.length()))
                    .append('\n');
        }
        Files.writeString(dir.resolve(name), text, StandardCharsets.ISO_8859_1);
    }

    private static int goldenReturnCode(String file) throws IOException {
        return Integer.parseInt(Files.readString(GOLDEN.resolve(file)).trim());
    }

    private static void assertMatchesGolden(String stepDir, Path actualDir) throws IOException {
        List<Path> expected;
        try (Stream<Path> files = Files.list(GOLDEN.resolve(stepDir))) {
            expected = files.filter(f -> !f.toString().endsWith(".rc")).sorted().toList();
        }
        assertThat(expected).isNotEmpty();
        for (Path golden : expected) {
            Path actual = actualDir.resolve(golden.getFileName());
            assertThat(actual).as(stepDir + "/" + golden.getFileName()).exists();
            assertThat(Files.readString(actual, StandardCharsets.ISO_8859_1))
                    .as(stepDir + "/" + golden.getFileName())
                    .isEqualTo(Files.readString(golden, StandardCharsets.ISO_8859_1));
        }
    }
}
