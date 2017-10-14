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

  public synchronized void resetSelect() {
    this.select.reset();
  }

  public synchronized void resetUpdate() {
    this.update.reset();
  }

  public synchronized void resetConvert() {
    this.convert.reset();
  }

  public synchronized long select(long count, long bytes, long microseconds) {
    return this.select.increment(count, bytes, microseconds);
  }

  public synchronized long update(long count, long bytes, long microseconds) {
    return this.update.increment(count, bytes, microseconds);
  }

  public synchronized long convert(long count, long bytes, long microseconds) {
    return this.convert.increment(count, bytes, microseconds);
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
