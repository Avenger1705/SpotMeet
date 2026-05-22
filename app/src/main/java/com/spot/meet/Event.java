package com.spot.meet;

import java.util.ArrayList;
import java.util.List;

public class Event {
    public String id;
    public String title;
    public String description;
    public String locationName;
    public double lat;
    public double lng;
    public String phone;
    public int totalPlaces;
    public int availablePlaces;
    public String mainImageUrl;
    public String thumbnailUrl;
    public List<String> otherImagesUrls;
    public String creatorUsername;
    public long timestamp;
    public long eventTimestamp;
    public List<String> bookedUsers;

    public Event() {
    }

    public Event(String id, String title, String description, String locationName, double lat, double lng, String phone, int totalPlaces, int availablePlaces, String mainImageUrl, String thumbnailUrl, List<String> otherImagesUrls, String creatorUsername, long timestamp, long eventTimestamp, List<String> bookedUsers) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.locationName = locationName;
        this.lat = lat;
        this.lng = lng;
        this.phone = phone;
        this.totalPlaces = totalPlaces;
        this.availablePlaces = availablePlaces;
        this.mainImageUrl = mainImageUrl;
        this.thumbnailUrl = thumbnailUrl;
        this.otherImagesUrls = otherImagesUrls;
        this.creatorUsername = creatorUsername;
        this.timestamp = timestamp;
        this.eventTimestamp = eventTimestamp;
        this.bookedUsers = bookedUsers != null ? bookedUsers : new ArrayList<>();
    }
}
