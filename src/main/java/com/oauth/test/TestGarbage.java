package com.oauth.test;

public class TestGarbage {
  public static void main(String[] args) {
    //
      String uid = "\\u" + String.format("%04x", 128 + 100);
    System.out.println(uid);
  }
}
