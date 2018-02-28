package org.lordofthejars.games.game;

import io.reactivex.Observable;
import io.reactivex.Observer;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Function3;
import io.reactivex.schedulers.Schedulers;
import java.util.List;
import javax.annotation.Resource;
import javax.enterprise.concurrent.ManagedExecutorService;
import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.container.AsyncResponse;
import javax.ws.rs.container.Suspended;
import javax.ws.rs.core.Response;
import org.lordofthejars.games.details.api.Detail;
import org.lordofthejars.games.details.api.DetailService;
import org.lordofthejars.games.game.api.Game;
import org.lordofthejars.games.game.api.GameInfo;
import org.lordofthejars.games.game.api.GameService;
import org.lordofthejars.games.reviews.api.Review;
import org.lordofthejars.games.reviews.api.ReviewService;

@Path("/")
public class GameResource {

    @Resource(name = "DefaultManagedExecutorService")
    ManagedExecutorService executor;

    @Inject
    DetailService detailService;

    @Inject
    GameService gameService;

    @Inject
    ReviewService reviewService;

    @GET
    @Path("api/v1/games/{game}")
    @Produces("application/json")
    public void findGame(@Suspended final AsyncResponse asyncResponse, @PathParam("game") long gameId) {
        Observable.zip(getGame(gameId)
                .subscribeOn(Schedulers.from(executor)),
            getDetail(gameId)
                .subscribeOn(Schedulers.from(executor)),
            getReviews(gameId)
                .subscribeOn(Schedulers.from(executor)),
            (game, detail, reviews) -> new GameInfo(game, detail, reviews))
        .subscribeOn(Schedulers.from(executor))
        .subscribe(sendGameInfo(asyncResponse));
    }

    private Observable<Game> getGame(final long gameId) {
        return Observable.create(e -> {
            if (!e.isDisposed()) {
                System.out.println(Thread.currentThread().getName());
                e.onNext(gameService.findGameById(gameId).orElse(new Game()));
                e.onComplete();
            }
        });
    }

    private Observable<Detail> getDetail(final long gameId) {
        return Observable.create(e -> {
            if (!e.isDisposed()) {
                System.out.println(Thread.currentThread().getName());
                e.onNext(detailService.findDetailByGameId(gameId).orElse(new Detail()));
                e.onComplete();
            }
        });
    }

    private Observable<List<Review>> getReviews(final long gameId) {
        return Observable.create(e -> {
            if (!e.isDisposed()) {
                System.out.println(Thread.currentThread().getName());
                e.onNext(reviewService.findReviewsByGameId(gameId));
                e.onComplete();
            }
        });
    }

    private Observer<GameInfo> sendGameInfo(final AsyncResponse asyncResponse) {
        return new Observer<GameInfo>() {
            @Override
            public void onSubscribe(Disposable disposable) {
            }

            @Override
            public void onNext(GameInfo gameInfo) {
                System.out.println(Thread.currentThread().getName());
                asyncResponse.resume(Response.ok(gameInfo).build());
            }

            @Override
            public void onError(Throwable throwable) {
                asyncResponse.resume(throwable);
            }

            @Override
            public void onComplete() {
            }
        };
    }

}
