package net.montaki.demoescqrsaxon.commands.exceptions;

public class InsufficientBalanceException extends IllegalStateException {
    public InsufficientBalanceException() {
        super("Insufficient balance");
    }
}
