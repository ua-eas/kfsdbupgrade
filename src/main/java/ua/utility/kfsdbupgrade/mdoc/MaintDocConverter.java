package ua.utility.kfsdbupgrade.mdoc;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Stopwatch.createStarted;
import static com.google.common.collect.ImmutableList.copyOf;
import static com.google.common.collect.Lists.newArrayList;
import static java.lang.Integer.parseInt;
import static java.lang.Runtime.getRuntime;
import static java.lang.String.format;
import static java.lang.System.getProperty;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static ua.utility.kfsdbupgrade.mdoc.Formats.getCount;
import static ua.utility.kfsdbupgrade.mdoc.Formats.getRate;
import static ua.utility.kfsdbupgrade.mdoc.Formats.getThroughputInSeconds;
import static ua.utility.kfsdbupgrade.mdoc.Formats.getTime;
import static ua.utility.kfsdbupgrade.mdoc.Lists.distribute;

import java.util.List;
import java.util.Properties;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

import javax.inject.Provider;

import org.apache.log4j.Logger;

import com.google.common.base.Stopwatch;
import com.google.common.collect.ImmutableList;

import ua.utility.kfsdbupgrade.EncryptionService;
import ua.utility.kfsdbupgrade.MaintainableXmlConversionService;

public final class MaintDocConverter implements Provider<Long> {

  private static final Logger LOGGER = Logger.getLogger(MaintDocConverter.class);

  private final Properties properties;
  private final EncryptionService encryptor;
  private final MaintainableXmlConversionService converter;
  private final int batchSize = parseInt(getProperty("mdoc.batch", "100"));

  @Override
  public Long get() {
    Stopwatch overall = createStarted();
    ConnectionProvider provider = new ConnectionProvider(properties);
    List<String> docHeaderIds = new HeaderIdsProvider(provider).get();
    int threads = new ThreadsProvider(properties).get();
    info("using %s threads, batch size: %s (%s cores)", threads, batchSize, getRuntime().availableProcessors());
    List<ConvertDocsCallable> callables = getCallables(docHeaderIds, threads, converter, encryptor);
    ExecutorService executor = new ExecutorProvider("mdoc", threads).get();
    Stopwatch sw = createStarted();
    List<BatchResult> batches = getFutures(submit(executor, callables));
    BatchResult br = new BatchResult(0, 0, 0);
    for (BatchResult batch : batches) {
      br = BatchResult.add(br, batch);
    }
    long elapsed = sw.elapsed(MILLISECONDS);
    String rate = getRate(elapsed, br.getBytes());
    info("converted -> %s in %s [%s, %s]", getCount(docHeaderIds.size()), getTime(elapsed), getThroughputInSeconds(elapsed, docHeaderIds.size(), "docs/second"), rate);
    return overall.elapsed(MILLISECONDS);
  }

  private ImmutableList<ConvertDocsCallable> getCallables(List<String> docHeaderIds, int threads, MaintainableXmlConversionService converter, EncryptionService encryptor) {
    List<ConvertDocsCallable> callables = newArrayList();
    for (List<String> distribution : distribute(docHeaderIds, threads)) {
      ConnectionProvider provider = new ConnectionProvider(properties);
      callables.add(new ConvertDocsCallable(provider, batchSize, distribution, converter, encryptor));
    }
    return ImmutableList.copyOf(callables);
  }

  private void info(String msg, Object... args) {
    if (args == null || args.length == 0) {
      LOGGER.info(msg);
    } else {
      LOGGER.info(format(msg, args));
    }
  }

  private static <T> ImmutableList<Future<T>> submit(ExecutorService executor, Iterable<? extends Callable<T>> callables) {
    List<Future<T>> futures = newArrayList();
    for (Callable<T> callable : callables) {
      Future<T> future = executor.submit(callable);
      futures.add(future);
    }
    return copyOf(futures);
  }

  private static <T> ImmutableList<T> getFutures(Iterable<Future<T>> futures) {
    List<T> elements = newArrayList();
    for (Future<T> future : futures) {
      elements.add(getUnchecked(future));
    }
    return copyOf(elements);
  }

  private static <T> T getUnchecked(Future<T> future) {
    try {
      return future.get();
    } catch (InterruptedException e) {
      throw new IllegalStateException(e);
    } catch (ExecutionException e) {
      throw new IllegalStateException(e);
    }
  }

  private MaintDocConverter(Builder builder) {
    this.properties = builder.properties;
    this.encryptor = builder.encryptor;
    this.converter = builder.converter;
  }

  public static Builder builder() {
    return new Builder();
  }

  public static class Builder {

    private Properties properties;
    private EncryptionService encryptor;
    private MaintainableXmlConversionService converter;

    public Builder withProperties(Properties properties) {
      this.properties = properties;
      return this;
    }

    public Builder withEncryptor(EncryptionService encryptor) {
      this.encryptor = encryptor;
      return this;
    }

    public Builder withConverter(MaintainableXmlConversionService converter) {
      this.converter = converter;
      return this;
    }

    public MaintDocConverter build() {
      return validate(new MaintDocConverter(this));
    }

    private static MaintDocConverter validate(MaintDocConverter instance) {
      checkNotNull(instance.encryptor, "encryptor may not be null");
      checkNotNull(instance.converter, "converter may not be null");
      return instance;
    }
  }

}
