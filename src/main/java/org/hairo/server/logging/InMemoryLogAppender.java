package org.hairo.server.logging;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.AppenderBase;
import java.util.concurrent.LinkedBlockingDeque;

public class InMemoryLogAppender extends AppenderBase<ILoggingEvent> {

    private static final int MAX_LOG_LINES = 10_000;
    private static final LinkedBlockingDeque<String> logBuffer = new LinkedBlockingDeque<>(MAX_LOG_LINES);

    @Override
    protected void append(ILoggingEvent event) {
        synchronized (logBuffer) {
            if (logBuffer.size() == MAX_LOG_LINES) {
                logBuffer.pollFirst();
            }
            logBuffer.addLast(
                    event.getTimeStamp() + " - " + event.getLoggerName() + " - " + event.getFormattedMessage());
        }
    }

    public static String getLogDump() {
        synchronized (logBuffer) {
            return String.join("<br>", logBuffer);
        }
    }
}