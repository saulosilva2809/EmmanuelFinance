package com.emmanuelfinance.transaction;

import com.emmanuelfinance.shared.modules.account.AccountClient;
import com.emmanuelfinance.shared.modules.category.CategoryClient;
import com.emmanuelfinance.shared.modules.creditcard.CreditCardClient;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@SpringBootApplication(scanBasePackages = {
		"com.emmanuelfinance"
})
@EnableJpaAuditing
@EnableCaching
@EnableFeignClients(clients = {
		AccountClient.class,
		CreditCardClient.class,
		CategoryClient.class
})
public class TransactionApplication {

	public static void main(String[] args) {
		SpringApplication.run(TransactionApplication.class, args);
	}

}
