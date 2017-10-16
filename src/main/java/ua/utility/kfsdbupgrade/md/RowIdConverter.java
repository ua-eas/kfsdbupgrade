package ua.utility.kfsdbupgrade.md;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Splitter.fixedLength;
import static java.lang.Integer.parseInt;
import static java.lang.Math.pow;
import static org.apache.commons.lang3.StringUtils.leftPad;

import org.apache.commons.lang3.StringUtils;

import com.google.common.base.Converter;
import com.google.common.base.Joiner;

import ua.utility.kfsdbupgrade.mdoc.BlockId;

public final class RowIdConverter extends Converter<String, RowId> {

  private RowIdConverter() {
  }

  private static final RowIdConverter SINGLETON = new RowIdConverter();

  public static RowIdConverter getInstance() {
    return SINGLETON;
  }

  private final char[] alphabet = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/".toCharArray();
  private final int radix = alphabet.length;
  private final int oracleRowIdLength = 18;

  public RowId doForward(String input) {
    checkArgument(input.length() == oracleRowIdLength, "row ids must be exactly %s characters", oracleRowIdLength);

    long objectId = getLong(input.substring(0, 6));
    long fileNumber = getLong(input.substring(7, 9));
    long blockNumber = getLong(input.substring(10, 15));
    long rowNumber = getLong(input.substring(16, 18));

    RowId.Builder builder = new RowId.Builder();
    builder.withObjectId(objectId);
    builder.withBlock(new BlockId(fileNumber, blockNumber));
    builder.withRowNumber(rowNumber);
    return builder.build();
  }

  @Override
  protected String doBackward(RowId b) {
    // OOOOOO.FFF.BBBBBB.RRR
    // ---------------------
    // OOOOOO is the object id
    // FFF is the file number
    // BBBBBB is the block number
    // RRR is the row number
    String objectId = base64(b.getObjectId(), 6);
    String fileNumber = base64(b.getBlock().getFileNumber(), 3);
    String blockNumber = base64(b.getBlock().getBlockNumber(), 6);
    String rowNumber = base64(b.getRowNumber(), 3);
    return Joiner.on("").join(objectId, fileNumber, blockNumber, rowNumber);

  }

  private int position(char c) {
    for (int i = 0; i < alphabet.length; i++) {
      if (c == alphabet[i]) {
        return i;
      }
    }
    throw new IllegalArgumentException("'" + c + "' is not a base64 character");
  }

  private long getLong(String s) {
    char[] chars = StringUtils.reverse(s).toCharArray();
    long value = 0;
    for (int i = 0; i < chars.length; i++) {
      int position = position(chars[i]);
      value += position * pow(radix, i);
    }
    return value;
  }

  private String base64(long value, int chars) {
    Iterable<String> tokens = fixedLength(6).split(leftPad(Long.toBinaryString(value), 6 * chars, "0"));
    StringBuilder sb = new StringBuilder();
    for (String token : tokens) {
      int index = parseInt(token, 2);
      sb.append(alphabet[index]);
    }
    return sb.toString();
  }

}