package com.helpingsapiens.controller;


import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.time.ZonedDateTime;
import java.util.*;

@RestController
@RequestMapping(path="/demo")
public class RatingController {

    @RequestMapping(path = "/getRating/{seeker_user_id}/{currentSeeker_rating}/{previousSeeker_ratings_time}", method = RequestMethod.GET)
    public Double getRatings( @PathVariable String seeker_user_id, @PathVariable Double currentSeeker_rating,@PathVariable List<String> previousSeeker_ratings_time) {
        String[] rating_time ;
        Double rating;
        String user_id;
        ZonedDateTime time;
        ZonedDateTime now = ZonedDateTime.now();
        ZonedDateTime thirtyDaysAgo = now.plusDays(-30);
        List biased_ratings = new ArrayList();
        Double final_rating=currentSeeker_rating;
        Iterator<String> iter = previousSeeker_ratings_time.iterator();
        int count = 0;
        while (iter.hasNext()) {
            rating_time = iter.next().split("@");
            user_id = rating_time[0];
            rating = Double.valueOf(rating_time[1]);
            time = ZonedDateTime.parse(rating_time[2]);
            count++;
            // Check Biased Ratings--------------------------------------
            if (user_id == seeker_user_id && time.toInstant().isBefore(thirtyDaysAgo.toInstant())) {
                biased_ratings.add(iter.next());
                if (biased_ratings.size() > 3)
                {
                    previousSeeker_ratings_time.remove(biased_ratings.get(0));
                    biased_ratings.remove(0);
                    rating = 0.0;
                    count--;
                }
            }

            final_rating = (final_rating + rating) ;
        }

        if (count < 0){
            return final_rating;
        }
        // Return the final average rating after checking the Biased rating.
        return final_rating/count ;
    }

    @RequestMapping(path = "/getRecommendation/{helpers_rating_time}/{helper_ratingCount}", method = RequestMethod.GET)
    public Map<String, Double> getRecommendations(@PathVariable List<String> helpers_rating_time , @PathVariable Map<String, Integer> helper_ratingCount){
        String[] rating_time ;
        Double rating;
        String user_id;
        ZonedDateTime time;
        ZonedDateTime now = ZonedDateTime.now();
        ZonedDateTime one_year_ago = now.plusDays(-365);
        Iterator<String> iter = helpers_rating_time.iterator();

        Map<String, Double> user_final_ratings = new HashMap();
        while (iter.hasNext()){
            rating_time = iter.next().split("@");
            user_id = rating_time[0];
            rating = Double.valueOf(rating_time[1]);
            time = ZonedDateTime.parse(rating_time[2]);

            //Decay Factor ----------------------------------
            if(time.toInstant().isBefore(one_year_ago.toInstant())){
                helpers_rating_time.remove(iter.next());
                user_id = "";
                rating = 0.0;
                if (helper_ratingCount.containsKey(user_id)){
                    helper_ratingCount.replace(user_id,(helper_ratingCount.get(user_id) -1));
                }
            }
            if (user_final_ratings.containsKey(user_id)){
                if (helper_ratingCount.containsKey(user_id)){
                    user_final_ratings.replace(user_id,(user_final_ratings.get(user_id) +rating));
                }
            }
            else {
                user_final_ratings.replace(user_id, rating);
            }
        }
        //Calculate the mean of ratings ------------
        for (Map.Entry<String,Double> entry : user_final_ratings.entrySet()){

            user_final_ratings.replace(entry.getKey(),entry.getValue()/ helper_ratingCount.get(entry.getKey()));
        }

        //Sort HelperList in descending order----------------
        Map<String, Double> recommendedList = new TreeMap<String, Double>(Collections.reverseOrder());
        recommendedList.putAll(user_final_ratings);

        //return the map of HelpersUserId and average rating in descending order
        return recommendedList;
    }
}
