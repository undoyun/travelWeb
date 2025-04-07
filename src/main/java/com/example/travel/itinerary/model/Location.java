package com.example.travel.itinerary.model;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "locations")
public class Location {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String country; // 대한민국 or 일본
    private String city; // 서울, 도쿄 등
    private String description; // 지역 설명

    // 기본 생성자
    public Location() {
    }

    // 생성자
    public Location(String country, String city, String description) {
        this.country = country;
        this.city = city;
        this.description = description;
    }

}