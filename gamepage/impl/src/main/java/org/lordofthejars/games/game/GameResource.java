package org.lordofthejars.games.game;

import freemarker.template.Template;
import freemarker.template.TemplateException;
import io.reactivex.Observable;
import io.reactivex.Observer;
import io.reactivex.Scheduler;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.Resource;
import javax.enterprise.concurrent.ManagedExecutorService;
import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.container.AsyncResponse;
import javax.ws.rs.container.Suspended;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;
import org.lordofthejars.games.details.api.Detail;
import org.lordofthejars.games.details.api.DetailService;
import org.lordofthejars.games.game.api.FreeMarker;
import org.lordofthejars.games.game.api.Game;
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

    @Inject
    @FreeMarker("gamepage.ftl")
    Template template;

    // The UI
    @GET
    @Path("games/{game}")
    @Produces(MediaType.TEXT_HTML)
    public void front(@Suspended final AsyncResponse asyncResponse, @PathParam("game") long gameId) throws IOException {
        final Game game = gameService.findGameById(gameId).orElse(new Game());
        final Detail detail = detailService.findDetailByGameId(gameId).orElse(new Detail());
        final List<Review> reviews = reviewService.findReviewsByGameId(gameId);

        final Map<String, Object> templateData = new HashMap<>();
        templateData.put("game", game);
        templateData.put("detail", detail);
        templateData.put("reviews", reviews);

        final StreamingOutput streamingOutput = (OutputStream out) -> {
            final PrintWriter writer = new PrintWriter(out);
            try {
                template.process(templateData, writer);
            } catch (TemplateException e) {
                throw new IllegalArgumentException(e);
            }
        };

        asyncResponse.resume(Response.ok(streamingOutput).build());
    }

    // The API
    @GET
    @Path("api/v1/games/{game}")
    @Produces(MediaType.APPLICATION_JSON)
    public void findGame(@Suspended final AsyncResponse asyncResponse, @PathParam("game") long gameId) {
        final Scheduler scheduler = Schedulers.from(executor);

        Observable.zip(getGame(gameId)
                .subscribeOn(scheduler),
            getDetail(gameId)
                .subscribeOn(scheduler),
            getReviews(gameId)
                .subscribeOn(scheduler),
            (game, detail, reviews) -> new GameInfo(game, detail, reviews))
        .subscribeOn(scheduler)
        .subscribe(sendGameInfo(asyncResponse));
    }

    private Observable<Game> getGame(final long gameId) {
        return Observable.create(e -> {
            if (!e.isDisposed()) {
                e.onNext(gameService.findGameById(gameId).orElse(new Game()));
                e.onComplete();
            }
        });
    }

    private Observable<Detail> getDetail(final long gameId) {
        return Observable.create(e -> {
            if (!e.isDisposed()) {
                e.onNext(detailService.findDetailByGameId(gameId).orElse(new Detail()));
                e.onComplete();
            }
        });
    }

    private Observable<List<Review>> getReviews(final long gameId) {
        return Observable.create(e -> {
            if (!e.isDisposed()) {
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
