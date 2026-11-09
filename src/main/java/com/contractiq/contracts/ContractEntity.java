package com.contractiq.contracts;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "contracts")
public class ContractEntity {
    @Id @Column(length = 36) public String id;
    @Column(name = "organization_id", nullable = false, length = 36) public String organizationId;
    @Column(nullable = false, length = 240) public String title;
    @Column(length = 240) public String counterparty;
    @Column(name = "contract_type", length = 100) public String contractType;
    @Column(nullable = false, length = 32) public String status;
    @Column(name = "created_by", nullable = false, length = 36) public String createdBy;
    @CreationTimestamp @Column(name = "created_at", nullable = false, updatable = false) public LocalDateTime createdAt;
    @UpdateTimestamp @Column(name = "updated_at", nullable = false) public LocalDateTime updatedAt;
}
