package toby.service.sql;

import org.springframework.context.annotation.Import;
import toby.common.config.SqlServiceContext;

@Import(value = SqlServiceContext.class)
public @interface EnableSqlService {
}
