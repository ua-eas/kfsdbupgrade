package ua.utility.kfsdbupgrade.mdoc;

public final class BatchResult {

  public static BatchResult add(BatchResult one, BatchResult two) {
    return new BatchResult(one.getCount() + two.getCount(), one.getBytes() + two.getBytes(), one.getElapsed() + two.getElapsed());
  }

  public BatchResult(int count, long bytes, long elapsed) {
    this.count = count;
    this.bytes = bytes;
    this.elapsed = elapsed;
  }

  private final int count;
  private final long bytes;
  private final long elapsed;

  public int getCount() {
    return count;
  }

  public long getBytes() {
    return bytes;
  }

  public long getElapsed() {
    return elapsed;
  }

}
