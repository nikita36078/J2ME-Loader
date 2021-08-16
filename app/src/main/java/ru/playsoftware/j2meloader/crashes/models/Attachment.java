package ru.playsoftware.j2meloader.crashes.models;

import java.util.UUID;

public class Attachment extends AbstractLog {
	public final String type = "errorAttachment";
	public final String contentType = "text/plain";
	public final String fileName;

	public String errorId;
	public String data;

	public Attachment(String fileName) {
		super(UUID.randomUUID().toString());
		this.fileName = fileName;
	}
}
