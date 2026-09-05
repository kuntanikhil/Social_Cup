package com.socialcup.user;

import com.socialcup.neighbourhood.Neighbourhood;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 255)
    private String email;

    @Column(name = "display_name", nullable = false, length = 100)
    private String displayName;

    @Column(name = "profile_photo_path", length = 500)
    private String profilePhotoPath;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "home_neighbourhood_id")
    private Neighbourhood homeNeighbourhood;

    @Column(name = "email_verified_at")
    private OffsetDateTime emailVerifiedAt;

    @Column(name = "onboarding_completed_at")
    private OffsetDateTime onboardingCompletedAt;

    @Column(name = "account_status", nullable = false, length = 30)
    private String accountStatus;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private UserRole role;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    protected User() {
    }

    private User(String email, String displayName) {
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        this.email = email;
        this.displayName = displayName;
        this.accountStatus = "ACTIVE";
        this.role = UserRole.MEMBER;
        this.createdAt = now;
        this.updatedAt = now;
    }

    public static User create(String email, String displayName) {
        return new User(email, displayName);
    }

    public void updateProfile(String displayName, Neighbourhood homeNeighbourhood) {
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        this.displayName = displayName;
        this.homeNeighbourhood = homeNeighbourhood;
        if (this.onboardingCompletedAt == null) {
            this.onboardingCompletedAt = now;
        }
        this.updatedAt = now;
    }

    public void promoteToAdmin() {
        this.role = UserRole.ADMIN;
        this.updatedAt = OffsetDateTime.now(ZoneOffset.UTC);
    }

    @PreUpdate
    void updateTimestamp() {
        updatedAt = OffsetDateTime.now(ZoneOffset.UTC);
    }

    public Long getId() {
        return id;
    }

    public String getEmail() {
        return email;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getProfilePhotoPath() {
        return profilePhotoPath;
    }

    public Neighbourhood getHomeNeighbourhood() {
        return homeNeighbourhood;
    }

    public OffsetDateTime getEmailVerifiedAt() {
        return emailVerifiedAt;
    }

    public OffsetDateTime getOnboardingCompletedAt() {
        return onboardingCompletedAt;
    }

    public String getAccountStatus() {
        return accountStatus;
    }

    public UserRole getRole() {
        return role;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }
}
