package ua.utility.kfsdbupgrade.analysis;

import static com.google.common.base.Preconditions.checkNotNull;

public final class JenkinsRequest {

  public JenkinsRequest(String folder, String view) {
    this.folder = checkNotNull(folder);
    this.view = checkNotNull(view);
  }

  private final String folder;
  private final String view;

  public String getFolder() {
    return folder;
  }

  public String getView() {
    return view;
  }

}
