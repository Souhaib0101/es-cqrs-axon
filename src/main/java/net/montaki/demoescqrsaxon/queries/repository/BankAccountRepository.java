package net.montaki.demoescqrsaxon.queries.repository;

import net.montaki.demoescqrsaxon.queries.entities.BankAccount;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BankAccountRepository extends JpaRepository<BankAccount, String> {
}
