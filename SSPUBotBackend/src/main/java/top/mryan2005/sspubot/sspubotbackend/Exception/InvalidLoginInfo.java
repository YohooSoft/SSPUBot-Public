package top.mryan2005.sspubot.sspubotbackend.Exception;

public class InvalidLoginInfo extends RuntimeException {
    public InvalidLoginInfo(String message) {
        super(message);
    }
}
