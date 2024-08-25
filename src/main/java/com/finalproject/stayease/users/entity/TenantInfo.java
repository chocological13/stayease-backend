package com.finalproject.stayease.users.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.time.OffsetDateTime;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.ColumnDefault;

@Getter
@Setter
@Entity
@Table(name = "tenant_info")
public class TenantInfo {

  @Id
  @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "tenant_info_id_gen")
  @SequenceGenerator(name = "tenant_info_id_gen", sequenceName = "tenant_info_id_seq", allocationSize = 1)
  @Column(name = "id", nullable = false)
  private Long id;

  @OneToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "user_id")
  private User user;

  @Size(max = 255)
  @Column(name = "business_name")
  private String businessName;

  @Size(max = 100)
  @Column(name = "tax_id", length = 100)
  private String taxId;

  @Column(name = "registration_date")
  private Instant registrationDate;

  @ColumnDefault("CURRENT_TIMESTAMP")
  @Column(name = "created_at")
  private Instant createdAt;

  @ColumnDefault("CURRENT_TIMESTAMP")
  @Column(name = "updated_at")
  private Instant updatedAt;

  @Column(name = "deleted_at")
  private Instant deletedAt;

  @PrePersist
  protected void onCreate() {
    createdAt = Instant.now();
    updatedAt = Instant.now();
    registrationDate = Instant.now();
  }

  @PreUpdate
  protected void onUpdate() {
    updatedAt = Instant.now();
  }

}