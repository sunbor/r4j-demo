package driver;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicLong;

import org.springframework.context.annotation.Bean;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;

@RestController
public class RetryController {
	private static final String template = "Hello, %s!";
	private Retry retryWithConfig;
	private final AtomicLong counter = new AtomicLong();
	public static String str = "";

	@GetMapping("/demo")
	public RetryGreet retry(@RequestParam(value = "name", defaultValue = "World") String name) {
		Runnable runnable = () -> divideBy5(counter.incrementAndGet());
		retryWithConfig.executeRunnable(runnable);

		return new RetryGreet(counter.get(), String.format(template, name), str);
	}

	@Bean
	public RetryGreet help() {
		RetryConfig config = RetryConfig.custom().maxAttempts(5).waitDuration(Duration.ofMillis(1000))
				.retryOnException(e -> e instanceof NotDivisibleByFiveException).retryExceptions().ignoreExceptions()
				.build();
		retryWithConfig = Retry.of("my", config);
		return null;
	}

	static void divideBy5(long l) {
		if (l % 5 != 0) {
			str = str + String.format("%d is not divisible by 5 \n", l);
			throw new NotDivisibleByFiveException(String.format("%d is not divisible by 5", l));
		} else {
			str = str + String.format("%d is divisible by 5", l);
			System.out.println(String.format("%d is divisible by 5", l));
		}
	}
}
