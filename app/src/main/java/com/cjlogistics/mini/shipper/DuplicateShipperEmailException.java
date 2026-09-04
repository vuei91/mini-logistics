package com.cjlogistics.mini.shipper;

public class DuplicateShipperEmailException extends RuntimeException {
    public DuplicateShipperEmailException(String email) {
        super("이미 가입된 화주 이메일입니다: " + email);
    }
}
