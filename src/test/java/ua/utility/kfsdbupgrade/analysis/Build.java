package ua.utility.kfsdbupgrade.analysis;

import static com.google.common.base.Optional.absent;
import static com.google.common.base.Preconditions.checkArgument;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

import com.google.common.base.Optional;

public final class Build {

    private final int number;
    private final String buildXml;
    private final Optional<String> changeLogXml;
    private final String log;

    public String getBuildXml() {
        return buildXml;
    }

    public Optional<String> getChangeLogXml() {
        return changeLogXml;
    }

    public String getLog() {
        return log;
    }

    private Build(Builder builder) {
        this.number = builder.number;
        this.buildXml = builder.buildXml;
        this.changeLogXml = builder.changeLogXml;
        this.log = builder.log;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {

        private int number;
        private String buildXml;
        private Optional<String> changeLogXml = absent();
        private String log;

        public Builder withNumber(int number) {
            this.number = number;
            return this;
        }

        public Builder withBuildXml(String buildXml) {
            this.buildXml = buildXml;
            return this;
        }

        public Builder withChangeLogXml(Optional<String> changeLogXml) {
            this.changeLogXml = changeLogXml;
            return this;
        }

        public Builder withLog(String log) {
            this.log = log;
            return this;
        }

        public Build build() {
            return validate(new Build(this));
        }

        private static Build validate(Build instance) {
            checkArgument(instance.number >= 1, "number must be >= 1");
            checkArgument(isNotBlank(instance.buildXml), "buildXml may not be blank");
            checkArgument(instance.changeLogXml.isPresent() ? isNotBlank(instance.changeLogXml.get()) : true, "changeLogXml cannot be blank");
            checkArgument(isNotBlank(instance.log), "log may not be blank");
            return instance;
        }
    }

    public int getNumber() {
        return number;
    }

}
