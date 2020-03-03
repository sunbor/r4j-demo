package com.example.demo;

public class Message {

	private final long id;
	private final String content;
	private final String str;

	public Message(long id, String content, String str) {
		this.id = id;
		this.content = content;
		this.str = str;
	}

	public long getId() {
		return id;
	}

	public String getContent() {
		return content;
	}

	public String getStr() {
		return str;
	}
}
