package ua.utility.kfsdbupgrade.md;

import static com.google.common.base.Optional.absent;
import static com.google.common.base.Preconditions.checkNotNull;
import static java.lang.Double.parseDouble;
import static java.lang.Integer.parseInt;

import java.sql.Connection;
import java.util.Properties;

import javax.inject.Provider;

import com.google.common.base.Function;
import com.google.common.base.Optional;

public final class MDocContextProvider implements Provider<MDocContext> {

  public MDocContextProvider(Properties props, Connection conn) {
    this.props = checkNotNull(props);
    this.conn = checkNotNull(conn);
  }

  private final Properties props;
  private final Connection conn;

  public MDocContext get() {
    // number of threads to use when converting a batch of documents (runs entirely on the ec2 instance)
    int ec2Threads = new Ec2ThreadsProvider(props).get();
    // function for converting a KFS3 document into a KFS7 document
    Function<MaintDoc, MaintDoc> converter = new MDocFunctionProvider(props).get();
    // number of connections to establish to the db for selects and updates
    int rdsThreads = new RdsThreadsProvider(props, conn).get();
    // maximum number of documents to convert (useful for testing purposes)
    int max = parseInt(props.getProperty("mdoc.max", "1000000"));
    // total number of documents to select/convert/update in one iteration
    int chunkSize = parseInt(props.getProperty("mdoc.chunk", "1000"));
    // maximum number of documents to select in one SQL query
    int selectSize = parseInt(props.getProperty("mdoc.select", Integer.toString(chunkSize / 10)));
    // maximum number of documents to process before calling execute batch
    int batchSize = parseInt(props.getProperty("mdoc.batch", Integer.toString(chunkSize / 10)));
    // number of maintenance document clobs to select when warming up the table
    double value = parseDouble(props.getProperty("mdoc.clobs.warmup", "7.5"));
    Optional<Double> warmupClobsPercent = absent();
    if (value > 0) {
      warmupClobsPercent = Optional.of(value);
    }
    MDocContext.Builder builder = MDocContext.builder();
    builder.withEc2Threads(ec2Threads);
    builder.withRdsThreads(rdsThreads);
    builder.withMax(max);
    builder.withChunkSize(chunkSize);
    builder.withSelectSize(selectSize);
    builder.withBatchSize(batchSize);
    builder.withConverter(converter);
    builder.withWarmupClobsPercent(warmupClobsPercent);
    return builder.build();
  }

}
