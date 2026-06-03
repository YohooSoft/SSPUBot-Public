package top.mryan2005.sspubot.sspubotbackend.Utils;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import top.mryan2005.sspubot.sspubotbackend.Pojo.User;

import java.security.Key;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

@Component
public class JwtUtils {

    // 密钥 (实际开发中应放在 application.yml，且长度至少 256 位)
    private static final String SECRET = "ILoveVentiILoveVentiILoveVentiILoveVentiILoveVenti";
    // 过期时间 (例如 10 小时)
    private static final long EXPIRATION_TIME = 1000 * 60 * 60 * 10;

    private Key getSignInKey() {
        return Keys.hmacShaKeyFor(SECRET.getBytes());
    }

    // 生成 Token
    public String generateToken(UserDetails userDetails) {
        User user = (User) userDetails; // 强转获取 Salt
        Map<String, Object> claims = new HashMap<>();

        // 存入 Salt
        claims.put("salt", user.getSalt());

        // 存入 Role (可选，方便前端获取权限)
        claims.put("role", user.getRole());

        // 存入 DisplayName (用于前端显示用户名)
        claims.put("displayName", user.getDisplayName());

        return createToken(claims, user.getUsername());
    }

    private String createToken(Map<String, Object> claims, String subject) {
        return Jwts.builder()
                .setClaims(claims)
                .setSubject(subject)
                .setIssuedAt(new Date(System.currentTimeMillis()))
                .setExpiration(new Date(System.currentTimeMillis() + EXPIRATION_TIME))
                .signWith(getSignInKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    // 从 Token 中获取用户名
    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    private Claims extractAllClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(getSignInKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    // 验证 Token 是否有效
    public boolean isTokenValid(String token, UserDetails userDetails) {
        // 1. 将 UserDetails 强转为你的 User 实体类
        // 因为我们确信 loadUserByUsername 返回的就是 User 类，所以这里强转是安全的
        User user = (User) userDetails;

        final String username = extractUsername(token);

        // 2. 从 Token 中取出 salt
        final String tokenSalt = extractClaim(token, claims -> claims.get("salt", String.class));

        // 3. 验证：用户名匹配 && Token没过期 && Salt一致
        return (username.equals(user.getUsername())
                && !isTokenExpired(token)
                && tokenSalt != null // 防止 Token 里没有 salt 报错
                && tokenSalt.equals(user.getSalt()));
    }

    private boolean isTokenExpired(String token) {
        return extractClaim(token, Claims::getExpiration).before(new Date());
    }
}