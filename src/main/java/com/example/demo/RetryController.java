package com.example.demo;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Supplier;

import org.apache.log4j.Logger;
import org.springframework.context.annotation.Bean;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import io.github.resilience4j.bulkhead.BulkheadFullException;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;

@RestController
public class RetryController {
	
	Logger logger = Logger.getLogger(RetryController.class);
	
	private static final String template = "Hello, %s!";
	private Retry retryWithConfig;
	private final AtomicLong counter = new AtomicLong();
	public static String str = "consumer side results: ";

	@GetMapping("/demo")
	public Message retry(@RequestParam(value = "name", defaultValue = "World") String name) {
		help();
		str = "consumer side results: ";
		
		CircuitBreakerController cbc = new CircuitBreakerController();
		Runnable runnable = () -> cbc.circuitBreaker();
		retryWithConfig.executeRunnable(runnable);

		String hello = "calling circuit breaker failed";
		
		Supplier<String> supplier = () -> cbc.circuitBreaker();
		hello = (String) retryWithConfig.executeSupplier(supplier);
		
		str += hello;
		//logger.trace("retry circuit breaker result: " + hello);
		
		return new Message(counter.get(), String.format(template, name), str);
	}

	@Bean
	public Message help() {
		RetryConfig config = RetryConfig.custom().maxAttempts(5).waitDuration(Duration.ofMillis(1000))
				.retryOnException(e -> e instanceof BulkheadFullException).retryExceptions().ignoreExceptions()
				.build();
		retryWithConfig = Retry.of("my", config);
		return null;
	}
}
