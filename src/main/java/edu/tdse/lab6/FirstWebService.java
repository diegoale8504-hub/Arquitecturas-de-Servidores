package edu.tdse.lab6;


@RestController
public class FirstWebService {

    @GetMapping("/")
    public String index() {
        return "<!DOCTYPE html><html><body>"
                + "<h1>Greetings from MicroSpringBoot!</h1>"
                + "<p>IoC Framework powered by Java Reflection.</p>"
                + "<ul>"
                + "<li><a href='/greeting?name=World'>GET /greeting?name=World</a></li>"
                + "<li><a href='/hello'>GET /hello</a></li>"
                + "<li><a href='/pi'>GET /pi</a></li>"
                + "<li><a href='/index.html'>Static HTML page</a></li>"
                + "</ul>"
                + "</body></html>";
    }

    @GetMapping("/hello")
    public String hello() {
        return "<!DOCTYPE html><html><body>"
                + "<h2>Hello from FirstWebService!</h2>"
                + "<p>This route was registered automatically via reflection.</p>"
                + "<a href='/'>Back to Home</a>"
                + "</body></html>";
    }
}
