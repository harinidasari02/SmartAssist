package com.smartassist.exception;

/** Thrown when an AI provider (Ollama, Grok) can't be reached or returns an error. */
public class AiProviderException extends RuntimeException {
    public AiProviderException(String message) { super(message); }
    public AiProviderException(String message, Throwable cause) { super(message, cause); }
}
