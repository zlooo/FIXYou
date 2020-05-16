package pl.zlooo.fixyou;

public class FIXYouException extends RuntimeException {
    public FIXYouException(String message) {
        super(message);
    }

    public FIXYouException(Throwable cause) {
        super(cause);
    }
}
