import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

public class PasswordEncoder {
    public static void main(String[] args) {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        // 修改这里的密码
        String rawPassword = args.length > 0 ? args[0] : "admin123456";
        String encodedPassword = encoder.encode(rawPassword);
        System.out.println("Raw password: " + rawPassword);
        System.out.println("Encoded password: " + encodedPassword);
        System.out.println();
        System.out.println("-- 在 MySQL 中执行以下 SQL 重置密码:");
        System.out.println("USE ai_gateway;");
        System.out.println("UPDATE admin_user SET password = '" + encodedPassword + "' WHERE username = 'admin';");
    }
}
