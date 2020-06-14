package me.nuguri.common.exception;

import lombok.NoArgsConstructor;

@NoArgsConstructor
public class NoElementException extends BaseException {

    public NoElementException(String message) {
        super(message);
    }

}
