package ua.utility.kfsdbupgrade.mdoc;

public final class MaintDocResult {

  public MaintDocResult(int converted, int errors, long elapsed) {
    this.converted = converted;
    this.errors = errors;
    this.elapsed = elapsed;
  }

  private final int converted;
  private final int errors;
  private final long elapsed;

  public int getConverted() {
    return converted;
  }

  public int getErrors() {
    return errors;
  }

  public long getElapsed() {
    return elapsed;
  }

}
