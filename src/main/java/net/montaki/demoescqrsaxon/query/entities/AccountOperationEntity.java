package net.montaki.demoescqrsaxon.query.entities;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import java.math.BigDecimal;
import java.time.Instant;

@Entity(name = "account_operation")
public class AccountOperationEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    private String accountId;
    private String type;
    private BigDecimal amount;
    private String currency;
    private Instant occurredAt;

    protected AccountOperationEntity() { }
    public AccountOperationEntity(String accountId, String type, BigDecimal amount, String currency, Instant occurredAt) {
        this.accountId = accountId; this.type = type; this.amount = amount; this.currency = currency; this.occurredAt = occurredAt;
    }
    public Long getId() { return id; }
    public String getAccountId() { return accountId; }
    public String getType() { return type; }
    public BigDecimal getAmount() { return amount; }
    public String getCurrency() { return currency; }
    public Instant getOccurredAt() { return occurredAt; }
}
