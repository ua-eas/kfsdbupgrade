package ua.utility.kfsdbupgrade.rds;

import static com.google.common.base.Preconditions.checkNotNull;
import static ua.utility.kfsdbupgrade.md.base.Props.checkedValue;

import java.util.Properties;

import javax.inject.Provider;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.BasicAWSCredentials;

public final class CredentialsProvider implements Provider<AWSCredentials> {

  public CredentialsProvider(Properties props) {
    this.props = checkNotNull(props);
  }

  private final Properties props;

  @Override
  public AWSCredentials get() {
    String accessKey = checkedValue(props, "aws.accessKeyId", "AWS_ACCESS_KEY_ID");
    String secretKey = checkedValue(props, "aws.secretKey", "AWS_SECRET_KEY");
    return new BasicAWSCredentials(accessKey, secretKey);
  }

}
