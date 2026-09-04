package com.cjlogistics.mini.dispatch;

import com.cjlogistics.mini.driver.Driver;

public record MatchCandidate(Driver driver, double score) {
}
