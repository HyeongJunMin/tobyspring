package toby.common.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportResource;
import org.springframework.transaction.TransactionManager;
import org.springframework.transaction.annotation.TransactionManagementConfigurer;

@Configuration
@ImportResource({ "classpath:META-INF/transaction-config.xml" })
public class TransactionConfig implements TransactionManagementConfigurer {

  @Autowired
  private TransactionManager transactionManager;

  public TransactionManager annotationDrivenTransactionManager() {
    return transactionManager;
  }

}
