package io.github.ziy1.nexevent.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Data
public class TicketMasterApiResponseDto {
    @JsonProperty("_embedded") // maps to the "_embedded" object in the JSON, same for other fields
    private Embedded embedded = new Embedded();

    @Data
    public static class Embedded {
        @JsonProperty("events")
        private List<Event> events = List.of();

        @Data
        public static class Event {
            @JsonProperty("id")
            private String id = "";
            @JsonProperty("name")
            private String name = "";
            @JsonProperty("url")
            private String url = "";
            @JsonProperty("distance")
            private double distance;
            @JsonProperty("images")
            private List<Image> images = List.of();
            @JsonProperty("classifications")
            private List<Classification> classifications = List.of();
            @JsonProperty("_embedded")
            private EmbeddedVenue embeddedVenue = new EmbeddedVenue();

            public String getImageUrl() {
                return images.stream()
                        .map(Image::getUrl)
                        .filter(url -> url != null && !url.isEmpty())
                        .findFirst()
                        .orElse("");
            }

            public Set<String> getCategories() {
                return classifications.stream()
                        .map(classification -> classification.getGenre().getName())
                        .filter(Objects::nonNull)
                        .collect(Collectors.toSet());
            }

            public String getAddress() {
                return Optional.ofNullable(embeddedVenue)
                        .map(EmbeddedVenue::getVenues)
                        .filter(venues -> !venues.isEmpty())
                        .map(venues -> {
                            String addressLines = String.join(" ",
                                    Optional.ofNullable(venues.get(0).getAddress().getLine1()).orElse(""),
                                    Optional.ofNullable(venues.get(0).getAddress().getLine2()).orElse(""),
                                    Optional.ofNullable(venues.get(0).getAddress().getLine3()).orElse("")
                            ).trim();
                            String cityName = Optional.ofNullable(venues.get(0).getCity().getName()).orElse("");
                            String stateName = Optional.ofNullable(venues.get(0).getState().getName()).orElse("");

                            return (addressLines + ", " + cityName + ", " + stateName).trim().replaceAll(", ,", ",");
                        })
                        .orElse("").trim();
            }


            @Data
            public static class Image {
                @JsonProperty("url")
                private String url = "";
            }

            @Data
            public static class Classification {
                @JsonProperty("genre")
                private Genre genre = new Genre();

                @Data
                public static class Genre {
                    @JsonProperty("name")
                    private String name = "";
                }
            }

            @Data
            public static class EmbeddedVenue {
                @JsonProperty("venues")
                private List<Venue> venues = List.of();

                @Data
                public static class Venue {
                    @JsonProperty("city")
                    private City city = new City();
                    @JsonProperty("state")
                    private State state = new State();
                    @JsonProperty("address")
                    private Address address = new Address();

                    @Data
                    public static class City {
                        @JsonProperty("name")
                        private String name = "";
                    }

                    @Data
                    public static class State {
                        @JsonProperty("name")
                        private String name = "";
                    }

                    @Data
                    public static class Address {
                        @JsonProperty("line1")
                        private String line1 = "";

                        @JsonProperty("line2")
                        private String line2 = "";

                        @JsonProperty("line3")
                        private String line3 = "";
                    }
                }
            }
        }
    }
}
