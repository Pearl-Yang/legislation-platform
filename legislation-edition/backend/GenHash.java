import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

public class GenHash {
    public static void main(String[] args) {
        BCryptPasswordEncoder enc = new BCryptPasswordEncoder();
        // 5 个种子账号统一用 123456
        String hash = enc.encode("123456");
        System.out.println(hash);
        System.out.println("matches=" + enc.matches("123456", hash));
    }
}