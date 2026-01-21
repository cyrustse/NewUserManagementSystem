import org.mindrot.jbcrypt.BCrypt;

public class TestPassword {
    public static void main(String[] args) {
        String hash = "$2a$12$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi";
        System.out.println("Testing hash: " + hash);
        System.out.println("Verify 'password': " + BCrypt.checkpw("password", hash));
        
        String newHash = BCrypt.hashpw("admin123", BCrypt.gensalt(12));
        System.out.println("New hash for admin123: " + newHash);
        System.out.println("Verify admin123: " + BCrypt.checkpw("admin123", newHash));
    }
}
