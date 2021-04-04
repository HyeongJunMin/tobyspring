package toby.common.exception;

public class SqlNotFoundException extends SqlRetrievalFailureException {
  public SqlNotFoundException(String message) {
    super(message);
  }
  public SqlNotFoundException(String message, Throwable cause) {
    super(message, cause);
  }
}
