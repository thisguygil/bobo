package bobo.utils;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.filter.Filter;
import ch.qos.logback.core.spi.FilterReply;

public class LoggingFilter extends Filter<ILoggingEvent> {
    @Override
    public FilterReply decide(ILoggingEvent iLoggingEvent) {
        String message = iLoggingEvent.getMessage();
        if (message.contains("There was some random exception while waiting for udp packets") ||
                message.contains("Something went wrong when decoding the track.") ||
                message.contains("Decrypt failed with error code FAILURE") ||
                message.contains("Failed to decrypt audio packet for user")
        ) return FilterReply.DENY;

        return FilterReply.NEUTRAL;
    }
}