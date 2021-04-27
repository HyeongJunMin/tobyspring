package toby.common.exception;

public class SqlUpdateFailureException extends RuntimeException {
  public SqlUpdateFailureException(String message) {
    super(message);
  }
}
