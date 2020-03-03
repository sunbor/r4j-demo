package com.example.demo;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ConnectException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.time.Duration;
import java.util.function.Supplier;

import org.apache.log4j.Logger;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.utility.ThrowingConsumer;

import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.vavr.CheckedFunction0;

@RestController
public class CircuitBreakerController {

	Logger logger = Logger.getLogger(CircuitBreakerController.class);
	static Logger statLog = Logger.getLogger(CircuitBreakerController.class);

	// circuitbreaker stuff
	CircuitBreakerConfig config = CircuitBreakerConfig.custom().failureRateThreshold(10).slidingWindowSize(50)
			.waitDurationInOpenState(Duration.ofMillis(5000)).build();
	CircuitBreakerRegistry cbr = CircuitBreakerRegistry.of(config);
	CircuitBreaker cb = cbr.circuitBreaker("breakMe");
	CheckedFunction0<String> whatIsThis = CircuitBreaker.decorateCheckedSupplier(cb, () -> accessProducer());

	@RequestMapping("/circuit_breaker")
	public String circuitBreaker() {

		cb.getEventPublisher().onStateTransition(event -> logger.info("circuit breaker flipped"));

		String inputLine = "this did not work";

		try {
			// Try<String> ded = Try.of(whatIsThis).map(someStr -> someStr);
			// inputLine = ded.get();
			logger.trace("inside circuit breaker");
			inputLine = cb.executeSupplier(throwingConsumerWrapper(() -> accessProducer()));
		} catch (CallNotPermittedException e) {
			logger.error("circuit breaker opened");
			inputLine = "circuit breaker opened";
			// e.printStackTrace();
		} catch (ConnectException e) {
			logger.trace("inside catch for connection exception");
		} catch (IOException e) {
			// TODO Auto-generated catch block
			logger.error("jhklwfajhklwajhkwrgeajhklwrgeahuilwrgejhklwgea");
			// e.printStackTrace();
		} catch (RuntimeException e) {
			logger.error("exception occurred while trying to connect");
			inputLine = "failed to connect.\n" + "producer application is down";
		}

		logger.trace("inputLine: " + inputLine);
		return inputLine;
	}

	// accesses the other application
	private String accessProducer() throws ConnectException {

		String inputLine = "accessProducer did not work";
		try {
			URL url = new URL("http://localhost:8081/test");
			HttpURLConnection conn = (HttpURLConnection) url.openConnection();
			conn.setRequestMethod("GET");

			BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));

			StringBuffer content = new StringBuffer();
			while ((inputLine = in.readLine()) != null) {
				content.append(inputLine);
			}
			inputLine = content.toString();
			in.close();

		} catch (MalformedURLException e) {
			logger.error("url format error while trying to connect to producer");
			e.printStackTrace();
		}
		catch (ConnectException e) {
			logger.error("failed to connect to producer");
			throw e;
			// e.printStackTrace();
		} catch (IOException e) {
			logger.error("ioexception while trying to connect to producer");
			e.printStackTrace();
		}

		logger.trace("producer output: " + inputLine);
		return inputLine;
	}
	
	// middle function required in order for a lambda function to throw an exception
	// apparently
	static Supplier<String> throwingConsumerWrapper(ThrowingConsumer<ConnectException> throwingConsumer)
			throws ConnectException, IOException {

		return () -> {
			String inputLine = "throwing function did not work";
			try {
				inputLine = (String) throwingConsumer.accept();
			} catch (ConnectException ex) {
				statLog.error("middle connect catch");
				throw new RuntimeException(ex);
			} catch (IOException e) {
				statLog.error("io error inside throwing wrapper");
				throw new RuntimeException(e);
			}
			return inputLine;
		};
	}
}
