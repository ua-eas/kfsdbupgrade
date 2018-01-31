package ua.utility.kfsdbupgrade.analysis;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import ua.utility.kfsdbupgrade.md.base.TimedInterval;

public final class DbUpgradeSummary {

    public DbUpgradeSummary(TimedInterval timing, int steps, long logs, LogStats stats) {
        checkArgument(steps >= 0, "steps must be >= 0");
        checkArgument(logs >= 0, "logs must be >= 0");
        this.timing = checkNotNull(timing);
        this.stats = checkNotNull(stats);
        this.steps = steps;
        this.logs = logs;
    }

    private final TimedInterval timing;
    private final int steps;
    private final long logs;
    private final LogStats stats;

    public TimedInterval getTiming() {
        return timing;
    }

    public int getSteps() {
        return steps;
    }

    public long getLogs() {
        return logs;
    }

    public LogStats getStats() {
        return stats;
    }

}
