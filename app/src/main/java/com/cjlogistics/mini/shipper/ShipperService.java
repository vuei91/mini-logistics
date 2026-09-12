package com.cjlogistics.mini.shipper;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.crypto.password.PasswordEncoder;
import java.util.Locale;
import com.cjlogistics.mini.auth.InvalidCredentialsException;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ShipperService {
    private static final String DUMMY_PASSWORD_HASH =
            "$2y$10$Q4bnl6HFAZLzSVQSEAfeR.OHSUksZLk1slRI70YziXQvwBpVLCBHK";

    private final ShipperRepository shipperRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public Shipper create(String name, String phone) {
        return shipperRepository.save(new Shipper(name, phone));
    }

    @Transactional
    public Shipper signup(String name, String phone, String email, String password) {
        String normalizedEmail = email.trim().toLowerCase(Locale.ROOT);
        if (shipperRepository.existsByEmail(normalizedEmail)) {
            throw new DuplicateShipperEmailException(normalizedEmail);
        }
        return shipperRepository.save(Shipper.register(name, phone, normalizedEmail, passwordEncoder.encode(password)));
    }

    public Shipper login(String email, String password) {
        Shipper shipper = shipperRepository.findByEmail(email.trim().toLowerCase(Locale.ROOT)).orElse(null);
        String passwordHash = shipper == null ? DUMMY_PASSWORD_HASH : shipper.getPasswordHash();
        boolean passwordMatches = passwordEncoder.matches(password, passwordHash);
        if (shipper == null || !passwordMatches) throw new InvalidCredentialsException();
        return shipper;
    }

    public Shipper get(Long id) {
        return shipperRepository.findById(id)
                .orElseThrow(() -> new ShipperNotFoundException(id));
    }

    @Transactional
    public Shipper updateProfile(Long id, String name, String phone) {
        Shipper shipper = get(id);
        shipper.updateProfile(name, phone);
        return shipper;
    }
}
