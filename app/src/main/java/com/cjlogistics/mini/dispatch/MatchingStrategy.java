package com.cjlogistics.mini.dispatch;

import com.cjlogistics.mini.driver.Driver;
import com.cjlogistics.mini.shipment.ShipmentRequest;

import java.util.List;

public interface MatchingStrategy {

    List<MatchCandidate> findCandidates(ShipmentRequest request, List<Driver> availableDrivers);
}
