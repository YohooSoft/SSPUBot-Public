package top.mryan2005.sspubot.sspubotbackend.Exception;

public class ThisEmailIsExisted extends RuntimeException {
    public ThisEmailIsExisted(String message) {
        super(message);
    }
}
