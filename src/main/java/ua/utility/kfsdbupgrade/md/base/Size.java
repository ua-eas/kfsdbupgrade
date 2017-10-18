package ua.utility.kfsdbupgrade.md.base;

/**
 * @author Jeff Caddel
 */
public enum Size {

  BYTE(1, "b", "bytes/s"), //
  KB(1024, "k", "KB/s"), //
  MB(1024 * KB.getValue(), "m", "MB/s"), //
  GB(1024 * MB.getValue(), "g", "GB/s"), //
  TB(1024 * GB.getValue(), "t", "TB/s"), //
  PB(1024 * TB.getValue(), "p", "PB/s"), //
  EB(1024 * PB.getValue(), "e", "EB/s");

  private final long value;
  private final String sizeLabel;
  private final String rateLabel;

  private Size(long value, String sizeLabel, String rateLabel) {
    this.value = value;
    this.sizeLabel = sizeLabel;
    this.rateLabel = rateLabel;
  }

  public long getValue() {
    return value;
  }

  public String getSizeLabel() {
    return sizeLabel;
  }

  public String getRateLabel() {
    return rateLabel;
  }

}
