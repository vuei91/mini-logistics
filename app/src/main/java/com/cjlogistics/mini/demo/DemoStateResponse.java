package com.cjlogistics.mini.demo;

import com.cjlogistics.mini.dispatch.dto.DispatchResponse;
import com.cjlogistics.mini.driver.dto.DriverResponse;
import com.cjlogistics.mini.shipment.dto.ShipmentRequestResponse;
import com.cjlogistics.mini.shipper.dto.ShipperResponse;

public record DemoStateResponse(
        ShipperResponse shipper,
        DriverResponse driver,
        ShipmentRequestResponse shipmentRequest,
        DispatchResponse dispatch
) {
}
