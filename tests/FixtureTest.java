package dev.bloatware.exorcism;

import java.nio.file.Files;
import java.nio.file.Path;

public final class FixtureTest {
    public static void main(String[] args) throws Exception {
        Path root = Path.of(args.length == 0 ? "tests/fixtures" : args[0]);
        int count = 0;
        for (String row : Files.readAllLines(root.resolve("cases.tsv"))) {
            if (row.isBlank()) continue;
            String[] c = row.split("\\t");
            String text = Files.readString(root.resolve(c[0]));
            boolean reject = c[2].equals("error");
            try {
                Policy.State state = Policy.parseState(text, Integer.parseInt(c[1]));
                if (reject || state.installed != Boolean.parseBoolean(c[2]) || state.enabled != Integer.parseInt(c[3])) throw new AssertionError("Unexpected parse: " + c[0]);
            } catch (IllegalStateException error) { if (!reject) throw new AssertionError(c[0], error); }
            count++;
        }
        System.out.println(count + " dumpsys fixtures passed; origins documented separately");
    }
}
