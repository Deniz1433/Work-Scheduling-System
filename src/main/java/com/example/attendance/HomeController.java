package com.example.attendance;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * HomeController, ana sayfa işlemleri için endpoint sağlar.
 */
@Controller
public class HomeController {

    private static final Logger log = LoggerFactory.getLogger(HomeController.class);
    private final AnimalRepository repo;

    public HomeController(AnimalRepository repo) {
        this.repo = repo;
    }

    /**
     * Ana sayfa ve kullanıcı listesini görüntüler.
     */
    @GetMapping("/")
    public String home(Model model,
                       @AuthenticationPrincipal OidcUser user) {

        model.addAttribute("user", user);
        return "index";
    }
}
