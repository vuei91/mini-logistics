package com.cjlogistics.mini.driver;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Embeddable
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@EqualsAndHashCode
public class PreferredRoute {

    @Column(nullable = false, length = 50)
    private String originRegion;

    @Column(nullable = false, length = 50)
    private String destinationRegion;
}
