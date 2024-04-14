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

        // Filter out the YouTube auth warning
        if (iLoggingEvent.getMessage().equals("YouTube auth tokens can't be retrieved because email and password is not set in YoutubeAudioSourceManager, age restricted videos will throw exceptions.")) {
            return FilterReply.DENY;
        }

        return FilterReply.NEUTRAL;
    }
}