package ua.utility.kfsdbupgrade.md;

import static com.google.common.base.Preconditions.checkArgument;
import static java.util.concurrent.Executors.newFixedThreadPool;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadFactory;

import javax.inject.Provider;

import com.google.common.util.concurrent.ThreadFactoryBuilder;

public final class ExecutorProvider implements Provider<ExecutorService> {

  public ExecutorProvider(String name, int threads) {
    checkArgument(isNotBlank(name), "name cannot be blank");
    checkArgument(threads > 0, "threads must greater than zero");
    this.name = name;
    this.threads = threads;
  }

  private final String name;
  private final int threads;

  @Override
  public ExecutorService get() {
    ThreadFactoryBuilder builder = new ThreadFactoryBuilder();
    builder.setNameFormat(name + "-%s");
    builder.setDaemon(true);
    ThreadFactory factory = builder.build();
    return newFixedThreadPool(threads, factory);
  }
}
