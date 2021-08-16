package ru.playsoftware.j2meloader.crashes.models;

public abstract class AbstractLog {
	public final String id;
	public String userId;
	public String timestamp;
	public Device device;

	public AbstractLog(String id) {
		this.id = id;
	}
}
