package com.cjlogistics.mini.shipper;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ShipperService {

    private final ShipperRepository shipperRepository;

    @Transactional
    public Shipper create(String name, String phone) {
        return shipperRepository.save(new Shipper(name, phone));
    }

    public Shipper get(Long id) {
        return shipperRepository.findById(id)
                .orElseThrow(() -> new ShipperNotFoundException(id));
    }
}
