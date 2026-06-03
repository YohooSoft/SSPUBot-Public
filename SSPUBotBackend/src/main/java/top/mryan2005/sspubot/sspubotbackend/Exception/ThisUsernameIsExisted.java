package top.mryan2005.sspubot.sspubotbackend.Exception;

public class ThisUsernameIsExisted extends RuntimeException {
    public ThisUsernameIsExisted(String message) {
        super(message);
    }
}
