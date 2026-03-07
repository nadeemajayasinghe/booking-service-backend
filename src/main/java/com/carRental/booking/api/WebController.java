package com.vehiclerental.bookingservice.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class WebController {

    @GetMapping("/")
    public String home() {
        return "index";
    }

    @GetMapping("/booking-form")
    public String bookingForm() {
        return "booking-form";
    }

    @GetMapping("/bookings")
    public String viewBookings() {
        return "view-bookings";
    }
}
