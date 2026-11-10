package net.montaki.demoescqrsaxon.queries.repository;

import net.montaki.demoescqrsaxon.queries.entities.AccountOperation;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface AccountOperationRepository extends JpaRepository<AccountOperation, String> {
    List<AccountOperation> findByBankAccountIdOrderBySequenceNumberAsc(String accountId);
}
