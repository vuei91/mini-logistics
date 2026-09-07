package com.cjlogistics.mini.dispatch;

import com.cjlogistics.mini.driver.VehicleType;
import com.cjlogistics.mini.shipment.ShipmentRequest;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * 운임(fare) 산정기.
 *
 * 사용 가능한 데이터(차량 종류, 총 화물 무게, 출발/도착 지역)만으로 근사 산정한다.
 * 실제 거리 정보가 없으므로, 지역이 다르면 지역 간 이동으로 보고 노선 계수를 가중한다.
 *
 * 운임 = (기본요금 + 무게요금) × 차량종류 계수 × 노선 계수  (100원 단위 반올림)
 */
@Component
public class FareCalculator {

    private static final BigDecimal BASE_FARE = BigDecimal.valueOf(50_000);
    private static final BigDecimal PER_KG = BigDecimal.valueOf(100);
    /** 서로 다른 지역 간 이동 시 가중 */
    private static final BigDecimal CROSS_REGION_MULTIPLIER = BigDecimal.valueOf(1.5);
    private static final BigDecimal SAME_REGION_MULTIPLIER = BigDecimal.ONE;
    private static final BigDecimal ROUNDING_UNIT = BigDecimal.valueOf(100);

    public BigDecimal calculate(ShipmentRequest request) {
        BigDecimal weightFare = PER_KG.multiply(BigDecimal.valueOf(request.getTotalCargoWeightKg()));
        BigDecimal subtotal = BASE_FARE.add(weightFare);

        BigDecimal vehicleMultiplier = vehicleMultiplier(request.getRequiredVehicleType());
        BigDecimal routeMultiplier = sameRegion(request) ? SAME_REGION_MULTIPLIER : CROSS_REGION_MULTIPLIER;

        BigDecimal total = subtotal.multiply(vehicleMultiplier).multiply(routeMultiplier);

        // 100원 단위 반올림
        return total.divide(ROUNDING_UNIT, 0, RoundingMode.HALF_UP).multiply(ROUNDING_UNIT);
    }

    private boolean sameRegion(ShipmentRequest request) {
        return request.getOriginRegion() != null
                && request.getOriginRegion().equalsIgnoreCase(request.getDestinationRegion());
    }

    private BigDecimal vehicleMultiplier(VehicleType type) {
        return switch (type) {
            case TRUCK_1T -> BigDecimal.valueOf(1.0);
            case TRUCK_2_5T -> BigDecimal.valueOf(1.3);
            case TRUCK_5T -> BigDecimal.valueOf(1.6);
            case TRUCK_11T -> BigDecimal.valueOf(2.2);
        };
    }
}
