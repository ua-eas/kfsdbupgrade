package ua.utility.kfsdbupgrade.rds;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.util.Arrays.asList;
import static ua.utility.kfsdbupgrade.md.base.Props.checkedValue;
import static ua.utility.kfsdbupgrade.rds.Credentials.fromProvider;

import java.util.List;
import java.util.Properties;

import javax.inject.Provider;

import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.AWSCredentialsProviderChain;
import com.amazonaws.auth.DefaultAWSCredentialsProviderChain;
import com.amazonaws.services.rds.AmazonRDS;

public final class DatabaseProvider implements Provider<String> {

  public DatabaseProvider(Properties props) {
    this.props = checkNotNull(props);
  }

  private final Properties props;

  @Override
  public String get() {
    List<AWSCredentialsProvider> providers = asList(DefaultAWSCredentialsProviderChain.getInstance(), fromProvider(new CredentialsProvider(props)));
    AWSCredentialsProviderChain chain = new AWSCredentialsProviderChain(providers);
    String region = props.getProperty("aws.region", "us-west-2");
    String snapshotDatabase = checkedValue(props, "db.snapshot.name");
    String instanceId = checkedValue(props, "db.name");
    AmazonRDS rds = new AmazonRdsProvider(region, chain.getCredentials()).get();
    String snapshotId = new LatestSnapshotProvider(rds, snapshotDatabase, true).get();
    new DeleteDatabaseProvider(rds, instanceId).get();
    new CreateDatabaseProvider(rds, instanceId, snapshotId).get();
    new HardenDatabaseProvider(rds, instanceId).get();
    new RebootDatabaseProvider(rds, instanceId).get();
    return instanceId;
  }

}
