package toby.common.exception;

public class DuplicateUserIdException extends RuntimeException {

  public static int ERROR_DUPLICATED_ENTRY = 1062;

  public DuplicateUserIdException() {  }

  public DuplicateUserIdException(Throwable cause) {
    super(cause);
  }

}
