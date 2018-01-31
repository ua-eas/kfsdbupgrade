package ua.utility.kfsdbupgrade.analysis;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

import javax.inject.Provider;

public final class SeverityProvider implements Provider<Severity> {

    public SeverityProvider(LogLineType type, String step, String line) {
        checkArgument(isNotBlank(step));
        this.line = checkNotNull(line);
        this.step = step;
        this.type = checkNotNull(type);
    }

    private final String line;
    private final String step;
    private final LogLineType type;

    private boolean isShadePlugin(String line) {
        if (line.contains("[WARNING] maven-shade-plugin has detected that some .class files")) {
            return true;
        } else if (line.contains("[WARNING] are present in two or more JARs. When this happens, only")) {
            return true;
        } else if (line.contains("[WARNING] one single version of the class is copied in the uberjar.")) {
            return true;
        } else if (line.contains("[WARNING] Usually this is not harmful and you can skeep these")) {
            return true;
        } else if (line.contains("[WARNING] warnings, otherwise try to manually exclude artifacts")) {
            return true;
        } else if (line.contains("[WARNING] based on mvn dependency:tree -Ddetail=true and the above")) {
            return true;
        } else if (line.endsWith("[WARNING] output")) {
            return true;
        } else if (line.endsWith("[WARNING] See http://docs.codehaus.org/display/MAVENUSER/Shade+Plugin")) {
            return true;
        }

        return false;
    }

    public Severity get() {
        if (isShadePlugin(line)) {
            return Severity.LOW;
        }
        if (line.contains("DistributedCacheManagerDecorator")
                && line.contains("failed to execute distributed flush for serviceName {http://rice.kuali.org/kew/v2_0}kewCacheAdminService")) {
            return Severity.LOW;
        }
        if (type == LogLineType.ERROR || type == LogLineType.SEVERE || type == LogLineType.FATAL) {
            return Severity.HIGH;
        }
        if (line.contains("define") && line.contains("overlappping classes:")) {
            return Severity.LOW;
        }
        if (line.contains("[WARNING]   - ")) {
            return Severity.LOW;
        }
        if (line.contains("is using schema version dbchangelog rather than version 1.9")) {
            return Severity.LOW;
        }
        if (line.contains("JAXBConfigImpl") && line.contains("is not available and hence set to empty")) {
            return Severity.LOW;
        }
        return Severity.MEDIUM;
    }

    public String getLine() {
        return line;
    }

    public String getStep() {
        return step;
    }

    public LogLineType getType() {
        return type;
    }

}
