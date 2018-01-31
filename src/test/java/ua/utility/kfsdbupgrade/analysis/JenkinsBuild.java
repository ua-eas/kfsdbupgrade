package ua.utility.kfsdbupgrade.analysis;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static java.util.Objects.hash;

import java.io.File;

import com.google.common.collect.ComparisonChain;

public final class JenkinsBuild implements Comparable<JenkinsBuild> {

    public JenkinsBuild(int number, File folder) {
        checkArgument(number >= 1, "build number must be >= 1");
        this.number = number;
        this.folder = checkNotNull(folder);
        this.hash = hash(number, folder);
    }

    private final int number;
    private final File folder;
    private final int hash;

    public int getNumber() {
        return number;
    }

    public File getFolder() {
        return folder;
    }

    @Override
    public int compareTo(JenkinsBuild other) {
        return ComparisonChain.start().compare(number, other.number).compare(folder, other.folder).result();
    }

    @Override
    public int hashCode() {
        return hash;
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        } else if (object == null || object.getClass() != getClass()) {
            return false;
        } else {
            JenkinsBuild other = (JenkinsBuild) object;
            return (hash == other.hash) && compareTo(other) == 0;
        }
    }

}
