package com.cjlogistics.mini.shipper;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.Locale;

@Entity
@Table(name = "shippers")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Shipper {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false, length = 20)
    private String phone;

    @Column(unique = true, length = 254)
    private String email;

    @Column(name = "password_hash")
    private String passwordHash;

    public Shipper(String name, String phone) {
        this.name = name;
        this.phone = phone;
    }

    public static Shipper register(String name, String phone, String email, String passwordHash) {
        Shipper shipper = new Shipper(name, phone);
        shipper.email = email.trim().toLowerCase(Locale.ROOT);
        shipper.passwordHash = passwordHash;
        return shipper;
    }
}
