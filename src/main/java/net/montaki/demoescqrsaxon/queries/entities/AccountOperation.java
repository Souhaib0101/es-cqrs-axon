package net.montaki.demoescqrsaxon.queries.entities;

import jakarta.persistence.*;
import lombok.*;
import net.montaki.demoescqrsaxon.enums.OperationType;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(indexes = @Index(name = "idx_operation_account_sequence", columnList = "bank_account_id,sequence_number"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AccountOperation {
    @Id
    private String id;
    @Column(nullable = false)
    private Instant operationDate;
    @Column(nullable = false)
    private long sequenceNumber;
    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;
    @Column(nullable = false, length = 3)
    private String currency;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OperationType type;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private BankAccount bankAccount;
}
