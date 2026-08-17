package com.example.demo.Dto;

import java.math.BigDecimal;

public class DashboardStats {

    private long totalProducts;
    private long pendingOrders;
    private long confirmedOrders;
    private long livredOrders;
    private BigDecimal totalRevenue;

    public DashboardStats() {}

    public DashboardStats(long totalProducts,
                          long pendingOrders,
                          long confirmedOrders,
                          long livredOrders,
                          BigDecimal totalRevenue) {
        this.totalProducts   = totalProducts;
        this.pendingOrders   = pendingOrders;
        this.confirmedOrders = confirmedOrders;
        this.livredOrders    = livredOrders;
        this.totalRevenue    = totalRevenue;
    }

    public long getTotalProducts()   { return totalProducts; }
    public long getPendingOrders()   { return pendingOrders; }
    public long getConfirmedOrders() { return confirmedOrders; }
    public long getLivredOrders()    { return livredOrders; }
    public BigDecimal getTotalRevenue() { return totalRevenue; }

    public void setTotalProducts(long v)      { this.totalProducts = v; }
    public void setPendingOrders(long v)      { this.pendingOrders = v; }
    public void setConfirmedOrders(long v)    { this.confirmedOrders = v; }
    public void setLivredOrders(long v)       { this.livredOrders = v; }
    public void setTotalRevenue(BigDecimal v) { this.totalRevenue = v; }
}