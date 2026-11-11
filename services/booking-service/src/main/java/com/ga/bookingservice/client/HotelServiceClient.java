package com.ga.bookingservice.client;
import com.ga.bookingservice.exception.custom.InformationNotFoundException;
import com.ga.bookingservice.exception.custom.ServiceUnavailableException;
import com.ga.bookingservice.exception.custom.UnauthorizedActionException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.time.Duration;
import java.util.List;
import java.util.Optional;

@Component
public class HotelServiceClient {
private final RestClient restClient;

public HotelServiceClient(@Value("${services.hotel.url}") String url){
    SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
    requestFactory.setConnectTimeout(Duration.ofSeconds(2));
    requestFactory.setReadTimeout(Duration.ofSeconds(3));
    this.restClient = RestClient.builder().baseUrl(url).requestFactory(requestFactory).build();

}
    public List<AvailableHotel> getAvailableRooms(String city, int adults, int children) {
        try{
            List<AvailableHotel> hotels = restClient.get()
                  .uri(uriBuilder -> uriBuilder.path("/api/hotels/available-rooms")
                          .queryParamIfPresent("city",Optional.ofNullable(city) )
                          .queryParam("adults", adults)
                          .queryParam("children", children)
                          .build())
                  .retrieve()
                    .body(new ParameterizedTypeReference<>() {
                    });
        if (hotels == null){
            return List.of();
        }
        return hotels;
        } catch (RestClientException e) {
            throw new ServiceUnavailableException("Hotel service is not available right now.");
        }
    }
    public HotelSummary getHotelSummary(Long id) {
        try{
            return restClient.get()
                      .uri("/api/hotels/{id}/summary",id)
                      .retrieve()
                      .body(HotelSummary.class);
        }catch (HttpClientErrorException.NotFound e) {
        throw new InformationNotFoundException("Hotel " + id + " not found"); }
    catch (RestClientException e) {
            throw new ServiceUnavailableException("Hotel service is not available right now.");
        }
    }
    public ChildPolicySummary getChildPolicySummary(Long id) {
        try{
            return restClient.get()
                    .uri("/api/hotels/{id}/child-policy",id)
                    .retrieve()
                    .body(ChildPolicySummary.class);
        }catch (HttpClientErrorException.NotFound e) {
            return null;
        }
    catch (RestClientException e) {
            throw new ServiceUnavailableException("Hotel service is not available right now.");
        }
    }
    public RoomSummary getRoomSummary(Long hotelId, Long roomId, String token) {
        try{
            return restClient.get()
                    .uri("/api/hotels/{hotelId}/rooms/{roomId}", hotelId, roomId)
                    .header(HttpHeaders.AUTHORIZATION, token)
                    .retrieve()
                    .body(RoomSummary.class);
        } catch (HttpClientErrorException.NotFound e){
            throw new InformationNotFoundException("Room " + roomId + " not found"); }
        catch (HttpClientErrorException.Forbidden | HttpClientErrorException.Unauthorized e) {
            throw new UnauthorizedActionException("You are not allowed to view room " + roomId);
        }
    catch (RestClientException e) {
            throw new ServiceUnavailableException("Hotel service is not available right now.");
        }
    }
    public List<HotelSummary> getManagersHotel(String token) {
        try{
            List<HotelSummary> managersHotel =  restClient.get()
                    .uri(u -> u.path("/api/hotels/my-hotels").queryParam("size", 1000).build())
                    .header(HttpHeaders.AUTHORIZATION, token)
                    .retrieve()
                    .body(new ParameterizedTypeReference<>() {
                    });
            return managersHotel == null ? List.of() : managersHotel;
        } catch (HttpClientErrorException.Forbidden | HttpClientErrorException.Unauthorized e) {
            throw new UnauthorizedActionException("Only hotel managers can do this");
        } catch (RestClientException e) {
            throw new ServiceUnavailableException("Hotel service is not available right now. Please try again.");
        }
    }


}
