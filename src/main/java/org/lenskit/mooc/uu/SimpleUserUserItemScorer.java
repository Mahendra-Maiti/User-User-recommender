package org.lenskit.mooc.uu;

import com.google.common.collect.Maps;
import it.unimi.dsi.fastutil.longs.Long2DoubleMap;
import it.unimi.dsi.fastutil.longs.Long2DoubleOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2DoubleSortedMap;
import org.grouplens.lenskit.vectors.SparseVector;
import org.lenskit.api.Result;
import org.lenskit.api.ResultMap;
import org.lenskit.basic.AbstractItemScorer;
import org.lenskit.data.dao.DataAccessObject;
import org.lenskit.data.entities.CommonAttributes;
import org.lenskit.data.ratings.Rating;
import org.lenskit.results.BasicResultMap;
import org.lenskit.results.Results;
import org.lenskit.util.ScoredIdAccumulator;
import org.lenskit.util.TopNScoredIdAccumulator;
import org.lenskit.util.collections.LongUtils;
import org.lenskit.util.io.ObjectStream;
import org.lenskit.util.math.Scalars;
import org.lenskit.util.math.Vectors;

import javax.annotation.Nonnull;
import javax.inject.Inject;
import java.util.*;

/**
 * User-user item scorer.
 */
public class SimpleUserUserItemScorer extends AbstractItemScorer {
    private final DataAccessObject dao;
    private final int neighborhoodSize;

    /**
     * Instantiate a new user-user item scorer.
     * @param dao The data access object.
     */
    @Inject
    public SimpleUserUserItemScorer(DataAccessObject dao) {
        this.dao = dao;
        neighborhoodSize = 30;
    }



    @Nonnull
    @Override
    public ResultMap scoreWithDetails(long user, @Nonnull Collection<Long> items) {
        // TODO Score the items for the user with user-user CF
        Long2DoubleOpenHashMap user_rating_vector=getUserRatingVector(user);
        Double sum=new Double(0);
        Double mean=new Double(0);

        List<Result> results =new ArrayList<>();

        ObjectStream <Rating> ratings=dao.query(Rating.class).stream();
        Map<Long,Map<Long,Double>> item_map =new HashMap<>();

        for(Rating r:ratings) //construct raters for each item.
        {
            Map<Long,Double> new_user_vector=new HashMap<>();
            Long key=r.getItemId();

            if(item_map.containsKey(key))
            {
                Map<Long,Double> user_vector=item_map.get(key);
                user_vector.put(r.getUserId(),r.getValue());

                item_map.put(key,user_vector); //update user vector for each item.
            }
            else
            {

                new_user_vector.put(r.getUserId(),r.getValue());
                item_map.put(key,new_user_vector);
            }
        }



        for (Long item: items) //predict rating of each item for this particular user
        {

            Map<Long,Double> user_set=item_map.get(item); //set of users who have rated this item
            Map<Long,Map<Long,Double>> uv=new HashMap<>();

            //Construct uv for each item
            for(Map.Entry<Long,Double> e1: user_set.entrySet()) //iterate the set of users that have rated this item
            {
                Long u_id=e1.getKey();
                if(u_id!=user) //add user rating vectors of those except target user who were not added previously
                {
                    uv.put(u_id,getUserRatingVector(u_id));
                }

            }

            Map<Long,Double> nbs=calculate_top_30(user_rating_vector,uv); //find top 30 nbs for this user
            if(nbs.size()>=2) //score items only when there are more than 2 neighbors
            {
                Double numerator=new Double(0);
                Double denominator=new Double(0);

                Map<Long,Double> mean_val=calculate_mean_rating(user,user_rating_vector,uv); //calculate mean rating value for each user including the target user.


                for(Map.Entry<Long,Double> e2: nbs.entrySet())
                {
                    Long key_val=e2.getKey();
                    Double similarity_Val=e2.getValue();

                    numerator+=(uv.get(key_val).get(item)-mean_val.get(key_val))*(similarity_Val);
                    denominator+=similarity_Val;
                }

                Double predicted_rating= mean_val.get(user)+(numerator/denominator);
                results.add(Results.create(item,predicted_rating));
            }

        }

        return Results.newResultMap(results);

    }




    public Map<Long,Double> calculate_top_30(Map<Long,Double> user_rating_vector,
                                       Map<Long,Map<Long,Double>> uv)
    {
        List<Result> results=new ArrayList<>();

            for(Map.Entry<Long,Map<Long,Double>> entry: uv.entrySet() )
            {
                Long u_id=entry.getKey();
                Double similarity=calculate_similarity(user_rating_vector,entry.getValue());
                results.add(Results.create(u_id,similarity));
            }




            Collections.sort(results, new Comparator<Result>() {
                @Override
                public int compare(Result result, Result t1) {
                    return result.getScore()>t1.getScore()?-1:(result.getScore()<t1.getScore()?1:0);
                }
            });

        Map<Long,Double> top_30=new HashMap<>();

        int count=0;
        while(count<30 && count<results.size()){
            Result r=results.get(count);
            Long nb_id=r.getId();
            Double similarity_val=r.getScore();
            if((similarity_val)>0)
            {
                top_30.put(nb_id,similarity_val);
            }

            count++;
        }

        return top_30;
    }

    public Double calculate_similarity(Map<Long,Double> t_usr_map, Map<Long,Double> usr_map)
    {
            Map<Long,Double> t_map=new HashMap<Long,Double>(t_usr_map); //vector for target user
            Map<Long,Double> u_map=new HashMap<Long,Double>(usr_map);// vector for neighbor

;

            Long2DoubleMap m1=LongUtils.frozenMap(t_map);
            Long2DoubleMap m2=LongUtils.frozenMap(u_map);

            Double t_sq=new Double(0);
            Double u_sq=new Double(0);
            Double t_mean=new Double(Vectors.mean(m1));
            Double usr_mean=new Double(Vectors.mean(m2));

            for(Map.Entry<Long,Double> entry: t_map.entrySet()) //calculating values for target user
            {
                Double new_val=entry.getValue()-t_mean;
                entry.setValue(new_val);
                t_sq+=new_val*new_val;
            }

            for(Map.Entry<Long,Double> e1: u_map.entrySet()) //calculating values for neighbors
            {
                Double new_val=e1.getValue()-usr_mean;
                e1.setValue(new_val);
                u_sq+=new_val*new_val;
            }

            Long2DoubleMap final_m1=LongUtils.frozenMap(t_map);
            Long2DoubleMap final_m2=LongUtils.frozenMap(u_map);

            Double denominator=Math.sqrt(t_sq)*Math.sqrt(u_sq);

            Double similarity_val=new Double(Vectors.dotProduct(final_m1,final_m2)/denominator);


            return similarity_val;

    }

    public Map<Long,Double> calculate_mean_rating(Long user,Map<Long,Double> user_rating_vector,
                                                  Map<Long,Map<Long,Double>> uv)
    {
            Map<Long,Double> mean_rating=new HashMap<>();

            for(Map.Entry<Long,Map<Long,Double>> entry: uv.entrySet())
            {
                Long2DoubleMap u_vector=LongUtils.frozenMap(entry.getValue());
                Double mean=Vectors.mean(u_vector);
                mean_rating.put(entry.getKey(),mean);

            }

            Long2DoubleMap t_vector=LongUtils.frozenMap(user_rating_vector);
            Double t_mean=Vectors.mean(t_vector);
            mean_rating.put(user,t_mean);

            return mean_rating;

    }

    /**
     * Get a user's rating vector.
     * @param user The user ID.
     * @return The rating vector, mapping item IDs to the user's rating
     *         for that item.
     */
    private Long2DoubleOpenHashMap getUserRatingVector(long user) {
        List<Rating> history = dao.query(Rating.class)
                                  .withAttribute(CommonAttributes.USER_ID, user)
                                  .get();

        Long2DoubleOpenHashMap ratings = new Long2DoubleOpenHashMap();
        for (Rating r: history) {
            ratings.put(r.getItemId(), r.getValue());
        }

        return ratings;


    }


}
