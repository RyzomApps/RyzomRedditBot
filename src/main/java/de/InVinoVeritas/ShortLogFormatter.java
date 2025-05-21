package de.InVinoVeritas;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.Formatter;
import java.util.logging.LogRecord;

public class ShortLogFormatter extends Formatter {
    private static final SimpleDateFormat dateFormat = new SimpleDateFormat("HH:mm:ss");

    @Override
    public String format(LogRecord record) {
        String timestamp = dateFormat.format(new Date(record.getMillis()));
        String level = record.getLevel().getLocalizedName().toUpperCase();
        String message = formatMessage(record);
        return String.format("[%s] [%s]: %s%n", timestamp, level, message);
    }
}