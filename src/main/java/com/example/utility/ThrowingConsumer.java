package com.example.utility;

import java.io.IOException;
import java.net.ConnectException;

public interface ThrowingConsumer <E extends ConnectException>{
	String accept() throws E;

}
