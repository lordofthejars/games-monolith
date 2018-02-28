package org.lordofthejars.games.game.impl;

import java.util.Optional;
import javax.enterprise.context.ApplicationScoped;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.transaction.Transactional;
import org.lordofthejars.games.game.api.Game;
import org.lordofthejars.games.game.api.GameService;

@ApplicationScoped
@Transactional
class GameServiceImpl implements GameService {

    @PersistenceContext
    EntityManager entityManager;

    @Override
    public Optional<Game> findGameById(long gameId) {
        return Optional.ofNullable(entityManager.find(Game.class, gameId));
    }
}
