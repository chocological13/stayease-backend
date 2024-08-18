package com.finalproject.stayease.exceptions;

public class DataNotFoundException extends RuntimeException {

  public DataNotFoundException(String message) {
    super(message);
  }

  public DataNotFoundException(String message, Throwable cause) {
    super(message, cause);
  }

}