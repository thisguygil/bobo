package bobo.utils;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.filter.Filter;
import ch.qos.logback.core.spi.FilterReply;

public class LoggingFilter extends Filter<ILoggingEvent> {
    @Override
    public FilterReply decide(ILoggingEvent iLoggingEvent) {
        // Filter out the random exception message
        if (iLoggingEvent.getMessage().contains("There was some random exception while waiting for udp packets")) {
            return FilterReply.DENY;
        }

        return FilterReply.NEUTRAL;
    }
}