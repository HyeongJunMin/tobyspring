package toby.service.sql;

import toby.common.exception.SqlRetrievalFailureException;

public interface SqlService {
  public static final String USER_ADD = "userAdd";
  public static final String USER_GET = "userGet";
  public static final String USER_GET_ALL = "userGetAll";
  public static final String USER_DELETE_ALL = "userDeleteAll";
  public static final String USER_GET_COUNT = "userGetCount";
  public static final String USER_UPDATE = "userUpdate";
  String getSql(String key) throws SqlRetrievalFailureException;
}
