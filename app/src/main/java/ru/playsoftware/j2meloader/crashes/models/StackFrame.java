package ru.playsoftware.j2meloader.crashes.models;

public class StackFrame {
	public String className;
	public String methodName;
	public int lineNumber;
	public String fileName;

	@SuppressWarnings("unused")
	public StackFrame() {}

	public StackFrame(StackTraceElement stackTraceElement) {
		className = stackTraceElement.getClassName();
		methodName = stackTraceElement.getMethodName();
		lineNumber = stackTraceElement.getLineNumber();
		fileName = stackTraceElement.getFileName();
	}
}
