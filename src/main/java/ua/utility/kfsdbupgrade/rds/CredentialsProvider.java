package ua.utility.kfsdbupgrade.rds;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.util.Arrays.asList;
import static ua.utility.kfsdbupgrade.rds.Credentials.fromProvider;

import java.util.List;
import java.util.Properties;

import javax.inject.Provider;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.AWSCredentialsProviderChain;
import com.amazonaws.auth.DefaultAWSCredentialsProviderChain;

public final class CredentialsProvider implements Provider<AWSCredentials> {

  public CredentialsProvider(Properties props) {
    this.props = checkNotNull(props);
  }

  private final Properties props;

  @Override
  public AWSCredentials get() {
    DefaultAWSCredentialsProviderChain defaultChain = DefaultAWSCredentialsProviderChain.getInstance();
    AWSCredentialsProvider custom = fromProvider(new BasicCredentialsProvider(props));
    List<AWSCredentialsProvider> providers = asList(defaultChain, custom);
    AWSCredentialsProviderChain chain = new AWSCredentialsProviderChain(providers);
    return chain.getCredentials();
  }

}
