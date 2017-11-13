package ua.utility.kfsdbupgrade.rds;

import static com.google.common.base.Preconditions.checkNotNull;

import javax.inject.Provider;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSCredentialsProvider;

import ua.utility.kfsdbupgrade.md.base.Providers;

public final class Credentials {

  private Credentials() {
  }

  public static AWSCredentialsProvider fromCredentials(AWSCredentials credentials) {
    return fromProvider(Providers.of(credentials));
  }

  public static AWSCredentialsProvider fromProvider(Provider<AWSCredentials> provider) {
    return new CredentialsAdapter(provider);
  }

  private static class CredentialsAdapter implements AWSCredentialsProvider {

    public CredentialsAdapter(Provider<AWSCredentials> provider) {
      this.provider = checkNotNull(provider);
    }

    private final Provider<AWSCredentials> provider;

    @Override
    public AWSCredentials getCredentials() {
      return provider.get();
    }

    @Override
    public void refresh() {
      // noop
    }

  }

}
