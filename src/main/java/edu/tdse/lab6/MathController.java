package edu.tdse.lab6;

/**
 * Math REST controller demonstrating multiple @GetMapping and @RequestParam.
 */
@RestController
public class MathController {

    @GetMapping("/pi")
    public String pi() {
        return "<!DOCTYPE html><html><body>"
                + "<h2>π = " + Math.PI + "</h2>"
                + "<p><em>Computed by MicroSpringBoot MathController</em></p>"
                + "<a href='/'>Back to Home</a>"
                + "</body></html>";
    }

    @GetMapping("/square")
    public String square(@RequestParam(value = "n", defaultValue = "5") String n) {
        try {
            double num = Double.parseDouble(n);
            double result = num * num;
            return "<!DOCTYPE html><html><body>"
                    + "<h2>" + num + "² = " + result + "</h2>"
                    + "<form action='/square' method='get'>"
                    + "  <label>Number: <input type='number' name='n' value='" + n + "'/></label>"
                    + "  <button type='submit'>Calculate</button>"
                    + "</form>"
                    + "<a href='/'>Back to Home</a>"
                    + "</body></html>";
        } catch (NumberFormatException e) {
            return "<html><body><h2>Invalid number: " + n + "</h2></body></html>";
        }
    }
}
