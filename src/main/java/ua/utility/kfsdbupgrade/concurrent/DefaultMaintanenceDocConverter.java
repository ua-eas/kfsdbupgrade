package ua.utility.kfsdbupgrade.concurrent;

import static com.google.common.base.Optional.absent;
import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Stopwatch.createStarted;
import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Maps.newLinkedHashMap;
import static com.google.common.collect.Ordering.natural;
import static java.lang.String.format;
import static org.apache.log4j.Logger.getLogger;
import static ua.utility.kfsdbupgrade.mdoc.Closeables.closeQuietly;
import static ua.utility.kfsdbupgrade.mdoc.Formats.getCount;
import static ua.utility.kfsdbupgrade.mdoc.Formats.getTime;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.inject.Provider;

import org.apache.log4j.Logger;

import com.google.common.base.Joiner;
import com.google.common.base.Optional;
import com.google.common.base.Stopwatch;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

import ua.utility.kfsdbupgrade.EncryptionService;
import ua.utility.kfsdbupgrade.MaintainableXMLConversionServiceImpl;

public final class DefaultMaintanenceDocConverter implements MaintenanceDocConverter {

  private static final Logger LOGGER = getLogger(DefaultMaintanenceDocConverter.class);

  private final Provider<Connection> provider;
  private final Optional<Integer> maximumDocumentIds;
  private final MaintainableXMLConversionServiceImpl converter;
  private final EncryptionService encryptor;
  private final int documentIdDisplay;

  @Override
  public ImmutableMap<String, String> convertDocuments(Map<String, String> documents) {
    Map<String, String> map = newLinkedHashMap();
    Map<String, Exception> errors = newLinkedHashMap();
    for (Entry<String, String> entry : documents.entrySet()) {
      try {
        String decrypted = encryptor.isEnabled() ? encryptor.decrypt(entry.getValue()) : entry.getValue();
        String converted = converter.transformMaintainableXML(decrypted);
        String encrypted = encryptor.isEnabled() ? encryptor.encrypt(converted) : converted;
        map.put(entry.getKey(), encrypted);
      } catch (Exception e) {
        errors.put(entry.getValue(), e);
      }
    }
    return ImmutableMap.copyOf(map);
  }

  @Override
  public ImmutableMap<String, String> getDocuments(Iterable<String> ids) {
    Map<String, String> map = newLinkedHashMap();
    String query = "SELECT DOC_HDR_ID, DOC_CNTNT FROM krns_maint_doc_t WHERE DOC_HDR_ID IN (" + Joiner.on(',').join(ids) + ")";
    Connection conn = null;
    Statement stmt = null;
    ResultSet rs = null;
    try {
      conn = provider.get();
      stmt = conn.createStatement();
      rs = stmt.executeQuery(query);
      while (rs.next()) {
        String id = rs.getString(1);
        String content = rs.getString(2);
        map.put(id, content);
      }
    } catch (Throwable e) {
      throw new IllegalStateException(e);
    } finally {
      closeQuietly(rs);
      closeQuietly(stmt);
      closeQuietly(conn);
    }
    return ImmutableMap.copyOf(map);
  }

  @Override
  public ImmutableList<String> getDocumentIds() {
    List<String> ids = newArrayList();
    Connection conn = null;
    Statement stmt = null;
    ResultSet rs = null;
    try {
      Stopwatch sw = createStarted();
      conn = provider.get();
      info("connected -> %s", sw, conn.getMetaData().getURL());
      int totalDocuments = getDocumentCount(conn);
      info("total maintenance documents -> %s", sw, getCount(totalDocuments));
      stmt = conn.createStatement();
      rs = stmt.executeQuery("SELECT DOC_HDR_ID FROM krns_maint_doc_t");
      int count = 0;
      while (rs.next()) {
        count++;
        if (maximumDocumentIds.isPresent() && count > maximumDocumentIds.get()) {
          count--;
          break;
        }
        if (count % documentIdDisplay == 0) {
          info("document ids [acquired:%s, max:%s, total:%s]", sw, getCount(count), getCount(maximumDocumentIds.or(-1)), getCount(totalDocuments));
        }
        ids.add(rs.getString(1));
      }
      info("document ids [acquired:%s, max:%s, total:%s]", sw, getCount(count), getCount(maximumDocumentIds.or(-1)), getCount(totalDocuments));
    } catch (Throwable e) {
      throw new IllegalStateException(e);
    } finally {
      closeQuietly(rs);
      closeQuietly(stmt);
      closeQuietly(conn);
    }
    return natural().immutableSortedCopy(ids);
  }

  private int getDocumentCount(Connection conn) throws SQLException {
    Statement stmt = null;
    ResultSet rs = null;
    try {
      stmt = conn.createStatement();
      rs = stmt.executeQuery("SELECT COUNT(*) FROM krns_maint_doc_t");
      rs.next();
      return rs.getInt(1);
    } catch (Throwable e) {
      throw new IllegalStateException(e);
    } finally {
      closeQuietly(rs);
      closeQuietly(stmt);
    }

  }

  private void info(String msg, Stopwatch sw, Object... args) {
    if (args == null || args.length == 0) {
      LOGGER.info(msg + " [" + getTime(sw) + "]");
    } else {
      LOGGER.info(format(msg, args) + " [" + getTime(sw) + "]");
    }
  }

  private DefaultMaintanenceDocConverter(Builder builder) {
    this.provider = builder.provider;
    this.maximumDocumentIds = builder.maximumDocumentIds;
    this.converter = builder.converter;
    this.encryptor = builder.encryptor;
    this.documentIdDisplay = builder.documentIdDisplay;
  }

  public static Builder builder() {
    return new Builder();
  }

  public static class Builder {

    private Provider<Connection> provider;
    private Optional<Integer> maximumDocumentIds = absent();
    private MaintainableXMLConversionServiceImpl converter;
    private EncryptionService encryptor;
    private int documentIdDisplay = 1000;

    public Builder withProvider(Provider<Connection> provider) {
      this.provider = provider;
      return this;
    }

    public Builder withMaximumDocumentIds(Optional<Integer> maximumDocumentIds) {
      this.maximumDocumentIds = maximumDocumentIds;
      return this;
    }

    public Builder withMaximumDocumentIds(int maximumDocumentIds) {
      return withMaximumDocumentIds(Optional.<Integer>of(maximumDocumentIds));
    }

    public Builder withConverter(MaintainableXMLConversionServiceImpl converter) {
      this.converter = converter;
      return this;
    }

    public Builder withEncryptor(EncryptionService encryptor) {
      this.encryptor = encryptor;
      return this;
    }

    public Builder withDocumentIdDisplay(int documentIdDisplay) {
      this.documentIdDisplay = documentIdDisplay;
      return this;
    }

    public DefaultMaintanenceDocConverter build() {
      return validate(new DefaultMaintanenceDocConverter(this));
    }

    private static DefaultMaintanenceDocConverter validate(DefaultMaintanenceDocConverter instance) {
      checkNotNull(instance.provider, "provider may not be null");
      checkNotNull(instance.maximumDocumentIds, "maximumDocumentIds may not be null");
      checkNotNull(instance.converter, "converter may not be null");
      checkNotNull(instance.encryptor, "encryptor may not be null");
      checkArgument(instance.documentIdDisplay > 0, "documentIdDisplay must be greater than zero");
      return instance;
    }
  }

}
