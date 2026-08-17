package com.example.demo.Dto;

public class MonthlyRevenueDTO {

    private String month;   // e.g. "2025-01"
    private Double revenue; // SUM(total_price)

    public MonthlyRevenueDTO(String month, Double revenue) {
        this.month   = month;
        this.revenue = revenue;
    }

    public String getMonth()   { return month; }
    public Double getRevenue() { return revenue; }
}