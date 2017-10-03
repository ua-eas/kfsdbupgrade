package ua.utility.kfsdbupgrade.mdoc;

public enum MaintDocField {

  DOC_HDR_ID, OBJ_ID, VER_NBR, DOC_CNTNT;

  public static MaintDocField asMaintDocField(String s) {
    if (s.equals("hid")) {
      return DOC_HDR_ID;
    }
    if (s.equals("oid")) {
      return OBJ_ID;
    }
    if (s.equals("version")) {
      return VER_NBR;
    }
    if (s.equals("content")) {
      return DOC_CNTNT;
    }
    return valueOf(s.toUpperCase());
  }

}
