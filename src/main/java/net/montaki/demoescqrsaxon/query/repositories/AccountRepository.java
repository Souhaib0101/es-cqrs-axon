package net.montaki.demoescqrsaxon.query.repositories;

import net.montaki.demoescqrsaxon.query.entities.AccountEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AccountRepository extends JpaRepository<AccountEntity, String> { }
