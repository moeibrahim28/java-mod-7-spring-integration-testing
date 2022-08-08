package com.example.SpringUnitTesting;

import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class JokeService {
    public String getDadJoke() {
        String apiURL = "https://icanhazdadjoke.com/";
        RestTemplate restTemplate = new RestTemplate();
        String result = restTemplate.getForObject(apiURL, DadJoke.class).joke;

        return result;
    }
}

class DadJoke {
    public String id;
    public String joke;
    public String status;
}