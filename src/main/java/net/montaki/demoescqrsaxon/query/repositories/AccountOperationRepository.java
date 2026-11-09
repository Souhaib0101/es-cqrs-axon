package net.montaki.demoescqrsaxon.query.repositories;

import java.util.List;
import net.montaki.demoescqrsaxon.query.entities.AccountOperationEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AccountOperationRepository extends JpaRepository<AccountOperationEntity, Long> {
    List<AccountOperationEntity> findByAccountIdOrderByOccurredAtDesc(String accountId);
}
