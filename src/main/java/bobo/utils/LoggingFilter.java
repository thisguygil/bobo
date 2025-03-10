package bobo.utils;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.filter.Filter;
import ch.qos.logback.core.spi.FilterReply;

public class LoggingFilter extends Filter<ILoggingEvent> {
    @Override
    public FilterReply decide(ILoggingEvent iLoggingEvent) {
        String message = iLoggingEvent.getMessage();
        if (message.contains("There was some random exception while waiting for udp packets") || message.contains("Something went wrong when decoding the track.")) {
            return FilterReply.DENY;
        }

        return FilterReply.NEUTRAL;
    }
}