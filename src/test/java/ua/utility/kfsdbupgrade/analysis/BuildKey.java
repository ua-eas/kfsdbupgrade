package ua.utility.kfsdbupgrade.analysis;

import static com.google.common.base.Optional.of;
import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.collect.Ordering.natural;
import static java.util.Objects.hash;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

import com.google.common.base.Joiner;
import com.google.common.base.Optional;
import com.google.common.collect.ComparisonChain;

public final class BuildKey implements Comparable<BuildKey> {

    public BuildKey(String job, int number) {
        this(Optional.<String>absent(), job, number);
    }

    public BuildKey(String folder, String job, int number) {
        this(of(folder), job, number);
    }

    public BuildKey(Optional<String> folder, String job, int number) {
        checkArgument(folder.isPresent() ? isNotBlank(folder.get()) : true, "folder cannot be blank");
        checkArgument(isNotBlank(job), "job cannot be blank");
        checkArgument(number >= 1, "number must be >=1");
        this.folder = folder;
        this.job = job;
        this.number = number;
        this.hash = hash(folder, job, number);
    }

    private final Optional<String> folder;
    private final String job;
    private final int number;
    private final int hash;

    @Override
    public int compareTo(BuildKey other) {
        ComparisonChain chain = ComparisonChain.start();
        chain = chain.compare(folder.orNull(), other.folder.orNull(), natural().nullsFirst());
        chain = chain.compare(job, other.job);
        chain = chain.compare(number, other.number);
        return chain.result();
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
            BuildKey other = (BuildKey) object;
            return (hash == other.hash) && compareTo(other) == 0;
        }
    }

    @Override
    public String toString() {
        if (folder.isPresent()) {
            return Joiner.on('/').join(folder.get(), job, number);
        } else {
            return Joiner.on('/').join(job, number);
        }
    }

    public Optional<String> getFolder() {
        return folder;
    }

    public String getJob() {
        return job;
    }

    public int getNumber() {
        return number;
    }

}
