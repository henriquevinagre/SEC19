package pt.ulisboa.tecnico.sec.instances.commands;

public class InvalidCommandException extends Exception {
    
    public InvalidCommandException(String message) {
        super(message);
    }

    public InvalidCommandException(String line, String type) {
        super(String.format("Error reading line '%s', is it of type '%s'?", line, type));
    }
}
