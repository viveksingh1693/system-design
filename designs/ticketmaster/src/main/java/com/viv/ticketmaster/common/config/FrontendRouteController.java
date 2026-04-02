package com.viv.ticketmaster.common.config;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class FrontendRouteController {

    @GetMapping({ "/", "/operations", "/bookings" })
    public String index() {
        return "forward:/index.html";
    }
}
