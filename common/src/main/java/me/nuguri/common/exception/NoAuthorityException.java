package me.nuguri.common.exception;

import lombok.NoArgsConstructor;

@NoArgsConstructor
public class NoAuthorityException extends RuntimeException {

    public NoAuthorityException(String message) {
        super(message);
    }

}
