package edu.tdse.lab6;

@RestController
public class CalculatorController {

    @GetMapping("/add")
    public String add(
            @RequestParam(value = "a", defaultValue = "0") String a,
            @RequestParam(value = "b", defaultValue = "0") String b) {
        double result = Double.parseDouble(a) + Double.parseDouble(b);
        return "<html><body><h2>" + a + " + " + b + " = " + result + "</h2>"
                + "<a href='/'>Volver</a></body></html>";
    }

    @GetMapping("/subtract")
    public String subtract(
            @RequestParam(value = "a", defaultValue = "0") String a,
            @RequestParam(value = "b", defaultValue = "0") String b) {
        double result = Double.parseDouble(a) - Double.parseDouble(b);
        return "<html><body><h2>" + a + " - " + b + " = " + result + "</h2>"
                + "<a href='/'>Volver</a></body></html>";
    }

    @GetMapping("/multiply")
    public String multiply(
            @RequestParam(value = "a", defaultValue = "1") String a,
            @RequestParam(value = "b", defaultValue = "1") String b) {
        double result = Double.parseDouble(a) * Double.parseDouble(b);
        return "<html><body><h2>" + a + " × " + b + " = " + result + "</h2>"
                + "<a href='/'>Volver</a></body></html>";
    }
}