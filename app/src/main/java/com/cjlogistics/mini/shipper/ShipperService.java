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
        Shipper shipper = shipperRepository.findByEmail(email.trim().toLowerCase(Locale.ROOT))
                .orElseThrow(InvalidCredentialsException::new);
        if (!passwordEncoder.matches(password, shipper.getPasswordHash())) throw new InvalidCredentialsException();
        return shipper;
    }

    public Shipper get(Long id) {
        return shipperRepository.findById(id)
                .orElseThrow(() -> new ShipperNotFoundException(id));
    }
}
