package com.example.demo.storage;

/** 状态无法可靠读写时抛出；上层不得把该异常当成保存成功。 */
public class SupportStorageException extends RuntimeException {

    public SupportStorageException(String message, Throwable cause) {
        super(message, cause);
    }
}
