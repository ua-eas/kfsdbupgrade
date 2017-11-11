package ua.utility.kfsdbupgrade.aws;

import static com.google.common.base.Stopwatch.createStarted;
import static org.apache.log4j.Logger.getLogger;
import static ua.utility.kfsdbupgrade.log.Logging.info;
import static ua.utility.kfsdbupgrade.md.base.Formats.getTime;
import static ua.utility.kfsdbupgrade.md.base.Preconditions.checkNotBlank;

import javax.inject.Provider;

import org.apache.log4j.Logger;

import com.amazonaws.services.rds.AmazonRDS;
import com.amazonaws.services.rds.AmazonRDSClient;
import com.amazonaws.services.rds.AmazonRDSClientBuilder;
import com.google.common.base.Stopwatch;

public final class AmazonRdsProvider implements Provider<AmazonRDS> {

  private static final Logger LOGGER = getLogger(AmazonRdsProvider.class);

  public AmazonRdsProvider(String region) {
    this.region = checkNotBlank(region, "region");
  }

  private final String region;

  public AmazonRDS get() {
    Stopwatch sw = createStarted();
    info(LOGGER, "connecting -> %s", region);
    AmazonRDSClientBuilder builder = AmazonRDSClient.builder();
    builder.withRegion(region);
    AmazonRDS rds = builder.build();
    info(LOGGER, "connected --> %s [%s]", region, getTime(sw));
    return rds;
  }

}
