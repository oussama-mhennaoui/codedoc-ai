import org.springframework.web.bind.annotation.*;

@RestController
public class AccountController {

    @PostMapping("/account/email")
    public String changeEmail(
            @RequestParam String email) {
        return "Email requested: " + email;
    }
}