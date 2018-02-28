package org.lordofthejars.games.details.impl;

import java.util.Optional;
import javax.enterprise.context.ApplicationScoped;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.transaction.Transactional;
import org.lordofthejars.games.details.api.Detail;
import org.lordofthejars.games.details.api.DetailService;

@ApplicationScoped
@Transactional
class DetailServiceImpl implements DetailService {

    @PersistenceContext
    EntityManager entityManager;

    @Override
    public Optional<Detail> findDetailByGameId(long gameId) {
        final Detail detail =
            entityManager.find(Detail.class, gameId);
        return Optional.ofNullable(detail);
    }
}
