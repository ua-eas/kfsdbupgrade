package ua.utility.kfsdbupgrade.mdoc;

import static com.google.common.base.Optional.of;
import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.base.Optional;

public final class ConversionResult {

  private final Optional<Throwable> exception;
  private final Optional<MaintDoc> newDocument;
  private final MaintDoc oldDocument;

  public ConversionResult(MaintDoc oldDocument, Throwable exception) {
    this(oldDocument, of(exception), Optional.<MaintDoc>absent());
  }

  public ConversionResult(MaintDoc oldDocument, MaintDoc newDocument) {
    this(oldDocument, Optional.<Throwable>absent(), of(newDocument));
  }

  private ConversionResult(MaintDoc oldDocument, Optional<Throwable> exception, Optional<MaintDoc> newDocument) {
    if (exception.isPresent()) {
      checkArgument(!newDocument.isPresent(), "new document not allowed if an exception occurred");
    }
    if (newDocument.isPresent()) {
      checkArgument(!exception.isPresent(), "exception not allowed if a new document is present");
    }
    this.exception = checkNotNull(exception);
    this.oldDocument = checkNotNull(oldDocument);
    this.newDocument = checkNotNull(newDocument);
  }

  public Optional<Throwable> getException() {
    return exception;
  }

  public Optional<MaintDoc> getNewDocument() {
    return newDocument;
  }

  public MaintDoc getOldDocument() {
    return oldDocument;
  }

}
