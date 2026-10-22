package dev.bloatware.exorcism;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

final class Shell {
    static final class Result {
        final int code;
        final String output;
        Result(int code, String output) { this.code = code; this.output = output; }
    }

    static Result run(String command) throws Exception { return run(command, ""); }

    static Result run(String command, String input) throws Exception {
        Process p = new ProcessBuilder("su", "-c", command).redirectErrorStream(true).start();
        p.getOutputStream().write(input.getBytes(StandardCharsets.UTF_8));
        p.getOutputStream().close();
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        Thread reader = new Thread(() -> {
            try (InputStream stream = p.getInputStream()) {
                byte[] bytes = new byte[4096];
                int n;
                while ((n = stream.read(bytes)) != -1) {
                    synchronized (buffer) {
                        if (buffer.size() < 1048576) buffer.write(bytes, 0, Math.min(n, 1048576 - buffer.size()));
                    }
                }
            } catch (Exception ignored) { }
        });
        reader.setDaemon(true);
        reader.start();
        if (!p.waitFor(30, TimeUnit.SECONDS)) {
            p.destroyForcibly();
            reader.join(1000);
            throw new IllegalStateException("Command exceeded 30 seconds. Check KernelSU and refresh the scan to reconcile the state.");
        }
        reader.join(2000);
        synchronized (buffer) { return new Result(p.exitValue(), buffer.toString(StandardCharsets.UTF_8.name()).trim()); }
    }
}
