package com.emmanuelfinance.shared.mongo;

public interface BaseProjector<T extends BaseDocument> {

    void project(T snapshot);
}
