package com.cjlogistics.mini.driver;

import jakarta.persistence.CascadeType;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "drivers")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Driver {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false, length = 20)
    private String phone;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private DriverStatus status;

    @OneToOne(cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @JoinColumn(name = "vehicle_id")
    private Vehicle vehicle;

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(
            name = "driver_preferred_routes",
            joinColumns = @JoinColumn(name = "driver_id")
    )
    private List<PreferredRoute> preferredRoutes = new ArrayList<>();

    public Driver(String name, String phone, Vehicle vehicle) {
        this.name = name;
        this.phone = phone;
        this.status = DriverStatus.AVAILABLE;
        this.vehicle = vehicle;
    }

    public void addPreferredRoute(PreferredRoute route) {
        this.preferredRoutes.add(route);
    }

    public void updateStatus(DriverStatus status) {
        this.status = status;
    }
}
