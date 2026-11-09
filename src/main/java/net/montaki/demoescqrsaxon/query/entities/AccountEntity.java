package net.montaki.demoescqrsaxon.query.entities;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import java.math.BigDecimal;
import net.montaki.demoescqrsaxon.common.AccountStatus;

@Entity(name = "bank_account")
public class AccountEntity {
    @Id private String id;
    private BigDecimal balance;
    private String currency;
    @Enumerated(EnumType.STRING) private AccountStatus status;

    protected AccountEntity() { }
    public AccountEntity(String id, BigDecimal balance, String currency, AccountStatus status) {
        this.id = id; this.balance = balance; this.currency = currency; this.status = status;
    }
    public String getId() { return id; }
    public BigDecimal getBalance() { return balance; }
    public String getCurrency() { return currency; }
    public AccountStatus getStatus() { return status; }
    public void setBalance(BigDecimal balance) { this.balance = balance; }
    public void setStatus(AccountStatus status) { this.status = status; }
}
