package ua.utility.kfsdbupgrade.analysis;

import static com.google.common.base.Optional.absent;
import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static ua.utility.kfsdbupgrade.analysis.JobResultType.UNKNOWN;

import com.google.common.base.Optional;

import ua.utility.kfsdbupgrade.md.base.TimedInterval;

public final class JobResult {

    private final Optional<BuildKey> parent;
    private final Optional<String> folder;
    private final String job;
    private final int buildNumber;
    private final TimedInterval timing;
    private final JobResultType result;
    private final String output;

    private JobResult(Builder builder) {
        this.parent = builder.parent;
        this.folder = builder.folder;
        this.job = builder.job;
        this.buildNumber = builder.buildNumber;
        this.timing = builder.timing;
        this.result = builder.result;
        this.output = builder.output;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {

        private Optional<BuildKey> parent = absent();
        private Optional<String> folder = absent();
        private String job;
        private int buildNumber = -1;
        private TimedInterval timing;
        private JobResultType result = UNKNOWN;
        private String output;

        public Builder withParent(Optional<BuildKey> parent) {
            this.parent = parent;
            return this;
        }

        public Builder withFolder(Optional<String> folder) {
            this.folder = folder;
            return this;
        }

        public Builder withJob(String job) {
            this.job = job;
            return this;
        }

        public Builder withBuildNumber(int buildNumber) {
            this.buildNumber = buildNumber;
            return this;
        }

        public Builder withTiming(TimedInterval timing) {
            this.timing = timing;
            return this;
        }

        public Builder withResult(JobResultType result) {
            this.result = result;
            return this;
        }

        public Builder withOutput(String output) {
            this.output = output;
            return this;
        }

        public JobResult build() {
            return validate(new JobResult(this));
        }

        private static JobResult validate(JobResult instance) {
            checkArgument(isNotBlank(instance.job), "job cannot be blank");
            checkArgument(instance.buildNumber >= 1, "buildNumber must be >= 1");
            checkNotNull(instance.timing, "timing may not be null");
            checkNotNull(instance.result, "result may not be null");
            checkArgument(isNotBlank(instance.output), "output cannot be blank");
            return instance;
        }
    }

    public String getJob() {
        return job;
    }

    public int getBuildNumber() {
        return buildNumber;
    }

    public TimedInterval getTiming() {
        return timing;
    }

    public JobResultType getResult() {
        return result;
    }

    public String getOutput() {
        return output;
    }

    public Optional<BuildKey> getParent() {
        return parent;
    }

    public Optional<String> getFolder() {
        return folder;
    }

}
