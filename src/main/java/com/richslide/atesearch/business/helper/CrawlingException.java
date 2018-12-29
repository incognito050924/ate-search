package com.richslide.atesearch.business.helper;

public class CrawlingException extends Exception {
    public CrawlingException() { }
    public CrawlingException(String message) { super(message); }
    public CrawlingException(Throwable t) { super(t); }
    public CrawlingException(String message, Throwable t) { super(message, t); }
}
