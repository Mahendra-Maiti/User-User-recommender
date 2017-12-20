# User-User Collaborative Filtering.

In this assignment, a user-user collaborative filter is implemented for LensKit.

Specifically, a model-free user-user collaborative filtering scorer that predicts a target user’s movie rating for a target item is built. The set of steps followed are as follows:

1. First, each user’s rating vector is adjusted by subtracting that user’s mean rating from each of their ratings (this corrects for the fact that some users think 5 stars is anything worth seeing and others think 3 stars is very good).
2. Next, the set of other users are identified who have rated the target item and who have a history of rating items similarly to the target user; specifically, we’ll limit this set to the 30 users with the highest cosine similarity between their adjusted rating vectors and the target user's adjusted rating vector.  This similarity measures the angle between the vectors, which is highest when both users have the rated the same items and have given those items the same rating (this won’t be perfectly the case here, since we’re predicting for unrated items).
3. Then the mean-adjusted ratings are combined from these “neighbor” users, weighted by their cosine similarity with the target user — i.e., the more similar the other user’s ratings, the more their rating of the target item influences the prediction for the target user.
4. Finally, the prediction is re-adjusted back to the target user’s original rating scale by adding the target user’s mean rating back into the prediction.

The program will give either specific predictions or predictions for top-10 recommended unrated items for the selected users; this part as it's already built into LensKit.  

## Basic Requirements

THe scoring in this class is implemented as follows:

-   Use user-user collaborative filtering.

-   User similarities are computed by taking the cosine between the users' mean-centered rating vectors (that is, subtract each user's mean rating from their rating vector, and compute the cosine between those two vectors).

-   For each item's score, 30 most similar users who have rated the item and whose
    similarity to the target user is positive are used .

-   Items are not scored if there are not at least 2 neighbors to contribute to the item's score.

-   Mean-centering is used to normalize ratings for scoring. That is, the
    weighted average of each neighbor ![equation](https://latex.codecogs.com/gif.latex?%24v%24)'s offset from average (![equation](https://latex.codecogs.com/gif.latex?%24r_%7Bv%2Ci%7D%20-%20%5Cmu_v%24) ) is computed. Then the user's average rating ![equation](https://latex.codecogs.com/gif.latex?%24%5Cmu_u%24) is added. Like this, where
    ![equation](https://latex.codecogs.com/gif.latex?%24N%28u%3Bi%29%24) is the neighbors of ![equation](https://latex.codecogs.com/gif.latex?%24u%24) who have rated ![equation](https://latex.codecogs.com/gif.latex?%24i%24) and ![equation](https://latex.codecogs.com/gif.latex?%24cos%28u%2Cv%29%24) is the
    cosine similarity between the rating vectors for users ![equation](https://latex.codecogs.com/gif.latex?%24u%24) and ![equation](https://latex.codecogs.com/gif.latex?%24v%24):

    ![equation](https://latex.codecogs.com/gif.latex?%24%24p_%7Bu%2Ci%7D%20%3D%20%5Cmu_u%20&plus;%20%5Cfrac%7B%5Csum_%7Bv%20%5Cin%20N%28u%3Bi%29%7D%20cos%28u%2Cv%29%20%28r_%7Bv%2Ci%7D%20-%20%5Cmu_v%29%7D%7B%5Csum_%7Bv%20%5Cin%20N%28u%3Bi%29%7D%20%7Ccos%28u%2Cv%29%7C%7D%24%24)

-   The cosine similarity is defined as follows:

    ![equation](https://latex.codecogs.com/gif.latex?%24%24cos%28u%2Cv%29%20%3D%20%5Cfrac%7B%5Cvec%20u%20%5Ccdot%20%5Cvec%20v%7D%7B%5C%7C%5Cvec%20u%5C%7C_2%20%5C%7C%5Cvec%20v%5C%7C_2%7D%20%3D%20%5Cfrac%7B%5Csum_i%20u_i%20v_i%7D%7B%5Csqrt%7B%5Csum_i%20u%5E2_i%7D%20%5Csqrt%7B%5Csum_i%20v%5E2_i%7D%7D%24%24)

## Running the Recommender

The recommender can be run by using Gradle; the `predict` target will generate predictions for the
user specified with `userId` and the items specified with `itemIds` . `recommend` will produce top-10 recommendations for a user.

User-user CF has an interesting penchant for recommending really obscure things.  There is an additional configuration for a hybrid recommender that blends the collaborative filtering output with popularity information to prefer more popular items.  To run this version of recommender, use `recommendBlended`.

All recommender-running tasks will send debug output to a log file under `build`.

## Example Output

Command:

    ./gradlew predict -PuserId=320 -PitemIds=260,153,527,588

Output:

```
predictions for user 320:
  153 (Batman Forever (1995)): 2.841
  260 (Star Wars: Episode IV - A New Hope (1977)): 4.549
  527 (Schindler's List (1993)): 4.319
  588 (Aladdin (1992)): 3.554
```

Command:

    ./gradlew recommend -PuserId=320

Output:

```
recommendations for user 320:
  858 (Godfather, The (1972)): 4.562
  2360 (Celebration, The (Festen) (1998)): 4.556
  318 (Shawshank Redemption, The (1994)): 4.556
  8638 (Before Sunset (2004)): 4.512
  7371 (Dogville (2003)): 4.511
  922 (Sunset Blvd. (a.k.a. Sunset Boulevard) (1950)): 4.503
  1217 (Ran (1985)): 4.497
  44555 (Lives of Others, The (Das leben der Anderen) (2006)): 4.491
  2859 (Stop Making Sense (1984)): 4.486
  1089 (Reservoir Dogs (1992)): 4.479
```

Command:

    ./gradlew recommendBlended -PuserId=320

Output:

```
recommendations for user 320:
  318 (Shawshank Redemption, The (1994)): 0.999
  858 (Godfather, The (1972)): 0.999
  58559 (Dark Knight, The (2008)): 0.995
  1089 (Reservoir Dogs (1992)): 0.995
  7153 (Lord of the Rings: The Return of the King, The (2003)): 0.994
  1258 (Shining, The (1980)): 0.989
  1210 (Star Wars: Episode VI - Return of the Jedi (1983)): 0.989
  79132 (Inception (2010)): 0.988
  1080 (Monty Python's Life of Brian (1979)): 0.986
  4973 (Amelie (Fabuleux destin d'Am?lie Poulain, Le) (2001)): 0.982
```

