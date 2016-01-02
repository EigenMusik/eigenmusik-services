package com.eigenmusik.exceptions;

public class EmailExistsException extends Exception {

    @Override
    public String getMessage() {
        return "Email already exists in our database.";
    }
}
