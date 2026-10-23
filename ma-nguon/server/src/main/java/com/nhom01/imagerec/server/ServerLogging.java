package com.nhom01.imagerec.server;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.logging.ConsoleHandler;
import java.util.logging.FileHandler;
import java.util.logging.Formatter;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

/** Cấu hình java.util.logging: console + logs/server.log, mỗi dòng {@code HH:mm:ss LEVEL message}. */
public final class ServerLogging {

    private static final Path LOG_DIR = Path.of("logs");
    private static final String LOG_FILE = "server.log";
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm:ss");

    private ServerLogging() {
    }

    /** Formatter 1 dòng; dùng chung cho console, file và khung log của {@link ServerWindow}. */
    public static final class LineFormatter extends Formatter {
        @Override
        public String format(LogRecord record) {
            String time = LocalTime.ofInstant(record.getInstant(), ZoneId.systemDefault()).format(TIME);
            StringBuilder line = new StringBuilder(time).append(' ').append(record.getLevel().getName())
                    .append(' ').append(formatMessage(record)).append(System.lineSeparator());
            if (record.getThrown() != null) {
                StringWriter trace = new StringWriter();
                record.getThrown().printStackTrace(new PrintWriter(trace));
                line.append(trace);
            }
            return line.toString();
        }
    }

    public static void configure() throws IOException {
        Files.createDirectories(LOG_DIR);
        Logger root = Logger.getLogger("");
        for (Handler handler : root.getHandlers()) {
            root.removeHandler(handler);
        }
        root.setLevel(Level.INFO);

        ConsoleHandler console = new ConsoleHandler();
        console.setFormatter(new LineFormatter());
        console.setLevel(Level.ALL);
        root.addHandler(console);

        FileHandler file = new FileHandler(LOG_DIR.resolve(LOG_FILE).toString(), true);
        file.setEncoding(StandardCharsets.UTF_8.name());
        file.setFormatter(new LineFormatter());
        root.addHandler(file);
    }
}
