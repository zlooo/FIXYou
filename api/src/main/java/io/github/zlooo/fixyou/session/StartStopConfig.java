package io.github.zlooo.fixyou.session;

import lombok.Builder;
import lombok.Getter;

import java.time.DayOfWeek;
import java.time.OffsetTime;

@Builder
@Getter
public class StartStopConfig {

    public static final StartStopConfig INFINITE = new StartStopConfig(null, null, null, null);

    private OffsetTime startTime;
    private OffsetTime stopTime;
    private DayOfWeek startDay;
    private DayOfWeek stopDay;
}
