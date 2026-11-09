package net.montaki.demoescqrsaxon.query.controllers;

public class AccountNotFoundException extends RuntimeException {
    public AccountNotFoundException(String id) { super("Account not found: " + id); }
}
