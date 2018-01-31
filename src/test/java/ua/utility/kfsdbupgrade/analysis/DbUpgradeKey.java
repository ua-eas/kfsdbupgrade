package ua.utility.kfsdbupgrade.analysis;

import static com.google.common.base.Optional.of;
import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.collect.Ordering.natural;
import static java.util.Objects.hash;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

import com.google.common.base.Joiner;
import com.google.common.base.Optional;
import com.google.common.collect.ComparisonChain;

public final class DbUpgradeKey implements Comparable<DbUpgradeKey> {

    public DbUpgradeKey(String view, int sequence) {
        this(Optional.<String>absent(), view, sequence);
    }

    public DbUpgradeKey(String folder, String view, int sequence) {
        this(of(folder), view, sequence);
    }

    public DbUpgradeKey(Optional<String> folder, String view, int sequence) {
        checkArgument(folder.isPresent() ? isNotBlank(folder.get()) : true, "folder cannot be blank");
        checkArgument(isNotBlank(view), "view cannot be blank");
        checkArgument(sequence >= 1, "sequence must be >= 1");
        this.folder = folder;
        this.view = view;
        this.sequence = sequence;
        this.hash = hash(folder, view, sequence);
    }

    private final Optional<String> folder;
    private final String view;
    private final int sequence;
    private final int hash;

    @Override
    public int compareTo(DbUpgradeKey other) {
        ComparisonChain chain = ComparisonChain.start();
        chain = chain.compare(folder.orNull(), other.folder.orNull(), natural().nullsFirst());
        chain = chain.compare(view, other.view);
        chain = chain.compare(sequence, other.sequence);
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
            DbUpgradeKey other = (DbUpgradeKey) object;
            return (hash == other.hash) && compareTo(other) == 0;
        }
    }

    @Override
    public String toString() {
        if (folder.isPresent()) {
            return Joiner.on('/').join(folder.get(), view, sequence);
        } else {
            return Joiner.on('/').join(view, sequence);
        }
    }

    public Optional<String> getFolder() {
        return folder;
    }

    public String getView() {
        return view;
    }

    public int getSequence() {
        return sequence;
    }

}
