package driver;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicLong;

import org.springframework.context.annotation.Bean;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import io.github.resilience4j.bulkhead.BulkheadFullException;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;

@RestController
public class RetryController {
	private static final String template = "Hello, %s!";
	private Retry retryWithConfig;
	private final AtomicLong counter = new AtomicLong();
	public static String str = "";

	@GetMapping("/demo")
	public Message retry(@RequestParam(value = "name", defaultValue = "World") String name) {
		help();
		
		CircuitBreakerController cbc = new CircuitBreakerController();
		Runnable runnable = () -> cbc.CBC();
		retryWithConfig.executeRunnable(runnable);

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
