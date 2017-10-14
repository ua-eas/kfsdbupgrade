package ua.utility.kfsdbupgrade.mdoc;

public final class MDocMetrics {

  public MDocMetrics() {
    this.select = new DataMetrics();
    this.update = new DataMetrics();
    this.convert = new DataMetrics();
  }

  private final DataMetrics select;
  private final DataMetrics update;
  private final DataMetrics convert;

  public synchronized void reset() {
    this.select.reset();
    this.update.reset();
    this.convert.reset();
  }

  public synchronized void select(long count, long bytes, long microseconds) {
    this.select.increment(count, bytes, microseconds);
  }

  public synchronized void update(long count, long bytes, long microseconds) {
    this.update.increment(count, bytes, microseconds);
  }

  public synchronized void convert(long count, long bytes, long microseconds) {
    this.convert.increment(count, bytes, microseconds);
  }

  public synchronized DataMetric getSelect() {
    return new DataMetric(select.getCount(), select.getBytes(), select.getMicroseconds());
  }

  public synchronized DataMetric getUpdate() {
    return new DataMetric(update.getCount(), update.getBytes(), update.getMicroseconds());
  }

  public synchronized DataMetric getConvert() {
    return new DataMetric(convert.getCount(), convert.getBytes(), convert.getMicroseconds());
  }

}
