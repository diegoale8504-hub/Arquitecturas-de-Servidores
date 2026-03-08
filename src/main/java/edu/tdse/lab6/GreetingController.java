package edu.tdse.lab6;

import java.util.concurrent.atomic.AtomicLong;


@RestController
public class GreetingController {

    private static final String template = "Hello, %s!";
    private final AtomicLong counter = new AtomicLong();

    @GetMapping("/greeting")
    public String greeting(@RequestParam(value = "name", defaultValue = "World") String name) {
        long count = counter.incrementAndGet();
        return "<!DOCTYPE html><html><body>"
                + "<h2>Hola " + name + "</h2>"
                + "<p>Request #" + count + " | " + String.format(template, name) + "</p>"
                + "<form action='/greeting' method='get'>"
                + "  <label>Name: <input type='text' name='name' value='" + name + "'/></label>"
                + "  <button type='submit'>Greet</button>"
                + "</form>"
                + "<br><a href='/'>Back to Home</a>"
                + "</body></html>";
    }
}
