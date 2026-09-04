package com.cjlogistics.mini.driver;

public class DuplicateDriverEmailException extends RuntimeException {
    public DuplicateDriverEmailException(String email) {
        super("이미 가입된 차주 이메일입니다: " + email);
    }
}
