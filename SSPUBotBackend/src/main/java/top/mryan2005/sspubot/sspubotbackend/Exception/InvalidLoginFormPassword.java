package top.mryan2005.sspubot.sspubotbackend.Exception;

public class InvalidLoginFormPassword extends RuntimeException {
    public InvalidLoginFormPassword(String message) {
        super(message);
    }
}
