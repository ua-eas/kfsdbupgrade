package ua.utility.kfsdbupgrade.mdoc;

import static com.google.common.base.Preconditions.checkNotNull;

public final class MaintDoc {

  public MaintDoc(String docHeaderId, String content) {
    this.docHeaderId = checkNotNull(docHeaderId);
    this.content = checkNotNull(content);
  }

  private final String docHeaderId;
  private final String content;

  public String getDocHeaderId() {
    return docHeaderId;
  }

  public String getContent() {
    return content;
  }

}
