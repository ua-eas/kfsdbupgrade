package ua.utility.kfsdbupgrade.rds;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Stopwatch.createStarted;
import static org.apache.log4j.Logger.getLogger;
import static ua.utility.kfsdbupgrade.log.Logging.info;
import static ua.utility.kfsdbupgrade.md.base.Formats.getTime;
import static ua.utility.kfsdbupgrade.md.base.Preconditions.checkNotBlank;
import static ua.utility.kfsdbupgrade.rds.Credentials.fromCredentials;

import javax.inject.Provider;

import org.apache.log4j.Logger;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.services.rds.AmazonRDS;
import com.amazonaws.services.rds.AmazonRDSClient;
import com.amazonaws.services.rds.AmazonRDSClientBuilder;
import com.google.common.base.Stopwatch;

public final class AmazonRdsProvider implements Provider<AmazonRDS> {

  private static final Logger LOGGER = getLogger(AmazonRdsProvider.class);

  public AmazonRdsProvider() {
    this("us-west-2", new CredentialsProvider().get());
  }

  public AmazonRdsProvider(String region, AWSCredentials credentials) {
    this.region = checkNotBlank(region, "region");
    this.credentials = checkNotNull(credentials);
  }

  private final String region;
  private final AWSCredentials credentials;

  public AmazonRDS get() {
    Stopwatch sw = createStarted();
    info(LOGGER, "connecting -> %s", region);
    AmazonRDSClientBuilder builder = AmazonRDSClient.builder();
    builder.withRegion(region);
    builder.withCredentials(fromCredentials(credentials));
    AmazonRDS rds = builder.build();
    info(LOGGER, "connected --> %s [%s]", region, getTime(sw));
    return rds;
  }

}
