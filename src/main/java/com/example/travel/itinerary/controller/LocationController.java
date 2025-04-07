package com.example.travel.itinerary.controller;

import com.example.travel.itinerary.model.Location;
import com.example.travel.itinerary.service.LocationService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/locations")
public class LocationController {

    private final LocationService locationService;

    public LocationController(LocationService locationService) {
        this.locationService = locationService;
    }

    @GetMapping("/search")
    public List<Location> searchLocations(@RequestParam(name = "query") String query) {
        return locationService.searchLocations(query);
    }
}
