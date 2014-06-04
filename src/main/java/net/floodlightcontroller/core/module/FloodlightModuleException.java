package net.floodlightcontroller.core.module;

public class FloodlightModuleException extends Exception {
    private static final long serialVersionUID = 1L;

    public FloodlightModuleException(String message) {
        super(message);
    }

    public FloodlightModuleException(String message, Throwable cause) {
        super(message, cause);
    }
}
